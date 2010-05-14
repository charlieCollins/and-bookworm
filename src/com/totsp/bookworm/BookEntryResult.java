package com.totsp.bookworm;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.totsp.bookworm.data.GoogleBookDataSource;
import com.totsp.bookworm.model.Author;
import com.totsp.bookworm.model.Book;
import com.totsp.bookworm.util.CoverImageUtil;

import java.util.HashSet;

public class BookEntryResult extends Activity {

   public static final String FROM_RESULT = "FROM_RESULT";

   private static final int MENU_SCANNING_TIPS = 0;

   private BookWormApplication application;

   // package scope for use in inner class (Android optimization)
   Button bookAddButton;
   TextView bookTitle;
   ImageView bookCover;
   TextView bookAuthors;
   TextView warnDupe;

   Book book;

   boolean fromSearch;

   private GetBookDataTask getBookDataTask;

   @Override
   public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.bookentryresult);
      application = (BookWormApplication) getApplication();

      getBookDataTask = null;

      bookTitle = (TextView) findViewById(R.id.bookentrytitle);
      bookCover = (ImageView) findViewById(R.id.bookentrycover);
      bookAuthors = (TextView) findViewById(R.id.bookentryauthors);
      warnDupe = (TextView) findViewById(R.id.bookentrywarndupe);

      bookAddButton = (Button) findViewById(R.id.bookentryaddbutton);
      bookAddButton.setVisibility(View.INVISIBLE);
      bookAddButton.setOnClickListener(new OnClickListener() {
         public void onClick(final View v) {
            BookEntryResult.this.bookAddClick();
         }
      });

      // several other activites can populate this one
      // ISBN must be present as intent extra to proceed
      String isbn = getIntent().getStringExtra(Constants.ISBN);
      if ((isbn == null) || (isbn.length() < 10) || (isbn.length() > 13)) {
         Log.e(Constants.LOG_TAG, "Invalid product code/ISBN passed "
                  + "to BookEntryResult (may not be an ISBN?) - " + isbn);
         BookMessageBean bean = new BookMessageBean();
         bean.code = isbn;
         bean.message = getString(R.string.msgInvalidISBN);
         setViewsForInvalidEntry(bean);
      } else {
         getBookDataTask = new GetBookDataTask();
         getBookDataTask.execute(isbn);
      }

      fromSearch = getIntent().getBooleanExtra(BookSearch.FROM_SEARCH, false);
   }

   @Override
   public void onPause() {
      if ((getBookDataTask != null) && getBookDataTask.dialog.isShowing()) {
         getBookDataTask.dialog.dismiss();
      }
      super.onPause();
   }

   @Override
   public boolean onCreateOptionsMenu(final Menu menu) {
      menu.add(0, BookEntryResult.MENU_SCANNING_TIPS, 0,
               getResources().getString(R.string.menuScanTips)).setIcon(
               android.R.drawable.ic_menu_info_details);
      return super.onCreateOptionsMenu(menu);
   }

   @Override
   public boolean onOptionsItemSelected(final MenuItem item) {
      switch (item.getItemId()) {
         case MENU_SCANNING_TIPS:
            new AlertDialog.Builder(BookEntryResult.this).setTitle(
                     getResources().getString(R.string.menuScanTips))
                     .setMessage(
                              Html.fromHtml(this
                                       .getString(R.string.msgScanningtips)))
                     .setNeutralButton(
                              getResources().getString(R.string.dialogDismiss),
                              new DialogInterface.OnClickListener() {
                                 public void onClick(final DialogInterface d,
                                          final int i) {

                                 }
                              }).show();
            return true;
      }
      return super.onOptionsItemSelected(item);
   }

   private void bookAddClick() {
      if ((book != null) && (book.isbn10 != null)) {
         // TODO check for book exists using more than just ISBN or title (these are not unique - use a combination maybe?)
         // if book exists do not resave, or allow user to choose
         // save book to database
         long bookId = application.dataHelper.insertBook(book);
         if (book.coverImage != null) {
            BookEntryResult.this.application.dataImageHelper.storeBitmap(
                     book.coverImage, book.title, bookId);
         }
      }
      if (fromSearch) {
         // if from search results, return to search
         Intent intent = new Intent(BookEntryResult.this, BookSearch.class);
         intent.putExtra(BookEntryResult.FROM_RESULT, true);
         startActivity(intent);
      } else {
         startActivity(new Intent(BookEntryResult.this, Main.class));
      }
   }

   private void setViewsForInvalidEntry(final BookMessageBean bean) {
      bookCover.setImageResource(R.drawable.book_invalid_isbn);
      bookAuthors.setText(String.format(this.getString(R.string.msgScanError),
               bean.code));
   }

   //
   // AsyncTasks
   //
   private class GetBookDataTask extends
            AsyncTask<String, Void, BookMessageBean> {
      private final ProgressDialog dialog =
               new ProgressDialog(BookEntryResult.this);

      private String coverImageProviderKey;
      // TODO hard coded to GoogleBookDataSource for now
      private final GoogleBookDataSource gbs;

      public GetBookDataTask() {
         gbs =
                  (GoogleBookDataSource) BookEntryResult.this.application.bookDataSource;
         if (BookEntryResult.this.application.debugEnabled && (gbs != null)) {
            gbs.setDebugEnabled(true);
         }
      }

      @Override
      protected void onPreExecute() {
         dialog.setMessage(BookEntryResult.this
                  .getString(R.string.msgRetrievingBookData));
         dialog.show();
         SharedPreferences prefs =
                  PreferenceManager
                           .getDefaultSharedPreferences(BookEntryResult.this);
         // default to OpenLibrary(2) for cover image provider - for now (doesn't require login)
         coverImageProviderKey = prefs.getString("coverimagelistpref", "2");
      }

      @Override
      protected BookMessageBean doInBackground(final String... isbns) {
         BookMessageBean bean = new BookMessageBean();
         if (isbns[0] != null) {
            bean.code = isbns[0];
            if (gbs != null) {
               Book b = gbs.getBook(isbns[0]);
               bean.book = b;
               if (b == null) {
                  Log
                           .e(
                                    Constants.LOG_TAG,
                                    "GetBookDataTask book returned from data source null (using product code/ISBN - "
                                             + isbns[0] + ").");
                  bean.message =
                           String
                                    .format(BookEntryResult.this
                                             .getString(R.string.msgFindError),
                                             isbns[0]);
               } else if (b.isbn10 != null) {
                  Bitmap coverImageBitmap =
                           CoverImageUtil.retrieveCoverImage(
                                    coverImageProviderKey, b.isbn10);
                  b.coverImage = (coverImageBitmap);
               } else if (b.isbn13 != null) {
                  Bitmap coverImageBitmap =
                           CoverImageUtil.retrieveCoverImage(
                                    coverImageProviderKey, b.isbn13);
                  b.coverImage = (coverImageBitmap);
               }
            } else {
               Log
                        .e(Constants.LOG_TAG,
                                 "GetBookDataTask book data source null, cannot add book.");
               bean.message =
                        BookEntryResult.this
                                 .getString(R.string.msgDataSourceError);
            }
         } else {
            Log
                     .e(Constants.LOG_TAG,
                              "GetBookDataTask product code/ISBN null, cannot add book.");
            bean.message =
                     BookEntryResult.this.getString(R.string.msgISBNError);
         }
         return bean;
      }

      @Override
      protected void onPostExecute(final BookMessageBean bean) {
         if (dialog.isShowing()) {
            dialog.dismiss();
         }

         if (bean.book != null) {
            BookEntryResult.this.bookTitle.setText(bean.book.title);
            String authors = null;
            for (Author a : bean.book.authors) {
               if (authors == null) {
                  authors = a.name;
               } else {
                  authors += ", " + a.name;
               }
            }
            BookEntryResult.this.bookAuthors.setText(authors);

            if (bean.book.coverImage != null) {
               if (BookEntryResult.this.application.debugEnabled) {
                  Log.d(Constants.LOG_TAG,
                           "book cover bitmap present, set cover");
               }
               BookEntryResult.this.bookCover
                        .setImageBitmap(bean.book.coverImage);
            } else {
               if (BookEntryResult.this.application.debugEnabled) {
                  Log.d(Constants.LOG_TAG,
                           "book cover not found, generate image");
               }
               Bitmap generatedCover =
                        BookEntryResult.this.application.dataImageHelper
                                 .createCoverImage(bean.book.title);
               BookEntryResult.this.bookCover.setImageBitmap(generatedCover);
               bean.book.coverImage = generatedCover;
            }

            BookEntryResult.this.book = bean.book;
            BookEntryResult.this.bookAddButton.setVisibility(View.VISIBLE);

            // check for dupes and warn if title and either isbn match
            HashSet<Book> potentialDupes =
                     BookEntryResult.this.application.dataHelper
                              .selectAllBooksByTitle(bean.book.title);
            if (potentialDupes != null) {
               boolean dupe = false;
               for (Book b : potentialDupes) {
                  if (b.title.equals(bean.book.title)
                           && (b.isbn10.equals(bean.book.isbn10) || b.isbn13
                                    .equals(bean.book.isbn13))) {
                     dupe = true;
                     break;
                  }
               }
               if (dupe) {
                  BookEntryResult.this.warnDupe.setVisibility(View.VISIBLE);
               }
            }
         } else {
            BookEntryResult.this.setViewsForInvalidEntry(bean);
         }
      }
   }

   class BookMessageBean {
      String code;
      Book book;
      String message;
   }
}
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

public class BookEntryResult extends Activity {   
   
   public static final String FROM_RESULT = "FROM_RESULT";

   private static final int MENU_SCANNING_TIPS = 0;
   
   private BookWormApplication application;

   // package scope for use in inner class (Android optimization)
   Button bookAddButton;
   TextView bookTitle;
   ImageView bookCover;
   TextView bookAuthors;

   Book book;

   boolean fromSearch;

   private GetBookDataTask getBookDataTask;

   @Override
   public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      this.setContentView(R.layout.bookentryresult);
      this.application = (BookWormApplication) this.getApplication();

      this.getBookDataTask = null;

      this.bookTitle = (TextView) this.findViewById(R.id.bookentrytitle);
      this.bookCover = (ImageView) this.findViewById(R.id.bookentrycover);
      this.bookAuthors = (TextView) this.findViewById(R.id.bookentryauthors);

      this.bookAddButton = (Button) this.findViewById(R.id.bookentryaddbutton);
      this.bookAddButton.setVisibility(View.INVISIBLE);
      this.bookAddButton.setOnClickListener(new OnClickListener() {
         public void onClick(final View v) {
            BookEntryResult.this.bookAddClick();
         }
      });

      // several other activites can populate this one
      // ISBN must be present as intent extra to proceed
      String isbn = this.getIntent().getStringExtra(Constants.ISBN);
      if ((isbn == null) || (isbn.length() < 10) || (isbn.length() > 13)) {
         Log
                  .e(Constants.LOG_TAG, "Invalid product code/ISBN passed to BookEntryResult (may not be an ISBN?) - "
                           + isbn);
         BookMessageBean bean = new BookMessageBean();
         bean.code = isbn;
         bean.message = "ISBN format invalid (not ISBN 10 or ISBN 13), cannot retrieve result.";
         this.setViewsForInvalidEntry(bean);
      } else {
         this.getBookDataTask = new GetBookDataTask();
         this.getBookDataTask.execute(isbn);
      }

      this.fromSearch = this.getIntent().getBooleanExtra(BookEntrySearch.FROM_SEARCH, false);
   }

   @Override
   public void onPause() {
      if ((this.getBookDataTask != null) && this.getBookDataTask.dialog.isShowing()) {
         this.getBookDataTask.dialog.dismiss();
      }
      super.onPause();
   }
   
   @Override
   public boolean onCreateOptionsMenu(final Menu menu) {
      menu.add(0, MENU_SCANNING_TIPS, 0, "Scanning Tips").setIcon(android.R.drawable.ic_menu_info_details);
      return super.onCreateOptionsMenu(menu);
   }

   @Override
   public boolean onOptionsItemSelected(final MenuItem item) {
      switch (item.getItemId()) {
      case MENU_SCANNING_TIPS:
         new AlertDialog.Builder(BookEntryResult.this).setTitle("Scanning Tips").setMessage(
                  Html.fromHtml(this.getResources().getString(R.string.scanningtips))).setNeutralButton("Dismiss",
                  new DialogInterface.OnClickListener() {
                     public void onClick(final DialogInterface d, final int i) {

                     }
                  }).show();
         return true;
      }
      return super.onOptionsItemSelected(item);
   }

   private void bookAddClick() {
      if ((this.book != null) && (this.book.isbn10 != null)) {
         // TODO check for book exists using more than just ISBN or title (these are not unique - use a combination maybe?)
         // if book exists do not resave, or allow user to choose
         // save book to database
         long bookId = this.application.getDataHelper().insertBook(this.book);
         if (this.book.coverImage != null) {
            BookEntryResult.this.application.getDataImageHelper().storeBitmap(this.book.coverImage, this.book.title,
                     bookId);
         }
      }
      if (this.fromSearch) {
         // if from search results, return to search
         Intent intent = new Intent(BookEntryResult.this, BookEntrySearch.class);
         intent.putExtra(BookEntryResult.FROM_RESULT, true);
         this.startActivity(intent);
      } else {
         this.startActivity(new Intent(BookEntryResult.this, Main.class));
      }
   }

   private void setViewsForInvalidEntry(final BookMessageBean bean) {
      this.bookCover.setImageResource(R.drawable.book_invalid_isbn);

      StringBuilder sb = new StringBuilder();
      sb.append("Whoops, that entry didn't work.");
      sb.append("The product code/ISBN used (from scanner or search) was " + bean.code + ".");     
      sb.append("Please try again, or use a different entry method.");
      sb.append("(If you are having trouble scanning a book, try the Menu->Scanning Tips.)");
      this.bookAuthors.setText(sb.toString());
   }

   //
   // AsyncTasks
   //
   private class GetBookDataTask extends AsyncTask<String, Void, BookMessageBean> {
      private final ProgressDialog dialog = new ProgressDialog(BookEntryResult.this);

      private String coverImageProviderKey;
      // TODO hard coded to GoogleBookDataSource for now
      private final GoogleBookDataSource gbs;

      public GetBookDataTask() {
         this.gbs = (GoogleBookDataSource) BookEntryResult.this.application.getBookDataSource();
         if (BookEntryResult.this.application.isDebugEnabled() && this.gbs != null) {
            gbs.setDebugEnabled(true);
         }
      }

      protected void onPreExecute() {
         this.dialog.setMessage("Retrieving book data..");
         this.dialog.show();
         SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(BookEntryResult.this);
         // default to OpenLibrary(2) for cover image provider - for now (doesn't require login)
         this.coverImageProviderKey = prefs.getString("coverimagelistpref", "2");
      }

      protected BookMessageBean doInBackground(final String... isbns) {
         BookMessageBean bean = new BookMessageBean();
         if (isbns[0] != null) {
            bean.code = isbns[0];
            if (this.gbs != null) {               
               Book b = this.gbs.getBook(isbns[0]);
               bean.book = b;
               if (b == null) {
                  Log.e(Constants.LOG_TAG,
                           "GetBookDataTask book returned from data source null (using product code/ISBN - " + isbns[0]
                                    + ").");
                  bean.message = "Book not found using ISBN/product code " + isbns[0] + " (may not be an ISBN?).";                  
               } else if (b.isbn10 != null) {
                  Bitmap coverImageBitmap = CoverImageUtil.retrieveCoverImage(this.coverImageProviderKey, b.isbn10);
                  b.coverImage = (coverImageBitmap);
               } else if (b.isbn13 != null) {
                  Bitmap coverImageBitmap = CoverImageUtil.retrieveCoverImage(this.coverImageProviderKey, b.isbn13);
                  b.coverImage = (coverImageBitmap);
               }               
            } else {
               Log.e(Constants.LOG_TAG, "GetBookDataTask book data source null, cannot add book.");
               bean.message = "Book data source not found, internal error, or networking issue..";
            }
         } else {
            Log.e(Constants.LOG_TAG, "GetBookDataTask product code/ISBN null, cannot add book.");
            bean.message = "Book ISBN/product code not present.";
         }
         return bean;
      }

      protected void onPostExecute(final BookMessageBean bean) {
         if (this.dialog.isShowing()) {
            this.dialog.dismiss();
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
               if (BookEntryResult.this.application.isDebugEnabled()) {
                  Log.d(Constants.LOG_TAG, "book cover bitmap present, set cover");
               }
               BookEntryResult.this.bookCover.setImageBitmap(bean.book.coverImage);
            } else {
               if (BookEntryResult.this.application.isDebugEnabled()) {
                  Log.d(Constants.LOG_TAG, "book cover not found, generate image");
               }
               Bitmap generatedCover =
                        BookEntryResult.this.application.getDataImageHelper().createCoverImage(bean.book.title);
               BookEntryResult.this.bookCover.setImageBitmap(generatedCover);
               bean.book.coverImage = generatedCover;
            }

            BookEntryResult.this.book = bean.book;
            BookEntryResult.this.bookAddButton.setVisibility(View.VISIBLE);
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
package com.totsp.bookworm;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.totsp.bookworm.model.Book;
import com.totsp.bookworm.util.BookUtil;
import com.totsp.bookworm.util.NetworkUtil;
import com.totsp.bookworm.util.StringUtil;

import java.util.ArrayList;

// TODO this class needs work, convoluted logic at this point after incremental changes
// a lot of side effect crap, need to split out AsyncTasks and make simplify this
public class BookEntryResult extends Activity {

   public static final String FROM_RESULT = "FROM_RESULT";

   private static final int MENU_SCANNING_TIPS = 0;

   BookWormApplication application;

   // package scope for use in inner class (Android optimization)
   Button bookAddButton;
   TextView bookTitle;
   ImageView bookCover;
   TextView bookAuthors;
   TextView warnDupe;

   //Book book;

   boolean fromSearch;

   private ProgressDialog progressDialog;

   ConnectivityManager cMgr;

   @Override
   public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.bookentryresult);
      application = (BookWormApplication) getApplication();

      cMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

      progressDialog = new ProgressDialog(this);
      progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
      progressDialog.setCancelable(false);
      progressDialog.setMessage(getString(R.string.msgRetrievingBookData));
      //progressDialog.setMax(1);

      bookTitle = (TextView) findViewById(R.id.bookentrytitle);
      bookCover = (ImageView) findViewById(R.id.bookentrycover);
      bookAuthors = (TextView) findViewById(R.id.bookentryauthors);
      warnDupe = (TextView) findViewById(R.id.bookentrywarndupe);

      bookAddButton = (Button) findViewById(R.id.bookentryaddbutton);
      bookAddButton.setVisibility(View.INVISIBLE);
      bookAddButton.setOnClickListener(new OnClickListener() {
         public void onClick(final View v) {
            bookAddClick(application.selectedBook);
         }
      });

      fromSearch = getIntent().getBooleanExtra(BookSearch.FROM_SEARCH, false);

      // several other activities can populate this one
      // *EITHER* use application.selectedBook (if from SEARCH)
      // or ISBN must be present as intent extra to proceed (in which case request will be made to get data)
      if (fromSearch && (application.selectedBook != null)) {
         new SetupBookResultTask(application.selectedBook).execute(null);
      } else {
         String isbn = getIntent().getStringExtra(Constants.ISBN);
         if ((isbn == null) || (isbn.length() < 10) || (isbn.length() > 13)) {
            Log.e(Constants.LOG_TAG, "Invalid product code/ISBN passed "
                     + "to BookEntryResult (may not be an ISBN?) - " + isbn);
            BookMessageBean bean = new BookMessageBean();
            bean.code = isbn;
            setViewsForInvalidEntry(bean);
         } else {
            new SetupBookResultTask().execute(isbn);
         }
      }
   }

   @Override
   public void onPause() {      
      if (progressDialog.isShowing()) {
         progressDialog.dismiss();
      }
      super.onPause();
   }

   @Override
   public void onSaveInstanceState(Bundle outState) {
      super.onSaveInstanceState(outState);
   }

   @Override
   public void onRestoreInstanceState(Bundle inState) {
      super.onRestoreInstanceState(inState);
   }

   @Override
   public boolean onCreateOptionsMenu(final Menu menu) {
      menu.add(0, BookEntryResult.MENU_SCANNING_TIPS, 0, getString(R.string.menuScanTips)).setIcon(
               android.R.drawable.ic_menu_info_details);
      return super.onCreateOptionsMenu(menu);
   }

   @Override
   public boolean onOptionsItemSelected(final MenuItem item) {
      switch (item.getItemId()) {
         case MENU_SCANNING_TIPS:
            new AlertDialog.Builder(BookEntryResult.this).setTitle(getString(R.string.menuScanTips)).setMessage(
                     Html.fromHtml(getString(R.string.msgScanningtips))).setNeutralButton(
                     getString(R.string.btnDismiss), new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface d, final int i) {
                        }
                     }).show();
            return true;
      }
      return super.onOptionsItemSelected(item);
   }

   private void bookAddClick(final Book book) {
      if (book != null) {
         // TODO check for book exists using more than just ISBN or title 
         // (these are not unique - use a combination maybe?)
         // if book exists do not resave, or allow user to choose?
         long bookId = application.dataManager.insertBook(book);
         if (book.coverImage != null) {
            application.imageManager.storeBitmap(book.coverImage, book.title, bookId);
         }
      } else {
         Log.e(Constants.LOG_TAG, "BookEntryResult bookAddClick invoked on null book.");
      }
      
      // where to next (back to Search, or to Main)
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
      bookAuthors.setText(String.format(getString(R.string.msgScanError), bean.code));
   }

   //
   // AsyncTasks
   //
   // TODO cleanup SetupBookTask so that it doesn't work two ways (ISBN or Book), if necc make two tasks
   private class SetupBookResultTask extends AsyncTask<String, Void, BookMessageBean> {
      private Book book;
      
      public SetupBookResultTask() {         
      }

      public SetupBookResultTask(final Book b) {
         this();
         book = b;
      }

      @Override
      protected void onPreExecute() {
         if (progressDialog.isShowing()) {
            progressDialog.dismiss();
         }         
      }

      @Override
      protected BookMessageBean doInBackground(final String... isbns) {
         publishProgress(new Void[] { });
         BookMessageBean bean = new BookMessageBean();
         // if we have the book (it was passed in to ctor), use it
         if (book != null) {
            bean.book = book;
            
         }
         // else, use the isbn to retrieve the data and create the book
         else {
            if (isbns[0] != null) {
               bean.code = isbns[0];
               if (application.bookDataSource != null) {
                  book = application.bookDataSource.getBook(isbns[0]);
                  bean.book = book;
                  application.selectedBook = book;
                  if (bean.book == null) {
                     Log.e(Constants.LOG_TAG,
                              "GetBookDataTask book returned from data source null (using product code/ISBN - "
                                       + isbns[0] + ").");
                  }
               } else {
                  Log.e(Constants.LOG_TAG, "GetBookDataTask book data source null, cannot add book.");
               }
            } else {
               Log.e(Constants.LOG_TAG, "GetBookDataTask product code/ISBN null, cannot add book.");
            }
         }

         // handle cover image 
         if ((bean.book != null) && (bean.book.coverImage == null)) {
            if (NetworkUtil.connectionPresent(cMgr)) {
               bean.book.coverImage = application.imageManager.getOrCreateCoverImage(bean.book);
            } else {
               bean.book.coverImage = application.imageManager.createCoverImage(bean.book.title);
               Log.i(Constants.LOG_TAG, "Cover retrieval for book " + bean.book.title
                        + " skipped because network was not available.");
            }
         }
         return bean;
      }
      
      @Override
      protected void onProgressUpdate(Void... progress) {
         if (!progressDialog.isShowing()) {
            progressDialog.show();
         }
      }

      @Override
      protected void onPostExecute(final BookMessageBean bean) {
         if (progressDialog.isShowing()) {
            progressDialog.dismiss();
         }

         if (bean.book != null) {
            bookTitle.setText(bean.book.title);
            bookAuthors.setText(StringUtil.contractAuthors(bean.book.authors));

            if (bean.book.coverImage != null) {
               if (application.debugEnabled) {
                  Log.d(Constants.LOG_TAG, "book cover bitmap present, set cover");
               }
               bookCover.setImageBitmap(bean.book.coverImage);
            } else {
               if (application.debugEnabled) {
                  Log.d(Constants.LOG_TAG, "book cover not found, generate image");
               }
               Bitmap generatedCover = application.imageManager.createCoverImage(bean.book.title);
               bookCover.setImageBitmap(generatedCover);
               bean.book.coverImage = generatedCover;
            }

            book = bean.book;
            bookAddButton.setVisibility(View.VISIBLE);

            // check for dupes and warn if title and either isbn match
            ArrayList<Book> potentialDupes = application.dataManager.selectAllBooksByTitle(bean.book.title);
            if (potentialDupes != null) {
               boolean dupe = false;
               // TODO move this to datahelper, do it with query
               for (int i = 0; i < potentialDupes.size(); i++) {
                  Book b = potentialDupes.get(i);
                  if (BookUtil.areBooksEffectiveDupes(bean.book, b)) {
                     dupe = true;
                     break;
                  }
               }
               if (dupe) {
                  warnDupe.setVisibility(View.VISIBLE);
               }
            }
         } else {
            setViewsForInvalidEntry(bean);
         }
      }
   }

   class BookMessageBean {
      String code;
      Book book;
      //String message;
   }
}
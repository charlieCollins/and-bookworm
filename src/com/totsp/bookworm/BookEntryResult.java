package com.totsp.bookworm;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.totsp.bookworm.model.Author;
import com.totsp.bookworm.model.Book;
import com.totsp.bookworm.util.CoverImageUtil;

public class BookEntryResult extends Activity {

   private BookWormApplication application;

   // package scope for use in inner class (Android optimization)
   Button bookAddButton;
   TextView bookTitle;
   ImageView bookCover;
   TextView bookAuthors;

   Book book;

   boolean fromSearchResults;

   @Override
   public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      this.application = (BookWormApplication) this.getApplication();

      this.setContentView(R.layout.bookentryresult);

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
      Log.i(Constants.LOG_TAG, "ISBN on entry result - " + isbn);
      if ((isbn == null) || (isbn.length() < 10) || (isbn.length() > 13)) {
         this.setViewsForInvalidEntry();
      } else {
         new GetBookDataTask().execute(isbn);
      }

      if (savedInstanceState != null && savedInstanceState.getBoolean("FROM_SEARCH_RESULTS", false)) {
         this.fromSearchResults = true;
      }
   }

   /*
   // go back to search results if coming from there
   @Override
   public boolean onKeyDown(int keyCode, KeyEvent event) {
      if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
         if (this.fromSearchResults) {
            this.startActivity(new Intent(BookEntryResult.this, BookEntrySearch.class));
            this.fromSearchResults = false;
         }
         return true;
      }
      return super.onKeyDown(keyCode, event);
   }
   */

   private void bookAddClick() {
      if ((this.book != null) && (this.book.getIsbn10() != null)) {
         // TODO check for book exists using more than just ISBN or title (these are not unique - use a combination maybe?)
         // save book to database
         long bookId = this.application.getDataHelper().insertBook(this.book);
         if (this.book.getCoverImage() != null) {
            BookEntryResult.this.application.getDataImageHelper().storeBitmap(this.book.getCoverImage(),
                     this.book.getTitle(), bookId);
         }
      }
      this.startActivity(new Intent(BookEntryResult.this, Main.class));
   }

   private void setViewsForInvalidEntry() {
      this.bookCover.setImageResource(R.drawable.book_invalid_isbn);
      this.bookAuthors
               .setText("Whoops, that entry didn't work. Please try again (and if one method fails, such as scanning, try a search or direct entry).");
   }

   private class GetBookDataTask extends AsyncTask<String, Void, Book> {
      private final ProgressDialog dialog = new ProgressDialog(BookEntryResult.this);

      // TODO ctor to pass provider keys
      private String coverImageProviderKey;

      protected void onPreExecute() {
         this.dialog.setMessage("Retrieving book data..");
         this.dialog.show();
         SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(BookEntryResult.this);
         // default to OpenLibrary(2) for cover image provider - for now (doesn't require login)
         this.coverImageProviderKey = prefs.getString("coverimagelistpref", "2");
      }

      protected Book doInBackground(final String... isbns) {
         Book b = BookEntryResult.this.application.getBookDataSource().getBook(isbns[0]);
         Bitmap coverImageBitmap = CoverImageUtil.retrieveCoverImage(this.coverImageProviderKey, b.getIsbn10());         
         b.setCoverImage(coverImageBitmap);
         return b;
      }

      protected void onPostExecute(final Book b) {
         if (this.dialog.isShowing()) {
            this.dialog.dismiss();
         }

         if (b != null) {
            BookEntryResult.this.bookTitle.setText(b.getTitle());
            String authors = null;
            for (Author a : b.getAuthors()) {
               if (authors == null) {
                  authors = a.getName();
               } else {
                  authors += ", " + a.getName();
               }
            }
            BookEntryResult.this.bookAuthors.setText(authors);

            if (b.getCoverImage() != null) {
               if (Constants.LOCAL_LOGV) {
                  Log.v(Constants.LOG_TAG, "book cover bitmap present, set cover");
               }
               BookEntryResult.this.bookCover.setImageBitmap(b.getCoverImage());
            } else {
               if (Constants.LOCAL_LOGV) {
                  Log.v(Constants.LOG_TAG, "book cover not found");
               }
               BookEntryResult.this.bookCover.setImageResource(R.drawable.book_cover_missing);                              
            }

            BookEntryResult.this.book = b;
            BookEntryResult.this.bookAddButton.setVisibility(View.VISIBLE);
         } else {
            BookEntryResult.this.setViewsForInvalidEntry();
         }
      }
   }
}
package com.totsp.bookworm;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import com.totsp.bookworm.util.OpenLibraryUtil;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

public class BookEntryResult extends Activity {

   private BookWormApplication application;

   // package scope for use in inner class (Android optimization)    
   Button bookAddButton;
   TextView bookTitle;
   ImageView bookCover;
   TextView bookAuthors;

   Bitmap bookCoverBitmap;
   Book book;

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
      // ISBN may be present as intent extra OR entire Book may already be set?

      String isbn = this.getIntent().getStringExtra(Constants.ISBN);
      Log.i(Constants.LOG_TAG, "ISBN after scan - " + isbn);
      if ((isbn == null) || (isbn.length() < 10) || (isbn.length() > 13)) {
         this.setViewsForInvalidEntry();
      } else {
         new GetBookDataTask().execute(isbn);
      }
   }
   
   private void bookAddClick() {
      // TODO add fallback to book isbn13 support
      if ((this.book != null) && (this.book.getIsbn10() != null)) {
         // TODO don't even let users get here if book exists, remove add button prev              
         Book retrieve = this.application.getDataHelper().selectBook(this.book.getIsbn10());
         if (retrieve == null) {
            // save image to ContentProvider
            if (this.bookCoverBitmap != null) {
               int imageId =
                        this.application.getDataImageHelper().saveBitmap(this.book.getTitle(), this.bookCoverBitmap);
               this.book.setCoverImageId(imageId);

               // also save one really small for use in ListView - rather than scaling later
               imageId =
                        this.application.getDataImageHelper().saveBitmap(this.book.getTitle() + "-T",
                                 Bitmap.createScaledBitmap(this.bookCoverBitmap, 55, 70, false));
               this.book.setCoverImageTinyId(imageId);
            }
            // save book to database
            this.application.getDataHelper().insertBook(this.book);
         } 
      }
      this.startActivity(new Intent(BookEntryResult.this, Main.class));
   }

   private void setViewsForInvalidEntry() {
      this.bookCover.setImageResource(R.drawable.book_invalid_isbn);
      this.bookAuthors
               .setText("Whoops, that entry didn't work. Please try again (and if one method fails, such as scanning, try a search or direct entry).");
   }

   @Override
   public void onStart() {
      super.onStart();
   }

   @Override
   public void onPause() {
      super.onPause();
   }

   @Override
   protected void onStop() {
      super.onStop();
   }

   private class GetBookDataTask extends AsyncTask<String, Void, Void> {
      private final ProgressDialog dialog = new ProgressDialog(BookEntryResult.this);

      private Book bookTask;
      private Bitmap bookCoverBitmapTask;
      
      private String coverImageProviderKey;

      // can use UI thread here
      protected void onPreExecute() {
         this.dialog.setMessage("Retrieving book data..");
         this.dialog.show();
         SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(BookEntryResult.this);
         // default to OpenLibrary(2) for cover image provider - for now (doesn't require login)
         this.coverImageProviderKey = prefs.getString("coverimagelistpref", "2");
      }

      // automatically done on worker thread (separate from UI thread)
      protected Void doInBackground(final String... isbns) {
         // book data itself
         this.bookTask = BookEntryResult.this.application.getBookDataSource().getBook(isbns[0]);

         // TODO better book cover get stuff (HttpHelper binary)
         // book cover image
         String imageUrl = null;
         if (this.coverImageProviderKey.equals("1")) {
            // 1 = Google Books (TODO 1 should equal "default for handler" or such - regardless of current handler)
            // TODO if user selects Google Books, get them to login and store token
            imageUrl = this.bookTask.getCoverImageURL();
         } else if (this.coverImageProviderKey.equals("2")) {
            // 2 = OpenLibrary
            imageUrl = OpenLibraryUtil.getCoverUrlMedium(isbns[0]);
         }            
         
         if (Constants.LOCAL_LOGD) {
            Log.d(Constants.LOG_TAG, "book cover imageUrl - " + imageUrl);
         }
         if ((imageUrl != null) && !imageUrl.equals("")) {
            try {
               URL url = new URL(imageUrl);
               URLConnection conn = url.openConnection();
               conn.setConnectTimeout(10000);
               conn.connect();
               BufferedInputStream bis = new BufferedInputStream(conn.getInputStream(), 8192);
               this.bookCoverBitmapTask = BitmapFactory.decodeStream(bis);
               if (this.bookCoverBitmapTask.getWidth() < 10) {
                  this.bookCoverBitmapTask = null;
               }
            } catch (IOException e) {
               Log.e(Constants.LOG_TAG, " ", e);
            }
         }
         return null;
      }

      // can use UI thread here
      protected void onPostExecute(final Void unused) {
         if (this.dialog.isShowing()) {
            this.dialog.dismiss();
         }

         if (this.bookTask != null) {
            BookEntryResult.this.bookTitle.setText(this.bookTask.getTitle());
            String authors = null;
            for (Author a : this.bookTask.getAuthors()) {
               if (authors == null) {
                  authors = a.getName();
               } else {
                  authors += ", " + a.getName();
               }
            }
            BookEntryResult.this.bookAuthors.setText(authors);

            if (this.bookCoverBitmapTask != null) {
               if (Constants.LOCAL_LOGV) {
                  Log.v(Constants.LOG_TAG, "book cover bitmap present, set cover");
               }
               BookEntryResult.this.bookCover.setImageBitmap(this.bookCoverBitmapTask);
               BookEntryResult.this.bookCoverBitmap = bookCoverBitmapTask;
            } else {
               if (Constants.LOCAL_LOGV) {
                  Log.v(Constants.LOG_TAG, "book cover not found");
               }
               BookEntryResult.this.bookCover.setImageResource(R.drawable.book_cover_missing);
            }

            BookEntryResult.this.book = this.bookTask;
            BookEntryResult.this.bookAddButton.setVisibility(View.VISIBLE);
         } else {
            BookEntryResult.this.setViewsForInvalidEntry();
         }
      }
   }
}
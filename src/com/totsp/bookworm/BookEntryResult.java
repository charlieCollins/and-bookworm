package com.totsp.bookworm;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.totsp.bookworm.data.GoogleBookDataSource;
import com.totsp.bookworm.data.IBookDataSource;
import com.totsp.bookworm.model.Author;
import com.totsp.bookworm.model.Book;
import com.totsp.bookworm.util.DateUtil;
import com.totsp.bookworm.util.OpenLibraryUtil;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

public class BookEntryResult extends Activity {

   private BookWormApplication application;   

   // package scope for use in inner class (Android optimization)    
   Button bookAddButton;
   TextView bookTitle;
   ImageView bookCover;
   TextView bookAuthors;
   TextView bookDate;
   
   Bitmap bookCoverBitmap;
   Book book;   

   // TODO allow different sources (put sources in file, then Class.forName based on user choice?)
   IBookDataSource bookDataSource;

   @Override
   public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      this.application = (BookWormApplication) this.getApplication();
      
      this.bookDataSource = new GoogleBookDataSource();

      this.setContentView(R.layout.bookentryresult);

      this.bookTitle = (TextView) this.findViewById(R.id.bookentrytitle);
      this.bookCover = (ImageView) this.findViewById(R.id.bookentrycover);
      this.bookAuthors = (TextView) this.findViewById(R.id.bookentryauthors);
      this.bookDate = (TextView) this.findViewById(R.id.bookentrydate);

      this.bookAddButton = (Button) this.findViewById(R.id.bookentryaddbutton);
      this.bookAddButton.setOnClickListener(new OnClickListener() {
         public void onClick(final View v) {
            BookEntryResult.this.bookAddClick();
         }
      });

      String isbn = this.getIntent().getStringExtra(Constants.ISBN);
      Log.d(Splash.APP_NAME, "ISBN after scan - " + isbn);
      if ((isbn == null) || (isbn.length() < 10) || (isbn.length() > 13)) {
         this.setViewsForInvalidEntry();
      } else {
         new GetBookDataTask().execute(isbn);
      }
   }

   private void bookAddClick() {
      // TODO make this another AsyncTask (db operations, etc)
      if ((this.book != null) && (this.book.getIsbn() != null)) {
         Log.d(Splash.APP_NAME, "Book object created, and ADD pressed");
         // TODO don't even let users get here if book exists, remove add button prev              
         Book retrieve = this.application.getDataHelper().selectBook(this.book.getIsbn());
         if (retrieve == null) {
            Log.d(Splash.APP_NAME, "Book does not already exist in DB, attempt to insert");
            // save image to ContentProvider
            if (this.bookCoverBitmap != null) {
               int imageId = this.application.getDataImageHelper().saveImage(this.book.getTitle(), this.bookCoverBitmap);
               this.book.setCoverImageId(imageId);
            }
            // save book to database
            this.application.getDataHelper().insertBook(this.book);
         } else {
            Log.d(Splash.APP_NAME, "Book already exists in DB, ignore");
         }
      }
      this.startActivity(new Intent(BookEntryResult.this, Main.class));
   }

   private void setViewsForInvalidEntry() {
      this.bookCover.setImageResource(R.drawable.book_invalid_isbn);
      this.bookAuthors.setText("Whoops, that entry (or scan) worked but the number doesn't seem to be an ISBN,"
               + " make sure the item is a book and try again.");
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

      // can use UI thread here
      protected void onPreExecute() {
         this.dialog.setMessage("Retrieving book data..");
         this.dialog.show();
      }

      // automatically done on worker thread (separate from UI thread)
      protected Void doInBackground(final String... isbns) {
         // book data itself
         this.bookTask = BookEntryResult.this.bookDataSource.getBook(isbns[0]);

         // TODO better book cover get stuff (HttpHelper binary)
         // book cover image
         String imageUrl = OpenLibraryUtil.getCoverUrlMedium(isbns[0]);
         Log.d(Splash.APP_NAME, "book cover imageUrl - " + imageUrl);
         if ((imageUrl != null) && !imageUrl.equals("")) {
            try {
               URL url = new URL(imageUrl);
               URLConnection conn = url.openConnection();
               conn.connect();
               BufferedInputStream bis = new BufferedInputStream(conn.getInputStream(), 8192);
               this.bookCoverBitmapTask = BitmapFactory.decodeStream(bis);
               if (this.bookCoverBitmapTask.getWidth() < 10) {
                  this.bookCoverBitmapTask = null;
               }
            } catch (IOException e) {
               Log.e(Splash.APP_NAME, " ", e);
            }
         }
         return null;
      }

      // can use UI thread here
      protected void onPostExecute(final Void unused) {
         this.dialog.dismiss();

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
            BookEntryResult.this.bookDate.setText(DateUtil.format(new Date(this.bookTask.getDatePubStamp())));

            if (this.bookCoverBitmapTask != null) {
               Log.d(Splash.APP_NAME, "book cover bitmap present, set cover");
               BookEntryResult.this.bookCover.setImageBitmap(this.bookCoverBitmapTask);
               BookEntryResult.this.bookCoverBitmap = bookCoverBitmapTask;
            } else {
               Log.d(Splash.APP_NAME, "book cover not found");
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
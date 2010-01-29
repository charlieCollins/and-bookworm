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
import com.totsp.bookworm.model.Author;
import com.totsp.bookworm.model.Book;
import com.totsp.bookworm.util.DateUtil;
import com.totsp.bookworm.util.OpenLibraryUtil;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

public class BookScanResult extends Activity {

   private BookWormApplication application;
   
   // TODO allow different sources
   private GoogleBookDataSource bookDataSource;

   private Button scanBookAddButton;
   private TextView scanBookTitle;
   private ImageView scanBookCover;
   private TextView scanBookAuthor;
   private TextView scanBookDate;

   private Bitmap bookCoverBitmap;
   private Book book;

   @Override
   public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      this.application = (BookWormApplication) this.getApplication();
      
      this.bookDataSource = new GoogleBookDataSource();

      this.setContentView(R.layout.bookscanresult);

      this.scanBookTitle = (TextView) this.findViewById(R.id.scanBookTitle);
      this.scanBookCover = (ImageView) this.findViewById(R.id.scanBookCover);
      this.scanBookAuthor = (TextView) this.findViewById(R.id.scanBookAuthor);
      this.scanBookDate = (TextView) this.findViewById(R.id.scanBookDate);

      this.scanBookAddButton = (Button) this.findViewById(R.id.scanbookaddbutton);
      this.scanBookAddButton.setOnClickListener(new OnClickListener() {
         public void onClick(final View v) {
            BookScanResult.this.scanBookAddClick();
         }
      });

      String isbn = this.getIntent().getStringExtra("SCAN_RESULT_CONTENTS");
      Log.d(Splash.APP_NAME, "ISBN after scan - " + isbn);
      if ((isbn == null) || (isbn.length() < 10) || (isbn.length() > 13)) {
         this.setViewsForInvalidScan();
      } else {
         new GetBookDataTask().execute(isbn);
      }
   }

   private void scanBookAddClick() {
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
      this.startActivity(new Intent(BookScanResult.this, Main.class));
   }

   private void setViewsForInvalidScan() {
      this.scanBookCover.setImageResource(R.drawable.book_invalid_isbn);
      this.scanBookTitle.setText("Whoops, that scan worked but the number doesn't seem to be an ISBN,"
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
      private final ProgressDialog dialog = new ProgressDialog(BookScanResult.this);

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
         this.bookTask = BookScanResult.this.bookDataSource.getBook(isbns[0]);

         // TODO better book cover get stuff (HttpHelper binary)
         // book cover image
         String imageUrl = OpenLibraryUtil.getCoverUrlSmall(isbns[0]);
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
            BookScanResult.this.scanBookTitle.setText(this.bookTask.getTitle());
            String authors = null;
            for (Author a : this.bookTask.getAuthors()) {
               if (authors == null) {
                  authors = a.getName();
               } else {
                  authors += ", " + a.getName();
               }
            }
            BookScanResult.this.scanBookAuthor.setText(authors);
            BookScanResult.this.scanBookDate.setText(DateUtil.format(new Date(this.bookTask.getDatePubStamp())));

            if (this.bookCoverBitmapTask != null) {
               Log.d(Splash.APP_NAME, "book cover bitmap present, set cover");
               BookScanResult.this.scanBookCover.setImageBitmap(this.bookCoverBitmapTask);
               BookScanResult.this.bookCoverBitmap = bookCoverBitmapTask;
            } else {
               Log.d(Splash.APP_NAME, "book cover not found");
               BookScanResult.this.scanBookCover.setImageResource(R.drawable.book_cover_missing);
            }

            BookScanResult.this.book = this.bookTask;
            BookScanResult.this.scanBookAddButton.setVisibility(View.VISIBLE);
         } else {
            BookScanResult.this.setViewsForInvalidScan();
         }
      }
   }
}
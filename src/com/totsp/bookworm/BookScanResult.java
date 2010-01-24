package com.totsp.bookworm;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.totsp.bookworm.data.GoogleBookDataSource;
import com.totsp.bookworm.data.IAsyncCallback;
import com.totsp.bookworm.data.IBookDataSource;
import com.totsp.bookworm.model.Book;
import com.totsp.bookworm.util.BookImageUtil;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

public class BookScanResult extends Activity {

   private Handler setImageHandler = new Handler() {
      @Override
      public void handleMessage(Message msg) {
         if (imageBitmap != null) {
            scanCover.setImageBitmap(imageBitmap);
         } else {
            // TODO need missing cover image
            scanCover.setImageResource(R.drawable.books48);
         }
      }
   };

   private IBookDataSource bookDataSource;
   
   private Bitmap imageBitmap;
   
   private ImageView scanCover;
   private TextView scanTitle;
   private TextView output;   

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      setContentView(R.layout.bookscanresult);

      this.scanCover = (ImageView) this.findViewById(R.id.scanCover);
      this.scanTitle = (TextView) this.findViewById(R.id.scanTitle);
      this.output = (TextView) this.findViewById(R.id.scanOutput);

      String scanResultContents = this.getIntent().getStringExtra("SCAN_RESULT_CONTENTS");
      this.output.setText("book UPC: " + scanResultContents);

      // get cover image
      this.getBookCoverImage(scanResultContents);
      
      // get book data
      // inject? - allow user to pick source (we have ISBN, now need to get rest of data)?
      this.bookDataSource = new GoogleBookDataSource();
      this.getBookDataFromSource(scanResultContents);      
   }  
   
   private void getBookDataFromSource(final String isbn) {
      new Thread() {
         public void run() {
            bookDataSource.getBook(isbn, new IAsyncCallback<Book>() {
               public void onSuccess(Book b) {
                  output.setText(b.toString());
                  scanTitle.setText(b.getTitle());            
               }
               public void onFailure(Throwable t) {
                  // TODO
                  Log.e(Splash.APP_NAME, "Error getting book", t);
                  output.setText(t.getMessage());
               }
            });
         }
      }.start();
   }
   
   // TODO cleanup getCover stuff
   // TODO need to make this an AsyncTask (or add some stuff to HTTPRequestHelper to return binary?)
   private void getBookCoverImage(final String isbn) {
      // TODO before going to network, check if we have image locally
      // TODO store images filesys/db, something
      new Thread() {
         public void run() { 
            String imageUrl = BookImageUtil.getCoverUrlMedium(isbn);
            Log.d(Splash.APP_NAME, "book cover imageUrl - " + imageUrl);
            if ((imageUrl != null) && !imageUrl.equals("")) {
               try {
                  URL url = new URL(imageUrl);
                  URLConnection conn = url.openConnection();
                  conn.connect();
                  BufferedInputStream bis = new BufferedInputStream(conn.getInputStream());
                  imageBitmap = BitmapFactory.decodeStream(bis);                  
               } catch (IOException e) {
                  Log.e(Splash.APP_NAME, " ", e);
               }
            }
            setImageHandler.sendEmptyMessage(1);
         }
      }.start();
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
}
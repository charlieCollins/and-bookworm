package com.totsp.bookworm;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.Xml;
import android.widget.ImageView;
import android.widget.TextView;

import com.totsp.bookworm.data.GoogleBooksHandler;
import com.totsp.bookworm.data.HTTPRequestHelper;
import com.totsp.bookworm.model.Book;
import com.totsp.bookworm.model.BookImageUtil;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

public class BookScanResult extends Activity {

   private HTTPRequestHelper httpHelper;

   private Handler httpHandler = new Handler() {
      public void handleMessage(final Message msg) {
         String responseError = msg.getData().getString(HTTPRequestHelper.HTTP_RESPONSE_ERROR);
         if (responseError != null) {
            // HANDLE HTTP ERROR HERE
         }         
         String response = msg.getData().getString(HTTPRequestHelper.HTTP_RESPONSE);
         Log.d(Splash.APP_NAME, "HANDLER returned with msg - " + msg);
         Log.d(Splash.APP_NAME, " response - " + response);
         if (response != null) {
            BookScanResult.this.parseResponse(response);
         }
      }
   };
   
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

   private Bitmap imageBitmap;
   
   private ImageView scanCover;
   private TextView scanTitle;
   private TextView output;   

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      this.httpHelper = new HTTPRequestHelper(this.httpHandler);

      setContentView(R.layout.bookscanresult);

      this.scanCover = (ImageView) this.findViewById(R.id.scanCover);
      this.scanTitle = (TextView) this.findViewById(R.id.scanTitle);
      this.output = (TextView) this.findViewById(R.id.scanOutput);

      String scanResultContents = this.getIntent().getStringExtra("SCAN_RESULT_CONTENTS");
      this.output.setText("book UPC: " + scanResultContents);

      this.getBookData(scanResultContents);
   }   
   
   private void getBookData(String isbn) {
      final String url = "http://books.google.com/books/feeds/volumes?q=isbn:" + isbn;
      // web url http://books.google.com/books?isbn=
      // network call on separate thread
      // TODO put this in AsyncTask - flesh out example/template for doing this in general
      // TODO progress dialog
      this.httpHelper.performGet(url);
   }   

   private void parseResponse(String response) {
      GoogleBooksHandler gBooksHandler = new GoogleBooksHandler();
      // TODO validate response quickly?
      // TODO do parsing async too?
      // TODO progress dialog
      try {
         Xml.parse(new ByteArrayInputStream(response.getBytes("UTF-8")), Xml.Encoding.UTF_8, gBooksHandler);
      } catch (Exception e) {
         throw new RuntimeException(e);
      }

      ArrayList<Book> books = gBooksHandler.getBooks();
      if (books != null && !books.isEmpty()) {
         this.output.setText(books.get(0).toString());
         this.scanTitle.setText(books.get(0).getTitle());
         this.getCoverImage(books.get(0).getIsbn());
      } else {
         this.output.setText("unable to parse HTTP response into book, or no results - check log");
      } 
      
      // TODO call method to validate and save to DB here, etc
   }
   
   // TODO need to make this an AsyncTask (or add some stuff to HTTPRequestHelper to return binary?)
   private void getCoverImage(final String isbn) {
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
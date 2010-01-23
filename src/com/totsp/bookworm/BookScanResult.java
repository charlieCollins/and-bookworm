package com.totsp.bookworm;

import android.app.Activity;
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

import java.io.ByteArrayInputStream;
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

   private TextView scanTitle;
   private TextView output;
   private ImageView cover;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      this.httpHelper = new HTTPRequestHelper(this.httpHandler);

      setContentView(R.layout.bookscanresult);

      this.cover = (ImageView) this.findViewById(R.id.scanCover);
      this.output = (TextView) this.findViewById(R.id.scanOutput);

      String scanResultContents = this.getIntent().getStringExtra("SCAN_RESULT_CONTENTS");
      this.output.setText("book UPC: " + scanResultContents);

      this.getBookData(scanResultContents);
   }

   private void getBookData(String isbn) {
      // TODO Parser for google books      
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
         this.output.setText(books.get(0).getTitle());
      } else {
         this.output.setText("unable to parse HTTP response into book, or no results - check log");
      }      
      // validate and save here, etc      
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
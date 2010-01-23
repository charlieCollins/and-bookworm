package com.totsp.bookworm;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.totsp.bookworm.data.HTTPRequestHelper;

public class BookScanResult extends Activity {

   private HTTPRequestHelper httpHelper;

   private Handler httpHandler = new Handler() {
      public void handleMessage(final Message msg) {
         String response = msg.getData().getString("RESPONSE");
         Log.d(Splash.APP_NAME, "HANDLER returned with msg - " + msg);
         Log.d(Splash.APP_NAME, " response - " + response);
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
      this.output.setText(scanResultContents);
      
      this.getBookData(scanResultContents);
   }

   private void getBookData(String isbn) {

      // TODO Parser for google books      
      final String url = "http://books.google.com/books/feeds/volumes?q=isbn:" + isbn;
      // web url http://books.google.com/books?isbn=

      // network call on separate thread
      // TODO put this in AsyncTask - flesh out example/template for doing this in general
      new Thread() {
         @Override
         public void run() {
            BookScanResult.this.httpHelper.performGet(url);
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
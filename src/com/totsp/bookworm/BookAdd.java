package com.totsp.bookworm;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.totsp.bookworm.zxing.ZXingIntentIntegrator;
import com.totsp.bookworm.zxing.ZXingIntentResult;

public class BookAdd extends Activity {

   ///private BookWormApplication application;

   private Button scanButton;
   private Button searchButton;
   private Button formButton;

   @Override
   public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      this.setContentView(R.layout.bookadd);
      ///this.application = (BookWormApplication) this.getApplication();      

      this.scanButton = (Button) this.findViewById(R.id.bookaddscanbutton);
      this.scanButton.setOnClickListener(new OnClickListener() {
         public void onClick(final View v) {
            ZXingIntentIntegrator.initiateScan(BookAdd.this, "Scan Book", "message", "Yes", "No");
         }
      });

      this.searchButton = (Button) this.findViewById(R.id.bookaddsearchbutton);
      this.searchButton.setOnClickListener(new OnClickListener() {
         public void onClick(final View v) {
            BookAdd.this.startActivity(new Intent(BookAdd.this, BookEntrySearch.class));
         }
      });

      this.formButton = (Button) this.findViewById(R.id.bookaddformbutton);
      this.formButton.setOnClickListener(new OnClickListener() {
         public void onClick(final View v) {
            BookAdd.this.startActivity(new Intent(BookAdd.this, BookEntryForm.class));
         }
      });
   }

   @Override
   public void onStart() {
      super.onStart();
      if (this.connectionPresent()) {
         this.scanButton.setEnabled(true);
         this.searchButton.setEnabled(true);
      } else {
         this.scanButton.setEnabled(false);
         this.searchButton.setEnabled(false);
         Toast.makeText(BookAdd.this, "Network connection not present, cannot scan or search at this time.",
                  Toast.LENGTH_LONG).show();
      }
   }

   @Override
   public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
      ZXingIntentResult scanResult = ZXingIntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
      if (scanResult != null) {
         String isbn = scanResult.getContents();
         if (scanResult.getFormatName() != null && !scanResult.getFormatName().equals("EAN_13")) {
            // if it's not EAN 13 we are likely gonna have issues 
            // we are using PRODUCT_MODE which limits to UPC and EAN
            // we *might* be able to parse ISBN from UPC, but pattern is not understood, yet
            // if it's EAN-8 though, we are screwed
            // for example UPC 008819265580
            if (scanResult.getFormatName().startsWith("UPC")) {
               isbn = scanResult.getContents();
               if (isbn.length() == 12) {
                  if (isbn.startsWith("0")) {
                     isbn = isbn.substring(1, isbn.length());
                  }
                  if (isbn.endsWith("0")) {
                     isbn = isbn.substring(0, isbn.length() - 1);
                  }
               }
               Log.w(Constants.LOG_TAG, "Scan result was a UPC code (not an EAN code), parsed into ISBN:" + isbn);
            }
         }

         // handle scan result
         Intent scanIntent = new Intent(this, BookEntryResult.class);
         scanIntent.putExtra(Constants.ISBN, isbn);
         scanIntent.putExtra("FORMAT_NAME", scanResult.getFormatName());
         this.startActivity(scanIntent);
      }
   }

   private boolean connectionPresent() {
      ConnectivityManager cMgr = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
      NetworkInfo netInfo = cMgr.getActiveNetworkInfo();
      if ((netInfo != null) && (netInfo.getState() != null)) {
         return netInfo.getState().equals(State.CONNECTED);
      } else {
         return false;
      }
   }
}
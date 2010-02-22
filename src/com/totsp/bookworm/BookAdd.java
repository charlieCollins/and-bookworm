package com.totsp.bookworm;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

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

      ///this.application = (BookWormApplication) this.getApplication();

      this.setContentView(R.layout.bookadd);

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
   public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
      ZXingIntentResult scanResult = ZXingIntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
      if (scanResult != null) {
         // handle scan result
         Intent scanIntent = new Intent(this, BookEntryResult.class);
         scanIntent.putExtra(Constants.ISBN, scanResult.getContents());
         this.startActivity(scanIntent);
      } 
   }   
}
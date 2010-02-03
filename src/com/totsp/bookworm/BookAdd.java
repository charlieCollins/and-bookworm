package com.totsp.bookworm;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TabHost;

import com.totsp.bookworm.zxing.ZXingIntentIntegrator;
import com.totsp.bookworm.zxing.ZXingIntentResult;

public class BookAdd extends TabActivity {

   ///private BookWormApplication application;

   private TabHost tabHost;

   private Button scanButton;
   private Button searchButton;
   private Button formButton;

   @Override
   public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      ///this.application = (BookWormApplication) this.getApplication();

      this.setContentView(R.layout.bookadd);

      // TODO maybe move all this stuff to a "createViews" type method?      
      this.tabHost = this.getTabHost();
      this.tabHost.addTab(this.tabHost.newTabSpec("tab1").setIndicator("Scan",
               this.getResources().getDrawable(android.R.drawable.ic_menu_add)).setContent(R.id.bookaddscantab));
      this.tabHost.addTab(this.tabHost.newTabSpec("tab2").setIndicator("Search",
               this.getResources().getDrawable(android.R.drawable.ic_menu_search)).setContent(R.id.bookaddsearchtab));
      this.tabHost.addTab(this.tabHost.newTabSpec("tab3").setIndicator("Form",
               this.getResources().getDrawable(android.R.drawable.ic_menu_camera)).setContent(R.id.bookaddformtab));
      this.tabHost.setCurrentTab(0);

      this.scanButton = (Button) this.findViewById(R.id.bookaddscanbutton);
      this.scanButton.setOnClickListener(new OnClickListener() {
         public void onClick(final View v) {
            ZXingIntentIntegrator.initiateScan(BookAdd.this, "Scan Book", "message", "Yes", "No");
         }
      });

      this.searchButton = (Button) this.findViewById(R.id.bookaddsearchbutton);
      this.searchButton.setOnClickListener(new OnClickListener() {
         public void onClick(final View v) {
            // TODO search here
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
   public void onPause() {
      super.onPause();
   }   

   @Override
   protected void onRestoreInstanceState(final Bundle savedInstanceState) {
      super.onRestoreInstanceState(savedInstanceState);
   }

   @Override
   protected void onSaveInstanceState(final Bundle saveState) {
      super.onSaveInstanceState(saveState);
   }

   @Override
   public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
      ZXingIntentResult scanResult = ZXingIntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
      if (scanResult != null) {
         // handle scan result
         Intent scanIntent = new Intent(this, BookEntryResult.class);
         scanIntent.putExtra(Constants.ISBN, scanResult.getContents());
         this.startActivity(scanIntent);
      } else {
         // TODO report scan problem?
      }
   }   
}
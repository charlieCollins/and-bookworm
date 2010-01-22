package com.totsp.bookworm;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.AdapterView.OnItemClickListener;

import com.totsp.bookworm.data.DataHelper;
import com.totsp.bookworm.data.DbConstants;
import com.totsp.bookworm.model.Book;

import java.util.ArrayList;
import java.util.Date;

public class Main extends TabActivity {

   private static final int MENU_HELP = 0;

   private DataHelper dh;
   private TabHost tabHost;

   // tab1 - book list
   private ListView bookList;

   // tab2 - book gallery

   // tab3 - add book
   private Button scanButton;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      this.dh = new DataHelper(this);

      setContentView(R.layout.main);

      this.tabHost = this.getTabHost();
      tabHost.addTab(tabHost.newTabSpec("tab1").setIndicator("Book List").setContent(R.id.booklist));
      tabHost.addTab(tabHost.newTabSpec("tab2").setIndicator("Book Gallery").setContent(R.id.bookgallery));
      tabHost.addTab(tabHost.newTabSpec("tab3").setIndicator("Add a Book").setContent(R.id.bookadd));
      this.tabHost.setCurrentTab(0);

      // TODO clean up this mess
      this.bookList = (ListView) this.findViewById(R.id.booklist);
      final ArrayList<String> bookNames = new ArrayList<String>();
      bookNames.addAll(this.dh.selectAllBookNames());
      Log.d(Splash.APP_NAME, "bookNames size - " + bookNames.size());
      ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, bookNames);
      this.bookList.setAdapter(adapter);
      this.bookList.setOnItemClickListener(new OnItemClickListener() {
         public void onItemClick(AdapterView parent, View v, int index, long id) {
            Log.d(Splash.APP_NAME, "onItemClick - " + bookNames.get(index));
            Intent intent = new Intent(Main.this, BookDetail.class);
            intent.putExtra(DbConstants.TITLE, bookNames.get(index));
            Main.this.startActivity(intent);
         }
      });

      this.scanButton = (Button) this.findViewById(R.id.scan_button);
      this.scanButton.setOnClickListener(new OnClickListener() {
         public void onClick(View v) {
            ZXingIntentIntegrator.initiateScan(Main.this, "Scan Book", "message", "Yes", "No");
         }
      });

   }

   @Override
   public void onPause() {
      this.dh.cleanup();
      super.onPause();
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      menu.add(0, MENU_HELP, 0, "Help").setIcon(android.R.drawable.ic_menu_help);
      return super.onCreateOptionsMenu(menu);
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      switch (item.getItemId()) {
      case MENU_HELP:
         this.startActivity(new Intent(Main.this, Help.class));
         return true;
      }
      return super.onOptionsItemSelected(item);
   }

   @Override
   public void onActivityResult(int requestCode, int resultCode, Intent intent) {
      ZXingIntentResult scanResult = ZXingIntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
      if (scanResult != null) {
         // handle scan result
         Log.d(Splash.APP_NAME, "************** scanResult - " + scanResult.getContents());

         // TODO validate that result looks like an ISBN? 

         this.getBookDataFromGoogleBooks(scanResult.getContents());

      }
      // else continue with any other code you need in the method
   }

   private void getBookDataFromGoogleBooks(String isbn) {

      Handler handler = new Handler() {
         public void handleMessage(final Message msg) {
            Log.d(Splash.APP_NAME, "HANDLER returned with msg - " + msg);
         }
      };

      String url = "http://books.google.com/books?isbn=" + isbn;
      
      HTTPRequestHelper http = new HTTPRequestHelper(handler);
      http.performGet(url);
   }

   private void insertTestData() {
      // temp
      try {
         Book b1 = new Book("1231", "book1", null, new Date());
         Book b2 = new Book("1232", "book2", null, new Date());
         Book b3 = new Book("1233", "book3", null, new Date());
         this.dh.insertBook(b1);
         this.dh.insertBook(b2);
         this.dh.insertBook(b3);
      } catch (Exception e) {
         Log.d(Splash.APP_NAME, "Error creating sample books", e);
      }
      // end temp
   }
}
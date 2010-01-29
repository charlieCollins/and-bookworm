package com.totsp.bookworm;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
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
import com.totsp.bookworm.zxing.ZXingIntentIntegrator;
import com.totsp.bookworm.zxing.ZXingIntentResult;

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
   private Button searchButton;
   private Button formButton;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      this.dh = new DataHelper(this);

      setContentView(R.layout.main);

      // TODO maybe move all this stuff to a "createViews" type method?      
      this.tabHost = this.getTabHost();
      tabHost.addTab(tabHost.newTabSpec("tab1").setIndicator("Book List").setContent(R.id.booklist));
      tabHost.addTab(tabHost.newTabSpec("tab2").setIndicator("Book Gallery").setContent(R.id.bookgallery));
      tabHost.addTab(tabHost.newTabSpec("tab3").setIndicator("Add a Book").setContent(R.id.bookadd));
      this.tabHost.setCurrentTab(0);

      this.scanButton = (Button) this.findViewById(R.id.bookaddscanbutton);
      this.scanButton.setOnClickListener(new OnClickListener() {
         public void onClick(View v) {
            ZXingIntentIntegrator.initiateScan(Main.this, "Scan Book", "message", "Yes", "No");
         }
      });

      this.searchButton = (Button) this.findViewById(R.id.bookaddsearchbutton);
      this.searchButton.setOnClickListener(new OnClickListener() {
         public void onClick(View v) {
            new AlertDialog.Builder(Main.this).setTitle("Click!").setMessage("Clicked search button").show();
         }
      });

      this.formButton = (Button) this.findViewById(R.id.bookaddformbutton);
      this.formButton.setOnClickListener(new OnClickListener() {
         public void onClick(View v) {
            new AlertDialog.Builder(Main.this).setTitle("Click!").setMessage("Clicked form button").show();
         }
      });

      this.bindBookList();
   }

   private void bindBookList() {
      this.bookList = (ListView) this.findViewById(R.id.booklist);
      final ArrayList<String> bookNames = new ArrayList<String>();
      bookNames.addAll(this.dh.selectAllBookNames());
      Log.d(Splash.APP_NAME, "bookNames size - " + bookNames.size());
      ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, bookNames);
      this.bookList.setAdapter(adapter);
      this.bookList.setOnItemClickListener(new OnItemClickListener() {
         public void onItemClick(AdapterView<?> parent, View v, int index, long id) {
            Log.d(Splash.APP_NAME, "onItemClick - " + bookNames.get(index));
            Intent intent = new Intent(Main.this, BookDetail.class);
            intent.putExtra(DbConstants.TITLE, bookNames.get(index));
            Main.this.startActivity(intent);
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
   protected void onRestoreInstanceState(Bundle savedInstanceState) {
      super.onRestoreInstanceState(savedInstanceState);
   }

   @Override
   protected void onSaveInstanceState(Bundle saveState) {
      super.onSaveInstanceState(saveState);
   }

   @Override
   protected Dialog onCreateDialog(int id) {
      switch (id) {
      case 1:
         final AlertDialog.Builder builder = new AlertDialog.Builder(this);
         builder.setTitle("title");
         builder.setIcon(android.R.drawable.ic_dialog_alert);
         builder.setMessage("message");
         builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
               // onclick
            }
         });
         builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
               // onclick
            }
         });
         builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
               // onclick
            }
         });
         builder.setCancelable(true);
         return builder.create();
      }
      return super.onCreateDialog(id);
   }

   @Override
   protected void onPrepareDialog(int id, Dialog dialog) {
      super.onPrepareDialog(id, dialog);
      switch (id) {
      case 1:
         dialog.setTitle("first dialog");
         break;
      }
   }

   @Override
   public void onActivityResult(int requestCode, int resultCode, Intent intent) {
      ZXingIntentResult scanResult = ZXingIntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
      if (scanResult != null) {
         // handle scan result
         Intent scanIntent = new Intent(this, BookScanResult.class);
         scanIntent.putExtra("SCAN_RESULT_CONTENTS", scanResult.getContents());
         this.startActivity(scanIntent);
      } else {
         // TODO report scan problem?
      }
   }

   private void insertTestData() {
      // temp
      try {
         Book b1 = new Book("1231", "book1");
         Book b2 = new Book("1232", "book2");
         Book b3 = new Book("1233", "book3");
         this.dh.insertBook(b1);
         this.dh.insertBook(b2);
         this.dh.insertBook(b3);
      } catch (Exception e) {
         Log.d(Splash.APP_NAME, "Error creating sample books", e);
      }
      // end temp
   }
}
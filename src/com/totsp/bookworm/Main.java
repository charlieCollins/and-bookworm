package com.totsp.bookworm;

import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

import com.totsp.bookworm.model.Book;
import com.totsp.bookworm.zxing.ZXingIntentIntegrator;
import com.totsp.bookworm.zxing.ZXingIntentResult;

import java.util.ArrayList;

public class Main extends TabActivity {

   private static final int MENU_HELP = 0;
   private static final int MENU_CONTEXT_EDIT = 0;
   private static final int MENU_CONTEXT_DELETE = 1;
   
   private BookWormApplication application;
   
   private TabHost tabHost;

   // tab1 - book list
   private ArrayList<Book> bookList;
   private ListView bookListView;

   // tab2 - book gallery

   // tab3 - add book
   private Button scanButton;
   private Button searchButton;
   private Button formButton;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      this.application = (BookWormApplication) this.getApplication();

      setContentView(R.layout.main);

      // TODO maybe move all this stuff to a "createViews" type method?      
      this.tabHost = this.getTabHost();
      tabHost.addTab(tabHost.newTabSpec("tab1").setIndicator("Book List").setContent(R.id.bookListView));
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
      this.bookListView = (ListView) this.findViewById(R.id.bookListView);
      this.bookList = new ArrayList<Book>();
      // TODO select only id/title, or cache/page - this could get slow with a lot of books?
      this.bookList.addAll(this.application.getDataHelper().selectAllBooks());
      Log.d(Splash.APP_NAME, "bookList size - " + this.bookList.size());
      ArrayAdapter<Book> adapter = new ArrayAdapter<Book>(this, android.R.layout.simple_list_item_1, this.bookList);
      this.bookListView.setAdapter(adapter);
      this.bookListView.setOnItemClickListener(new OnItemClickListener() {
         public void onItemClick(AdapterView<?> parent, View v, int index, long id) {
            Log.d(Splash.APP_NAME, "onItemClick - " + bookList.get(index));
            Main.this.application.setSelectedBook(bookList.get(index));
            Main.this.startActivity(new Intent(Main.this, BookDetail.class));
         }
      });
      registerForContextMenu(this.bookListView);
   }

   @Override
   public void onPause() {
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
      default:
         return super.onOptionsItemSelected(item);
      }
   }

   public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
      super.onCreateContextMenu(menu, v, menuInfo);
      menu.add(0, MENU_CONTEXT_EDIT, 0, "Edit Book");
      menu.add(1, MENU_CONTEXT_DELETE, 0, "Delete Book");
   }

   public boolean onContextItemSelected(MenuItem item) {
      AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
      long listIndex = info.id;
      Book b = this.bookList.get((int) listIndex);
      switch (item.getItemId()) {
      case MENU_CONTEXT_EDIT:         
         new AlertDialog.Builder(Main.this).setTitle("Click! EDIT" + b.getTitle()).setMessage(b.getTitle()).show();
         return true;
      case MENU_CONTEXT_DELETE:        
         this.application.getDataHelper().deleteBook(b.getId());
         // TODO put buttons here - sure/cancel
         new AlertDialog.Builder(Main.this).setTitle("Deleted book").setMessage(b.getTitle()).show();
         return true;
      default:
         return super.onContextItemSelected(item);
      }
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

   /*
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
   */
}
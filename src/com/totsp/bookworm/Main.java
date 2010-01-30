package com.totsp.bookworm;

import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;
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
      BookAdapter adapter = new BookAdapter(this, android.R.layout.simple_list_item_1, this.bookList);
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
      menu.setHeaderTitle("Action");
   }

   public boolean onContextItemSelected(MenuItem item) {
      AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
      long listIndex = info.id;
      final Book b = this.bookList.get((int) listIndex);
      switch (item.getItemId()) {
      case MENU_CONTEXT_EDIT:
         new AlertDialog.Builder(Main.this).setTitle("Click! EDIT - TODO").setMessage(b.getTitle()).show();
         return true;
      case MENU_CONTEXT_DELETE:
         new AlertDialog.Builder(Main.this).setTitle("Delete book?").setMessage(b.getTitle()).setPositiveButton(
                  "Yes, I'm Sure", new DialogInterface.OnClickListener() {
                     public void onClick(DialogInterface d, int i) {
                        Main.this.application.getDataHelper().deleteBook(b.getId());
                        Main.this.startActivity(getIntent());
                        Main.this.finish();
                     }
                  }).setNegativeButton("No, Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, int i) {
            }
         }).show();
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

   //
   // BookAdapter
   //

   private class BookAdapter extends ArrayAdapter<Book> {

      LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      private ArrayList<Book> books;

      public BookAdapter(Context context, int resId, ArrayList<Book> books) {
         super(context, resId, books);
         this.books = books;
      }

      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
         View v = convertView;
         if (v == null) {
            v = vi.inflate(R.layout.items_list_item, null);
         }

         Book book = books.get(position);
         if (book != null) {
            ImageView iv = (ImageView) v.findViewById(R.id.items_list_item_image);
            if (book.getCoverImageId() > 0) {
               Bitmap coverImage = application.getDataImageHelper().getImage((int) book.getCoverImageId());
               if (coverImage != null && coverImage.getWidth() > 10) {
                  iv.setImageBitmap(coverImage);
               }
            } 

            TextView tv1 = (TextView) v.findViewById(R.id.items_list_item_text_line1);
            tv1.setText(book.getTitle());           
         }
         return v;
      }
   }
}
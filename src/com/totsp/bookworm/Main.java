package com.totsp.bookworm;

import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
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
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

import com.totsp.bookworm.model.Book;
import com.totsp.bookworm.zxing.ZXingIntentIntegrator;
import com.totsp.bookworm.zxing.ZXingIntentResult;

import java.util.ArrayList;

public class Main extends TabActivity {

   private static final int MENU_ABOUT = 0;
   private static final int MENU_SEARCH = 1;
   private static final int MENU_SORT_RATING = 2;
   private static final int MENU_SORT_ALPHA = 3;
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
      tabHost.addTab(tabHost.newTabSpec("tab1").setIndicator("Book List",
               getResources().getDrawable(android.R.drawable.ic_menu_agenda)).setContent(R.id.booklistview));
      tabHost.addTab(tabHost.newTabSpec("tab2").setIndicator("Add Book",
               getResources().getDrawable(android.R.drawable.ic_menu_add)).setContent(R.id.bookadd));
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
            //Toast.makeText(Main.this, "TODO Search Entry", Toast.LENGTH_SHORT).show();
            Main.this.startActivity(new Intent(Main.this, BookEntrySearch.class));
         }
      });

      this.formButton = (Button) this.findViewById(R.id.bookaddformbutton);
      this.formButton.setOnClickListener(new OnClickListener() {
         public void onClick(View v) {
            //Toast.makeText(Main.this, "TODO Form Entry", Toast.LENGTH_SHORT).show();
            Main.this.startActivity(new Intent(Main.this, BookEntryForm.class));
         }
      });

      this.bindBookList();
   }

   private void bindBookList() {
      this.bookListView = (ListView) this.findViewById(R.id.booklistview);
      this.bookList = new ArrayList<Book>();
      // TODO select only id/title, or cache/page - this could get slow with a lot of books?
      this.bookList.addAll(this.application.getDataHelper().selectAllBooks());
      BookAdapter adapter = new BookAdapter(this, android.R.layout.simple_list_item_1, this.bookList);
      this.bookListView.setAdapter(adapter);
      this.bookListView.setOnItemClickListener(new OnItemClickListener() {
         public void onItemClick(AdapterView<?> parent, View v, int index, long id) {
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
      menu.add(0, MENU_ABOUT, 2, "About").setIcon(android.R.drawable.ic_menu_help);
      menu.add(0, MENU_SEARCH, 1, "Search").setIcon(android.R.drawable.ic_menu_search);
      menu.add(0, MENU_SORT_RATING, 0, "Sort|Rating").setIcon(android.R.drawable.ic_menu_sort_by_size);
      menu.add(0, MENU_SORT_ALPHA, 0, "Sort|Alpha").setIcon(android.R.drawable.ic_menu_sort_alphabetically);
      return super.onCreateOptionsMenu(menu);
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      switch (item.getItemId()) {
      case MENU_ABOUT:
         this.startActivity(new Intent(Main.this, About.class));
         return true;
      case MENU_SEARCH:
         Toast.makeText(this, "TODO Search", Toast.LENGTH_SHORT).show();
         return true;
      case MENU_SORT_RATING:
         Toast.makeText(this, "TODO Sort Rating", Toast.LENGTH_SHORT).show();
         return true;
      case MENU_SORT_ALPHA:
         Toast.makeText(this, "TODO Sort Alpha", Toast.LENGTH_SHORT).show();
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
         Toast.makeText(this, "TODO Edit Book", Toast.LENGTH_SHORT).show();
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
         Intent scanIntent = new Intent(this, BookEntryResult.class);
         scanIntent.putExtra(Constants.ISBN, scanResult.getContents());
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
            ImageView iv = (ImageView) v.findViewById(R.id.itemslistitemimage);
            if (book.getCoverImageId() > 0) {
               Bitmap coverImage = application.getDataImageHelper().getImage((int) book.getCoverImageId());
               iv.setImageBitmap(coverImage);
            } else {
               iv.setImageResource(R.drawable.book_cover_missing);
            }
            TextView above = (TextView) v.findViewById(R.id.itemslistitemtextabove);
            above.setText(book.getTitle());
            TextView below = (TextView) v.findViewById(R.id.itemslistitemtextbelow);
            below.setText(book.getSubTitle());
         }
         return v;
      }
   }
}
package com.totsp.bookworm;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

import com.totsp.bookworm.model.Book;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;

public class Main extends Activity {

   private static final int MENU_ABOUT = 0;
   private static final int MENU_PREFS = 1;
   private static final int MENU_BOOKADD = 2;
   private static final int MENU_SORT_RATING = 3;
   private static final int MENU_SORT_ALPHA = 4;
   private static final int MENU_MANAGE = 5;

   private static final int MENU_CONTEXT_EDIT = 0;
   private static final int MENU_CONTEXT_DELETE = 1;

   private BookWormApplication application;

   private TextView bookListViewEmpty;
   private ListView bookListView;
   BookAdapter adapter;

   private final ArrayList<Book> bookList = new ArrayList<Book>();

   @Override
   public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      this.application = (BookWormApplication) this.getApplication();

      this.setContentView(R.layout.main);

      this.bookListViewEmpty = (TextView) this.findViewById(R.id.booklistviewempty);
      this.bookListView = (ListView) this.findViewById(R.id.booklistview);
      this.bookListView.setEmptyView(this.findViewById(R.id.booklistviewempty));
      
      new SelectAllBooksTask().execute();
   }

   @Override
   public void onStart() {
      
   }

   private void bindBookList(final ArrayList<Book> books) {
      this.adapter = new BookAdapter(this, 0, books);
      this.bookListView.setAdapter(this.adapter);
      this.bookListView.setTextFilterEnabled(true);
      this.bookListView.setOnItemClickListener(new OnItemClickListener() {
         public void onItemClick(final AdapterView<?> parent, final View v, final int index, final long id) {
            if (Constants.LOCAL_LOGV) {
               Log.v(Constants.LOG_TAG, "book selected - " + index);
            }
            Main.this.application.setSelectedBook(books.get(index));
            Main.this.startActivity(new Intent(Main.this, BookDetail.class));
         }
      });
      this.registerForContextMenu(this.bookListView);
   }

   @Override
   public boolean onCreateOptionsMenu(final Menu menu) {
      menu.add(0, Main.MENU_SORT_RATING, 0, "Sort|Rating").setIcon(android.R.drawable.ic_menu_sort_by_size);
      menu.add(0, Main.MENU_SORT_ALPHA, 1, "Sort|Alpha").setIcon(android.R.drawable.ic_menu_sort_alphabetically);
      menu.add(0, Main.MENU_BOOKADD, 2, "Add Book").setIcon(android.R.drawable.ic_menu_add);
      menu.add(0, Main.MENU_ABOUT, 3, "About").setIcon(android.R.drawable.ic_menu_help);
      menu.add(0, Main.MENU_PREFS, 4, "Prefs").setIcon(android.R.drawable.ic_menu_preferences);
      menu.add(0, Main.MENU_MANAGE, 6, "Manage Database").setIcon(android.R.drawable.ic_menu_manage);
      return super.onCreateOptionsMenu(menu);
   }

   @Override
   public boolean onOptionsItemSelected(final MenuItem item) {
      switch (item.getItemId()) {
      case MENU_ABOUT:
         this.startActivity(new Intent(Main.this, About.class));
         return true;
      case MENU_PREFS:
         this.startActivity(new Intent(Main.this, Preferences.class));
         return true;
      case MENU_BOOKADD:
         this.startActivity(new Intent(Main.this, BookAdd.class));
         return true;
      case MENU_SORT_RATING:
         this.adapter.sort(new RatingComparator());
         return true;
      case MENU_SORT_ALPHA:
         this.adapter.sort(new AlphaComparator());
         return true;
      case MENU_MANAGE:
         this.startActivity(new Intent(Main.this, ManageData.class));
         return true;
      default:
         return super.onOptionsItemSelected(item);
      }
   }

   public void onCreateContextMenu(final ContextMenu menu, final View v, final ContextMenuInfo menuInfo) {
      super.onCreateContextMenu(menu, v, menuInfo);
      menu.add(0, Main.MENU_CONTEXT_EDIT, 0, "Edit Book");
      menu.add(0, Main.MENU_CONTEXT_DELETE, 1, "Delete Book");
      menu.setHeaderTitle("Action");
   }

   public boolean onContextItemSelected(final MenuItem item) {
      AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
      long listIndex = info.id;
      final Book b = this.bookList.get((int) listIndex);
      switch (item.getItemId()) {
      case MENU_CONTEXT_EDIT:
         Main.this.application.setSelectedBook(b);
         Main.this.startActivity(new Intent(Main.this, BookEdit.class));
         return true;
      case MENU_CONTEXT_DELETE:
         new AlertDialog.Builder(Main.this).setTitle("Delete book?").setMessage(b.getTitle()).setPositiveButton(
                  "Yes, I'm Sure", new DialogInterface.OnClickListener() {
                     public void onClick(final DialogInterface d, final int i) {
                        Main.this.application.getDataHelper().deleteBook(b.getId());
                        Main.this.startActivity(Main.this.getIntent());
                        Main.this.finish();
                     }
                  }).setNegativeButton("No, Cancel", new DialogInterface.OnClickListener() {
            public void onClick(final DialogInterface d, final int i) {
            }
         }).show();
         return true;
      default:
         return super.onContextItemSelected(item);
      }
   }

   //
   // Sort Comparators
   //
   private class AlphaComparator implements Comparator<Book> {
      public int compare(final Book b1, final Book b2) {
         String title1 = b1.getTitle();
         String title2 = b2.getTitle();
         return title1.toLowerCase().compareTo(title2.toLowerCase());
      }
   }

   private class RatingComparator implements Comparator<Book> {
      public int compare(final Book b1, final Book b2) {
         Integer rat1 = b1.getRating();
         Integer rat2 = b2.getRating();
         return rat2.compareTo(rat1);
      }
   }

   //
   // BookAdapter
   //
   private class BookAdapter extends ArrayAdapter<Book> implements Filterable {

      LayoutInflater vi = (LayoutInflater) Main.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      private final ArrayList<Book> books;

      private Bitmap coverImage;
      private ImageView coverImageView;
      private TextView aboveTextView;
      private TextView belowTextView;

      public BookAdapter(final Context context, final int resId, final ArrayList<Book> books) {
         super(context, resId, books);
         this.books = books;
      }

      @Override
      public View getView(final int position, final View convertView, final ViewGroup parent) {
         View v = convertView;
         if (v == null) {
            v = this.vi.inflate(R.layout.itemslistitem, null);
         }

         Book book = this.books.get(position);
         if (book != null) {
            this.coverImageView = (ImageView) v.findViewById(R.id.itemslistitemimage);
            if (book.getCoverImageId() > 0) {
               this.coverImage = Main.this.application.getDataImageHelper().getBitmap((int) book.getCoverImageId());
               this.coverImageView.setImageBitmap(this.coverImage);
            } else {
               this.coverImageView.setImageResource(R.drawable.book_cover_missing);
            }

            this.aboveTextView = (TextView) v.findViewById(R.id.itemslistitemtextabove);
            this.aboveTextView.setText(book.getTitle());
            this.belowTextView = (TextView) v.findViewById(R.id.itemslistitemtextbelow);
            this.belowTextView.setText(book.getSubTitle());
         }
         return v;
      }
   }

   // TODO don't select ALL - rather page data smarter (or at least leave images out and add in later?)
   //
   // AsyncTasks
   //
   private class SelectAllBooksTask extends AsyncTask<String, Void, HashSet<Book>> {
      private final ProgressDialog dialog = new ProgressDialog(Main.this);

      // can use UI thread here
      protected void onPreExecute() {
         this.dialog.setMessage("Retrieving book data...");
         this.dialog.show();
      }

      // automatically done on worker thread (separate from UI thread)
      protected HashSet<Book> doInBackground(final String... args) {
         return Main.this.application.getDataHelper().selectAllBooks();
      }

      // can use UI thread here
      protected void onPostExecute(final HashSet<Book> books) {
         if (this.dialog.isShowing()) {
            this.dialog.dismiss();
         }
         if (books.size() > 0) {
            Main.this.bookList.clear();
            Main.this.bookList.addAll(books);
            if (Constants.LOCAL_LOGV) {
               Log.v(Constants.LOG_TAG, "bookList size - " + Main.this.bookList.size());
            }
            Main.this.bindBookList(Main.this.bookList);
         } else {
            Main.this.bookListViewEmpty.setText(R.string.books_list_empty);
         }
      }
   }
}
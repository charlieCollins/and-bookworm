package com.totsp.bookworm;

import android.app.Activity;
import android.app.AlertDialog;
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

public class Main extends Activity {

   private static final int MENU_ABOUT = 0;
   private static final int MENU_BOOKADD = 1;
   private static final int MENU_SORT_RATING = 2;
   private static final int MENU_SORT_ALPHA = 3;

   private static final int MENU_CONTEXT_EDIT = 0;
   private static final int MENU_CONTEXT_DELETE = 1;

   private BookWormApplication application;

   BookAdapter adapter;
   private ListView bookListView;

   private final ArrayList<Book> bookList = new ArrayList<Book>();

   @Override
   public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      this.application = (BookWormApplication) this.getApplication();

      this.setContentView(R.layout.main);

      this.bookListView = (ListView) this.findViewById(R.id.booklistview);
      this.bookListView.setEmptyView(this.findViewById(R.id.booklistviewempty));
      // TODO retrieve first X books, then rest in background (rather than all)?
      this.bookList.addAll(this.application.getDataHelper().selectAllBooks());
      Log.d(Constants.LOG_TAG, "bookList size - " + bookList.size());
      this.bindBookList(this.bookList);
   }

   private void bindBookList(final ArrayList<Book> books) {
      this.adapter = new BookAdapter(this, 0, books);
      this.bookListView.setAdapter(this.adapter);
      this.bookListView.setTextFilterEnabled(true);
      this.bookListView.setOnItemClickListener(new OnItemClickListener() {
         public void onItemClick(final AdapterView<?> parent, final View v, final int index, final long id) {
            Log.d(Constants.LOG_TAG, "book selected - " + index);
            Main.this.application.setSelectedBook(books.get(index));
            Main.this.startActivity(new Intent(Main.this, BookDetail.class));
         }
      });
      this.registerForContextMenu(this.bookListView);
   }

   @Override
   public void onPause() {
      super.onPause();
   }

   @Override
   public boolean onCreateOptionsMenu(final Menu menu) {
      menu.add(0, Main.MENU_ABOUT, 0, "About").setIcon(android.R.drawable.ic_menu_help);
      menu.add(0, Main.MENU_BOOKADD, 1, "Add Book").setIcon(android.R.drawable.ic_menu_add);
      menu.add(0, Main.MENU_SORT_RATING, 2, "Sort|Rating").setIcon(android.R.drawable.ic_menu_sort_by_size);
      menu.add(0, Main.MENU_SORT_ALPHA, 3, "Sort|Alpha").setIcon(android.R.drawable.ic_menu_sort_alphabetically);
      return super.onCreateOptionsMenu(menu);
   }

   @Override
   public boolean onOptionsItemSelected(final MenuItem item) {
      switch (item.getItemId()) {
      case MENU_ABOUT:
         this.startActivity(new Intent(Main.this, About.class));
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

   @Override
   protected void onRestoreInstanceState(final Bundle savedInstanceState) {
      super.onRestoreInstanceState(savedInstanceState);
   }

   @Override
   protected void onSaveInstanceState(final Bundle saveState) {
      super.onSaveInstanceState(saveState);
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
         return rat1.compareTo(rat2);
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
               this.coverImageView.setImageBitmap(coverImage);
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

      /*
      public Filter getFilter() {
         return new Filter() {
            public Filter.FilterResults performFiltering(CharSequence constraint) {
               
            }
            public void publishResults(CharSequence constraint, Filter.FilterResults results) {
               
            }
         };
      }
      */
   }
}
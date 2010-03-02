package com.totsp.bookworm;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
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
import android.widget.CursorAdapter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

import com.totsp.bookworm.data.DataConstants;
import com.totsp.bookworm.data.DataHelper;
import com.totsp.bookworm.model.Book;

import java.util.ArrayList;

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

   private ListView bookListView;
   private CursorAdapter adapter;
   private Cursor cursor;

   private final ArrayList<Book> bookList = new ArrayList<Book>();

   // TODO empty view not working
   
   @Override
   public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      this.application = (BookWormApplication) this.getApplication();

      this.setContentView(R.layout.main);

      this.bookListView = (ListView) this.findViewById(R.id.booklistview);
      ///this.bookListView.setEmptyView(this.findViewById(R.id.booklistviewempty)); 
      this.bookListView.setTextFilterEnabled(true);
      this.bookListView.setOnItemClickListener(new OnItemClickListener() {
         public void onItemClick(final AdapterView<?> parent, final View v, final int index, final long id) {
            cursor.moveToPosition(index);
            // note - this is tricky, table doesn't have _id, but CursorAdapter requires it
            // in the query we used "book.bid as _id" so here we have to use _id too
            int bookId = cursor.getInt(cursor.getColumnIndex("_id"));
            Book book = Main.this.application.getDataHelper().selectBook(bookId);
            if (book != null) {
               if (Constants.LOCAL_LOGV) {
                  Log.v(Constants.LOG_TAG, "book selected - " + book.getTitle());
               }
               Main.this.application.setSelectedBook(book);
               Main.this.startActivity(new Intent(Main.this, BookDetail.class));
            } else {
               Toast.makeText(Main.this, "Unrecoverable error selecting book", Toast.LENGTH_SHORT).show();
            }
         }
      });
      this.registerForContextMenu(this.bookListView);      

      // TODO get last sort order from prefs
      this.bindBookList(DataHelper.ORDER_BY_TITLE_ASC);
   }

   @Override
   public void onStart() {
      super.onStart();
   }

   private void bindBookList(String orderBy) {
      this.cursor = this.application.getDataHelper().getSelectBookJoinCursor(orderBy);
      if (this.cursor != null && this.cursor.getCount() > 0) {
         this.startManagingCursor(cursor);
         this.adapter = new BookCursorAdapter(cursor);
         this.bookListView.setAdapter(this.adapter);         
      }
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
         this.bindBookList(DataHelper.ORDER_BY_RATING_ASC);
         return true;
      case MENU_SORT_ALPHA:
         this.bindBookList(DataHelper.ORDER_BY_TITLE_ASC);
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
   // BookCursorAdapter
   //
   private class BookCursorAdapter extends CursorAdapter implements Filterable {

      LayoutInflater vi = (LayoutInflater) Main.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

      public BookCursorAdapter(Cursor c) {
         super(Main.this, c, true);
      }

      @Override
      public void bindView(View v, Context context, Cursor c) {
         this.populateView(v, c);
      }

      @Override
      public View newView(Context context, Cursor c, ViewGroup parent) {
         View v = vi.inflate(R.layout.list_items_item, parent, false);
         this.populateView(v, c);
         return v;
      }

      private void populateView(View v, Cursor c) {
         if (c != null && !c.isClosed()) {
            int covImageId = c.getInt(c.getColumnIndex(DataConstants.COVERIMAGEID));
            ///int rating = c.getInt(c.getColumnIndex(DataConstants.RATING));
            ///int readStatus = c.getInt(c.getColumnIndex(DataConstants.READSTATUS));
            String title = c.getString(c.getColumnIndex(DataConstants.TITLE));
            String subTitle = c.getString(c.getColumnIndex(DataConstants.SUBTITLE));

            ImageView coverImageView = (ImageView) v.findViewById(R.id.list_items_item_image);
            if (covImageId > 0) {
               Bitmap coverImage = Main.this.application.getDataImageHelper().getBitmap(covImageId);
               coverImageView.setImageBitmap(coverImage);
            } else {
               coverImageView.setImageResource(R.drawable.book_cover_missing);
            }

            TextView aboveTextView = (TextView) v.findViewById(R.id.list_items_item_textabove);
            aboveTextView.setText(title);
            TextView belowTextView = (TextView) v.findViewById(R.id.list_items_item_textbelow);
            belowTextView.setText(subTitle);
         }
      }
   }

   //
   // OLD BookAdapter (works well for small data sets, larger want CursorAdapter or such)
   //
   /*
   private class BookAdapter extends ArrayAdapter<Book> implements Filterable {

      LayoutInflater vi = (LayoutInflater) Main.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      private final ArrayList<Book> books;

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
            ImageView coverImageView = (ImageView) v.findViewById(R.id.itemslistitemimage);
            if (book.getCoverImageId() > 0) {
               Bitmap coverImage = Main.this.application.getDataImageHelper().getBitmap((int) book.getCoverImageId());
               coverImageView.setImageBitmap(coverImage);
            } else {
               coverImageView.setImageResource(R.drawable.book_cover_missing);
            }

            TextView aboveTextView = (TextView) v.findViewById(R.id.itemslistitemtextabove);
            aboveTextView.setText(book.getTitle());
            TextView belowTextView = (TextView) v.findViewById(R.id.itemslistitemtextbelow);
            belowTextView.setText(book.getSubTitle());
         }
         return v;
      }
   }
   */

   //
   // AsyncTasks
   //
   /*
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
            ///Main.this.bindBookList(Main.this.bookList);
         } else {
            Main.this.bookListViewEmpty.setText(R.string.books_list_empty);
         }
      }
   }
   */

   //
   // Sort Comparators
   //
   /*
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
   */
}
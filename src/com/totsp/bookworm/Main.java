package com.totsp.bookworm;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import java.util.HashSet;

public class Main extends Activity {

   private static final int MENU_ABOUT = 0;
   private static final int MENU_PREFS = 1;
   private static final int MENU_BOOKADD = 2;
   private static final int MENU_SORT_RATING = 3;
   private static final int MENU_SORT_ALPHA = 4;
   private static final int MENU_MANAGE = 5;
   private static final int MENU_RESET_COVER_IMAGES = 6;

   private static final int MENU_CONTEXT_EDIT = 0;
   private static final int MENU_CONTEXT_DELETE = 1;

   private BookWormApplication application;

   private ListView bookListView;
   private CursorAdapter adapter;
   private Cursor cursor;

   private Bitmap coverImageMissing;

   private final ArrayList<Book> bookList = new ArrayList<Book>();

   // TODO empty view not working

   @Override
   public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      this.application = (BookWormApplication) this.getApplication();

      this.setContentView(R.layout.main);

      this.coverImageMissing = BitmapFactory.decodeResource(this.getResources(), R.drawable.book_cover_missing);

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
      menu.add(0, Main.MENU_RESET_COVER_IMAGES, 7, "Reset Cover Images").setIcon(android.R.drawable.ic_menu_gallery);
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
      case MENU_RESET_COVER_IMAGES:
         new ResetCoverImagesTask().execute();
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
            coverImageView.setImageBitmap(Main.this.coverImageMissing);

            if (coverImageView.isShown()) {
               new PopulateCoverImageTask(coverImageView).execute(covImageId);
            }

            ((TextView) v.findViewById(R.id.list_items_item_textabove)).setText(title);
            ((TextView) v.findViewById(R.id.list_items_item_textbelow)).setText(subTitle);
         }
      }
   }

   private class PopulateCoverImageTask extends AsyncTask<Integer, Void, Bitmap> {

      private ImageView v;

      public PopulateCoverImageTask(ImageView v) {
         super();
         this.v = v;
      }

      protected void onPreExecute() {
      }

      protected Bitmap doInBackground(final Integer... args) {
         Bitmap bitmap = null;
         int covImageId = args[0];
         if (covImageId > 0) {
            bitmap = Main.this.application.getDataImageHelper().getBitmap(covImageId);
         }
         return bitmap;
      }

      protected void onPostExecute(final Bitmap bitmap) {
         if (bitmap != null) {
            this.v.setImageBitmap(bitmap);
         }
      }
   }

   private class ResetCoverImagesTask extends AsyncTask<Void, String, Void> {
      private final ProgressDialog dialog = new ProgressDialog(Main.this);
      
      protected void onPreExecute() {
         this.dialog.setMessage("Resetting cover images, this could take a few minutes ...");
         this.dialog.show();
      }

      protected void onProgressUpdate(final String... args) {
         this.dialog.setMessage(args[0]);
      }
      
      protected Void doInBackground(final Void... args) {
         HashSet<Book> books = Main.this.application.getDataHelper().selectAllBooks();
         for (Book b : books) {
            if (Constants.LOCAL_LOGV) {
               Log.v(Constants.LOG_TAG, "resetting cover image for book - " + b.getTitle());
            }
            this.publishProgress("processing: " + b.getTitle());
            Main.this.application.getDataImageHelper().resetCoverImage(Main.this.application.getDataHelper(), "2", b);
         }
         return null;
      }

      protected void onPostExecute(final Void v) {         
         Main.this.bindBookList(DataHelper.ORDER_BY_TITLE_ASC);
         if (this.dialog.isShowing()) {
            this.dialog.dismiss();
         }
      }
   }
}
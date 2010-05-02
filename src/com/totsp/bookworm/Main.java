package com.totsp.bookworm;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

import com.totsp.bookworm.data.DataHelper;
import com.totsp.bookworm.model.Book;

import java.util.HashSet;

public class Main extends Activity {

   private static final int MENU_ABOUT = 0;
   private static final int MENU_PREFS = 1;
   private static final int MENU_BOOKADD = 2;
   private static final int MENU_SORT = 3;
   private static final int MENU_SEND = 4;
   private static final int MENU_MANAGE = 5;
   private static final int MENU_RESET_COVER_IMAGES = 6;

   private static final int MENU_CONTEXT_EDIT = 0;
   private static final int MENU_CONTEXT_DELETE = 1;

   BookWormApplication application;

   private SharedPreferences prefs;

   private ListView bookListView;
   private CursorAdapter adapter;
   private Cursor cursor;

   private Bitmap coverImageMissing;
   private Bitmap star0;
   private Bitmap star1;
   private Bitmap star2;
   private Bitmap star3;
   private Bitmap star4;
   private Bitmap star5;

   private ResetAllCoverImagesTask resetAllCoverImagesTask;
   
   private AlertDialog.Builder sortDialog;   

   @Override
   public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      this.setContentView(R.layout.main);
      this.application = (BookWormApplication) this.getApplication();
      this.prefs = PreferenceManager.getDefaultSharedPreferences(this);

      this.resetAllCoverImagesTask = null;

      this.coverImageMissing = BitmapFactory.decodeResource(this.getResources(), R.drawable.book_cover_missing);
      this.star0 = BitmapFactory.decodeResource(this.getResources(), R.drawable.star0);
      this.star1 = BitmapFactory.decodeResource(this.getResources(), R.drawable.star1);
      this.star2 = BitmapFactory.decodeResource(this.getResources(), R.drawable.star2);
      this.star3 = BitmapFactory.decodeResource(this.getResources(), R.drawable.star3);
      this.star4 = BitmapFactory.decodeResource(this.getResources(), R.drawable.star4);
      this.star5 = BitmapFactory.decodeResource(this.getResources(), R.drawable.star5);

      this.bookListView = (ListView) this.findViewById(R.id.booklistview);
      this.bookListView.setEmptyView(this.findViewById(R.id.empty));
      this.bookListView.setTextFilterEnabled(true);
      this.bookListView.setOnItemClickListener(new OnItemClickListener() {
         public void onItemClick(final AdapterView<?> parent, final View v, final int index, final long id) {
            Main.this.cursor.moveToPosition(index);
            // note - this is tricky, table doesn't have _id, but CursorAdapter requires it
            // in the query we used "book.bid as _id" so here we have to use _id too
            int bookId = Main.this.cursor.getInt(Main.this.cursor.getColumnIndex("_id"));
            Book book = Main.this.application.getDataHelper().selectBook(bookId);
            if (book != null) {
               if (Main.this.application.isDebugEnabled()) {
                  Log.d(Constants.LOG_TAG, "book selected - " + book.title);
               }
               Main.this.application.setSelectedBook(book);
               Main.this.startActivity(new Intent(Main.this, BookDetail.class));
            } else {
               Toast.makeText(Main.this, "Unrecoverable error selecting book", Toast.LENGTH_SHORT).show();
            }
         }
      });
      
      this.sortDialog = new AlertDialog.Builder(this);
      sortDialog.setTitle("Sort by");
      sortDialog.setItems(new CharSequence[] { "Title", "Author(s)", "Rating", "Read", "Publisher" }, new DialogInterface.OnClickListener() {
         public void onClick(DialogInterface d, int selected) {
            switch (selected) {
            case 0:
               Main.this.bindBookList(DataHelper.ORDER_BY_TITLE_ASC);
               Main.this.saveSortOrder(DataHelper.ORDER_BY_TITLE_ASC);
               break;
            case 1: 
               Main.this.bindBookList(DataHelper.ORDER_BY_AUTHORS_ASC);
               Main.this.saveSortOrder(DataHelper.ORDER_BY_AUTHORS_ASC);                  
               break;
            case 2:                  
               Main.this.bindBookList(DataHelper.ORDER_BY_RATING_DESC);
               Main.this.saveSortOrder(DataHelper.ORDER_BY_RATING_DESC);
               break;
            case 3:
               Main.this.bindBookList(DataHelper.ORDER_BY_READ_DESC);
               Main.this.saveSortOrder(DataHelper.ORDER_BY_READ_DESC);
               break;
            case 4:
               Main.this.bindBookList(DataHelper.ORDER_BY_PUB_ASC);
               Main.this.saveSortOrder(DataHelper.ORDER_BY_PUB_ASC);
               break;
            }
         }
      });
      this.sortDialog.create();      
      
      this.registerForContextMenu(this.bookListView);

      String sortOrder = this.prefs.getString(Constants.DEFAULT_SORT_ORDER, DataHelper.ORDER_BY_TITLE_ASC);
      this.bindBookList(sortOrder);
   }

   @Override
   public void onStart() {
      super.onStart();
   }

   @Override
   public boolean onCreateOptionsMenu(final Menu menu) {
      menu.add(0, Main.MENU_SORT, 1, "Sort Books").setIcon(android.R.drawable.ic_menu_sort_by_size);
      menu.add(0, Main.MENU_BOOKADD, 2, "Add Book").setIcon(android.R.drawable.ic_menu_add);
      menu.add(0, Main.MENU_SEND, 3, "Send Books").setIcon(android.R.drawable.ic_menu_send);
      menu.add(0, Main.MENU_ABOUT, 4, "About").setIcon(android.R.drawable.ic_menu_help);
      menu.add(0, Main.MENU_PREFS, 5, "Prefs").setIcon(android.R.drawable.ic_menu_preferences);
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
      case MENU_SORT:         
         this.sortDialog.show();
         return true;
      case MENU_SEND:
         Toast.makeText(this, "TODO send as CSV or HTML or TEXT", Toast.LENGTH_SHORT).show();         
         return true;
      case MENU_MANAGE:
         this.startActivity(new Intent(Main.this, ManageData.class));
         return true;
      case MENU_RESET_COVER_IMAGES:
         new AlertDialog.Builder(Main.this).setTitle("Reset all cover images?").setMessage(
                  "This will use the network to try to find and replace all cover images.").setPositiveButton(
                  "Yes, I'm Sure", new DialogInterface.OnClickListener() {
                     public void onClick(final DialogInterface d, final int i) {
                        Main.this.resetAllCoverImagesTask = new ResetAllCoverImagesTask();
                        Main.this.resetAllCoverImagesTask.execute();
                     }
                  }).setNegativeButton("No, Cancel", new DialogInterface.OnClickListener() {
            public void onClick(final DialogInterface d, final int i) {
            }
         }).show();
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
      long bookId = info.id;
      final Book b = this.application.getDataHelper().selectBook(bookId);
      switch (item.getItemId()) {
      case MENU_CONTEXT_EDIT:
         Main.this.application.setSelectedBook(b);
         Main.this.startActivity(new Intent(Main.this, BookForm.class));
         return true;
      case MENU_CONTEXT_DELETE:
         new AlertDialog.Builder(Main.this).setTitle("Delete book?").setMessage(b.title).setPositiveButton(
                  "Yes, I'm Sure", new DialogInterface.OnClickListener() {
                     public void onClick(final DialogInterface d, final int i) {
                        Main.this.application.getDataImageHelper().deleteBitmapSourceFile(b.title, b.id);
                        Main.this.application.getDataHelper().deleteBook(b.id);
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
   public void onPause() {
      if ((this.resetAllCoverImagesTask != null) && this.resetAllCoverImagesTask.dialog.isShowing()) {
         this.resetAllCoverImagesTask.dialog.dismiss();
      }
      super.onPause();
   }

   // go to home on back from Main (avoid loop with BookEntrySearch which comes here)
   @Override
   public boolean onKeyDown(final int keyCode, final KeyEvent event) {
      if ((keyCode == KeyEvent.KEYCODE_BACK) && (event.getRepeatCount() == 0)) {
         Intent intent = new Intent(Intent.ACTION_MAIN);
         intent.addCategory(Intent.CATEGORY_HOME);
         this.startActivity(intent);
         return true;
      }
      return super.onKeyDown(keyCode, event);
   }

   private void bindBookList(final String orderBy) {
      this.cursor = this.application.getDataHelper().getSelectBookJoinCursor(orderBy);
      if ((this.cursor != null) && (this.cursor.getCount() > 0)) {
         this.startManagingCursor(this.cursor);
         this.adapter = new BookCursorAdapter(this.cursor);
         this.bookListView.setAdapter(this.adapter);
      }
   }

   private void saveSortOrder(final String order) {
      Editor editor = this.prefs.edit();
      editor.putString(Constants.DEFAULT_SORT_ORDER, order);
      editor.commit();
   }

   // static and package access as an Android optimization (used in inner class)
   static class ViewHolder {
      ImageView coverImage;
      ImageView ratingImage;
      TextView textAbove;
      TextView textBelow;
      CheckBox readStatus;
   }

   //
   // BookCursorAdapter
   //
   private class BookCursorAdapter extends CursorAdapter implements Filterable {

      LayoutInflater vi = (LayoutInflater) Main.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

      public BookCursorAdapter(final Cursor c) {
         super(Main.this, c, true);
      }

      @Override
      public void bindView(final View v, final Context context, final Cursor c) {
         this.populateView(v, c);
      }

      @Override
      public View newView(final Context context, final Cursor c, final ViewGroup parent) {
         // use ViewHolder pattern to avoid extra trips to findViewById
         View v = this.vi.inflate(R.layout.list_items_item, parent, false);
         ViewHolder holder = new ViewHolder();
         holder.coverImage = (ImageView) v.findViewById(R.id.list_items_item_image);
         holder.ratingImage = (ImageView) v.findViewById(R.id.list_items_item_rating_image);
         holder.textAbove = (TextView) v.findViewById(R.id.list_items_item_textabove);
         ;
         holder.textBelow = (TextView) v.findViewById(R.id.list_items_item_textbelow);
         holder.readStatus = (CheckBox) v.findViewById(R.id.list_items_item_read_status);
         v.setTag(holder);
         this.populateView(v, c);
         return v;
      }

      private void populateView(final View v, final Cursor c) {
         // use ViewHolder pattern to avoid extra trips to findViewById
         ViewHolder holder = (ViewHolder) v.getTag();

         if ((c != null) && !c.isClosed()) {
            long id = c.getLong(0);           
            
            // TODO investigate, may need to file Android/SQLite bug
            // Log.i(Constants.LOG_TAG, "COLUMN INDEX rating - " + c.getColumnIndex(DataConstants.RATING));
            // as soon as query has group by or group_concat the getColumnIndex fails? (explicit works)
            /*
            bid = 0
            tit = 1
            subtit = 2
            pub = 3
            datepub = 4
            format = 5
            rstat = 6
            rat = 7
            blurb = 8
            authors = 9
             */
            
            int rating = c.getInt(7);
            int readStatus = c.getInt(6);
            String title = c.getString(1);
            String subTitle = c.getString(2);

            if (Main.this.application.isDebugEnabled()) {
               Log.d(Constants.LOG_TAG, "book (id|title) from cursor - " + id + "|" + title);
            }

            ImageView coverImage = holder.coverImage;
            Bitmap coverImageBitmap = Main.this.application.getDataImageHelper().retrieveBitmap(title, id, true);
            if (coverImageBitmap != null) {
               coverImage.setImageBitmap(coverImageBitmap);
            } else {
               coverImage.setImageBitmap(Main.this.coverImageMissing);
            }

            ImageView ratingImage = holder.ratingImage;
            switch (rating) {
            case 0:
               ratingImage.setImageBitmap(Main.this.star0);
               break;
            case 1:
               ratingImage.setImageBitmap(Main.this.star1);
               break;
            case 2:
               ratingImage.setImageBitmap(Main.this.star2);
               break;
            case 3:
               ratingImage.setImageBitmap(Main.this.star3);
               break;
            case 4:
               ratingImage.setImageBitmap(Main.this.star4);
               break;
            case 5:
               ratingImage.setImageBitmap(Main.this.star5);
               break;
            }

            holder.textAbove.setText(title);
            holder.textBelow.setText(subTitle);

            if (readStatus == 1) {
               holder.readStatus.setChecked(true);
            } else {
               holder.readStatus.setChecked(false);
            }
         }
      }
   }

   private class ResetAllCoverImagesTask extends AsyncTask<Void, String, Void> {
      private final ProgressDialog dialog = new ProgressDialog(Main.this);

      protected void onPreExecute() {
         this.dialog.setMessage("Resetting cover images, this could take a few minutes ...");
         this.dialog.show();
      }

      protected void onProgressUpdate(final String... args) {
         this.dialog.setMessage(args[0]);
      }

      protected Void doInBackground(final Void... args) {
         Main.this.application.getDataImageHelper().clearAllBitmapSourceFiles();
         HashSet<Book> books = Main.this.application.getDataHelper().selectAllBooks();
         for (Book b : books) {
            if (Main.this.application.isDebugEnabled()) {
               Log.d(Constants.LOG_TAG, "resetting cover image for book - " + b.title);
            }
            this.publishProgress("processing: " + b.title);
            Main.this.application.getDataImageHelper().resetCoverImage(Main.this.application.getDataHelper(), b);
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
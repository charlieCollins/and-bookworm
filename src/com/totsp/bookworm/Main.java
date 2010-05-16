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
import android.widget.FilterQueryProvider;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

import com.totsp.bookworm.data.DataHelper;
import com.totsp.bookworm.model.Book;
import com.totsp.bookworm.model.BookListStats;
import com.totsp.bookworm.util.AuthorsStringUtil;

import java.util.ArrayList;

public class Main extends Activity {

   private static final int MENU_SORT = 1;
   private static final int MENU_BOOKADD = 2;
   private static final int MENU_STATS = 3;
   private static final int MENU_ABOUT = 4;
   private static final int MENU_PREFS = 5;
   // after first 5 next items go in "more" selection
   ///private static final int MENU_SEND = 6;
   private static final int MENU_MANAGE = 7;
   private static final int MENU_RESET_COVER_IMAGES = 8;

   private static final int MENU_CONTEXT_EDIT = 0;
   private static final int MENU_CONTEXT_DELETE = 1;

   BookWormApplication application;
   SharedPreferences prefs;

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
   private AlertDialog.Builder statsDialog;

   @Override
   public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      ///Debug.startMethodTracing("BookWorm");

      this.setContentView(R.layout.main);
      this.application = (BookWormApplication) this.getApplication();
      this.prefs = PreferenceManager.getDefaultSharedPreferences(this);

      this.resetAllCoverImagesTask = null;

      this.coverImageMissing =
               BitmapFactory.decodeResource(this.getResources(),
                        R.drawable.book_cover_missing);
      this.star0 =
               BitmapFactory.decodeResource(this.getResources(),
                        R.drawable.star0);
      this.star1 =
               BitmapFactory.decodeResource(this.getResources(),
                        R.drawable.star1);
      this.star2 =
               BitmapFactory.decodeResource(this.getResources(),
                        R.drawable.star2);
      this.star3 =
               BitmapFactory.decodeResource(this.getResources(),
                        R.drawable.star3);
      this.star4 =
               BitmapFactory.decodeResource(this.getResources(),
                        R.drawable.star4);
      this.star5 =
               BitmapFactory.decodeResource(this.getResources(),
                        R.drawable.star5);

      this.bookListView = (ListView) this.findViewById(R.id.booklistview);
      this.bookListView.setEmptyView(this.findViewById(R.id.empty));
      this.bookListView.setTextFilterEnabled(true);
      this.bookListView.setOnItemClickListener(new OnItemClickListener() {
         public void onItemClick(final AdapterView<?> parent, final View v,
                  final int index, final long id) {
            Main.this.cursor.moveToPosition(index);
            // note - this is tricky, table doesn't have _id, but CursorAdapter requires it
            // in the query we used "book.bid as _id" so here we have to use _id too
            int bookId =
                     Main.this.cursor.getInt(Main.this.cursor
                              .getColumnIndex("_id"));
            Book book = Main.this.application.dataHelper.selectBook(bookId);
            if (book != null) {
               if (Main.this.application.debugEnabled) {
                  Log.d(Constants.LOG_TAG, "book selected - " + book.title);
               }
               Main.this.application.lastMainListPosition = index;
               Main.this.application.selectedBook = book;
               Main.this.startActivity(new Intent(Main.this, BookDetail.class));
            } else {
               Toast.makeText(Main.this,
                        Main.this.getString(R.string.msgSelectBookError),
                        Toast.LENGTH_SHORT).show();
            }
         }
      });

      this.sortDialog = new AlertDialog.Builder(this);
      this.sortDialog.setTitle(this.getString(R.string.btnSortBy));
      this.sortDialog.setItems(new CharSequence[] {
               this.getString(R.string.labelTitle),
               this.getString(R.string.labelAuthorsShort),
               this.getString(R.string.labelRating),
               this.getString(R.string.labelReadstatus),
               this.getString(R.string.labelSubject),
               this.getString(R.string.labelDatepub),
               this.getString(R.string.labelPublisher) },
               new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface d, int selected) {
                     switch (selected) {
                        case 0:
                           Main.this
                                    .saveSortOrder(DataHelper.ORDER_BY_TITLE_ASC);
                           break;
                        case 1:
                           Main.this
                                    .saveSortOrder(DataHelper.ORDER_BY_AUTHORS_ASC);
                           break;
                        case 2:
                           Main.this
                                    .saveSortOrder(DataHelper.ORDER_BY_RATING_DESC);
                           break;
                        case 3:
                           Main.this
                                    .saveSortOrder(DataHelper.ORDER_BY_READ_DESC);
                           break;
                        case 4:
                           Main.this
                                    .saveSortOrder(DataHelper.ORDER_BY_SUBJECT_DESC);
                           break;
                        case 5:
                           Main.this
                                    .saveSortOrder(DataHelper.ORDER_BY_DATE_PUB_DESC);
                           break;
                        case 6:
                           Main.this.saveSortOrder(DataHelper.ORDER_BY_PUB_ASC);
                           break;
                     }
                     Main.this.application.lastMainListPosition = 0;
                     Main.this.bindBookList();
                  }
               });
      this.sortDialog.create();

      this.statsDialog = new AlertDialog.Builder(this);
      this.statsDialog.setTitle(this.getString(R.string.msgBookListStats));
      this.statsDialog.setNeutralButton(this.getString(R.string.btnDismiss),
               new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface d, int i) {
                  };
               });
      this.statsDialog.create();

      this.registerForContextMenu(this.bookListView);

      this.bindBookList();
   }

   @Override
   public void onStart() {
      super.onStart();
   }

   @Override
   public boolean onCreateOptionsMenu(final Menu menu) {
      menu.add(0, Main.MENU_SORT, 1, this.getString(R.string.menuSortBooks))
               .setIcon(android.R.drawable.ic_menu_sort_by_size);
      menu.add(0, Main.MENU_BOOKADD, 2, this.getString(R.string.menuAddBook))
               .setIcon(android.R.drawable.ic_menu_add);
      menu.add(0, Main.MENU_STATS, 3, this.getString(R.string.menuListStats))
               .setIcon(android.R.drawable.ic_menu_info_details);
      menu.add(0, Main.MENU_ABOUT, 4, this.getString(R.string.menuAbout))
               .setIcon(android.R.drawable.ic_menu_help);
      menu.add(0, Main.MENU_PREFS, 5, this.getString(R.string.menuPrefs))
               .setIcon(android.R.drawable.ic_menu_preferences);
      ///menu.add(0, Main.MENU_SEND, 6, "Send Book List").setIcon(android.R.drawable.ic_menu_send);
      menu.add(0, Main.MENU_MANAGE, 7, this.getString(R.string.menuManageData))
               .setIcon(android.R.drawable.ic_menu_manage);
      menu.add(0, Main.MENU_RESET_COVER_IMAGES, 8,
               this.getString(R.string.menuResetCoverImages)).setIcon(
               android.R.drawable.ic_menu_gallery);
      return super.onCreateOptionsMenu(menu);
   }

   @Override
   public boolean onOptionsItemSelected(final MenuItem item) {
      switch (item.getItemId()) {
         case MENU_SORT:
            this.sortDialog.show();
            return true;
         case MENU_BOOKADD:
            this.startActivity(new Intent(Main.this, BookAdd.class));
            return true;
         case MENU_STATS:
            BookListStats stats = this.application.dataHelper.getStats();
            // TODO this stringbuilder is NOT i18n'd
            // use string.format and resource strings
            StringBuilder sb = new StringBuilder();
            sb.append("Total books: " + stats.totalBooks + "\n");
            sb.append("Read books: " + stats.readBooks + "\n");
            sb.append("5 star books: " + stats.fiveStarBooks + "\n");
            sb.append("4 star books: " + stats.fourStarBooks + "\n");
            sb.append("3 star books: " + stats.threeStarBooks + "\n");
            sb.append("2 star books: " + stats.twoStarBooks + "\n");
            sb.append("1 star books: " + stats.oneStarBooks + "\n");
            sb.append("Unrated books: "
                     + (stats.totalBooks - (stats.fiveStarBooks
                              + stats.fourStarBooks + stats.threeStarBooks
                              + stats.twoStarBooks + stats.oneStarBooks))
                     + "\n");
            this.statsDialog.setMessage(sb.toString());
            this.statsDialog.show();
            return true;
         case MENU_ABOUT:
            this.startActivity(new Intent(Main.this, About.class));
            return true;
         case MENU_PREFS:
            this.startActivity(new Intent(Main.this, Preferences.class));
            return true;
            // below first 5 are "more" options
            /*
            case MENU_SEND:
            Toast.makeText(this, "TODO send as CSV or HTML or TEXT", Toast.LENGTH_SHORT).show();
            return true;
            */
         case MENU_MANAGE:
            this.startActivity(new Intent(Main.this, ManageData.class));
            return true;
         case MENU_RESET_COVER_IMAGES:
            new AlertDialog.Builder(Main.this)
                     .setTitle(
                              Main.this
                                       .getString(R.string.msgResetAllCoverImages))
                     .setMessage(
                              Main.this
                                       .getString(R.string.msgResetAllCoverImagesExplain))
                     .setPositiveButton(Main.this.getString(R.string.btnYes),
                              new DialogInterface.OnClickListener() {
                                 public void onClick(final DialogInterface d,
                                          final int i) {
                                    Main.this.resetAllCoverImagesTask =
                                             new ResetAllCoverImagesTask();
                                    Main.this.resetAllCoverImagesTask.execute();
                                 }
                              }).setNegativeButton(
                              Main.this.getString(R.string.btnNo),
                              new DialogInterface.OnClickListener() {
                                 public void onClick(final DialogInterface d,
                                          final int i) {
                                 }
                              }).show();
            return true;
         default:
            return super.onOptionsItemSelected(item);
      }
   }

   @Override
   public void onCreateContextMenu(final ContextMenu menu, final View v,
            final ContextMenuInfo menuInfo) {
      super.onCreateContextMenu(menu, v, menuInfo);
      menu.add(0, Main.MENU_CONTEXT_EDIT, 0, this
               .getString(R.string.menuEditBook));
      menu.add(0, Main.MENU_CONTEXT_DELETE, 1, this
               .getString(R.string.menuDeleteBook));
      menu.setHeaderTitle("Action");
   }

   @Override
   public boolean onContextItemSelected(final MenuItem item) {
      AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
      long bookId = info.id;
      final Book b = this.application.dataHelper.selectBook(bookId);
      Main.this.application.lastMainListPosition = info.position;
      switch (item.getItemId()) {
         case MENU_CONTEXT_EDIT:
            Main.this.application.selectedBook = b;
            Main.this.startActivity(new Intent(Main.this, BookForm.class));
            return true;
         case MENU_CONTEXT_DELETE:
            new AlertDialog.Builder(Main.this).setTitle(
                     Main.this.getString(R.string.menuDeleteBook)).setMessage(
                     b.title).setPositiveButton(
                     Main.this.getString(R.string.btnYes),
                     new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface d, final int i) {
                           Main.this.application.dataImageHelper
                                    .deleteBitmapSourceFile(b.title, b.id);
                           Main.this.application.dataHelper.deleteBook(b.id);
                           Main.this.startActivity(Main.this.getIntent());
                        }
                     }).setNegativeButton(Main.this.getString(R.string.btnNo),
                     new DialogInterface.OnClickListener() {
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
      if ((this.resetAllCoverImagesTask != null)
               && this.resetAllCoverImagesTask.dialog.isShowing()) {
         this.resetAllCoverImagesTask.dialog.dismiss();
      }
      ///Debug.stopMethodTracing();
      super.onPause();
   }

   @Override
   public void onDestroy() {
      super.onDestroy();
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

   private void bindBookList() {
      String orderBy =
               this.prefs.getString(Constants.DEFAULT_SORT_ORDER,
                        DataHelper.ORDER_BY_TITLE_ASC);
      this.cursor =
               this.application.dataHelper.getSelectBookJoinCursor(orderBy,
                        null);
      if ((this.cursor != null) && (this.cursor.getCount() > 0)) {
         this.startManagingCursor(this.cursor);
         this.adapter = new BookCursorAdapter(this.cursor);
         this.bookListView.setAdapter(this.adapter);
         int lastMainPos = this.application.lastMainListPosition;
         if ((lastMainPos - 1) < this.adapter.getCount()) {
            this.bookListView
                     .setSelection(this.application.lastMainListPosition - 1);
         }
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
      TextView text1;
      TextView text2;
      CheckBox readStatus;
   }

   //
   // BookCursorAdapter
   //
   private class BookCursorAdapter extends CursorAdapter implements
            FilterQueryProvider {

      LayoutInflater vi =
               (LayoutInflater) Main.this
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

      public BookCursorAdapter(final Cursor c) {
         super(Main.this, c, true);
         this.setFilterQueryProvider(this);
      }

      // FilterQueryProvider impl
      public Cursor runQuery(CharSequence constraint) {
         ///Log.i(Constants.LOG_TAG, "RUN QUERY - " + constraint);
         Cursor c = null;
         if ((constraint == null) || (constraint.length() == 0)) {
            c = this.getCursor();
         } else {
            String pattern = "'%" + constraint + "%'";
            String orderBy =
                     Main.this.prefs.getString(Constants.DEFAULT_SORT_ORDER,
                              DataHelper.ORDER_BY_TITLE_ASC);
            c =
                     Main.this.application.dataHelper.getSelectBookJoinCursor(
                              orderBy, "where book.tit like " + pattern);
         }
         Main.this.cursor = c;
         return c;
      }

      @Override
      public void bindView(final View v, final Context context, final Cursor c) {
         this.populateView(v, c);
      }

      @Override
      public View newView(final Context context, final Cursor c,
               final ViewGroup parent) {
         // use ViewHolder pattern to avoid extra trips to findViewById
         View v = this.vi.inflate(R.layout.list_items_item, parent, false);
         ViewHolder holder = new ViewHolder();
         holder.coverImage =
                  (ImageView) v.findViewById(R.id.list_items_item_image);
         holder.ratingImage =
                  (ImageView) v.findViewById(R.id.list_items_item_rating_image);
         holder.text1 = (TextView) v.findViewById(R.id.list_items_item_text1);
         holder.text2 = (TextView) v.findViewById(R.id.list_items_item_text2);
         holder.readStatus =
                  (CheckBox) v.findViewById(R.id.list_items_item_read_status);
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
            //String subTitle = c.getString(2);
            String authors = c.getString(9);

            if (Main.this.application.debugEnabled) {
               Log.d(Constants.LOG_TAG, "book (id|title) from cursor - " + id
                        + "|" + title);
            }

            ImageView coverImage = holder.coverImage;
            Bitmap coverImageBitmap =
                     Main.this.application.dataImageHelper.retrieveBitmap(
                              title, id, true);
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

            holder.text1.setText(title);
            holder.text2.setText(AuthorsStringUtil
                     .addSpacesToCSVString(authors));

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

      @Override
      protected void onPreExecute() {
         this.dialog.setMessage(Main.this
                  .getString(R.string.msgResetCoverImagesWarnTime));
         this.dialog.show();
      }

      @Override
      protected void onProgressUpdate(final String... args) {
         this.dialog.setMessage(args[0]);
      }

      @Override
      protected Void doInBackground(final Void... args) {
         Main.this.application.dataImageHelper.clearAllBitmapSourceFiles();
         ArrayList<Book> books =
                  Main.this.application.dataHelper.selectAllBooks();
         for (int i = 0; i < books.size(); i++) {
            Book b = books.get(i);
            if (Main.this.application.debugEnabled) {
               Log.d(Constants.LOG_TAG, "resetting cover image for book - "
                        + b.title);
            }
            this.publishProgress(String.format(Main.this.getString(
                     R.string.msgProcessingBookX, b.title)));
            Main.this.application.dataImageHelper.resetCoverImage(
                     Main.this.application.dataHelper, b);
         }
         return null;
      }

      @Override
      protected void onPostExecute(final Void v) {
         Main.this.bindBookList();
         if (this.dialog.isShowing()) {
            this.dialog.dismiss();
         }
      }
   }
}
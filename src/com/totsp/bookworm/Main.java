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
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
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

import com.totsp.bookworm.data.DataConstants;
import com.totsp.bookworm.data.DataCsvExporter;
import com.totsp.bookworm.model.Book;
import com.totsp.bookworm.model.BookListStats;
import com.totsp.bookworm.util.StringUtil;

import java.io.File;
import java.util.ArrayList;

public class Main extends Activity {

   private static final int MENU_SORT = 1;
   private static final int MENU_BOOKADD = 2;
   private static final int MENU_STATS = 3;
   private static final int MENU_ABOUT = 4;
   private static final int MENU_PREFS = 5;
   // after first 5 next items go in "more" selection
   private static final int MENU_EXPORT_CSV = 6;
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
      // /Debug.startMethodTracing("BookWorm");
      setContentView(R.layout.main);
      application = (BookWormApplication) getApplication();
      prefs = PreferenceManager.getDefaultSharedPreferences(this);

      resetAllCoverImagesTask = null;

      coverImageMissing = BitmapFactory.decodeResource(this.getResources(), R.drawable.book_cover_missing);
      star0 = BitmapFactory.decodeResource(getResources(), R.drawable.star0);
      star1 = BitmapFactory.decodeResource(getResources(), R.drawable.star1);
      star2 = BitmapFactory.decodeResource(getResources(), R.drawable.star2);
      star3 = BitmapFactory.decodeResource(getResources(), R.drawable.star3);
      star4 = BitmapFactory.decodeResource(getResources(), R.drawable.star4);
      star5 = BitmapFactory.decodeResource(getResources(), R.drawable.star5);

      bookListView = (ListView) findViewById(R.id.booklistview);
      bookListView.setEmptyView(findViewById(R.id.empty));
      bookListView.setTextFilterEnabled(true);
      bookListView.setOnItemClickListener(new OnItemClickListener() {
         public void onItemClick(final AdapterView<?> parent, final View v, final int index, final long id) {
            Main.this.cursor.moveToPosition(index);
            // note - this is tricky, table doesn't have _id, but
            // CursorAdapter requires it
            // in the query we used "book.bid as _id" so here we have to use
            // _id too
            int bookId = Main.this.cursor.getInt(Main.this.cursor.getColumnIndex("_id"));
            Book book = Main.this.application.dataManager.selectBook(bookId);
            if (book != null) {
               if (Main.this.application.debugEnabled) {
                  Log.d(Constants.LOG_TAG, "book selected - " + book.title);
               }
               Main.this.application.lastMainListPosition = index;
               Main.this.application.selectedBook = book;
               Main.this.startActivity(new Intent(Main.this, BookDetail.class));
            } else {
               Toast.makeText(Main.this, Main.this.getString(R.string.msgSelectBookError), Toast.LENGTH_SHORT).show();
            }
         }
      });

      registerForContextMenu(bookListView);

      bindAdapter();

      setupDialogs();
   }

   @Override
   public void onStart() {
      super.onStart();
   }

   @Override
   public boolean onCreateOptionsMenu(final Menu menu) {
      menu.add(0, Main.MENU_SORT, 1, getString(R.string.menuSortBooks))
               .setIcon(android.R.drawable.ic_menu_sort_by_size);
      menu.add(0, Main.MENU_BOOKADD, 2, getString(R.string.menuAddBook)).setIcon(android.R.drawable.ic_menu_add);
      menu.add(0, Main.MENU_STATS, 3, getString(R.string.menuListStats)).setIcon(
               android.R.drawable.ic_menu_info_details);
      menu.add(0, Main.MENU_ABOUT, 4, getString(R.string.menuAbout)).setIcon(android.R.drawable.ic_menu_help);
      menu.add(0, Main.MENU_PREFS, 5, getString(R.string.menuPrefs)).setIcon(android.R.drawable.ic_menu_preferences);
      menu.add(0, Main.MENU_EXPORT_CSV, 6, getString(R.string.menuExportCSV)).setIcon(android.R.drawable.ic_menu_send);
      menu.add(0, Main.MENU_MANAGE, 7, getString(R.string.menuManageData)).setIcon(android.R.drawable.ic_menu_manage);
      menu.add(0, Main.MENU_RESET_COVER_IMAGES, 8, getString(R.string.menuResetCoverImages)).setIcon(
               android.R.drawable.ic_menu_gallery);
      return super.onCreateOptionsMenu(menu);
   }

   @Override
   public boolean onOptionsItemSelected(final MenuItem item) {
      switch (item.getItemId()) {
         case MENU_SORT:
            sortDialog.show();
            return true;
         case MENU_BOOKADD:
            startActivity(new Intent(Main.this, BookAdd.class));
            return true;
         case MENU_STATS:
            BookListStats stats = application.dataManager.getStats();
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
                     + (stats.totalBooks - (stats.fiveStarBooks + stats.fourStarBooks + stats.threeStarBooks
                              + stats.twoStarBooks + stats.oneStarBooks)) + "\n");
            statsDialog.setMessage(sb.toString());
            statsDialog.show();
            return true;
         case MENU_ABOUT:
            startActivity(new Intent(Main.this, About.class));
            return true;
         case MENU_PREFS:
            startActivity(new Intent(Main.this, Preferences.class));
            return true;
         case MENU_EXPORT_CSV:
            new AlertDialog.Builder(Main.this).setMessage(R.string.labelExportCSV).setPositiveButton(
                     Main.this.getString(R.string.btnYes), new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface arg0, final int arg1) {
                           DataCsvExporter exporter = new DataCsvExporter();
                           // TODO select the current FILTERED list of books?
                           exporter.export(application.dataManager.selectAllBooks());
                           // send email using chooser, with CSV as attach
                           Intent sendIntent = new Intent(Intent.ACTION_SEND);
                           sendIntent.setType("text/csv");
                           sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"
                                    + DataConstants.EXTERNAL_DATA_PATH + File.separator
                                    + DataCsvExporter.EXPORT_FILENAME));
                           sendIntent.putExtra(Intent.EXTRA_SUBJECT, "BookWorm CSV Export");
                           sendIntent.putExtra(Intent.EXTRA_TEXT, "CSV export attached.");
                           startActivity(Intent.createChooser(sendIntent, "Email:"));
                        }
                     }).setNegativeButton(Main.this.getString(R.string.btnNo), new DialogInterface.OnClickListener() {
               public void onClick(final DialogInterface arg0, final int arg1) {
               }
            }).show();
            return true;
         case MENU_MANAGE:
            startActivity(new Intent(Main.this, ManageData.class));
            return true;
         case MENU_RESET_COVER_IMAGES:
            new AlertDialog.Builder(Main.this).setTitle(Main.this.getString(R.string.msgResetAllCoverImages))
                     .setMessage(Main.this.getString(R.string.msgResetAllCoverImagesExplain)).setPositiveButton(
                              Main.this.getString(R.string.btnYes), new DialogInterface.OnClickListener() {
                                 public void onClick(final DialogInterface d, final int i) {
                                    Main.this.resetAllCoverImagesTask = new ResetAllCoverImagesTask();
                                    Main.this.resetAllCoverImagesTask.execute();
                                 }
                              }).setNegativeButton(Main.this.getString(R.string.btnNo),
                              new DialogInterface.OnClickListener() {
                                 public void onClick(final DialogInterface d, final int i) {
                                 }
                              }).show();
            return true;
         default:
            return super.onOptionsItemSelected(item);
      }
   }

   @Override
   public void onCreateContextMenu(final ContextMenu menu, final View v, final ContextMenuInfo menuInfo) {
      super.onCreateContextMenu(menu, v, menuInfo);
      menu.add(0, Main.MENU_CONTEXT_EDIT, 0, this.getString(R.string.menuEditBook));
      menu.add(0, Main.MENU_CONTEXT_DELETE, 1, this.getString(R.string.menuDeleteBook));
      menu.setHeaderTitle("Action");
   }

   @Override
   public boolean onContextItemSelected(final MenuItem item) {
      AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
      long bookId = info.id;
      final Book b = application.dataManager.selectBook(bookId);
      Main.this.application.lastMainListPosition = info.position;
      switch (item.getItemId()) {
         case MENU_CONTEXT_EDIT:
            Main.this.application.selectedBook = b;
            Main.this.startActivity(new Intent(Main.this, BookForm.class));
            return true;
         case MENU_CONTEXT_DELETE:
            new AlertDialog.Builder(Main.this).setTitle(Main.this.getString(R.string.menuDeleteBook)).setMessage(
                     b.title).setPositiveButton(Main.this.getString(R.string.btnYes),
                     new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface d, final int i) {
                           Main.this.application.imageManager.deleteBitmapSourceFile(b.title, b.id);
                           Main.this.application.dataManager.deleteBook(b.id);
                           Main.this.startActivity(Main.this.getIntent());
                        }
                     }).setNegativeButton(Main.this.getString(R.string.btnNo), new DialogInterface.OnClickListener() {
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
      if ((resetAllCoverImagesTask != null) && resetAllCoverImagesTask.dialog.isShowing()) {
         resetAllCoverImagesTask.dialog.dismiss();
      }
      // /Debug.stopMethodTracing();		
      super.onPause();
   }

   @Override
   public void onDestroy() {
      super.onDestroy();
   }

   // go to home on back from Main 
   // (avoid loop with BookEntrySearch which comes here)
   @Override
   public boolean onKeyDown(final int keyCode, final KeyEvent event) {
      if ((keyCode == KeyEvent.KEYCODE_BACK) && (event.getRepeatCount() == 0)) {
         Intent intent = new Intent(Intent.ACTION_MAIN);
         intent.addCategory(Intent.CATEGORY_HOME);
         startActivity(intent);
         return true;
      }
      return super.onKeyDown(keyCode, event);
   }

   private void bindAdapter() {
      // bind bookListView and adapter
      String orderBy = prefs.getString(Constants.DEFAULT_SORT_ORDER, DataConstants.ORDER_BY_TITLE_ASC);
      cursor = application.dataManager.getBookCursor(orderBy, null);
      if ((cursor != null) && (cursor.getCount() > 0)) {
         startManagingCursor(cursor);
         adapter = new BookCursorAdapter(cursor);
         bookListView.setAdapter(adapter);
         int lastMainPos = application.lastMainListPosition;
         if ((lastMainPos - 1) < adapter.getCount()) {
            bookListView.setSelection(application.lastMainListPosition - 1);
         }
      }
   }

   private void setupDialogs() {
      sortDialog = new AlertDialog.Builder(this);
      sortDialog.setTitle(getString(R.string.btnSortBy));
      sortDialog.setItems(new CharSequence[] { getString(R.string.labelTitle), getString(R.string.labelAuthorsShort),
               getString(R.string.labelRating), getString(R.string.labelReadstatus), getString(R.string.labelSubject),
               getString(R.string.labelDatepub), getString(R.string.labelPublisher) },
               new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface d, int selected) {
                     switch (selected) {
                        case 0:
                           Main.this.saveSortOrder(DataConstants.ORDER_BY_TITLE_ASC);
                           break;
                        case 1:
                           Main.this.saveSortOrder(DataConstants.ORDER_BY_AUTHORS_ASC);
                           break;
                        case 2:
                           Main.this.saveSortOrder(DataConstants.ORDER_BY_RATING_DESC);
                           break;
                        case 3:
                           Main.this.saveSortOrder(DataConstants.ORDER_BY_READ_DESC);
                           break;
                        case 4:
                           Main.this.saveSortOrder(DataConstants.ORDER_BY_SUBJECT_ASC);
                           break;
                        case 5:
                           Main.this.saveSortOrder(DataConstants.ORDER_BY_DATE_PUB_DESC);
                           break;
                        case 6:
                           Main.this.saveSortOrder(DataConstants.ORDER_BY_PUB_ASC);
                           break;
                     }
                     Main.this.application.lastMainListPosition = 0;
                     //Main.this.adapter.notifyDataSetChanged();
                     // TODO notifyDataSetChanged doesn't cut it, sorts underlying collection but doesn't update view
                     // need to research (shouldn't have to re-bind the entire adapter)
                     Main.this.bindAdapter();
                  }
               });
      sortDialog.create();

      statsDialog = new AlertDialog.Builder(this);
      statsDialog.setTitle(getString(R.string.msgBookListStats));
      statsDialog.setNeutralButton(getString(R.string.btnDismiss), new DialogInterface.OnClickListener() {
         public void onClick(DialogInterface d, int i) {
         };
      });
      statsDialog.create();
   }

   private void saveSortOrder(final String order) {
      Editor editor = prefs.edit();
      editor.putString(Constants.DEFAULT_SORT_ORDER, order);
      editor.commit();
   }

   // static and package access as an Android optimization 
   // (used in inner class)
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
   private class BookCursorAdapter extends CursorAdapter implements FilterQueryProvider {

      LayoutInflater vi = (LayoutInflater) Main.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

      public BookCursorAdapter(final Cursor c) {
         super(Main.this, c, true);
         setFilterQueryProvider(this);
      }

      // FilterQueryProvider impl
      public Cursor runQuery(CharSequence constraint) {
         Cursor c = null;
         if ((constraint == null) || (constraint.length() == 0)) {
            c = getCursor();
         } else {
            String pattern = "'%" + constraint + "%'";
            String orderBy = Main.this.prefs.getString(Constants.DEFAULT_SORT_ORDER, DataConstants.ORDER_BY_TITLE_ASC);
            c = Main.this.application.dataManager.getBookCursor(orderBy, "where book.tit like " + pattern);
         }
         Main.this.cursor = c;
         return c;
      }

      @Override
      public void bindView(final View v, final Context context, final Cursor c) {
         populateView(v, c);
      }

      @Override
      public View newView(final Context context, final Cursor c, final ViewGroup parent) {
         // use ViewHolder pattern to avoid extra trips to findViewById
         View v = vi.inflate(R.layout.list_items_item, parent, false);
         ViewHolder holder = new ViewHolder();
         holder.coverImage = (ImageView) v.findViewById(R.id.list_items_item_image);
         holder.ratingImage = (ImageView) v.findViewById(R.id.list_items_item_rating_image);
         holder.text1 = (TextView) v.findViewById(R.id.list_items_item_text1);
         holder.text2 = (TextView) v.findViewById(R.id.list_items_item_text2);
         holder.readStatus = (CheckBox) v.findViewById(R.id.list_items_item_read_status);
         v.setTag(holder);
         populateView(v, c);
         return v;
      }

      private void populateView(final View v, final Cursor c) {
         // use ViewHolder pattern to avoid extra trips to findViewById
         ViewHolder holder = (ViewHolder) v.getTag();

         if ((c != null) && !c.isClosed()) {
            long id = c.getLong(0);

            // TODO investigate, may need to file Android/SQLite bug
            // Log.i(Constants.LOG_TAG, "COLUMN INDEX rating - " +
            // c.getColumnIndex(DataConstants.RATING));
            // as soon as query has group by or group_concat the
            // getColumnIndex fails? (explicit works)
            /*
             * bid = 0 tit = 1 subtit = 2 subject = 3 pub = 4 datepub = 5
             * format = 6 rstat = 7 rat = 8 blurb = 9 authors = 10
             */

            int rating = c.getInt(8);
            int readStatus = c.getInt(7);
            String title = c.getString(1);
            String authors = c.getString(10);

            if (Main.this.application.debugEnabled) {
               Log.d(Constants.LOG_TAG, "book (id|title) from cursor - " + id + "|" + title);
            }

            ImageView coverImage = holder.coverImage;
            Bitmap coverImageBitmap = Main.this.application.imageManager.retrieveBitmap(title, id, true);
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
            holder.text2.setText(StringUtil.addSpacesToCSVString(authors));

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
         dialog.setMessage(Main.this.getString(R.string.msgResetCoverImagesWarnTime));
         dialog.show();
      }

      @Override
      protected void onProgressUpdate(final String... args) {
         dialog.setMessage(args[0]);
      }

      @Override
      protected Void doInBackground(final Void... args) {
         Main.this.application.imageManager.clearAllBitmapSourceFiles();
         ArrayList<Book> books = Main.this.application.dataManager.selectAllBooks();
         for (int i = 0; i < books.size(); i++) {
            Book b = books.get(i);
            publishProgress(String.format(Main.this.getString(R.string.msgProcessingBookX, b.title)));
            Main.this.application.imageManager.resetCoverImage(b);
            SystemClock.sleep(100); // sleep a little, too many requests too quickly with large data sets is bad mojo
         }
         return null;
      }

      @Override
      protected void onPostExecute(final Void v) {
         Main.this.adapter.notifyDataSetChanged();
         if (dialog.isShowing()) {
            dialog.dismiss();
         }
      }
   }
}
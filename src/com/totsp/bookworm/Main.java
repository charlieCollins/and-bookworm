package com.totsp.bookworm;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
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
import android.view.WindowManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
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

import com.totsp.bookworm.data.CsvManager;
import com.totsp.bookworm.data.DataConstants;
import com.totsp.bookworm.model.Book;
import com.totsp.bookworm.model.BookListStats;
import com.totsp.bookworm.util.ExternalStorageUtil;
import com.totsp.bookworm.util.NetworkUtil;
import com.totsp.bookworm.util.StringUtil;
import com.totsp.bookworm.zxing.ZXingIntentIntegrator;
import com.totsp.bookworm.zxing.ZXingIntentResult;

import java.io.File;
import java.util.ArrayList;

public class Main extends Activity {

   private static final int MENU_ABOUT = 1;
   private static final int MENU_PREFS = 2;
   private static final int MENU_STATS = 3;

   private static final int MENU_CONTEXT_EDIT = 0;
   private static final int MENU_CONTEXT_DELETE = 1;

   BookWormApplication application;
   SharedPreferences prefs;
   ConnectivityManager cMgr;

   private ListView bookListView;
   private CursorAdapter adapter;
   private Cursor cursor;

   private ImageView sortImage;
   private ImageView addScanImage;
   private ImageView addSearchImage;
   private ImageView addFormImage;
   private ImageView manageDataImage;

   private Bitmap coverImageMissing;
   private Bitmap star0;
   private Bitmap star1;
   private Bitmap star2;
   private Bitmap star3;
   private Bitmap star4;
   private Bitmap star5;

   private ProgressDialog progressDialog;
   private AlertDialog sortDialog;
   private AlertDialog manageDataDialog;
   private AlertDialog statsDialog;

   @Override
   public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      // /Debug.startMethodTracing("BookWorm");
      setContentView(R.layout.main);
      application = (BookWormApplication) getApplication();
      prefs = PreferenceManager.getDefaultSharedPreferences(this);
      cMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

      progressDialog = new ProgressDialog(this);
      progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
      progressDialog.setCancelable(false);

      coverImageMissing = BitmapFactory.decodeResource(getResources(), R.drawable.book_cover_missing);
      star0 = BitmapFactory.decodeResource(getResources(), R.drawable.star0);
      star1 = BitmapFactory.decodeResource(getResources(), R.drawable.star1);
      star2 = BitmapFactory.decodeResource(getResources(), R.drawable.star2);
      star3 = BitmapFactory.decodeResource(getResources(), R.drawable.star3);
      star4 = BitmapFactory.decodeResource(getResources(), R.drawable.star4);
      star5 = BitmapFactory.decodeResource(getResources(), R.drawable.star5);

      // action bar images
      sortImage = (ImageView) findViewById(R.id.actionsort);
      sortImage.setOnClickListener(new OnClickListener() {
         public void onClick(View v) {
            if (!sortDialog.isShowing()) {
               sortDialog.show();
            }
         }
      });
      addScanImage = (ImageView) findViewById(R.id.actionaddscan);
      addScanImage.setOnClickListener(new OnClickListener() {
         public void onClick(View v) {
            if (NetworkUtil.connectionPresent(cMgr)) {
               try {
                  ZXingIntentIntegrator.initiateScan(Main.this, getString(R.string.labelInstallScanner),
                           getString(R.string.msgScannerNotPresent), getString(R.string.btnYes),
                           getString(R.string.btnNo));
               } catch (ActivityNotFoundException e) {
                  // this doesn't need to be i18n, should only happen on emulator (or roms without Market)
                  Toast.makeText(Main.this, "Unable to search Market for Barcode scanner, scanning unavailable.",
                           Toast.LENGTH_LONG).show();
               }
            } else {
               Toast.makeText(Main.this, getString(R.string.msgNetworkNAError), Toast.LENGTH_LONG).show();
            }
         }
      });
      addSearchImage = (ImageView) findViewById(R.id.actionaddsearch);
      addSearchImage.setOnClickListener(new OnClickListener() {
         public void onClick(View v) {
            if (NetworkUtil.connectionPresent(cMgr)) {
               startActivity(new Intent(Main.this, BookSearch.class));
            } else {
               Toast.makeText(Main.this, getString(R.string.msgNetworkNAError), Toast.LENGTH_LONG).show();
            }
         }
      });
      addFormImage = (ImageView) findViewById(R.id.actionaddform);
      addFormImage.setOnClickListener(new OnClickListener() {
         public void onClick(View v) {
            application.selectedBook = null;
            startActivity(new Intent(Main.this, BookForm.class));
         }
      });
      manageDataImage = (ImageView) findViewById(R.id.actionmanagedata);
      manageDataImage.setOnClickListener(new OnClickListener() {
         public void onClick(View v) {
            manageDataDialog.show();
         }
      });

      // listview
      bookListView = (ListView) findViewById(R.id.booklistview);
      bookListView.setEmptyView(findViewById(R.id.empty));
      bookListView.setTextFilterEnabled(true);
      bookListView.setOnItemClickListener(new OnItemClickListener() {
         public void onItemClick(final AdapterView<?> parent, final View v, final int index, final long id) {
            cursor.moveToPosition(index);
            // NOTE - this is tricky, table doesn't have _id, but CursorAdapter requires it
            // in the query we used "book.bid as _id" so here we have to use _id too
            int bookId = cursor.getInt(cursor.getColumnIndex("_id"));
            Book book = application.dataManager.selectBook(bookId);
            if (book != null) {
               if (application.debugEnabled) {
                  Log.d(Constants.LOG_TAG, "book selected - " + book.title);
               }
               application.lastMainListPosition = index;
               application.selectedBook = book;
               startActivity(new Intent(Main.this, BookDetail.class));
            } else {
               Toast.makeText(Main.this, getString(R.string.msgSelectBookError), Toast.LENGTH_SHORT).show();
            }
         }
      });
      registerForContextMenu(bookListView);

      // addtl
      setupDialogs();
      bindAdapter(false);

      // check backup restore
      if (!this.getIntent().getBooleanExtra("fromDeleteAll", false)) {
         checkForRestore();
      }
   }

   @Override
   public void onStart() {
      super.onStart();
   }

   @Override
   public void onResume() {
      super.onResume();
      bindAdapter(false);
   }

   @Override
   public void onPause() {
      if (sortDialog.isShowing()) {
         sortDialog.dismiss();
      }
      if (manageDataDialog.isShowing()) {
         manageDataDialog.dismiss();
      }
      if (statsDialog.isShowing()) {
         statsDialog.dismiss();
      }

      if (progressDialog.isShowing()) {
         progressDialog.dismiss();
      }

      // cleanup any other activity long term state from application
      application.bookSearchStateBean = null;

      // /Debug.stopMethodTracing();      
      super.onPause();
   }

   @Override
   public void onDestroy() {
      super.onDestroy();
   }

   @Override
   public boolean onCreateOptionsMenu(final Menu menu) {
      menu.add(0, Main.MENU_ABOUT, 1, getString(R.string.menuAbout)).setIcon(android.R.drawable.ic_menu_help);
      menu.add(0, Main.MENU_PREFS, 2, getString(R.string.menuPrefs)).setIcon(android.R.drawable.ic_menu_preferences);
      menu.add(0, Main.MENU_STATS, 3, getString(R.string.menuStats)).setIcon(android.R.drawable.ic_menu_info_details);
      return super.onCreateOptionsMenu(menu);
   }

   @Override
   public boolean onOptionsItemSelected(final MenuItem item) {
      switch (item.getItemId()) {
         case MENU_ABOUT:
            startActivity(new Intent(Main.this, About.class));
            return true;
         case MENU_PREFS:
            startActivity(new Intent(Main.this, Preferences.class));
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
         default:
            return super.onOptionsItemSelected(item);
      }
   }

   @Override
   public void onCreateContextMenu(final ContextMenu menu, final View v, final ContextMenuInfo menuInfo) {
      super.onCreateContextMenu(menu, v, menuInfo);
      menu.add(0, Main.MENU_CONTEXT_EDIT, 0, getString(R.string.menuEditBook));
      menu.add(0, Main.MENU_CONTEXT_DELETE, 1, getString(R.string.menuDeleteBook));
      menu.setHeaderTitle("Action");
   }

   @Override
   public boolean onContextItemSelected(final MenuItem item) {
      AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
      long bookId = info.id;
      final Book b = application.dataManager.selectBook(bookId);
      application.lastMainListPosition = info.position;
      switch (item.getItemId()) {
         case MENU_CONTEXT_EDIT:
            application.selectedBook = b;
            startActivity(new Intent(Main.this, BookForm.class));
            return true;
         case MENU_CONTEXT_DELETE:
            new AlertDialog.Builder(Main.this).setTitle(getString(R.string.menuDeleteBook)).setMessage(b.title)
                     .setPositiveButton(getString(R.string.btnYes), new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface d, final int i) {
                           application.imageManager.deleteBitmapSourceFile(b.title, b.id);
                           application.dataManager.deleteBook(b.id);
                           startActivity(getIntent());
                        }
                     }).setNegativeButton(getString(R.string.btnNo), new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface d, final int i) {
                        }
                     }).show();
            return true;
         default:
            return super.onContextItemSelected(item);
      }
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

   @Override
   public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
      ZXingIntentResult scanResult = ZXingIntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
      if (scanResult != null) {
         String isbn = scanResult.getContents();
         if (application.debugEnabled) {
            Log.d(Constants.LOG_TAG, "Scan result format was - " + scanResult.getFormatName());
            Log.d(Constants.LOG_TAG, "Scan result contents are - " + scanResult.getContents());
         }

         // if someone presses scan, then just does nothing (no scan) just ignore
         if (scanResult.getFormatName() == null) {
            return;
         }

         if ((scanResult.getFormatName() != null) && !scanResult.getFormatName().equals("EAN_13")) {
            // if it's not EAN 13 we are likely gonna have issues 
            // we are using PRODUCT_MODE which limits to UPC and EAN
            // we *might* be able to parse ISBN from UPC, but pattern is not understood, yet
            // if it's EAN-8 though, we are screwed
            // for example UPC 008819265580
            if (scanResult.getFormatName().startsWith("UPC")) {
               isbn = scanResult.getContents();
               if (isbn.length() == 12) {
                  if (isbn.startsWith("0")) {
                     isbn = isbn.substring(1, isbn.length());
                  }
                  if (isbn.endsWith("0")) {
                     isbn = isbn.substring(0, isbn.length() - 1);
                  }
               }
               Log.w(Constants.LOG_TAG, "Scan result was a UPC code (not an EAN code), parsed into ISBN:" + isbn);
            }
         }

         // handle scan result
         Intent scanIntent = new Intent(this, BookEntryResult.class);
         scanIntent.putExtra(Constants.ISBN, isbn);
         startActivity(scanIntent);
      }
   }

   private void bindAdapter(final boolean resetListPosition) {
      if (resetListPosition) {
         application.lastMainListPosition = 0;
      }
      // adapter.notifyDataSetChanged();
      // NOTE notifyDataSetChanged doesn't cut it, sorts underlying collection but doesn't update view
      // need to research (shouldn't have to re-bind the entire adapter, but for now doing so)
      // bind bookListView and adapter
      String orderBy = prefs.getString(Constants.DEFAULT_SORT_ORDER, DataConstants.ORDER_BY_TITLE_ASC);
      cursor = application.dataManager.getBookCursor(orderBy, null);
      if (cursor != null) {
         startManagingCursor(cursor);
         adapter = new BookCursorAdapter(cursor);
         bookListView.setAdapter(adapter);
         int lastMainPos = application.lastMainListPosition;
         if (lastMainPos < adapter.getCount()) {
            bookListView.setSelection(lastMainPos);
         }
      }
   }

   private void checkForRestore() {
      // if the current database is EMPTY, and yet the internal CSV backup file is present restore the data
      // (this file is maintained as users add/remove data, and backed up with BackupAgent)
      if (application.dataManager.selectAllBooks().size() == 0 && adapter != null && adapter.getCount() == 0) {
         File csvFile = new File(getFilesDir() + File.separator + DataConstants.EXPORT_FILENAME);
         if (csvFile.exists() && csvFile.canRead()) {
            Toast.makeText(this, getString(R.string.msgRestoreFromInternalBackup), Toast.LENGTH_LONG).show();
            new RestoreTask().execute();
         }
      }
   }

   private void setupDialogs() {
      AlertDialog.Builder sortDialogBuilder = new AlertDialog.Builder(this);
      sortDialogBuilder.setTitle(getString(R.string.btnSortBy));
      sortDialogBuilder.setItems(new CharSequence[] { getString(R.string.labelTitle),
               getString(R.string.labelAuthorsShort), getString(R.string.labelRating),
               getString(R.string.labelReadstatus), getString(R.string.labelSubject), getString(R.string.labelDatepub),
               getString(R.string.labelPublisher) }, new DialogInterface.OnClickListener() {
         public void onClick(DialogInterface d, int selected) {
            switch (selected) {
               case 0:
                  saveSortOrder(DataConstants.ORDER_BY_TITLE_ASC);
                  break;
               case 1:
                  saveSortOrder(DataConstants.ORDER_BY_AUTHORS_ASC);
                  break;
               case 2:
                  saveSortOrder(DataConstants.ORDER_BY_RATING_DESC);
                  break;
               case 3:
                  saveSortOrder(DataConstants.ORDER_BY_READ_DESC);
                  break;
               case 4:
                  saveSortOrder(DataConstants.ORDER_BY_SUBJECT_ASC);
                  break;
               case 5:
                  saveSortOrder(DataConstants.ORDER_BY_DATE_PUB_DESC);
                  break;
               case 6:
                  saveSortOrder(DataConstants.ORDER_BY_PUB_ASC);
                  break;
            }
            bindAdapter(true);
         }
      });
      sortDialogBuilder.setOnCancelListener(new OnCancelListener() {
         public void onCancel(DialogInterface d) {
            sortDialog.dismiss();
         }
      });
      sortDialog = sortDialogBuilder.create();

      AlertDialog.Builder manageDataDialogBuilder = new AlertDialog.Builder(this);
      manageDataDialogBuilder.setTitle(getString(R.string.labelManageData));
      manageDataDialogBuilder.setItems(new CharSequence[] { getString(R.string.btnExportCSV),
               getString(R.string.btnImportCSV), getString(R.string.btnEmailCSV),
               getString(R.string.btnResetCoverImages), getString(R.string.btnDeleteData),
               getString(R.string.btnDeleteInternalBackup) }, new DialogInterface.OnClickListener() {
         public void onClick(DialogInterface d, int selected) {
            switch (selected) {
               case 0:
                  // EXPORT CSV
                  new AlertDialog.Builder(Main.this).setMessage(R.string.msgReplaceExistingCSVExport)
                           .setPositiveButton(getString(R.string.btnYes), new DialogInterface.OnClickListener() {
                              public void onClick(final DialogInterface arg0, final int arg1) {
                                 if (ExternalStorageUtil.isExternalStorageAvail()) {
                                    // NOTE -- possible AsyncTask for CSV export? (very fast, not necc?)                                                      
                                    CsvManager.exportExternal(Main.this, application.dataManager.selectAllBooks());
                                    Toast.makeText(Main.this, getString(R.string.msgExportSuccess), Toast.LENGTH_SHORT)
                                             .show();
                                 } else {
                                    Toast.makeText(Main.this, getString(R.string.msgExternalStorageNAError),
                                             Toast.LENGTH_SHORT).show();
                                 }
                              }
                           }).setNegativeButton(getString(R.string.btnNo), new DialogInterface.OnClickListener() {
                              public void onClick(final DialogInterface arg0, final int arg1) {
                              }
                           }).show();
                  break;
               case 1:
                  // IMPORT CSV
                  startActivity(new Intent(Main.this, CsvImport.class));
                  break;
               case 2:
                  // EMAIL CSV
                  if (ExternalStorageUtil.isExternalStorageAvail()) {
                     File f = new File(DataConstants.EXTERNAL_DATA_PATH + File.separator + DataConstants.EXPORT_FILENAME);
                     if (f.exists() && f.canRead()) {
                        Intent sendCSVIntent = new Intent(Intent.ACTION_SEND);
                        sendCSVIntent.setType("text/csv");
                        sendCSVIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + f.getAbsolutePath()));
                        sendCSVIntent.putExtra(Intent.EXTRA_SUBJECT, "BookWorm CSV Export");
                        sendCSVIntent.putExtra(Intent.EXTRA_TEXT, "CSV export attached.");
                        startActivity(Intent.createChooser(sendCSVIntent, "Email:"));
                     } else {
                        Toast.makeText(Main.this, getString(R.string.msgExportBeforeEmail), Toast.LENGTH_LONG).show();
                     }
                  } else {
                     Toast.makeText(Main.this, getString(R.string.msgExternalStorageNAError), Toast.LENGTH_SHORT)
                              .show();
                  }
                  break;
               case 3:
                  // RESET ALL COVER IMAGES
                  if (adapter != null && adapter.getCount() > 0) {
                     new AlertDialog.Builder(Main.this).setTitle(getString(R.string.msgResetAllCoverImages))
                              .setMessage(getString(R.string.msgResetAllCoverImagesExplain)).setPositiveButton(
                                       getString(R.string.btnYes), new DialogInterface.OnClickListener() {
                                          public void onClick(final DialogInterface d, final int i) {
                                             new ResetAllCoverImagesTask().execute();
                                          }
                                       }).setNegativeButton(getString(R.string.btnNo),
                                       new DialogInterface.OnClickListener() {
                                          public void onClick(final DialogInterface d, final int i) {
                                          }
                                       }).show();
                  }
                  break;
               case 4:
                  // DELETE ALL DATA
                  new AlertDialog.Builder(Main.this).setMessage(getString(R.string.msgDeleteData)).setPositiveButton(
                           getString(R.string.btnYes), new DialogInterface.OnClickListener() {
                              public void onClick(final DialogInterface arg0, final int arg1) {
                                 Log.i(Constants.LOG_TAG, "deleting database and image data");
                                 new DeleteDataTask().execute();
                              }
                           }).setNegativeButton(getString(R.string.btnNo), new DialogInterface.OnClickListener() {
                     public void onClick(final DialogInterface arg0, final int arg1) {
                     }
                  }).show();
                  break;
               case 5:
                  // DELETE INTERNAL BACKUP DATA
                  new AlertDialog.Builder(Main.this).setMessage(getString(R.string.msgDeleteInternalBackupData))
                           .setPositiveButton(getString(R.string.btnYes), new DialogInterface.OnClickListener() {
                              public void onClick(final DialogInterface arg0, final int arg1) {
                                 Log.i(Constants.LOG_TAG, "deleting internal backup file");
                                 File csvFile =
                                          new File(getFilesDir() + File.separator + DataConstants.EXPORT_FILENAME);
                                 if (csvFile.exists() && csvFile.canRead()) {
                                    csvFile.delete();
                                 }
                                 Toast.makeText(Main.this, getString(R.string.msgDataDeleted), Toast.LENGTH_SHORT)
                                          .show();
                                 Intent intent = new Intent(Main.this, Main.class);
                                 intent.putExtra("fromDeleteAll", true);
                                 startActivity(intent);
                              }
                           }).setNegativeButton(getString(R.string.btnNo), new DialogInterface.OnClickListener() {
                              public void onClick(final DialogInterface arg0, final int arg1) {
                              }
                           }).show();
                  break;
            }
         }
      });
      manageDataDialog = manageDataDialogBuilder.create();

      AlertDialog.Builder statsDialogBuilder =
               new AlertDialog.Builder(this).setTitle(getString(R.string.msgBookListStats)).setNeutralButton(
                        getString(R.string.btnDismiss), new DialogInterface.OnClickListener() {
                           public void onClick(DialogInterface d, int i) {
                           };
                        });
      statsDialog = statsDialogBuilder.create();
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

      LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

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
            String orderBy = prefs.getString(Constants.DEFAULT_SORT_ORDER, DataConstants.ORDER_BY_TITLE_ASC);
            c = application.dataManager.getBookCursor(orderBy, "where book.tit like " + pattern);
         }
         cursor = c;
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

            if (application.debugEnabled) {
               Log.d(Constants.LOG_TAG, "book (id|title) from cursor - " + id + "|" + title);
            }

            ImageView coverImage = holder.coverImage;
            Bitmap coverImageBitmap = application.imageManager.retrieveBitmap(title, id, true);
            if (coverImageBitmap != null) {
               coverImage.setImageBitmap(coverImageBitmap);
            } else {
               coverImage.setImageBitmap(coverImageMissing);
            }

            ImageView ratingImage = holder.ratingImage;
            switch (rating) {
               case 0:
                  ratingImage.setImageBitmap(star0);
                  break;
               case 1:
                  ratingImage.setImageBitmap(star1);
                  break;
               case 2:
                  ratingImage.setImageBitmap(star2);
                  break;
               case 3:
                  ratingImage.setImageBitmap(star3);
                  break;
               case 4:
                  ratingImage.setImageBitmap(star4);
                  break;
               case 5:
                  ratingImage.setImageBitmap(star5);
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

   //
   // AsyncTasks
   //
   private class ResetAllCoverImagesTask extends AsyncTask<Void, String, Void> {

      @Override
      protected void onPreExecute() {
         if (progressDialog.isShowing()) {
            progressDialog.dismiss();
         }
         // keep screen on, and prevent orientation change, during potentially long running task
         getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
         setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
      }

      @Override
      protected Void doInBackground(final Void... args) {
         application.imageManager.clearAllBitmapSourceFiles();
         ArrayList<Book> books = application.dataManager.selectAllBooks();
         String[] progress = new String[3];
         progress[2] = Integer.toString(books.size());
         for (int i = 0; i < books.size(); i++) {
            Book b = books.get(i);
            progress[0] = String.format(getString(R.string.msgProcessingBookX, b.title));
            progress[1] = Integer.toString(i);
            publishProgress(progress);
            application.imageManager.resetCoverImage(b);
            // sleep a little, too many requests too quickly with large data sets is bad mojo
            SystemClock.sleep(100);
         }
         return null;
      }

      @Override
      protected void onProgressUpdate(String... progress) {
         progressDialog.setMessage(progress[0]);
         if ((progress[1].equals("1")) && !progressDialog.isShowing()) {
            //Toast.makeText(Main.this, R.string.msgResetCoverImagesWarnTime, Toast.LENGTH_SHORT).show();
            progressDialog.setMax(Integer.valueOf(progress[2]));
            progressDialog.show();
         } else if (progress[1].equals(progress[2]) && progressDialog.isShowing()) {
            progressDialog.dismiss();
         }
         progressDialog.setProgress(Integer.valueOf(progress[1]));
      }

      @Override
      protected void onPostExecute(final Void v) {
         if (adapter != null) {
            adapter.notifyDataSetChanged();
         }
         if (progressDialog.isShowing()) {
            progressDialog.dismiss();
         }
         // reset screen and orientation params
         getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
         setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
      }
   }

   private class DeleteDataTask extends AsyncTask<Void, Integer, Void> {

      @Override
      protected void onPreExecute() {
         if (progressDialog.isShowing()) {
            progressDialog.dismiss();
         }
         // keep screen on, and prevent orientation change, during potentially long running task
         getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
         setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
      }

      @Override
      protected Void doInBackground(final Void... args) {
         publishProgress(1);
         application.dataManager.deleteAllDataYesIAmSure();
         application.dataManager.resetDb();
         publishProgress(2);
         application.imageManager.clearAllBitmapSourceFiles();
         publishProgress(3);
         return null;
      }

      @Override
      protected void onProgressUpdate(Integer... progress) {
         if (!progressDialog.isShowing()) {
            progressDialog.show();
         }
         if (progress[0] == 1) {
            progressDialog.setMax(3);
         }
         progressDialog.setProgress(progress[0]);
         progressDialog.setMessage(Main.this.getString(R.string.msgDeletingData));
      }

      @Override
      protected void onPostExecute(final Void arg) {
         if (progressDialog.isShowing()) {
            progressDialog.dismiss();
         }
         // reset screen and orientation params
         getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
         setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

         Intent intent = new Intent(Main.this, Main.class);
         intent.putExtra("fromDeleteAll", true);
         startActivity(intent);
      }
   }

   private class RestoreTask extends AsyncTask<Void, String, Void> {

      // we shouldn't need to check for dupes here, only used if DB is empty
      // and backup file should never wind up with dupes in it

      @Override
      protected void onPreExecute() {
         if (progressDialog.isShowing()) {
            progressDialog.dismiss();
         }
         // keep screen on, and prevent orientation change, during potentially long running task
         getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
         setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
      }

      @Override
      protected Void doInBackground(final Void... args) {
         File csvFile = new File(getFilesDir() + File.separator + DataConstants.EXPORT_FILENAME);
         if (csvFile.exists() && csvFile.canRead()) {
            ArrayList<Book> restoreBooks = CsvManager.parseCSVFile(null, csvFile);
            String[] progress = new String[3];
            progress[2] = Integer.toString(restoreBooks.size());
            for (int i = 0; i < restoreBooks.size(); i++) {
               Book b = restoreBooks.get(i);
               Log.i(Constants.LOG_TAG, "Importing book: " + b.title);
               progress[0] = String.format(getString(R.string.msgCsvImportingBook, b.title));
               progress[1] = Integer.toString(i);
               publishProgress(progress);
               b.id = application.dataManager.insertBook(b);
               application.imageManager.resetCoverImage(b);
            }
         }
         return null;
      }

      @Override
      protected void onProgressUpdate(String... progress) {
         progressDialog.setMessage(progress[0]);
         if ((progress[1].equals("1")) && !progressDialog.isShowing()) {
            progressDialog.setMax(Integer.valueOf(progress[2]));
            progressDialog.show();
         } else if (progress[1].equals(progress[2]) && progressDialog.isShowing()) {
            progressDialog.dismiss();
         }
         progressDialog.setProgress(Integer.valueOf(progress[1]));
      }

      @Override
      protected void onPostExecute(final Void arg) {
         if (progressDialog.isShowing()) {
            progressDialog.dismiss();
         }
         // reset screen and orientation params
         getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
         setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

         bindAdapter(true);
      }
   }
}
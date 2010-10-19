package com.totsp.bookworm.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.SystemClock;
import android.util.Log;

import com.totsp.bookworm.BackupManagerWrapper;
import com.totsp.bookworm.Constants;
import com.totsp.bookworm.data.dao.AuthorDAO;
import com.totsp.bookworm.data.dao.BookDAO;
import com.totsp.bookworm.data.dao.BookUserDataDAO;
import com.totsp.bookworm.model.Book;
import com.totsp.bookworm.model.BookListStats;

import java.util.ArrayList;

/**
 * Android DataManager to encapsulate SQL and DB details.
 * Includes SQLiteOpenHelper, and uses DAO objects (in specified order)
 * to create/update and clear tables, and manipulate data.
 *
 * @author ccollins
 *
 */
public class DataManager {

   private static final int DATABASE_VERSION = 10;

   private Context context;

   // Use a wrapper BackupManager, because it's only available on API level 8 and above
   private BackupManagerWrapper backupManager;

   private SQLiteDatabase db;

   private AuthorDAO authorDAO;
   private BookDAO bookDAO;

   private enum FileChangeMode {
      APPEND, REPLACE;
   }
   
   public DataManager(final Context context) {

      this.context = context;

      try { 
         BackupManagerWrapper.isAvailable();
         backupManager = new BackupManagerWrapper(this.context);
      } catch (Throwable t) {
         Log.i(Constants.LOG_TAG, "BackupManager not available (older Android version). BackupManager will not be used.");
      }      

      OpenHelper openHelper = new OpenHelper(this.context);
      db = openHelper.getWritableDatabase();
      Log.i(Constants.LOG_TAG, "DataManager created, db open status: " + db.isOpen());

      // DAOs are all needed here for onCreate/onUpgrade/deleteAll, etc.
      // in some cases though they are not used to manipulate data directly
      // (rather they are nested, see bookDAO, which includes authorDAO, for now)
      // (future they probably should be more separated)
      authorDAO = new AuthorDAO(db);
      bookDAO = new BookDAO(db);

      if (openHelper.isDbCreated()) {
         // insert default data here if needed
      }
   }

   public SQLiteDatabase getDb() {
      return db;
   }

   public void openDb() {
      if (!db.isOpen()) {
         db = SQLiteDatabase.openDatabase(DataConstants.DATABASE_PATH, null, SQLiteDatabase.OPEN_READWRITE);
         // since we pass db into DAO, have to recreate DAO if db is re-opened
         authorDAO = new AuthorDAO(db);
         bookDAO = new BookDAO(db);
      }
   }

   public void closeDb() {
      if (db.isOpen()) {
         db.close();
      }
   }

   public void resetDb() {
      Log.i(Constants.LOG_TAG, "Resetting database connection (close and re-open).");
      closeDb();
      SystemClock.sleep(500);
      openDb();
   }

   //
   // wrapped DB methods
   //
   public Book selectBook(final long id) {
      return bookDAO.select(id);
   }

   public ArrayList<Book> selectAllBooks() {
      return bookDAO.selectAll();
   }

   public ArrayList<Book> selectAllBooksByTitle(final String title) {
      return bookDAO.selectAllBooksByTitle(title);
   }

   public long insertBook(final Book b) {
      long id = bookDAO.insert(b);
      this.dataChanged(FileChangeMode.APPEND, b);
      return id;
   }

   public void updateBook(final Book b) {
      bookDAO.update(b);
      // for now don't worry about updating backup on an update (it will happen at next insert, should be enough)
      ///this.dataChanged(FileChangeMode.REPLACE, b);
   }

   public void deleteBook(final long id) {
      bookDAO.delete(id);
      // for now don't worry about updating backup on a delete (it will happen at next insert, should be enough)
      ///this.dataChanged(FileChangeMode.REPLACE, null);
   }  

   // write changed data to INTERNAL backup file, and also tell BackupManager that data changed
   private void dataChanged(final FileChangeMode mode, final Book book) {
      // replacing the entire backup file, especially if large, is expensive
      // allow user to specify if operation should APPEND or REPLACE 
      // (because Files are difficult to update [and doing so basically as ineffient as replace anyway]
      // on update or delete if data we will replace the entire file, unless becomes a performance issue)
      if (mode.equals(FileChangeMode.REPLACE)) {
         CsvManager.exportInternal(context, selectAllBooks());
      } else if (mode.equals(FileChangeMode.APPEND)) {
         ArrayList<Book> list = new ArrayList<Book>(1);
         list.add(book);
         CsvManager.appendInternal(context, list);
      }
      if (backupManager != null) {
         backupManager.dataChanged();
      }
   }

   public Cursor getBookCursor(final String orderBy, final String whereClauseLimit) {
      return bookDAO.getCursor(orderBy, whereClauseLimit);
   }

   // super delete - clears all tables
   public void deleteAllDataYesIAmSure() {
      Log.i(Constants.LOG_TAG, "deleting all data from database - deleteAllYesIAmSure invoked");
      db.beginTransaction();
      try {
         // TODO I think there is a NASTY Android bug lurking hereabouts (need to investigate)
         // if one of these deletes gets a message such as:
         // "sqlite returned: error code = 17, msg = prepared statement aborts at 7: [DELETE FROM author]"
         // the NO EXCEPTION IS THROWN (and note that Android makes SQLException unchecked)
         // this means the trans will commit and continue on, this is very bad
         // (noticed this when I had bug in BookDAO that DROPPED table rather than delete rows from here)        
         authorDAO.deleteAll();
         bookDAO.deleteAll();
         db.setTransactionSuccessful();
      } finally {
         db.endTransaction();
      }
      db.execSQL("vacuum");
   }

   // stats specific (should be separated out, and more detailed)
   public BookListStats getStats() {
      BookListStats stats = new BookListStats();
      stats.totalBooks = getCountFromTable(DataConstants.BOOK_TABLE, "");
      stats.totalAuthors = getCountFromTable(DataConstants.AUTHOR_TABLE, "");
      stats.readBooks = getCountFromTable(DataConstants.BOOKUSERDATA_TABLE, "where bookuserdata.rstat = 1");
      stats.fiveStarBooks = getCountFromTable(DataConstants.BOOKUSERDATA_TABLE, "where bookuserdata.rat = 5");
      stats.fourStarBooks = getCountFromTable(DataConstants.BOOKUSERDATA_TABLE, "where bookuserdata.rat = 4");
      stats.threeStarBooks = getCountFromTable(DataConstants.BOOKUSERDATA_TABLE, "where bookuserdata.rat = 3");
      stats.twoStarBooks = getCountFromTable(DataConstants.BOOKUSERDATA_TABLE, "where bookuserdata.rat = 2");
      stats.oneStarBooks = getCountFromTable(DataConstants.BOOKUSERDATA_TABLE, "where bookuserdata.rat = 1");
      return stats;
   }

   // protected scope to allow method to be exposed for automated testing 
   protected int getCountFromTable(final String table, final String whereClause) {
      int result = 0;
      Cursor c = db.rawQuery("select count(*) from " + table + " " + whereClause, null);
      if (c.moveToFirst()) {
         result = c.getInt(0);
      }
      if (!c.isClosed()) {
         c.close();
      }
      return result;
   }

   //
   // end DB methods
   //  

   //
   // SQLiteOpenHelper   
   //
   private static class OpenHelper extends SQLiteOpenHelper {

      private boolean dbCreated;

      OpenHelper(final Context context) {
         super(context, DataConstants.DATABASE_NAME, null, DataManager.DATABASE_VERSION);
      }

      @Override
      public void onCreate(final SQLiteDatabase db) {
         Log.i(Constants.LOG_TAG, "BookWorm DataHelper.OpenHelper onCreate creating database bookworm.db");
         AuthorDAO.onCreate(db);
         BookDAO.onCreate(db);
         BookUserDataDAO.onCreate(db);
         dbCreated = true;
      }

      @Override
      public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
         Log
                  .i(Constants.LOG_TAG, "SQLiteOpenHelper onUpgrade - oldVersion:" + oldVersion + " newVersion:"
                           + newVersion);
         AuthorDAO.onUpgrade(db, oldVersion, newVersion);
         BookUserDataDAO.onUpgrade(db, oldVersion, newVersion);
         BookDAO.onUpgrade(db, oldVersion, newVersion);
      }

      public boolean isDbCreated() {
         return dbCreated;
      }
   }
}
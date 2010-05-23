package com.totsp.bookworm.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.totsp.bookworm.Constants;
import com.totsp.bookworm.data.dao.BookDAO;
import com.totsp.bookworm.model.Book;
import com.totsp.bookworm.model.BookListStats;

import java.util.ArrayList;

/**
 * Android DataHelper to encapsulate SQL and DB details.
 * Includes SQLiteOpenHelper.
 *
 * @author ccollins
 *
 */
public class DataManager {

   public static final String ORDER_BY_AUTHORS_ASC =
            "authors asc, book.tit asc";
   public static final String ORDER_BY_AUTHORS_DESC =
            "authors desc, book.tit asc";
   public static final String ORDER_BY_TITLE_ASC = "book.tit asc";
   public static final String ORDER_BY_TITLE_DESC = "book.tit desc";
   public static final String ORDER_BY_SUBJECT_ASC =
            "book.subject asc, book.tit asc";
   public static final String ORDER_BY_SUBJECT_DESC =
            "book.subject desc, book.tit asc";
   public static final String ORDER_BY_RATING_ASC =
            "bookuserdata.rat asc, book.tit asc";
   public static final String ORDER_BY_RATING_DESC =
            "bookuserdata.rat desc, book.tit asc";
   public static final String ORDER_BY_READ_ASC =
            "bookuserdata.rstat asc, book.tit asc";
   public static final String ORDER_BY_READ_DESC =
            "bookuserdata.rstat desc, book.tit asc";
   public static final String ORDER_BY_PUB_ASC = "book.pub asc, book.tit asc";
   public static final String ORDER_BY_PUB_DESC = "book.pub desc, book.tit asc";
   public static final String ORDER_BY_DATE_PUB_ASC =
            "book.datepub asc, book.tit asc";
   public static final String ORDER_BY_DATE_PUB_DESC =
            "book.datepub desc, book.tit asc";

   private static final int DATABASE_VERSION = 10;

   private SQLiteDatabase db;

   private BookDAO bookDAO;

   public DataManager(final Context context) {
      OpenHelper openHelper = new OpenHelper(context);
      db = openHelper.getWritableDatabase();

      // app only needs access to book DAO at present (can't create authors on their own, etc.)
      bookDAO = new BookDAO(db);

      if (openHelper.isDbCreated()) {
         // insert default data here if needed
      }
   }

   public SQLiteDatabase getDb() {
      return db;
   }

   public void resetDbConnection() {
      Log.i(Constants.LOG_TAG,
               "Resetting database connection (close and re-open).");
      cleanup();
      db =
               SQLiteDatabase.openDatabase(DataConstants.DATABASE_PATH, null,
                        SQLiteDatabase.OPEN_READWRITE);
   }

   public void cleanup() {
      if ((db != null) && db.isOpen()) {
         db.close();
      }
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
      return bookDAO.insert(b);
   }

   public void updateBook(final Book b) {
      bookDAO.update(b);
   }

   public void deleteBook(final long id) {
      bookDAO.delete(id);
   }

   public Cursor getSelectBookJoinCursor(final String orderBy,
            final String whereClauseLimit) {
      return bookDAO.getSelectBookJoinCursor(orderBy, whereClauseLimit);
   }

   // super delete - clears all tables
   public void deleteAllDataYesIAmSure() {
      Log.i(Constants.LOG_TAG,
               "deleting all data from database - deleteAllYesIAmSure invoked");
      db.beginTransaction();
      try {
         db.delete(DataConstants.AUTHOR_TABLE, null, null);
         db.delete(DataConstants.BOOKAUTHOR_TABLE, null, null);
         db.delete(DataConstants.BOOKUSERDATA_TABLE, null, null);
         db.delete(DataConstants.BOOK_TABLE, null, null);
         db.setTransactionSuccessful();
      } finally {
         db.endTransaction();
      }
      db.execSQL("vacuum");
   }

   // stats specific
   public BookListStats getStats() {
      BookListStats stats = new BookListStats();
      stats.totalBooks = getCountFromTable(DataConstants.BOOK_TABLE, "");
      stats.totalAuthors = getCountFromTable(DataConstants.AUTHOR_TABLE, "");
      stats.readBooks =
               getCountFromTable(DataConstants.BOOKUSERDATA_TABLE,
                        "where bookuserdata.rstat = 1");
      stats.fiveStarBooks =
               getCountFromTable(DataConstants.BOOKUSERDATA_TABLE,
                        "where bookuserdata.rat = 5");
      stats.fourStarBooks =
               getCountFromTable(DataConstants.BOOKUSERDATA_TABLE,
                        "where bookuserdata.rat = 4");
      stats.threeStarBooks =
               getCountFromTable(DataConstants.BOOKUSERDATA_TABLE,
                        "where bookuserdata.rat = 3");
      stats.twoStarBooks =
               getCountFromTable(DataConstants.BOOKUSERDATA_TABLE,
                        "where bookuserdata.rat = 2");
      stats.oneStarBooks =
               getCountFromTable(DataConstants.BOOKUSERDATA_TABLE,
                        "where bookuserdata.rat = 1");
      return stats;
   }

   private int getCountFromTable(final String table, final String whereClause) {
      int result = 0;
      Cursor c =
               db.rawQuery("select count(*) from " + table + " " + whereClause,
                        null);
      if (c.moveToFirst()) {
         result = c.getInt(0);
      }
      if ((c != null) && !c.isClosed()) {
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
         super(context, DataConstants.DATABASE_NAME, null,
                  DataManager.DATABASE_VERSION);
      }

      @Override
      public void onCreate(final SQLiteDatabase db) {
         Log
                  .i(Constants.LOG_TAG,
                           "BookWorm DataHelper.OpenHelper onCreate creating database bookworm.db");

         // using StringBuilder here because it is easier to read/reuse lines
         StringBuilder sb = new StringBuilder();

         // book table
         sb.append("CREATE TABLE " + DataConstants.BOOK_TABLE + " (");
         sb.append(DataConstants.BOOKID + " INTEGER PRIMARY KEY, ");
         sb.append(DataConstants.ISBN10 + " TEXT, ");
         sb.append(DataConstants.ISBN13 + " TEXT, ");
         sb.append(DataConstants.TITLE + " TEXT, ");
         sb.append(DataConstants.SUBTITLE + " TEXT, ");
         sb.append(DataConstants.PUBLISHER + " TEXT, ");
         sb.append(DataConstants.DESCRIPTION + " TEXT, ");
         sb.append(DataConstants.FORMAT + " TEXT, ");
         sb.append(DataConstants.SUBJECT + " TEXT, ");
         sb.append(DataConstants.DATEPUB + " INTEGER");
         sb.append(");");
         db.execSQL(sb.toString());

         // author table
         sb.setLength(0);
         sb.append("CREATE TABLE " + DataConstants.AUTHOR_TABLE + " (");
         sb.append(DataConstants.AUTHORID + " INTEGER PRIMARY KEY, ");
         sb.append(DataConstants.NAME + " TEXT");
         sb.append(");");
         db.execSQL(sb.toString());

         // bookauthor table
         sb.setLength(0);
         sb.append("CREATE TABLE " + DataConstants.BOOKAUTHOR_TABLE + " (");
         sb.append(DataConstants.BOOKAUTHORID + " INTEGER PRIMARY KEY, ");
         sb.append(DataConstants.BOOKID + " INTEGER, ");
         sb.append(DataConstants.AUTHORID + " INTEGER, ");
         sb.append("FOREIGN KEY(" + DataConstants.BOOKID + ") REFERENCES "
                  + DataConstants.BOOK_TABLE + "(" + DataConstants.BOOKID
                  + "), ");
         sb.append("FOREIGN KEY(" + DataConstants.AUTHORID + ") REFERENCES "
                  + DataConstants.AUTHOR_TABLE + "(" + DataConstants.AUTHORID
                  + ") ");
         sb.append(");");
         db.execSQL(sb.toString());

         // bookdata table (users book data, ratings, reviews, etc)
         sb.setLength(0);
         sb.append("CREATE TABLE " + DataConstants.BOOKUSERDATA_TABLE + " (");
         sb.append(DataConstants.BOOKUSERDATAID + " INTEGER PRIMARY KEY, ");
         sb.append(DataConstants.BOOKID + " INTEGER, ");
         sb.append(DataConstants.READSTATUS + " INTEGER, ");
         sb.append(DataConstants.RATING + " INTEGER, ");
         sb.append(DataConstants.BLURB + " TEXT, ");
         sb.append("FOREIGN KEY(" + DataConstants.BOOKID + ") REFERENCES "
                  + DataConstants.BOOK_TABLE + "(" + DataConstants.BOOKID
                  + ") ");
         sb.append(");");
         db.execSQL(sb.toString());

         // constraints 
         // (ISBN cannot be unique - some books don't have em - use TITLE unique?)
         //db.execSQL("CREATE UNIQUE INDEX uidxBookTitle ON " + BOOK_TABLE + "(" + DataConstants.TITLE
         //         + " COLLATE NOCASE)");
         db.execSQL("CREATE UNIQUE INDEX uidxAuthorName ON "
                  + DataConstants.AUTHOR_TABLE + "(" + DataConstants.NAME
                  + " COLLATE NOCASE)");
         db.execSQL("CREATE UNIQUE INDEX uidxBookIdForUserData ON "
                  + DataConstants.BOOKUSERDATA_TABLE + "("
                  + DataConstants.BOOKID + " COLLATE NOCASE)");

         dbCreated = true;
      }

      @Override
      public void onUpgrade(final SQLiteDatabase db, final int oldVersion,
               final int newVersion) {
         Log.i(Constants.LOG_TAG, "SQLiteOpenHelper onUpgrade - oldVersion:"
                  + oldVersion + " newVersion:" + newVersion);
         // export old data first, then upgrade, then import
         db.execSQL("DROP TABLE IF EXISTS " + DataConstants.BOOK_TABLE);
         db.execSQL("DROP TABLE IF EXISTS " + DataConstants.AUTHOR_TABLE);
         db.execSQL("DROP TABLE IF EXISTS " + DataConstants.BOOKUSERDATA_TABLE);
         db.execSQL("DROP TABLE IF EXISTS " + DataConstants.BOOKAUTHOR_TABLE);
         onCreate(db);
      }

      public boolean isDbCreated() {
         return dbCreated;
      }
   }

}
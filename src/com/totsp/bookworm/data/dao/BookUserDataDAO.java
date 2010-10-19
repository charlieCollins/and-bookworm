package com.totsp.bookworm.data.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import com.totsp.bookworm.Constants;
import com.totsp.bookworm.data.DataConstants;
import com.totsp.bookworm.model.BookUserData;

import java.util.ArrayList;

/**
 * DAO for BookUserData entity. 
 * 
 * @author ccollins
 *
 */
public class BookUserDataDAO implements DAO<BookUserData> {

   private final SQLiteStatement bookUserDataInsertStmt;
   private static final String BOOKUSERDATA_INSERT =
            "insert into " + DataConstants.BOOKUSERDATA_TABLE + "(" + DataConstants.BOOKID + ","
                     + DataConstants.READSTATUS + "," + DataConstants.RATING + "," + DataConstants.BLURB
                     + ") values (?, ?, ?, ?)";

   private SQLiteDatabase db;

   public BookUserDataDAO(SQLiteDatabase db) {
      this.db = db;

      // statements
      bookUserDataInsertStmt = db.compileStatement(BookUserDataDAO.BOOKUSERDATA_INSERT);
   }

   public static void onCreate(SQLiteDatabase db) {
      StringBuilder sb = new StringBuilder();

      // book table must exist (pre-req)

      // bookuserdata table (users book data, ratings, reviews, etc)
      sb.setLength(0);
      sb.append("CREATE TABLE " + DataConstants.BOOKUSERDATA_TABLE + " (");
      sb.append(DataConstants.BOOKUSERDATAID + " INTEGER PRIMARY KEY, ");
      sb.append(DataConstants.BOOKID + " INTEGER, ");
      sb.append(DataConstants.READSTATUS + " INTEGER, ");
      sb.append(DataConstants.RATING + " INTEGER, ");
      sb.append(DataConstants.BLURB + " TEXT, ");
      sb.append("FOREIGN KEY(" + DataConstants.BOOKID + ") REFERENCES " + DataConstants.BOOK_TABLE + "("
               + DataConstants.BOOKID + ") ");
      sb.append(");");
      db.execSQL(sb.toString());

      // constraints         
      db.execSQL("CREATE UNIQUE INDEX uidxAuthorName ON " + DataConstants.AUTHOR_TABLE + "(" + DataConstants.NAME
               + " COLLATE NOCASE)");
      db.execSQL("CREATE UNIQUE INDEX uidxBookIdForUserData ON " + DataConstants.BOOKUSERDATA_TABLE + "("
               + DataConstants.BOOKID + " COLLATE NOCASE)");

   }

   public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      db.execSQL("DROP TABLE IF EXISTS " + DataConstants.BOOKUSERDATA_TABLE);
      BookUserDataDAO.onCreate(db);
   }

   @Override
   public void deleteAll() {
      db.delete(DataConstants.BOOKUSERDATA_TABLE, null, null);
   }

   @Override
   public Cursor getCursor(final String orderBy, final String whereClauseLimit) {
      throw new UnsupportedOperationException("Not yet implemented");
   }

   @Override
   public BookUserData select(final long id) {
      BookUserData b = null;
      Cursor c =
               db.query(DataConstants.BOOKUSERDATA_TABLE, new String[] { DataConstants.READSTATUS,
                        DataConstants.RATING, DataConstants.BLURB }, DataConstants.BOOKUSERDATAID + " = ?",
                        new String[] { String.valueOf(id) }, null, null, null, "1");
      if (c.moveToFirst()) {
         b = new BookUserData();
         b.read = c.getInt(0) == 0 ? false : true;
         b.rating = c.getInt(1);
         b.blurb = c.getString(2);         
      }
      if (!c.isClosed()) {
         c.close();
      }
      return b;
   }

   public BookUserData selectByBookId(final long bookId) {
      BookUserData b = null;
      Cursor c =
               db.query(DataConstants.BOOKUSERDATA_TABLE, new String[] { DataConstants.READSTATUS,
                        DataConstants.RATING, DataConstants.BLURB }, DataConstants.BOOKID + " = ?",
                        new String[] { String.valueOf(bookId) }, null, null, null, "1");
      if (c.moveToFirst()) {
         b = new BookUserData();
         b.read = c.getInt(0) == 0 ? false : true;
         b.rating = c.getInt(1);
         b.blurb = c.getString(2);
      }
      if (!c.isClosed()) {
         c.close();
      }
      return b;
   }

   @Override
   public ArrayList<BookUserData> selectAll() {
      throw new UnsupportedOperationException("Not yet implemented.");
   }

   @Override
   public long insert(final BookUserData b) {
      long id = 0L;
      bookUserDataInsertStmt.clearBindings();
      bookUserDataInsertStmt.bindLong(1, b.bookId);
      bookUserDataInsertStmt.bindLong(2, b.read ? 1 : 0);
      bookUserDataInsertStmt.bindLong(3, b.rating);
      if (b.blurb != null) {
         bookUserDataInsertStmt.bindString(4, b.blurb);
      }
      try {
         id = bookUserDataInsertStmt.executeInsert();
      } catch (SQLiteConstraintException e) {
         // not sure how this occurs, but sometimes get constraint except
         // for bookuserdata -- if this occurs, delete the bookuserdata row
         // and stop (this will clean up the bad data, user will have to re-add book?)
         this.delete(b.bookId);
         Log.i(Constants.LOG_TAG, "Constraint issue inserting bookuserdata, cleaning up table (bookId=" + b.bookId
                  + ")");
         ///Log.e(Constants.LOG_TAG, "Constraint error " + e.getMessage(), e);
      }
      return id;
   }

   @Override
   public void update(final BookUserData b) {
      // insert in case not present - if book was added before this was avail, etc
      BookUserData existingData = select(b.id);
      if (existingData == null) {
         insert(b);
      } else {
         final ContentValues values = new ContentValues();
         values.put(DataConstants.READSTATUS, b.read ? 1 : 0);
         values.put(DataConstants.RATING, b.rating);
         values.put(DataConstants.BLURB, b.blurb);
         db.update(DataConstants.BOOKUSERDATA_TABLE, values, DataConstants.BOOKID + " = ?", new String[] { String
                  .valueOf(b.bookId) });
      }
   }

   @Override
   public void delete(final long bookId) {
      if (bookId > 0) {
         db.delete(DataConstants.BOOKUSERDATA_TABLE, DataConstants.BOOKID + " = ?", new String[] { String
                  .valueOf(bookId) });
      }
   }

}
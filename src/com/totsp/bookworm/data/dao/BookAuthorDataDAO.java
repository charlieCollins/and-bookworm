package com.totsp.bookworm.data.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.totsp.bookworm.data.DataConstants;
import com.totsp.bookworm.model.BookUserData;

import java.util.ArrayList;

public class BookAuthorDataDAO implements DAO<BookUserData> {

   private final SQLiteStatement bookUserDataInsertStmt;
   private static final String BOOKUSERDATA_INSERT =
            "insert into " + DataConstants.BOOKUSERDATA_TABLE + "("
                     + DataConstants.BOOKID + "," + DataConstants.READSTATUS
                     + "," + DataConstants.RATING + "," + DataConstants.BLURB
                     + ") values (?, ?, ?, ?)";

   private SQLiteDatabase db;

   public BookAuthorDataDAO(SQLiteDatabase db) {
      this.db = db;

      // statements
      bookUserDataInsertStmt = db.compileStatement(BOOKUSERDATA_INSERT);

   }   

   @Override
   public BookUserData select(final long id) {
      BookUserData b = null;
      Cursor c =
               db.query(DataConstants.BOOKUSERDATA_TABLE, new String[] {
                        DataConstants.READSTATUS, DataConstants.RATING,
                        DataConstants.BLURB }, DataConstants.BOOKID + " = ?",
                        new String[] { String.valueOf(id) }, null, null,
                        null, "1");
      if (c.moveToFirst()) {
         b = new BookUserData();
         b.read = (c.getInt(0) == 0 ? false : true);
         b.rating = (c.getInt(1));
         // TODO not yet persisting user blurb
      }
      if ((c != null) && !c.isClosed()) {
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
      bookUserDataInsertStmt.clearBindings();
      bookUserDataInsertStmt.bindLong(1, b.bookId);
      bookUserDataInsertStmt.bindLong(2, b.read ? 1 : 0);
      bookUserDataInsertStmt.bindLong(3, b.rating);
      if (b.blurb != null) {
         bookUserDataInsertStmt.bindString(4, b.blurb);
      }
      long id = 0L;
      try {
         id = bookUserDataInsertStmt.executeInsert();
      } catch (SQLiteConstraintException e) {
         // TODO sometimes constraint occurs, seems to be related to db versions, not sure
         // for now catch and update instead (hack)
         update(b);
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
         db.update(DataConstants.BOOKUSERDATA_TABLE, values,
                  DataConstants.BOOKID + " = ?", new String[] { String
                           .valueOf(b.bookId) });
      }
   }

   @Override
   public void delete(final long bookId) {
      if (bookId > 0) {
         db.delete(DataConstants.BOOKUSERDATA_TABLE, DataConstants.BOOKID
                  + " = ?", new String[] { String.valueOf(bookId) });
      }
   }
  

}
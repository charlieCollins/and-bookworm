package com.totsp.bookworm.data.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import com.totsp.bookworm.Constants;
import com.totsp.bookworm.data.DataConstants;
import com.totsp.bookworm.model.Author;

import java.util.ArrayList;

public class AuthorDAO implements DAO<Author> {

   private static final String QUERY_AUTHORS_BY_BOOK_ID_PREFIX =
            "select author.name from author join bookauthor on bookauthor.aid = author.aid join book on bookauthor.bid = book.bid";

   private final SQLiteStatement authorInsertStmt;
   private static final String AUTHOR_INSERT =
            "insert into " + DataConstants.AUTHOR_TABLE + "(" + DataConstants.NAME + ") values (?)";

   private SQLiteDatabase db;

   public AuthorDAO(SQLiteDatabase db) {
      this.db = db;

      // statements
      authorInsertStmt = db.compileStatement(AuthorDAO.AUTHOR_INSERT);
   }

   @Override
   public Author select(final long id) {
      Author a = null;
      Cursor c =
               db.query(DataConstants.AUTHOR_TABLE, new String[] { DataConstants.NAME }, DataConstants.AUTHORID
                        + " = ?", new String[] { String.valueOf(id) }, null, null, null, "1");
      if (c.moveToFirst()) {
         a = new Author();
         a.id = (id);
         a.name = (c.getString(0));
      }
      if (!c.isClosed()) {
         c.close();
      }
      return a;
   }

   public Author select(final String name) {
      Author a = null;
      Cursor c =
               db.query(DataConstants.AUTHOR_TABLE, new String[] { DataConstants.AUTHORID }, DataConstants.NAME
                        + " = ?", new String[] { name }, null, null, null, "1");
      if (c.moveToFirst()) {
         a = select(c.getLong(0));
      }
      if (!c.isClosed()) {
         c.close();
      }
      return a;
   }

   @Override
   public ArrayList<Author> selectAll() {
      ArrayList<Author> set = new ArrayList<Author>();
      Cursor c =
               db.query(DataConstants.AUTHOR_TABLE, new String[] { DataConstants.AUTHORID, DataConstants.NAME }, null,
                        null, null, null, DataConstants.NAME + " desc", null);
      if (c.moveToFirst()) {
         do {
            Author a = new Author();
            a.id = (c.getLong(0));
            a.name = (c.getString(1));
            set.add(a);
         } while (c.moveToNext());
      }
      if (!c.isClosed()) {
         c.close();
      }
      return set;
   }

   public ArrayList<Author> selectByBookId(final long bookId) {
      ArrayList<Author> authors = new ArrayList<Author>();
      // TODO string.format this with final String, faster?
      StringBuilder sb = new StringBuilder();
      sb.append(AuthorDAO.QUERY_AUTHORS_BY_BOOK_ID_PREFIX);
      sb.append(" where book.bid = " + bookId);
      sb.append(" order by author.name asc");
      Cursor c = db.rawQuery(sb.toString(), null);

      if (c.moveToFirst()) {
         do {
            Author a = new Author();
            a.name = c.getString(0);
            authors.add(a);
         } while (c.moveToNext());
      }
      if (!c.isClosed()) {
         c.close();
      }
      return authors;
   }

   @Override
   public long insert(final Author a) {
      authorInsertStmt.clearBindings();
      authorInsertStmt.bindString(1, a.name);
      return authorInsertStmt.executeInsert();
   }

   @Override
   public void update(Author a) {
      if ((a != null) && (a.id != 0)) {
         Author exists = select(a.id);
         if (exists == null) {
            throw new IllegalArgumentException("Cannot update entity that does not already exist.");
         }

         // use transaction
         db.beginTransaction();
         try {
            // update book
            final ContentValues values = new ContentValues();
            values.put(DataConstants.NAME, a.name);
            db.update(DataConstants.AUTHOR_TABLE, values, DataConstants.AUTHORID + " = ?", new String[] { String
                     .valueOf(a.id) });

            db.setTransactionSuccessful();
         } catch (SQLException e) {
            Log.e(Constants.LOG_TAG, "Error updating author.", e);
         } finally {
            db.endTransaction();
         }
      } else {
         throw new IllegalArgumentException("Error, author cannot be null.");
      }
   }

   @Override
   public void delete(final long id) {
      Author a = select(id);
      if (a != null) {
         db.delete(DataConstants.AUTHOR_TABLE, DataConstants.AUTHORID + " = ?", new String[] { String.valueOf(id) });
      }
   }

   public void delete(final String name) {
      Author a = select(name);
      if (a != null) {
         db.delete(DataConstants.AUTHOR_TABLE, DataConstants.NAME + " = ?", new String[] { name });
      }
   }
}
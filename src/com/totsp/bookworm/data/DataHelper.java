package com.totsp.bookworm.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import com.totsp.bookworm.model.Author;
import com.totsp.bookworm.model.Book;

import java.util.Date;

public class DataHelper {

   private static final String DATABASE_NAME = "bookworm.db";
   private static final int DATABASE_VERSION = 1;
   private static final String BOOK_TABLE = "book";
   private static final String BOOKDATA_TABLE = "bookdata";
   private static final String BOOKLIST_TABLE = "booklist";
   private static final String BOOKLISTJOIN_TABLE = "booklistjoin";
   private static final String AUTHOR_TABLE = "author";

   private Context context;
   private SQLiteDatabase db;

   private SQLiteStatement bookInsertStmt;
   private static final String BOOK_INSERT =
            "insert into " + BOOK_TABLE + "(" + DbConstants.ISBN + "," + DbConstants.AUTHORID + "," + DbConstants.TITLE
                     + "," + DbConstants.DATEPUB + ") values (?, ?, ?, ?)";
   private SQLiteStatement authorInsertStmt;
   private static final String AUTHOR_INSERT = "insert into " + AUTHOR_TABLE + "(" + DbConstants.NAME + ") values (?)";

   public DataHelper(Context context) {
      this.context = context;
      OpenHelper openHelper = new OpenHelper(this.context);
      this.db = openHelper.getWritableDatabase();

      this.bookInsertStmt = this.db.compileStatement(BOOK_INSERT);
   }

   public long insertBook(Book b) {
      this.bookInsertStmt.bindString(1, b.getIsbn());
      this.bookInsertStmt.bindLong(2, b.getAuthorId());
      this.bookInsertStmt.bindString(3, b.getTitle());
      this.bookInsertStmt.bindLong(4, b.getDatePub().getTime());
      return this.bookInsertStmt.executeInsert();
   }

   public Book selectBook(long id) {
      Book b = null;
      Cursor c =
               this.db.query(BOOK_TABLE, new String[] { DbConstants.ISBN, DbConstants.AUTHORID, DbConstants.TITLE,
                        DbConstants.DATEPUB }, DbConstants.BOOKID + " = ?", new String[] { String.valueOf(id) }, null,
                        null, null, "1");
      if (c.moveToFirst()) {
         b = new Book();
         b.setId(id);
         b.setIsbn(c.getString(0));
         b.setAuthorId(Long.valueOf(c.getString(1)));
         b.setTitle(c.getString(2));
         b.setDatePub(new Date(c.getLong(3)));
      }
      return b;
   }

   public long insertAuthor(Author a) {
      this.authorInsertStmt.bindString(1, a.getName());
      return this.authorInsertStmt.executeInsert();
   }

   public Author selectAuthor(long id) {
      Author a = null;
      Cursor c =
               this.db.query(AUTHOR_TABLE, new String[] { DbConstants.NAME }, DbConstants.AUTHORID + " = ?",
                        new String[] { String.valueOf(id) }, null, null, null, "1");
      if (c.moveToFirst()) {
         a = new Author();
         a.setId(id);
         a.setName(c.getString(0));
      }
      return a;
   }

   public void cleanup() {
      if (this.db != null && this.db.isOpen()) {
         this.db.close();
      }
   }

   private static class OpenHelper extends SQLiteOpenHelper {

      OpenHelper(Context context) {
         super(context, DATABASE_NAME, null, DATABASE_VERSION);
      }

      @Override
      public void onCreate(SQLiteDatabase db) {

         // (select count(*) from sqlite_master where name="foo"

         ///db.execSQL("select count(*) from 

         // book table
         db.execSQL("CREATE TABLE " + BOOK_TABLE + " (" + DbConstants.BOOKID + " INTEGER PRIMARY KEY,"
                  + DbConstants.ISBN + " TEXT," + DbConstants.TITLE + " TEXT," + DbConstants.AUTHORID + " INTEGER,"
                  + DbConstants.DATEPUB + " INTEGER" + ");");

         // author table
         db.execSQL("CREATE TABLE " + AUTHOR_TABLE + " (" + DbConstants.AUTHORID + " INTEGER PRIMARY KEY,"
                  + DbConstants.NAME + " TEXT" + ");");

         // bookdata table (users book data, ratings, reviews, etc)
         db.execSQL("CREATE TABLE " + BOOKDATA_TABLE + " (" + DbConstants.BOOKDATAID + " INTEGER PRIMARY KEY,"
                  + DbConstants.BOOKID + " INTEGER," + DbConstants.RATING + " INTEGER," + DbConstants.BLURB + " TEXT"
                  + ");");

         // booklist table 
         db.execSQL("CREATE TABLE " + BOOKLIST_TABLE + " (" + DbConstants.BOOKLISTID + " INTEGER PRIMARY KEY,"
                  + DbConstants.NAME + " TEXT" + ");");

         // booklistjoin table 
         db.execSQL("CREATE TABLE " + BOOKLISTJOIN_TABLE + " (" + DbConstants.BOOKID + " INTEGER,"
                  + DbConstants.BOOKLISTID + " INTEGER" + ");");
      }

      @Override
      public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
         Log.w("BookWorm", "Upgrading database not yet implemented");
         // export old data first, then upgrade, then import
         //db.execSQL("DROP TABLE IF EXISTS " + BOOK_TABLE);
         //onCreate(db);
      }
   }

}
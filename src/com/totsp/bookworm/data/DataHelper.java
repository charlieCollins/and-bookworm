package com.totsp.bookworm.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import com.totsp.bookworm.model.Book;

public class DataHelper {

   private static final String DATABASE_NAME = "bookworm.db";
   private static final int DATABASE_VERSION = 1;
   private static final String BOOK_TABLE_NAME = "book";
   private static final String BOOKDATA_TABLE_NAME = "bookdata";
   private static final String BOOKLIST_TABLE_NAME = "booklist";
   private static final String BOOKLISTJOIN_TABLE_NAME = "booklistjoin";
   private static final String AUTHOR_TABLE_NAME = "author";

   private Context context;
   private SQLiteDatabase db;

   private SQLiteStatement bookInsertStmt;
   private static final String BOOK_INSERT =
            "insert into " + BOOK_TABLE_NAME + "(" + DbConstants.ISBN + "," + DbConstants.AUTHORID + ","
                     + DbConstants.TITLE + "," + DbConstants.DATEPUB + ") values (?, ?, ?, ?)";
   private SQLiteStatement bookSelectStmt;
   private static final String BOOK_SELECT =
            "select " + DbConstants.ISBN + "," + DbConstants.AUTHORID + "," + DbConstants.TITLE + ","
                     + DbConstants.DATEPUB + " from " + BOOK_TABLE_NAME;

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

   public void selectBook(long id) {
      
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
         db.execSQL("CREATE TABLE " + BOOK_TABLE_NAME + " (" + DbConstants.BOOKID + " INTEGER PRIMARY KEY,"
                  + DbConstants.ISBN + " TEXT," + DbConstants.TITLE + " TEXT," + DbConstants.AUTHORID + " INTEGER,"
                  + DbConstants.DATEPUB + " INTEGER" + ");");

         // author table
         db.execSQL("CREATE TABLE " + AUTHOR_TABLE_NAME + " (" + DbConstants.AUTHORID + " INTEGER PRIMARY KEY,"
                  + DbConstants.NAME + " TEXT" + ");");

         // bookdata table (users book data, ratings, reviews, etc)
         db.execSQL("CREATE TABLE " + BOOKDATA_TABLE_NAME + " (" + DbConstants.BOOKDATAID + " INTEGER PRIMARY KEY,"
                  + DbConstants.BOOKID + " INTEGER," + DbConstants.RATING + " INTEGER," + DbConstants.BLURB + " TEXT"
                  + ");");

         // booklist table 
         db.execSQL("CREATE TABLE " + BOOKLIST_TABLE_NAME + " (" + DbConstants.BOOKLISTID + " INTEGER PRIMARY KEY,"
                  + DbConstants.NAME + " TEXT" + ");");

         // booklistjoin table 
         db.execSQL("CREATE TABLE " + BOOKLISTJOIN_TABLE_NAME + " (" + DbConstants.BOOKID + " INTEGER,"
                  + DbConstants.BOOKLISTID + " INTEGER" + ");");
      }

      @Override
      public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
         Log.w("BookWorm", "Upgrading database not yet implemented");
         // export old data first, then upgrade, then import
         //db.execSQL("DROP TABLE IF EXISTS " + BOOK_TABLE_NAME);
         //onCreate(db);
      }
   }

}
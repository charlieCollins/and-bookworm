package com.totsp.bookworm.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
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
    private DatabaseHelper dbHelper;
    
    public DataHelper(Context context) {
       this.context = context;
       this.dbHelper = new DatabaseHelper(this.context);
    }
    
    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
           
           // book table
            db.execSQL("CREATE TABLE " + BOOK_TABLE_NAME + " ("
                    + DbConstants.BOOKID + " INTEGER PRIMARY KEY,"
                    + DbConstants.ISBN + " TEXT,"
                    + DbConstants.TITLE + " TEXT,"
                    + DbConstants.AUTHORID + " INTEGER,"
                    + DbConstants.DATEPUB + " INTEGER"                   
                    + ");");
            
            // author table
            db.execSQL("CREATE TABLE " + AUTHOR_TABLE_NAME + " ("
                     + DbConstants.AUTHORID + " INTEGER PRIMARY KEY,"
                     + DbConstants.NAME + " TEXT"
                     + ");");
            
            // bookdata table (users book data, ratings, reviews, etc)
            db.execSQL("CREATE TABLE " + BOOKDATA_TABLE_NAME + " ("
                     + DbConstants.BOOKDATAID + " INTEGER PRIMARY KEY,"
                     + DbConstants.BOOKID + " INTEGER,"
                     + DbConstants.RATING + " INTEGER,"
                     + DbConstants.BLURB + " TEXT"
                     + ");");
            
            // booklist table 
            db.execSQL("CREATE TABLE " + BOOKLIST_TABLE_NAME + " ("
                     + DbConstants.BOOKLISTID + " INTEGER PRIMARY KEY,"
                     + DbConstants.NAME + " TEXT"
                     + ");");
            
            // booklistjoin table 
            db.execSQL("CREATE TABLE " + BOOKLISTJOIN_TABLE_NAME + " ("                   
                     + DbConstants.BOOKID + " INTEGER,"
                     + DbConstants.BOOKLISTID + " INTEGER"
                     + ");");
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
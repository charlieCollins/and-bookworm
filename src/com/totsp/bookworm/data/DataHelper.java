package com.totsp.bookworm.data;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import com.totsp.bookworm.Constants;
import com.totsp.bookworm.model.Author;
import com.totsp.bookworm.model.Book;

import java.util.HashSet;

public class DataHelper {

   private static final String DATABASE_NAME = "bookworm.db";
   private static final int DATABASE_VERSION = 3;
   private static final String BOOK_TABLE = "book";
   private static final String BOOKUSERDATA_TABLE = "bookuserdata";
   private static final String BOOKAUTHOR_TABLE = "bookauthor";
   private static final String AUTHOR_TABLE = "author";

   private Context context;
   private SQLiteDatabase db;

   private SQLiteStatement bookInsertStmt;
   private static final String BOOK_INSERT =
            "insert into " + BOOK_TABLE + "(" + DataConstants.ISBN + "," + DataConstants.TITLE + ","
                     + DataConstants.COVERIMAGEID + "," + DataConstants.PUBLISHER + "," + DataConstants.DESCRIPTION + ","
                     + DataConstants.FORMAT + "," + DataConstants.SUBJECT + "," + DataConstants.OVERVIEWURL + ","
                     + DataConstants.DATEPUB + ") values (?, ?, ?, ?, ?, ?, ?, ?, ?)";
   private SQLiteStatement bookAuthorInsertStmt;
   private static final String BOOKAUTHOR_INSERT =
            "insert into " + BOOKAUTHOR_TABLE + "(" + DataConstants.BOOKID + "," + DataConstants.AUTHORID
                     + ") values (?, ?)";
   private SQLiteStatement authorInsertStmt;
   private static final String AUTHOR_INSERT = "insert into " + AUTHOR_TABLE + "(" + DataConstants.NAME + ") values (?)";

   public DataHelper(Context context) {
      this.context = context;
      OpenHelper openHelper = new OpenHelper(this.context);
      this.db = openHelper.getWritableDatabase();

      // statements
      this.bookInsertStmt = this.db.compileStatement(BOOK_INSERT);
      this.bookAuthorInsertStmt = this.db.compileStatement(BOOKAUTHOR_INSERT);
      this.authorInsertStmt = this.db.compileStatement(AUTHOR_INSERT);

      if (openHelper.isDbCreated()) {
         // insert default data here if needed
      }
   }

   public void cleanup() {
      if (this.db != null && this.db.isOpen()) {
         this.db.close();
      }
   }

   //
   // DB methods
   //

   // book
   public long insertBook(Book b) {
      long bookId = 0L;

      // TODO validate book, must have isbn and title, etc

      // TODO simplify this with ContentValues?
      // what is speed vs Statement?
      /*
      final ContentValues values = new ContentValues();
      values.put(DbConstants.ISBN, b.getIsbn());
      values.put(DbConstants.TITLE, b.getIsbn());
      values.put(DbConstants.COVERIMAGEID, b.getIsbn());
      values.put(DbConstants.PUBLISHER, b.getIsbn());
      values.put(DbConstants.DESCRIPTION, b.getIsbn());
      values.put(DbConstants.FORMAT, b.getIsbn());
      values.put(DbConstants.SUBJECT, b.getIsbn());
      values.put(DbConstants.OVERVIEWURL, b.getIsbn());
      values.put(DbConstants.DATEPUB, b.getIsbn());
      db.insert(BOOK_TABLE, DbConstants.DATEPUB, values);
      */

      Book bookExists = this.selectBook(b.getIsbn());
      if (bookExists != null) {
         return bookExists.getId();
      }

      // use transaction
      this.db.beginTransaction();
      try {
         // insert authors as needed
         HashSet<Long> authorIds = new HashSet<Long>();
         if (b.getAuthors() != null && !b.getAuthors().isEmpty()) {
            for (Author a : b.getAuthors()) {
               Author authorExists = this.selectAuthor(a.getName());
               if (authorExists == null) {
                  authorIds.add(this.insertAuthor(a));
               } else {
                  authorIds.add(authorExists.getId());
               }
            }
         }

         // insert book
         this.bookInsertStmt.clearBindings();
         this.bookInsertStmt.bindString(1, b.getIsbn());
         this.bookInsertStmt.bindString(2, b.getTitle());
         this.bookInsertStmt.bindLong(3, b.getCoverImageId());
         this.bookInsertStmt.bindString(4, b.getPublisher());
         this.bookInsertStmt.bindString(5, b.getDescription());
         this.bookInsertStmt.bindString(6, b.getFormat());
         this.bookInsertStmt.bindString(7, b.getSubject());
         this.bookInsertStmt.bindString(8, b.getOverviewUrl());
         this.bookInsertStmt.bindLong(9, b.getDatePubStamp());
         bookId = this.bookInsertStmt.executeInsert();

         // insert bookauthors
         for (Long authorId : authorIds) {
            this.bookAuthorInsertStmt.clearBindings();
            this.bookAuthorInsertStmt.bindLong(1, bookId);
            this.bookAuthorInsertStmt.bindLong(2, authorId);
            this.bookAuthorInsertStmt.executeInsert();
         }

         db.setTransactionSuccessful();
      } catch (SQLException e) {
         Log.e(Constants.LOG_TAG, "Error inserting book", e);
      } finally {
         this.db.endTransaction();
      }

      return bookId;
   }

   public Book selectBook(long id) {
      Book b = null;
      Cursor c =
               this.db.query(BOOK_TABLE, new String[] { DataConstants.ISBN, DataConstants.TITLE, DataConstants.COVERIMAGEID,
                        DataConstants.PUBLISHER, DataConstants.DESCRIPTION, DataConstants.FORMAT, DataConstants.SUBJECT,
                        DataConstants.OVERVIEWURL, DataConstants.DATEPUB }, DataConstants.BOOKID + " = ?",
                        new String[] { String.valueOf(id) }, null, null, null, "1");
      if (c.moveToFirst()) {
         b = new Book();
         b.setId(id);
         b.setIsbn(c.getString(0));
         b.setTitle(c.getString(1));
         b.setCoverImageId(c.getLong(2));
         b.setPublisher(c.getString(3));
         b.setDescription(c.getString(4));
         b.setFormat(c.getString(5));
         b.setSubject(c.getString(6));
         b.setOverviewUrl(c.getString(7));
         b.setDatePubStamp(c.getLong(8));
         b.setAuthors(this.selectAuthorsByBookId(id));
      }
      if (c != null && !c.isClosed()) {
         c.close();
      }
      return b;
   }

   public Book selectBook(String isbn) {
      Book b = null;
      Cursor c =
               this.db.query(BOOK_TABLE, new String[] { DataConstants.BOOKID }, DataConstants.ISBN + " = ?",
                        new String[] { isbn }, null, null, null, "1");
      if (c.moveToFirst()) {
         b = this.selectBook(c.getLong(0));
      }
      if (c != null && !c.isClosed()) {
         c.close();
      }
      return b;
   }

   public HashSet<Book> selectAllBooks() {
      HashSet<Book> set = new HashSet<Book>();
      Cursor c =
               this.db.query(BOOK_TABLE, new String[] { DataConstants.BOOKID, DataConstants.ISBN, DataConstants.TITLE,
                        DataConstants.COVERIMAGEID, DataConstants.PUBLISHER, DataConstants.DESCRIPTION, DataConstants.FORMAT,
                        DataConstants.SUBJECT, DataConstants.OVERVIEWURL, DataConstants.DATEPUB }, null, null, null, null,
                        null, null);
      if (c.moveToFirst()) {
         do {
            Book b = new Book();
            b.setId(c.getLong(0));
            b.setIsbn(c.getString(1));
            b.setTitle(c.getString(2));
            b.setCoverImageId(c.getLong(3));
            b.setPublisher(c.getString(4));
            b.setDescription(c.getString(5));
            b.setFormat(c.getString(6));
            b.setSubject(c.getString(7));
            b.setOverviewUrl(c.getString(8));
            b.setDatePubStamp(c.getLong(9));
            b.setAuthors(this.selectAuthorsByBookId(b.getId()));
            set.add(b);
         } while (c.moveToNext());
      }
      if (c != null && !c.isClosed()) {
         c.close();
      }
      return set;
   }

   public HashSet<String> selectAllBookNames() {
      HashSet<String> set = new HashSet<String>();
      Cursor c =
               this.db.query(BOOK_TABLE, new String[] { DataConstants.TITLE }, null, null, null, null, DataConstants.TITLE
                        + " desc");
      if (c.moveToFirst()) {
         do {
            set.add(c.getString(0));
         } while (c.moveToNext());
      }
      if (c != null && !c.isClosed()) {
         c.close();
      }
      return set;
   }

   public void deleteBook(long id) {
      // TODO deleting a book should check authors (if no other books, delete author too)
      this.db.delete(BOOK_TABLE, DataConstants.BOOKID + "= ?", new String[] { String.valueOf(id) });
   }

   public void deleteBook(String isbn) {
      // TODO deleting a book should check authors (if no other books, delete author too)
      this.db.delete(BOOK_TABLE, DataConstants.ISBN + "= ?", new String[] { isbn });
   }

   // author
   public long insertAuthor(Author a) {
      this.authorInsertStmt.clearBindings();
      this.authorInsertStmt.bindString(1, a.getName());
      return this.authorInsertStmt.executeInsert();
   }

   public Author selectAuthor(long id) {
      Author a = null;
      Cursor c =
               this.db.query(AUTHOR_TABLE, new String[] { DataConstants.NAME }, DataConstants.AUTHORID + " = ?",
                        new String[] { String.valueOf(id) }, null, null, null, "1");
      if (c.moveToFirst()) {
         a = new Author();
         a.setId(id);
         a.setName(c.getString(0));
      }
      if (c != null && !c.isClosed()) {
         c.close();
      }
      return a;
   }

   public Author selectAuthor(String name) {
      Author a = null;
      Cursor c =
               this.db.query(AUTHOR_TABLE, new String[] { DataConstants.AUTHORID }, DataConstants.NAME + " = ?",
                        new String[] { name }, null, null, null, "1");
      if (c.moveToFirst()) {
         a = new Author();
         a.setId(c.getLong(0));
         a.setName(name);
      }
      if (c != null && !c.isClosed()) {
         c.close();
      }
      return a;
   }

   public HashSet<Author> selectAuthorsByBookId(long bookId) {
      HashSet<Author> authors = new HashSet<Author>();
      HashSet<Long> authorIds = new HashSet<Long>();
      Cursor c =
               this.db.query(BOOKAUTHOR_TABLE, new String[] { DataConstants.AUTHORID }, DataConstants.BOOKID + " = ?",
                        new String[] { String.valueOf(bookId) }, null, null, null, null);
      if (c.moveToFirst()) {
         do {
            authorIds.add(c.getLong(0));
         } while (c.moveToNext());
      }
      if (c != null && !c.isClosed()) {
         c.close();
      }
      if (!authorIds.isEmpty()) {
         for (Long authorId : authorIds) {
            authors.add(this.selectAuthor(authorId));
         }
      }
      return authors;
   }

   public HashSet<Author> selectAllAuthors() {
      HashSet<Author> set = new HashSet<Author>();
      Cursor c =
               this.db.query(AUTHOR_TABLE, new String[] { DataConstants.AUTHORID, DataConstants.NAME }, null, null, null,
                        null, DataConstants.NAME + " desc");
      if (c.moveToFirst()) {
         do {
            Author a = new Author();
            a.setId(c.getLong(0));
            a.setName(c.getString(1));
            set.add(a);
         } while (c.moveToNext());
      }
      if (c != null && !c.isClosed()) {
         c.close();
      }
      return set;
   }

   public void deleteAuthor(long id) {
      this.db.delete(AUTHOR_TABLE, DataConstants.AUTHORID + "= ?", new String[] { String.valueOf(id) });
   }

   public void deleteAuthor(String name) {
      this.db.delete(AUTHOR_TABLE, DataConstants.NAME + "= ?", new String[] { name });
   }

   //
   // end DB methods
   //  

   //
   // SQLiteOpenHelper   
   //

   private static class OpenHelper extends SQLiteOpenHelper {

      private boolean dbCreated;

      OpenHelper(Context context) {
         super(context, DATABASE_NAME, null, DATABASE_VERSION);
         Log.d(Constants.LOG_TAG, "SQLiteOpenHelper");
      }

      @Override
      public void onCreate(SQLiteDatabase db) {

         Log.d(Constants.LOG_TAG, "SQLiteOpenHelper onCreate");

         // book table
         db.execSQL("CREATE TABLE " + BOOK_TABLE + " (" + DataConstants.BOOKID + " INTEGER PRIMARY KEY,"
                  + DataConstants.ISBN + " TEXT," + DataConstants.TITLE + " TEXT," + DataConstants.COVERIMAGEID + " INTEGER,"
                  + DataConstants.PUBLISHER + " TEXT," + DataConstants.DESCRIPTION + " TEXT," + DataConstants.FORMAT
                  + " TEXT," + DataConstants.SUBJECT + " TEXT," + DataConstants.OVERVIEWURL + " TEXT,"
                  + DataConstants.DATEPUB + " INTEGER" + ");");

         // author table
         db.execSQL("CREATE TABLE " + AUTHOR_TABLE + " (" + DataConstants.AUTHORID + " INTEGER PRIMARY KEY,"
                  + DataConstants.NAME + " TEXT" + ");");

         // bookauthor table
         db.execSQL("CREATE TABLE " + BOOKAUTHOR_TABLE + " (" + DataConstants.BOOKAUTHORID + " INTEGER PRIMARY KEY,"
                  + DataConstants.BOOKID + " INTEGER," + DataConstants.AUTHORID + " INTEGER)");

         // bookdata table (users book data, ratings, reviews, etc)
         db.execSQL("CREATE TABLE " + BOOKUSERDATA_TABLE + " (" + DataConstants.BOOKUSERDATAID + " INTEGER PRIMARY KEY,"
                  + DataConstants.BOOKID + " INTEGER," + DataConstants.READSTATUS + " INTEGER," + DataConstants.RATING
                  + " INTEGER," + DataConstants.BLURB + " TEXT" + ");");

         // constraints
         db.execSQL("CREATE UNIQUE INDEX uidxBookIsbn ON " + BOOK_TABLE + "(" + DataConstants.ISBN + " COLLATE NOCASE)");
         db.execSQL("CREATE UNIQUE INDEX uidxAuthorName ON " + AUTHOR_TABLE + "(" + DataConstants.NAME
                  + " COLLATE NOCASE)");

         this.dbCreated = true;
      }

      @Override
      public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
         Log.w(Constants.LOG_TAG, "Upgrading database not yet implemented");
         // export old data first, then upgrade, then import
         db.execSQL("DROP TABLE IF EXISTS " + BOOK_TABLE);
         db.execSQL("DROP TABLE IF EXISTS " + AUTHOR_TABLE);
         db.execSQL("DROP TABLE IF EXISTS " + BOOKUSERDATA_TABLE);
         db.execSQL("DROP TABLE IF EXISTS " + BOOKAUTHOR_TABLE);
         onCreate(db);
      }

      public boolean isDbCreated() {
         return this.dbCreated;
      }
   }

}
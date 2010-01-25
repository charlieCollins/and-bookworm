package com.totsp.bookworm.data;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import com.totsp.bookworm.Splash;
import com.totsp.bookworm.model.Author;
import com.totsp.bookworm.model.Book;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class DataHelper {

   private static final String DATABASE_NAME = "bookworm.db";
   private static final int DATABASE_VERSION = 2;
   private static final String BOOK_TABLE = "book";
   private static final String BOOKUSERDATA_TABLE = "bookuserdata";
   private static final String BOOKAUTHOR_TABLE = "bookauthor";
   private static final String AUTHOR_TABLE = "author";

   private Context context;
   private SQLiteDatabase db;

   private SQLiteStatement bookInsertStmt;
   private static final String BOOK_INSERT =
            "insert into " + BOOK_TABLE + "(" + DbConstants.ISBN + "," + DbConstants.TITLE + "," + DbConstants.DATEPUB
                     + ") values (?, ?, ?)";
   private SQLiteStatement bookAuthorInsertStmt;
   private static final String BOOKAUTHOR_INSERT =
            "insert into " + BOOKAUTHOR_TABLE + "(" + DbConstants.BOOKID + "," + DbConstants.AUTHORID
                     + ") values (?, ?)";
   private SQLiteStatement authorInsertStmt;
   private static final String AUTHOR_INSERT = "insert into " + AUTHOR_TABLE + "(" + DbConstants.NAME + ") values (?)";

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

   // book
   public long insertBook(Book b) {
      long bookId = 0L;

      Book bookExists = this.selectBook(b.getIsbn());
      if (bookExists != null) {
         return bookExists.getId();
      }

      // use transaction
      this.db.beginTransaction();
      try {
         // insert authors as needed
         Set<Long> authorIds = new HashSet<Long>();
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
         if (b.getDatePub() != null) {
            this.bookInsertStmt.bindLong(3, b.getDatePub().getTime());
         }
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
         Log.e(Splash.APP_NAME, "Error inserting book", e);
      } finally {
         this.db.endTransaction();
      }

      return bookId;
   }

   // TODO all these queries need to be redone - have added many fields to Book
   
   public Book selectBook(long id) {
      Book b = null;
      Cursor c =
               this.db.query(BOOK_TABLE, new String[] { DbConstants.ISBN, DbConstants.TITLE, DbConstants.DATEPUB },
                        DbConstants.BOOKID + " = ?", new String[] { String.valueOf(id) }, null, null, null, "1");
      if (c.moveToFirst()) {
         b = new Book();
         b.setId(id);
         b.setIsbn(c.getString(0));
         b.setTitle(c.getString(1));
         b.setDatePub(new Date(c.getLong(2)));
         // TODO add authors to book
      }
      return b;
   }

   public Book selectBook(String isbn) {
      Book b = null;
      Cursor c =
               this.db.query(BOOK_TABLE, new String[] { DbConstants.BOOKID, DbConstants.TITLE, DbConstants.DATEPUB },
                        DbConstants.ISBN + " = ?", new String[] { isbn }, null, null, null, "1");
      if (c.moveToFirst()) {
         b = new Book();
         b.setId(c.getLong(0));
         b.setIsbn(isbn);
         b.setTitle(c.getString(1));
         b.setDatePub(new Date(c.getLong(2)));
         // TODO add authors to book
      }
      return b;
   }

   public Set<Book> selectAllBooks() {
      Set<Book> set = new HashSet<Book>();
      Cursor c =
               this.db.query(BOOK_TABLE, new String[] { DbConstants.BOOKID, DbConstants.ISBN, DbConstants.TITLE,
                        DbConstants.DATEPUB }, null, null, null, null, DbConstants.TITLE + " desc");
      if (c.moveToFirst()) {
         do {
            Book b = new Book();
            b.setId(c.getInt(0));
            b.setIsbn(c.getString(1));
            b.setTitle(c.getString(2));
            b.setDatePub(new Date(c.getLong(3)));
            // TODO add authors to book
            set.add(b);
         } while (c.moveToNext());
      }
      return set;
   }
   
   public Set<String> selectAllBookNames() {
      Set<String> set = new HashSet<String>();
      Cursor c =
               this.db.query(BOOK_TABLE, new String[] { DbConstants.TITLE }, null, null, null, null, DbConstants.TITLE + " desc");
      if (c.moveToFirst()) {
         do {            
            set.add(c.getString(0));
         } while (c.moveToNext());
      }
      return set;
   }

   /*
   public List<Book> selectAllBooksByAuthor(long authorId) {
      List<Book> list = new ArrayList<Book>();
      // TODO
      return list;
   }
   */

   public void deleteBook(long id) {
      // TODO deleting a book should check authors (if no other books, delete author too)
      this.db.delete(BOOK_TABLE, DbConstants.BOOKID + "= ?", new String[] { String.valueOf(id) });
   }

   public void deleteBook(String isbn) {
      // TODO deleting a book should check authors (if no other books, delete author too)
      this.db.delete(BOOK_TABLE, DbConstants.ISBN + "= ?", new String[] { isbn });
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
               this.db.query(AUTHOR_TABLE, new String[] { DbConstants.NAME }, DbConstants.AUTHORID + " = ?",
                        new String[] { String.valueOf(id) }, null, null, null, "1");
      if (c.moveToFirst()) {
         a = new Author();
         a.setId(id);
         a.setName(c.getString(0));
      }
      return a;
   }

   public Author selectAuthor(String name) {
      Author a = null;
      Cursor c =
               this.db.query(AUTHOR_TABLE, new String[] { DbConstants.AUTHORID }, DbConstants.NAME + " = ?",
                        new String[] { name }, null, null, null, "1");
      if (c.moveToFirst()) {
         a = new Author();
         a.setId(c.getLong(0));
         a.setName(name);
      }
      return a;
   }

   public Set<Author> selectAllAuthors() {
      Set<Author> set = new HashSet<Author>();
      Cursor c =
               this.db.query(AUTHOR_TABLE, new String[] { DbConstants.AUTHORID, DbConstants.NAME }, null, null, null,
                        null, DbConstants.NAME + " desc");
      if (c.moveToFirst()) {
         do {
            Author a = new Author();
            a.setId(c.getLong(0));
            a.setName(c.getString(1));
            set.add(a);
         } while (c.moveToNext());
      }
      return set;
   }

   /*
   public Set<Author> selectAllAuthors(Book b) {
      Set<Author> set = new HashSet<Author>();
     
      
      
      Cursor c =
               this.db.query(AUTHOR_TABLE, new String[] { DbConstants.NAME }, null, null, null, null, DbConstants.NAME
                        + " desc");
      if (c.moveToFirst()) {
         do {
            Author a = new Author();
            a.setId(c.getLong(0));
            a.setName(c.getString(1));
            list.add(a);
         } while (c.moveToNext());
      }
      return list;
   }
   */

   public void deleteAuthor(long id) {
      this.db.delete(AUTHOR_TABLE, DbConstants.AUTHORID + "= ?", new String[] { String.valueOf(id) });
   }

   public void deleteAuthor(String name) {
      this.db.delete(AUTHOR_TABLE, DbConstants.NAME + "= ?", new String[] { name });
   }

   public void cleanup() {
      if (this.db != null && this.db.isOpen()) {
         this.db.close();
      }
   }

   private static class OpenHelper extends SQLiteOpenHelper {

      private boolean dbCreated;

      OpenHelper(Context context) {
         super(context, DATABASE_NAME, null, DATABASE_VERSION);
         Log.d(Splash.APP_NAME, "SQLiteOpenHelper");
      }

      @Override
      public void onCreate(SQLiteDatabase db) {

         Log.d(Splash.APP_NAME, "SQLiteOpenHelper onCreate");

         ///Cursor c = db.rawQuery("select count(*) from sqlite_master where name = ?", new String[] { BOOK_TABLE });
         ///int count = c.getCount();
         ///Log.d(Splash.APP_NAME, "count - " + count);

         // book table
         db.execSQL("CREATE TABLE " + BOOK_TABLE + " (" + DbConstants.BOOKID + " INTEGER PRIMARY KEY,"
                  + DbConstants.ISBN + " TEXT," + DbConstants.TITLE + " TEXT,"                   
                  + DbConstants.PUBLISHER + " TEXT,"
                  + DbConstants.IMAGEURL + " TEXT,"
                  + DbConstants.OVERVIEWURL + " TEXT,"
                  + DbConstants.DESCRIPTION + " TEXT,"
                  + DbConstants.SUBJECT + " TEXT,"
                  + DbConstants.FORMAT + " TEXT,"                  
                  + DbConstants.DATEPUB + " INTEGER"
                  + ");");

         // author table
         db.execSQL("CREATE TABLE " + AUTHOR_TABLE + " (" + DbConstants.AUTHORID + " INTEGER PRIMARY KEY,"
                  + DbConstants.NAME + " TEXT" + ");");

         // bookauthor table
         db.execSQL("CREATE TABLE " + BOOKAUTHOR_TABLE + " (" + DbConstants.BOOKAUTHORID + " INTEGER PRIMARY KEY,"
                  + DbConstants.BOOKID + " INTEGER," + DbConstants.AUTHORID + " INTEGER)");

         // bookdata table (users book data, ratings, reviews, etc)
         db.execSQL("CREATE TABLE " + BOOKUSERDATA_TABLE + " (" + DbConstants.BOOKUSERDATAID + " INTEGER PRIMARY KEY,"
                  + DbConstants.BOOKID + " INTEGER," + DbConstants.READSTATUS + " INTEGER," + DbConstants.RATING
                  + " INTEGER," + DbConstants.BLURB + " TEXT" + ");");

         // constraints
         db.execSQL("CREATE UNIQUE INDEX uidxBookIsbn ON " + BOOK_TABLE + "(" + DbConstants.ISBN + " COLLATE NOCASE)");
         db.execSQL("CREATE UNIQUE INDEX uidxAuthorName ON " + AUTHOR_TABLE + "(" + DbConstants.NAME
                  + " COLLATE NOCASE)");

         this.dbCreated = true;
      }

      @Override
      public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
         Log.w("BookWorm", "Upgrading database not yet implemented");
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
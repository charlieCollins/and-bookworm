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

/**
 * Android DataHelper to encapsulate SQL and DB details.
 * Includes SQLiteOpenHelper.
 * 
 * @author ccollins
 *
 */
public class DataHelper {

   // TODO make a generic interface then impl for particular data type (IDataHelper<Book>)
   // TODO create a cache for data type?

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
                     + DataConstants.SUBTITLE + "," + DataConstants.COVERIMAGEID + "," + DataConstants.COVERIMAGETINYID
                     + "," + DataConstants.PUBLISHER + "," + DataConstants.DESCRIPTION + "," + DataConstants.FORMAT
                     + "," + DataConstants.SUBJECT + "," + DataConstants.DATEPUB
                     + ") values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

   private SQLiteStatement bookAuthorInsertStmt;
   private static final String BOOKAUTHOR_INSERT =
            "insert into " + BOOKAUTHOR_TABLE + "(" + DataConstants.BOOKID + "," + DataConstants.AUTHORID
                     + ") values (?, ?)";

   private SQLiteStatement authorInsertStmt;
   private static final String AUTHOR_INSERT =
            "insert into " + AUTHOR_TABLE + "(" + DataConstants.NAME + ") values (?)";

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
      if (b != null && b.getTitle() != null) {
         // TODO simplify this with ContentValues?
         // what is speed vs Statement?
         /*
         final ContentValues values = new ContentValues();
         values.put(DbConstants.ISBN, b.getIsbn());
         ...
         db.insert(BOOK_TABLE, DbConstants.DATEPUB, values);
         */

         Book bookExists = this.selectBook(b.getTitle());
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
            this.bookInsertStmt.bindString(3, b.getSubTitle());
            this.bookInsertStmt.bindLong(4, b.getCoverImageId());
            this.bookInsertStmt.bindLong(5, b.getCoverImageTinyId());
            this.bookInsertStmt.bindString(6, b.getPublisher());
            this.bookInsertStmt.bindString(7, b.getDescription());
            this.bookInsertStmt.bindString(8, b.getFormat());
            this.bookInsertStmt.bindString(9, b.getSubject());
            this.bookInsertStmt.bindLong(10, b.getDatePubStamp());
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
      } else {
         throw new IllegalArgumentException("Error, book cannot be null, and must have a unique title");
      }
      return bookId;
   }

   public Book selectBook(long id) {
      Book b = null;
      Cursor c =
               this.db.query(BOOK_TABLE, new String[] { DataConstants.ISBN, DataConstants.TITLE,
                        DataConstants.SUBTITLE, DataConstants.COVERIMAGEID, DataConstants.COVERIMAGETINYID,
                        DataConstants.PUBLISHER, DataConstants.DESCRIPTION, DataConstants.FORMAT,
                        DataConstants.SUBJECT, DataConstants.DATEPUB }, DataConstants.BOOKID + " = ?",
                        new String[] { String.valueOf(id) }, null, null, null, "1");
      if (c.moveToFirst()) {
         b = new Book();
         b.setId(id);
         b.setIsbn(c.getString(0));
         b.setTitle(c.getString(1));
         b.setSubTitle(c.getString(2));
         b.setCoverImageId(c.getLong(3));
         b.setCoverImageTinyId(c.getLong(4));
         b.setPublisher(c.getString(5));
         b.setDescription(c.getString(6));
         b.setFormat(c.getString(7));
         b.setSubject(c.getString(8));
         b.setDatePubStamp(c.getLong(9));
         b.setAuthors(this.selectAuthorsByBookId(id));
      }
      if (c != null && !c.isClosed()) {
         c.close();
      }
      return b;
   }

   public Book selectBook(String title) {
      Book b = null;
      Cursor c =
               this.db.query(BOOK_TABLE, new String[] { DataConstants.BOOKID }, DataConstants.TITLE + " = ?",
                        new String[] { title }, null, null, null, "1");
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
                        DataConstants.SUBTITLE, DataConstants.COVERIMAGEID, DataConstants.COVERIMAGETINYID,
                        DataConstants.PUBLISHER, DataConstants.DESCRIPTION, DataConstants.FORMAT,
                        DataConstants.SUBJECT, DataConstants.DATEPUB }, null, null, null, null, DataConstants.TITLE
                        + " desc", null);
      if (c.moveToFirst()) {
         do {
            Book b = new Book();
            b.setId(c.getLong(0));
            b.setIsbn(c.getString(1));
            b.setTitle(c.getString(2));
            b.setSubTitle(c.getString(3));
            b.setCoverImageId(c.getLong(4));
            b.setCoverImageTinyId(c.getLong(5));
            b.setPublisher(c.getString(6));
            b.setDescription(c.getString(7));
            b.setFormat(c.getString(8));
            b.setSubject(c.getString(9));
            b.setDatePubStamp(c.getLong(10));
            b.setAuthors(this.selectAuthorsByBookId(b.getId()));
            set.add(b);
         } while (c.moveToNext());
      }
      if (c != null && !c.isClosed()) {
         c.close();
      }
      return set;
   }

   public HashSet<Book> selectAllBooksByAuthor(String name) {
      HashSet<Book> set = new HashSet<Book>();
      Author a = this.selectAuthor(name);
      if (a != null) {
         Cursor c =
                  this.db.query(BOOKAUTHOR_TABLE, new String[] { DataConstants.BOOKID }, DataConstants.AUTHORID
                           + " = ?", new String[] { String.valueOf(a.getId()) }, null, DataConstants.TITLE + " desc",
                           null);
         if (c.moveToFirst()) {
            do {
               Book b = this.selectBook(c.getLong(0));
               set.add(b);
            } while (c.moveToNext());
         }
         if (c != null && !c.isClosed()) {
            c.close();
         }
      }
      return set;
   }

   public HashSet<String> selectAllBookNames() {
      HashSet<String> set = new HashSet<String>();
      Cursor c =
               this.db.query(BOOK_TABLE, new String[] { DataConstants.TITLE }, null, null, null, null,
                        DataConstants.TITLE + " desc");
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
      Book b = this.selectBook(id);
      if (b != null) {
         HashSet<Author> authors = this.selectAuthorsByBookId(id);
         this.db.delete(BOOKAUTHOR_TABLE, DataConstants.BOOKID + " = ?", new String[] { String.valueOf(b.getId()) });
         this.db.delete(BOOK_TABLE, DataConstants.BOOKID + " = ?", new String[] { String.valueOf(id) });
         // if no other books by same author, also delete author
         for (Author a : authors) {
            HashSet<Book> books = this.selectAllBooksByAuthor(a.getName());
            if (books.isEmpty()) {
               this.deleteAuthor(a.getId());
            }
         }
      }
   }

   public void deleteBook(String isbn) {
      Book b = this.selectBook(isbn);
      if (b != null) {
         this.deleteBook(b.getId());
      }
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
         a = this.selectAuthor(c.getLong(0));
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
            Author a = this.selectAuthor(authorId);
            if (a != null) {
               authors.add(a);
            }
         }
      }
      return authors;
   }

   public HashSet<Author> selectAllAuthors() {
      HashSet<Author> set = new HashSet<Author>();
      Cursor c =
               this.db.query(AUTHOR_TABLE, new String[] { DataConstants.AUTHORID, DataConstants.NAME }, null, null,
                        null, null, DataConstants.NAME + " desc", null);
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
      Author a = this.selectAuthor(id);
      if (a != null) {
         this.db.delete(AUTHOR_TABLE, DataConstants.AUTHORID + " = ?", new String[] { String.valueOf(id) });
      }
   }

   public void deleteAuthor(String name) {
      Author a = this.selectAuthor(name);
      if (a != null) {
         this.db.delete(AUTHOR_TABLE, DataConstants.NAME + " = ?", new String[] { name });
      }
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
         Log.d(Constants.LOG_TAG, "SQLiteOpenHelper ctor");
      }

      @Override
      public void onCreate(SQLiteDatabase db) {

         Log.d(Constants.LOG_TAG, "SQLiteOpenHelper onCreate");

         // using StringBuilder here because it is easier to read/reuse lines
         StringBuilder sb = new StringBuilder();

         // book table
         sb.append("CREATE TABLE " + BOOK_TABLE + " (");
         sb.append(DataConstants.BOOKID + " INTEGER PRIMARY KEY, ");
         sb.append(DataConstants.ISBN + " TEXT, ");
         sb.append(DataConstants.TITLE + " TEXT, ");
         sb.append(DataConstants.SUBTITLE + " TEXT, ");
         sb.append(DataConstants.COVERIMAGEID + " INTEGER, ");
         sb.append(DataConstants.COVERIMAGETINYID + " INTEGER, ");
         sb.append(DataConstants.PUBLISHER + " TEXT, ");
         sb.append(DataConstants.DESCRIPTION + " TEXT, ");
         sb.append(DataConstants.FORMAT + " TEXT, ");
         sb.append(DataConstants.SUBJECT + " TEXT, ");
         sb.append(DataConstants.DATEPUB + " INTEGER");
         sb.append(");");
         db.execSQL(sb.toString());

         // author table
         sb.setLength(0);
         sb.append("CREATE TABLE " + AUTHOR_TABLE + " (");
         sb.append(DataConstants.AUTHORID + " INTEGER PRIMARY KEY, ");
         sb.append(DataConstants.NAME + " TEXT");
         sb.append(");");
         db.execSQL(sb.toString());

         // bookauthor table
         sb.setLength(0);
         sb.append("CREATE TABLE " + BOOKAUTHOR_TABLE + " (");
         sb.append(DataConstants.BOOKAUTHORID + " INTEGER PRIMARY KEY, ");
         sb.append(DataConstants.BOOKID + " INTEGER, ");
         sb.append(DataConstants.AUTHORID + " INTEGER, ");
         sb.append("FOREIGN KEY(" + DataConstants.BOOKID + ") REFERENCES " + BOOK_TABLE + "(" + DataConstants.BOOKID
                  + "), ");
         sb.append("FOREIGN KEY(" + DataConstants.AUTHORID + ") REFERENCES " + AUTHOR_TABLE + "("
                  + DataConstants.AUTHORID + ") ");
         sb.append(");");
         db.execSQL(sb.toString());

         // bookdata table (users book data, ratings, reviews, etc)
         sb.setLength(0);
         sb.append("CREATE TABLE " + BOOKUSERDATA_TABLE + " (");
         sb.append(DataConstants.BOOKUSERDATAID + " INTEGER PRIMARY KEY, ");
         sb.append(DataConstants.BOOKID + " INTEGER, ");
         sb.append(DataConstants.READSTATUS + " INTEGER, ");
         sb.append(DataConstants.RATING + " INTEGER, ");
         sb.append(DataConstants.BLURB + " TEXT, ");
         sb.append("FOREIGN KEY(" + DataConstants.BOOKID + ") REFERENCES " + BOOK_TABLE + "(" + DataConstants.BOOKID
                  + ") ");
         sb.append(");");
         db.execSQL(sb.toString());

         // constraints 
         // (ISBN cannot be unique - some books don't have em - use TITLE unique?)
         db.execSQL("CREATE UNIQUE INDEX uidxBookTitle ON " + BOOK_TABLE + "(" + DataConstants.TITLE
                  + " COLLATE NOCASE)");
         db.execSQL("CREATE UNIQUE INDEX uidxAuthorName ON " + AUTHOR_TABLE + "(" + DataConstants.NAME
                  + " COLLATE NOCASE)");

         this.dbCreated = true;
      }

      @Override
      public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
         Log
                  .i(Constants.LOG_TAG, "SQLiteOpenHelper onUpgrade - oldVersion:" + oldVersion + " newVersion:"
                           + newVersion);
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
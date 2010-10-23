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
import com.totsp.bookworm.model.Book;
import com.totsp.bookworm.model.BookUserData;

import java.util.ArrayList;

/**
 * DAO for Book entity (that currently also uses the Author and BookUserData DAOs).
 * 
 * @author ccollins
 *
 */
public class BookDAO implements DAO<Book> {

   private static final String QUERY_CURSOR_PREFIX =
            "select book.bid as _id, book.tit, book.subtit, book.subject, book.pub, book.datepub, book.format, "
                     + "bookuserdata.rstat, bookuserdata.rat, bookuserdata.blurb, group_concat(author.name) as authors "
                     + "from book left outer join bookuserdata on book.bid = bookuserdata.bid "
                     + "left outer join bookauthor on bookauthor.bid = book.bid left outer join author on author.aid = bookauthor.aid";

   private final SQLiteStatement bookInsertStmt;
   private static final String BOOK_INSERT =
            "insert into " + DataConstants.BOOK_TABLE + "(" + DataConstants.ISBN10 + "," + DataConstants.ISBN13 + ","
                     + DataConstants.TITLE + "," + DataConstants.SUBTITLE + "," + DataConstants.PUBLISHER + ","
                     + DataConstants.DESCRIPTION + "," + DataConstants.FORMAT + "," + DataConstants.SUBJECT + ","
                     + DataConstants.DATEPUB + ") values (?, ?, ?, ?, ?, ?, ?, ?, ?)";
   private final SQLiteStatement bookAuthorInsertStmt;
   private static final String BOOKAUTHOR_INSERT =
            "insert into " + DataConstants.BOOKAUTHOR_TABLE + "(" + DataConstants.BOOKID + "," + DataConstants.AUTHORID
                     + ") values (?, ?)";

   private SQLiteDatabase db;
   private BookUserDataDAO bookUserDataDAO;
   private AuthorDAO authorDAO;

   public BookDAO(SQLiteDatabase db) {
      this.db = db;

      // right now BookDAO is in charge of BOOK and BOOKAUTHOR
      // and it uses BookUseDataDAO and AuthorDAO

      // TODO refactor other DAOs out, make each DAO handle its own abstraction
      // (ok to combine the DAOs at the manager/service layer, but maybe not inside another DAO?)
      // here we wire in other DAOs manually, for now (not cleanest approach, but simple)
      // (other DAOs are not used elsewhere at present, can't create just an author, for ex)

      bookUserDataDAO = new BookUserDataDAO(db);
      authorDAO = new AuthorDAO(db);

      // statements
      bookInsertStmt = db.compileStatement(BookDAO.BOOK_INSERT);
      bookAuthorInsertStmt = db.compileStatement(BookDAO.BOOKAUTHOR_INSERT);
   }

   @Override
   public void deleteAll() {
      bookUserDataDAO.deleteAll();
      db.delete(DataConstants.BOOKAUTHOR_TABLE, null, null);         
      db.delete(DataConstants.BOOK_TABLE, null, null);      
   }

   public static void onCreate(SQLiteDatabase db) {
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

      // bookauthor table
      sb.setLength(0);
      sb.append("CREATE TABLE " + DataConstants.BOOKAUTHOR_TABLE + " (");
      sb.append(DataConstants.BOOKAUTHORID + " INTEGER PRIMARY KEY, ");
      sb.append(DataConstants.BOOKID + " INTEGER, ");
      sb.append(DataConstants.AUTHORID + " INTEGER, ");
      sb.append("FOREIGN KEY(" + DataConstants.BOOKID + ") REFERENCES " + DataConstants.BOOK_TABLE + "("
               + DataConstants.BOOKID + "), ");
      sb.append("FOREIGN KEY(" + DataConstants.AUTHORID + ") REFERENCES " + DataConstants.AUTHOR_TABLE + "("
               + DataConstants.AUTHORID + ") ");
      sb.append(");");
      db.execSQL(sb.toString());
   }

   public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      db.execSQL("DROP TABLE IF EXISTS " + DataConstants.BOOKAUTHOR_TABLE);
      db.execSQL("DROP TABLE IF EXISTS " + DataConstants.BOOK_TABLE);      
      BookDAO.onCreate(db);
   }

   public Cursor getCursor(final String orderBy, final String whereClauseLimit) {
      // note that query MUST have a column named _id
      StringBuilder sb = new StringBuilder();
      sb.append(BookDAO.QUERY_CURSOR_PREFIX);
      if ((whereClauseLimit != null) && (whereClauseLimit.length() > 0)) {
         sb.append(" " + whereClauseLimit);
      }
      sb.append(" group by book.bid");
      if ((orderBy != null) && (orderBy.length() > 0)) {
         sb.append(" order by " + orderBy);
      }

      return db.rawQuery(sb.toString(), null);
   }

   @Override
   public Book select(final long id) {
      Book b = null;
      Cursor c =
               db.query(DataConstants.BOOK_TABLE,
                        new String[] { DataConstants.BOOKID, DataConstants.ISBN10, DataConstants.ISBN13,
                                 DataConstants.TITLE, DataConstants.SUBTITLE, DataConstants.PUBLISHER,
                                 DataConstants.DESCRIPTION, DataConstants.FORMAT, DataConstants.SUBJECT,
                                 DataConstants.DATEPUB }, DataConstants.BOOKID + " = ?", new String[] { String
                                 .valueOf(id) }, null, null, null, "1");
      if (c.moveToFirst()) {
         b = buildBookFromFullQueryCursor(c);
      }
      if (!c.isClosed()) {
         c.close();
      }
      return b;
   }

   @Override
   public ArrayList<Book> selectAll() {
      ArrayList<Book> set = new ArrayList<Book>();
      Cursor c =
               db.query(DataConstants.BOOK_TABLE,
                        new String[] { DataConstants.BOOKID, DataConstants.ISBN10, DataConstants.ISBN13,
                                 DataConstants.TITLE, DataConstants.SUBTITLE, DataConstants.PUBLISHER,
                                 DataConstants.DESCRIPTION, DataConstants.FORMAT, DataConstants.SUBJECT,
                                 DataConstants.DATEPUB }, null, null, null, null, DataConstants.TITLE + " asc", null);
      if (c.moveToFirst()) {
         do {
            Book b = buildBookFromFullQueryCursor(c);
            set.add(b);
         } while (c.moveToNext());
      }
      if (!c.isClosed()) {
         c.close();
      }
      return set;
   }

   public ArrayList<Book> selectAllBooksByAuthor(final String name) {
      ArrayList<Book> set = new ArrayList<Book>();
      Author a = authorDAO.select(name);
      // TODO do this in a single join (else make a separate BookAuthorDAO?)
      if (a != null) {
         Cursor c =
                  db.query(DataConstants.BOOKAUTHOR_TABLE, new String[] { DataConstants.BOOKID },
                           DataConstants.AUTHORID + " = ?", new String[] { String.valueOf(a.id) }, null, null, null,
                           null);
         if (c.moveToFirst()) {
            do {
               // makes an addtl query for every name, not best approach here
               Book b = select(c.getLong(0));
               set.add(b);
            } while (c.moveToNext());
         }
         if (!c.isClosed()) {
            c.close();
         }
      }
      return set;
   }

   public ArrayList<Book> selectAllBooksByTitle(final String title) {
      ArrayList<Book> set = new ArrayList<Book>();
      // TODO do this in a single join
      Cursor c =
               db.query(DataConstants.BOOK_TABLE, new String[] { DataConstants.BOOKID }, DataConstants.TITLE + " = ?",
                        new String[] { title }, null, null, DataConstants.TITLE + " asc", null);
      if (c.moveToFirst()) {
         do {
            // makes an addtl query for every title, not the best approach here
            Book b = select(c.getLong(0));
            set.add(b);
         } while (c.moveToNext());
      }
      if (!c.isClosed()) {
         c.close();
      }
      return set;
   }

   public ArrayList<String> selectAllBookNames() {
      ArrayList<String> set = new ArrayList<String>();
      Cursor c =
               db.query(DataConstants.BOOK_TABLE, new String[] { DataConstants.TITLE }, null, null, null, null,
                        DataConstants.TITLE + " asc");
      if (c.moveToFirst()) {
         do {
            set.add(c.getString(0));
         } while (c.moveToNext());
      }
      if (!c.isClosed()) {
         c.close();
      }
      return set;
   }

   @Override
   public long insert(final Book b) {
      long bookId = 0L;
      if ((b != null) && (b.title != null)) {
         // TODO check for existing book using multiple criteria (not just title)        

         // use transaction
         db.beginTransaction();
         try {
            // insert authors as needed
            ArrayList<Long> authorIds = new ArrayList<Long>();
            if ((b.authors != null) && !b.authors.isEmpty()) {
               for (int i = 0; i < b.authors.size(); i++) {
                  Author a = b.authors.get(i);
                  Author authorExists = authorDAO.select(a.name);
                  if (authorExists == null) {
                     authorIds.add(authorDAO.insert(a));
                  } else {
                     authorIds.add(authorExists.id);
                  }
               }
            }

            // insert book
            bookInsertStmt.clearBindings();
            bookInsertStmt.bindString(1, b.isbn10);
            bookInsertStmt.bindString(2, b.isbn13);
            bookInsertStmt.bindString(3, b.title);
            bookInsertStmt.bindString(4, b.subTitle);
            bookInsertStmt.bindString(5, b.publisher);
            bookInsertStmt.bindString(6, b.description);
            bookInsertStmt.bindString(7, b.format);
            bookInsertStmt.bindString(8, b.subject);
            bookInsertStmt.bindLong(9, b.datePubStamp);
            bookId = bookInsertStmt.executeInsert();

            if (bookId > 0) {
               // insert bookauthors
               insertBookAuthorData(bookId, authorIds);

               // insert bookuserdata
               BookUserData newBookUserData = new BookUserData(bookId, b.bookUserData.rating, b.bookUserData.read, b.bookUserData.blurb);
               bookUserDataDAO.insert(newBookUserData);

               db.setTransactionSuccessful();
            } else {
               Log.e(Constants.LOG_TAG,
                        "Error book insert failed, book will not be persisted, transaction rolled back.");
            }
         } catch (SQLException e) {
            bookId = 0;
            Log.e(Constants.LOG_TAG, "Error inserting book.", e);
         } finally {
            db.endTransaction();
         }
      } else {
         throw new IllegalArgumentException("Error, book cannot be null, and must have a title.");
      }
      return bookId;
   }

   @Override
   public void update(final Book b) {
      if ((b != null) && (b.id != 0)) {
         Book bookExists = select(b.id);
         if (bookExists == null) {
            throw new IllegalArgumentException("Cannot update book that does not already exist.");
         }

         // use transaction
         db.beginTransaction();
         try {

            // insert authors as needed            
            ArrayList<Long> authorIds = new ArrayList<Long>();
            if ((b.authors != null) && !b.authors.isEmpty()) {
               for (int i = 0; i < b.authors.size(); i++) {
                  Author a = b.authors.get(i);
                  Author authorExists = authorDAO.select(a.name);
                  if (authorExists == null) {
                     authorIds.add(authorDAO.insert(a));
                  } else {
                     authorIds.add(authorExists.id);
                  }
               }
            }

            // update/insert book/author associations
            deleteBookAuthorData(b.id);
            insertBookAuthorData(b.id, authorIds);

            // update/insert book user data
            bookUserDataDAO.delete(b.id);
            BookUserData newBookUserData = new BookUserData(b.id, b.bookUserData.rating, b.bookUserData.read, b.bookUserData.blurb);
            bookUserDataDAO.insert(newBookUserData);

            // update book
            final ContentValues values = new ContentValues();
            values.put(DataConstants.ISBN10, b.isbn10);
            values.put(DataConstants.ISBN13, b.isbn13);
            values.put(DataConstants.TITLE, b.title);
            values.put(DataConstants.SUBTITLE, b.subTitle);
            values.put(DataConstants.PUBLISHER, b.publisher);
            values.put(DataConstants.DESCRIPTION, b.description);
            values.put(DataConstants.FORMAT, b.format);
            values.put(DataConstants.SUBJECT, b.subject);
            values.put(DataConstants.DATEPUB, b.datePubStamp);

            db.update(DataConstants.BOOK_TABLE, values, DataConstants.BOOKID + " = ?", new String[] { String
                     .valueOf(b.id) });

            db.setTransactionSuccessful();
         } catch (SQLException e) {
            Log.e(Constants.LOG_TAG, "Error inserting book.", e);
         } finally {
            db.endTransaction();
         }
      } else {
         throw new IllegalArgumentException("Error, book cannot be null, and must have a title.");
      }
   }

   @Override
   public void delete(final long id) {
      Book b = select(id);
      if (b != null) {
         ArrayList<Author> authors = authorDAO.selectByBookId(id);
         db
                  .delete(DataConstants.BOOKAUTHOR_TABLE, DataConstants.BOOKID + " = ?", new String[] { String
                           .valueOf(b.id) });
         db.delete(DataConstants.BOOK_TABLE, DataConstants.BOOKID + " = ?", new String[] { String.valueOf(id) });
         // if no other books by same author, also delete author
         for (int i = 0; i < authors.size(); i++) {
            ArrayList<Book> books = selectAllBooksByAuthor(authors.get(i).name);
            if (books.isEmpty()) {
               authorDAO.delete(authors.get(i).id);
            }
         }
      }
   }

   private Book buildBookFromFullQueryCursor(final Cursor c) {
      Book b = null;
      if (!c.isClosed()) {
         b = new Book();
         b.id = c.getLong(0);
         try {
            b.isbn10 = (c.getString(1));
         } catch (IllegalArgumentException e) {
         }
         try {
            b.isbn13 = (c.getString(2));
         } catch (IllegalArgumentException e) {
         }
         b.title = (c.getString(3));
         b.subTitle = (c.getString(4));
         b.publisher = (c.getString(5));
         b.description = (c.getString(6));
         b.format = (c.getString(7));
         b.subject = (c.getString(8));
         b.datePubStamp = (c.getLong(9));
         b.authors = authorDAO.selectByBookId(b.id);

         // TODO add join to bookuserdata - rather than sep query
         b.bookUserData.bookId = b.id;
         BookUserData userData = bookUserDataDAO.selectByBookId(b.id);
         if (userData != null) {
            b.bookUserData.read = userData.read;
            b.bookUserData.rating = userData.rating;
            b.bookUserData.blurb = userData.blurb;
         }
      }
      return b;
   }

   //
   // book-author data (not yet a separate DAO, only used here)
   //   
   public void insertBookAuthorData(final long bookId, final ArrayList<Long> authorIds) {
      for (int i = 0; i < authorIds.size(); i++) {
         Long authorId = authorIds.get(i);
         bookAuthorInsertStmt.clearBindings();
         bookAuthorInsertStmt.bindLong(1, bookId);
         bookAuthorInsertStmt.bindLong(2, authorId);
         bookAuthorInsertStmt.executeInsert();
      }
   }

   public void deleteBookAuthorData(final long bookId) {
      db.delete(DataConstants.BOOKAUTHOR_TABLE, DataConstants.BOOKID + " = ?", new String[] { String.valueOf(bookId) });
   }
}
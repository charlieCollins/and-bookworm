package com.totsp.bookworm.data;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.SystemClock;
import android.util.Log;

import com.totsp.bookworm.Constants;
import com.totsp.bookworm.data.dao.BookDAO;
import com.totsp.bookworm.data.dao.BookUserDataDAO;
import com.totsp.bookworm.data.dao.TagDAO;
import com.totsp.bookworm.model.Book;
import com.totsp.bookworm.model.BookListStats;
import com.totsp.bookworm.model.Tag;

/**
 * Android DataManager to encapsulate SQL and DB details.
 * Includes SQLiteOpenHelper.
 *
 * @author ccollins
 *
 */
public class DataManager {

   // Use an invalid book ID to store default tags in tagbooks table
   public static final long DEFAULT_BOOK_TAGS_ID = 0;	
   
   private static final int DATABASE_VERSION = 12;
	

   private SQLiteDatabase db;
   private Context context;

   private BookDAO bookDAO;
   private TagDAO tagDAO;

   public DataManager(final Context context) {
	   this.context = context; 		// Keep a reference to context so we can re-open the database on import
	   openDb();
	   Log.i(Constants.LOG_TAG, "DataManager created, db open status: " + db.isOpen());
   }

   public SQLiteDatabase getDb() {
      return db;
   }

   public void openDb() {
	   if ((db == null) || (!db.isOpen())) {
		   OpenHelper openHelper = new OpenHelper(context);
		   db = openHelper.getWritableDatabase();

		   // app only needs access to book & tag DAO at present (can't create authors on their own, etc.)
		   // since we pass db into DAO, have to recreate DAO if db is re-opened
		   bookDAO = new BookDAO(db);
		   tagDAO = new TagDAO(db);

		   if (openHelper.isDbCreated()) {
			   // insert default data here if needed
		   }
	   }
   }

   public void closeDb() {
      if (db.isOpen()) {
         db.close();
      }
   }

   public void resetDb() {
      Log.i(Constants.LOG_TAG, "Resetting database connection (close and re-open).");
      closeDb();
      SystemClock.sleep(500);
      openDb();
   }

   //
   // wrapped DB methods
   //
   public Book selectBook(final long id) {
      return bookDAO.select(id);
   }

   public ArrayList<Book> selectAllBooks() {
      return bookDAO.selectAll();
   }

   public ArrayList<Book> selectAllBooksByTitle(final String title) {
      return bookDAO.selectAllBooksByTitle(title);
   }

   public long insertBook(final Book b) {
	   long bookId = bookDAO.insert(b);
	   
	   // If the new book already has it's read flag set, convert it to a tag link (required for CSV import)
	   if ((b.bookUserData != null) && (b.bookUserData.read)) {
		   ContentValues entry = new ContentValues();
		   Tag readTag = selectTag("Read");
		   if (readTag != null) {
			   entry.put(DataConstants.TAG_ID, readTag.id);
			   entry.put(DataConstants.BOOKID, bookId);
			   db.insert(DataConstants.TAG_BOOKS_TABLE, null, entry);
		   }
	   }
      return bookId;
   }

   public void updateBook(final Book b) {
      bookDAO.update(b);
   }

   public void deleteBook(final long id) {
      bookDAO.delete(id);
   }

   public Cursor getBookCursor(final String orderBy, final String whereClauseLimit) {
      return bookDAO.getCursor(orderBy, whereClauseLimit);
   }

   public Tag selectTag(final long id) {
	   return tagDAO.select(id);
   }


   public Tag selectTag(final String name) {
	   return tagDAO.select(name);
   }

   public long insertTag(final Tag tag) {
	   return tagDAO.insert(tag);
   }

   public void updateTag(final Tag tag) {
	   tagDAO.update(tag);
   }

   public void deleteTag(final long id) {
	   tagDAO.delete(id);
   }
   

   public Cursor getTagCursor(final String orderBy, final String whereClauseLimit) {
	   return tagDAO.getCursor(orderBy, whereClauseLimit);
   }
   
   public Cursor getTagSelectorCursor(final long bookId) {
	   return tagDAO.getSelectorCursor(bookId);
   }

   /**
    * Queries whether the tag is linked to the specified book.
    * 
    * @param tagId   ID of tag to query.
    * @param bookId  ID of book to query
    * @return        True if the tag is linked to the book, false otherwise
    */
   public boolean isTagged(final long tagId, final long bookId) {
	   return tagDAO.isTagged(tagId, bookId);
   }

   /**
    * Sets whether a tag is linked to a specified book.
    * 
    * @param bookId ID of book to be tagged/un-tagged.
    * @param tagId  ID of tag to be added or removed.
    * @param tagged Tagged state. If true, adds the tag to the book, otherwise the tag link is removed.
    */
   public void setBookTagged(final long bookId, final long tagId, boolean tagged) {
	   if (tagged) {
		   tagDAO.insertBook(tagId, bookId);
	   }
	   else
	   {
		   tagDAO.deleteBook(tagId, bookId);
	   }
   }
   
   /**
    * Toggles the current tag link state for the specified book and tag.
    * 
    * @param bookId ID of book to be tagged/un-tagged.
    * @param tagId  ID of tag to be toggled.
    */
   public void toggleBookTagged(final long bookId, final long tagId) {
	   setBookTagged(bookId, tagId, !isTagged(tagId, bookId));
   }

   public void addTagToBook(final long tagId, final long bookId) {
	   // Insert new books at end of group by default
	   tagDAO.insertBook(tagId, bookId);
   }


   public void removeTagFromBook(final long tagId, final long bookId) {
	   tagDAO.deleteBook(tagId, bookId);
   }	

   /**
    * Returns a string containing all of the tags which are applied against the specified book.
    * 
    * @param bookId     Book to query
    * @param separator  Tag delimiter text
    * @return        String containing tag text
    */
   public String getBookTagsString(final long bookId, CharSequence separator) {
	   return tagDAO.getTagsString(bookId, separator);
   }


	/**
	 * Swaps the order of two books in the tag books table.
	 * 
	 * @param tagId    ID of tag collection to be updated
	 * @param bookId1  ID of first book
	 * @param bookId2  ID of second book
	 */
   public void swapBooksInTagTable(long tagId, long bookId1, long bookId2) {
	   tagDAO.swapBooks(tagId, bookId1, bookId2);
   }
   
   public TagSelectorBuilder getTagSelectorBuilder(Context context, long bookId) {
	  return new TagSelectorBuilder(context, bookId); 
   }

	
   /**
    * Queries the default tags.
    * @return
    */
   public ArrayList<Long> getDefaultTags() {
	   return tagDAO.getLinkedTagIds(DataManager.DEFAULT_BOOK_TAGS_ID);
   }

	
   // super delete - clears all tables
   public void deleteAllDataYesIAmSure() {
      Log.i(Constants.LOG_TAG, "deleting all data from database - deleteAllYesIAmSure invoked");
      db.beginTransaction();
      try {
         db.delete(DataConstants.AUTHOR_TABLE, null, null);
         db.delete(DataConstants.BOOKAUTHOR_TABLE, null, null);
         db.delete(DataConstants.BOOKUSERDATA_TABLE, null, null);
         db.delete(DataConstants.BOOK_TABLE, null, null);
         tagDAO.deleteTable();
         db.setTransactionSuccessful();
      } finally {
         db.endTransaction();
      }
      db.execSQL("vacuum");
      
      // Re-create built-in tags when done
      createBuiltInTags(db);
   }

   // stats specific
   public BookListStats getStats() {
      BookListStats stats = new BookListStats();
      stats.totalBooks = getCountFromTable(DataConstants.BOOK_TABLE, "");
      stats.totalAuthors = getCountFromTable(DataConstants.AUTHOR_TABLE, "");
      stats.readBooks = getCountFromTable(DataConstants.BOOKUSERDATA_TABLE, "where bookuserdata.rstat = 1");
      stats.fiveStarBooks = getCountFromTable(DataConstants.BOOKUSERDATA_TABLE, "where bookuserdata.rat = 5");
      stats.fourStarBooks = getCountFromTable(DataConstants.BOOKUSERDATA_TABLE, "where bookuserdata.rat = 4");
      stats.threeStarBooks = getCountFromTable(DataConstants.BOOKUSERDATA_TABLE, "where bookuserdata.rat = 3");
      stats.twoStarBooks = getCountFromTable(DataConstants.BOOKUSERDATA_TABLE, "where bookuserdata.rat = 2");
      stats.oneStarBooks = getCountFromTable(DataConstants.BOOKUSERDATA_TABLE, "where bookuserdata.rat = 1");
      return stats;
   }

  // Changed to protected scope to allow method to be exposed for automated testing 
   protected int getCountFromTable(final String table, final String whereClause) {
      int result = 0;
      Cursor c = db.rawQuery("select count(*) from " + table + " " + whereClause, null);
      if (c.moveToFirst()) {
         result = c.getInt(0);
      }
      if (!c.isClosed()) {
         c.close();
      }
      return result;
   }

   /**
    * Creates built-in tags. This is done here rather than in the TagDAO, since built-in tags are specific to this 
    * application and are not intrinsic to the tag classes themselves.
    * 
    * @param db  Database to be updated with built-in tags
    */
   private static void createBuiltInTags(final SQLiteDatabase db) {
		 ContentValues entry = new ContentValues();				
		 
		 // TODO: Handle error if failed to create tags (theoretical pathological case, maybe due to lack of resources)
		 entry.put(DataConstants.TAGTEXT, "Owned");
		 db.insert(DataConstants.TAG_TABLE, null, entry);

		 entry.clear();				
		 entry.put(DataConstants.TAGTEXT, "Read");
		 db.insert(DataConstants.TAG_TABLE, null, entry);    	  
   }
   

   
   //
   // end DB methods
   //  

   //
   // SQLiteOpenHelper   
   //
   private static class OpenHelper extends SQLiteOpenHelper {

      private boolean dbCreated;

      OpenHelper(final Context context) {
         super(context, DataConstants.DATABASE_NAME, null, DataManager.DATABASE_VERSION);
      }

      @Override
      public void onCreate(final SQLiteDatabase db) {
         Log.i(Constants.LOG_TAG, "BookWorm DataHelper.OpenHelper onCreate creating database bookworm.db");

         // using StringBuilder here because it is easier to read/reuse lines
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

         // author table
         sb.setLength(0);
         sb.append("CREATE TABLE " + DataConstants.AUTHOR_TABLE + " (");
         sb.append(DataConstants.AUTHORID + " INTEGER PRIMARY KEY, ");
         sb.append(DataConstants.NAME + " TEXT");
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

         // bookdata table (users book data, ratings, reviews, etc)
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

         TagDAO.onCreate(db);
         createBuiltInTags(db);
         
         dbCreated = true;
      }

      @Override
      public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
         Log.i(Constants.LOG_TAG, "SQLiteOpenHelper onUpgrade - oldVersion:" + oldVersion + " newVersion:"
                           + newVersion);
         // export old data first, then upgrade, then import
         if (oldVersion < 10) {
            db.execSQL("DROP TABLE IF EXISTS " + DataConstants.BOOK_TABLE);
         	db.execSQL("DROP TABLE IF EXISTS " + DataConstants.AUTHOR_TABLE);
         	db.execSQL("DROP TABLE IF EXISTS " + DataConstants.BOOKUSERDATA_TABLE);
         	db.execSQL("DROP TABLE IF EXISTS " + DataConstants.BOOKAUTHOR_TABLE);
         	onCreate(db);
         }
         else
         {
	         TagDAO.onUpgrade(db, oldVersion, newVersion);
	         if (oldVersion < 12) {
	        	 createBuiltInTags(db);
	        	 convertReadStatusToTag(db);
	         }
				
	     }
      }

      
      /**
       * Converts the "Have Read?" status in the BookUserData table to the "Read" tag.
       * Note that since this conversion runs during the database open operation, normal database accessor methods 
       * cannot be used yet.
       *   
       * @param db Database containing tables to be converted
       */
      private void convertReadStatusToTag(final SQLiteDatabase db) {
    	  ContentValues entry = new ContentValues();				
    	  long readTagId;

    	  Cursor c = db.query(DataConstants.TAG_TABLE, new String[] { DataConstants.TAG_ID }, DataConstants.TAGTEXT
    			  + " = \"Read\"", null, null, null, null, "1");
    	  if (c.moveToFirst()) {
    		  readTagId = c.getLong(0);
    	  } else {
    		  // Do no conversion if the built-in tag could be found
    		  return;
    	  }
    	  if (!c.isClosed()) {
    		  c.close();
    	  }

    	  ArrayList<Long> readBooks = BookUserDataDAO.queryAllRead(db);
    	  for (int i=0; i < readBooks.size(); i++) {
    		  entry.clear();
    		  entry.put(DataConstants.TAG_ID, readTagId);
    		  entry.put(DataConstants.BOOKID, readBooks.get(i));
    		  db.insert(DataConstants.TAG_BOOKS_TABLE, null, entry);
    	  }
      }

      public boolean isDbCreated() {
         return dbCreated;
      }
   }
   
   
   /**
    * Creates a new tag selector dialog builder for a specific book.
    * The dialog consists of a multi-selection list of all tags with tags linked to the specified book selected. 
    * Selecting or de-selecting books will update the DB links automatically.
    * 
    * Wrapper class for {@link TagDAO.SelectorDialogBuilder} to avoid requiring clients to use TagDAO directly.
    * Delegates all calls to TagDAO.SelectorDialogBuilder.
    * 
    * Note that only a limited subset of {@link AlertDialog.Builder} methods are implemented.
    */
   public class TagSelectorBuilder {
	   private TagDAO.SelectorDialogBuilder selector;	

	   public TagSelectorBuilder(Context context, long bookId) {
		   selector = tagDAO.getSelectorDialogBuilder(context, bookId);
	   }

	   public void show() {
		   selector.show();
	   }

	   public void setOnClickListener(DialogInterface.OnClickListener onClickListener) {
		   selector.setOnClickListener(onClickListener);
	   }

	   public void create() {
		   selector.create();

	   }	    	   
   }
   
}
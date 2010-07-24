package com.totsp.bookworm.data.dao;

import java.util.ArrayList;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import com.totsp.bookworm.Constants;
import com.totsp.bookworm.data.DataConstants;
import com.totsp.bookworm.model.Tag;


/**
 *  DAO for item tag DB tables.
 *  As well as implementing the DAO methods for maintaining the tag entries
 *  this class also includes static methods to be used by the SQL open helper to 
 *  create and upgrade the tag tables.  
 */
public class TagDAO implements DAO<Tag>{

	private static final String QUERY_CURSOR_PREFIX =
		"select tags.tid as _id, tags.ttext from tags";


	private static final String QUERY_TAGS_BY_BOOK_ID =
		"select tags.ttext from tags join tagbooks on (tagbooks.tid=tags.tid)" 
		+ " join book on (book.bid = tagbooks.bid) where book.bid=? order by tags.ttext asc";

	/*
	 * Prefix for a query to return all books in the specified tags.
	 * The tag entry in the tag table is found by tag ID, which is used to look up all entries
	 * in the tagbooks table with a matching tag ID.
	 */
	private static final String QUERY_BOOKS_BY_TAG_ID_PREFIX =
		"select book.bid as _id, book.tit, book.subtit, "
		+ "group_concat(author.name) as authors "
		+ "from book "
		+ "left outer join bookauthor on bookauthor.bid = book.bid left outer join author on author.aid = bookauthor.aid";

	private static final String QUERY_TAG_SELECTOR_BY_BOOK_ID =
		"select distinct tags.tid as _id, tags.ttext as taggedText, "
		+ "(exists (select * from tagbooks where tagbooks.tid=tags.tid and tagbooks.bid=?)) as tagged "
		+ "from tags order by tags.ttext asc";	

	private final SQLiteStatement tagInsertStmt;
	private static final String TAG_INSERT =
		"insert into " + DataConstants.TAG_TABLE + "(" + DataConstants.TAGTEXT + ") values (?)";

	private SQLiteDatabase db;

	public TagDAO(SQLiteDatabase db) {
		this.db = db;

		// statements
		tagInsertStmt = db.compileStatement(TagDAO.TAG_INSERT);
	}

	/**
	 * Create tags and tag books tables in DB
	 * @param db  SQLite database 
	 */
	public static void onCreate(final SQLiteDatabase db) {
		// using StringBuilder here because it is easier to read/reuse lines
		StringBuilder sb = new StringBuilder();

		// tag table
		sb.setLength(0);
		sb.append("CREATE TABLE " + DataConstants.TAG_TABLE + " (");
		sb.append(DataConstants.TAG_ID + " INTEGER PRIMARY KEY, ");
		sb.append(DataConstants.TAGTEXT + " TEXT UNIQUE");
		sb.append(");");
		db.execSQL(sb.toString());

		// tagged books table
		sb.setLength(0);
		sb.append("CREATE TABLE " + DataConstants.TAG_BOOKS_TABLE + " (");
		sb.append(DataConstants.TAG_BOOK_ID + " INTEGER PRIMARY KEY, ");
		sb.append(DataConstants.TAG_ID + " INTEGER, ");
		sb.append(DataConstants.BOOKID + " INTEGER, ");

		sb.append("FOREIGN KEY(" + DataConstants.BOOKID + ") REFERENCES " + DataConstants.BOOK_TABLE + "("
				+ DataConstants.BOOKID + "), ");
		sb.append("FOREIGN KEY(" + DataConstants.TAG_ID + ") REFERENCES " + DataConstants.TAG_TABLE + "("
				+ DataConstants.TAG_ID + ") ");
		sb.append(");");
		db.execSQL(sb.toString());
		
	}


	public static void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
		if (oldVersion < 11) 
		{
			onCreate(db);
		}
	}

	/**
	 * Queries whether the book has the specified tag.
	 * 
	 * @param tagId    ID of the tag
	 * @param bookId   ID of the book
	 * 
	 * @return  True if the book the specified tag, false otherwise
	 */
	public boolean isTagged(final long tagId, final long bookId) {
		boolean result;
		Cursor c =
			db.query(DataConstants.TAG_BOOKS_TABLE, new String[] {"*"}, 
					DataConstants.TAG_ID + " = ? AND " + DataConstants.BOOKID + " = ?", 
					new String[] { String.valueOf(tagId), String.valueOf(bookId) }, null, null, null, "1");
		result = (c.getCount() > 0);
		if (!c.isClosed()) {
			c.close();
		}
		return result;
		
	}
	
	public void insertBook(final long tagId, final long bookId) {
		if (!isTagged(tagId, bookId)) {
			ContentValues entry = new ContentValues();
			entry.put(DataConstants.TAG_ID, tagId);
			entry.put(DataConstants.BOOKID, bookId);
			db.insert(DataConstants.TAG_BOOKS_TABLE, null, entry);
		}
	}
	
	public void deleteBook(final long tagId, final long bookId) {

		db.delete(DataConstants.TAG_BOOKS_TABLE, 
				  DataConstants.TAG_ID + " = ? AND " + DataConstants.BOOKID + " = ?", 
				  new String[] { String.valueOf(tagId), String.valueOf(bookId) });
	}

	
	
	public void deleteTable() {
        db.delete(DataConstants.TAG_TABLE, null, null);
        db.delete(DataConstants.TAG_BOOKS_TABLE, null, null);
	}
	

	/**
	 * Queries the tags with a calculated column indicating whether each tag is applied to the specified book.
	 * Used to generate a cursor that can be used to populate a multi-select tag ListView for a single book.
	 * 
	 * @param bookId ID of book against which tags are applied
	 * 
	 * @return  Cursor containing columns named _id, tagText and tagged
	 */
	public Cursor getSelectorCursor(final long bookId) {
		try {
			return db.rawQuery(QUERY_TAG_SELECTOR_BY_BOOK_ID, new String[] { String.valueOf(bookId) });				
		} catch (SQLException e) {
			Log.d(Constants.LOG_TAG, "TagDAO", e);
			return null;
		}
	}
	
	
	/**
	 * Returns a string containing all of the tags which are applied against the specified book.
	 * 
	 * @param bookId  Book to query
	 * @return        String containing comma separated tag text
	 */
	public String getTagsString(final long bookId) {
		StringBuilder sb = new StringBuilder();
		int numRows;
		
		try {
			Cursor c = db.rawQuery(QUERY_TAGS_BY_BOOK_ID, new String[] { String.valueOf(bookId) });	
			numRows = c.getCount();
			c.moveToFirst();
			for (int i = 0; i < numRows; i++) {
				sb.append(c.getString(0));
				if (i < numRows-1) {
					sb.append(", ");
				}
				c.moveToNext();
			}
			if (!c.isClosed()) {
				c.close();
			}
		} catch (SQLException e) {
			Log.d(Constants.LOG_TAG, "TagDAO", e);
		}
		
		return sb.toString();		
	}
	
	@Override
	public Cursor getCursor(final String orderBy, final String whereClauseLimit) {
		// note that query MUST have a column named _id
		StringBuilder sb = new StringBuilder();
		sb.append(TagDAO.QUERY_CURSOR_PREFIX);
		if ((whereClauseLimit != null) && (whereClauseLimit.length() > 0)) {
			sb.append(" " + whereClauseLimit);
		}
		sb.append(" group by tags.tid");
		if ((orderBy != null) && (orderBy.length() > 0)) {
			sb.append(" order by " + orderBy);
		}

		return db.rawQuery(sb.toString(), null);
	}

	@Override
	public Tag select(final long id) {
		Tag tag = null;
		Cursor c =
			db.query(DataConstants.TAG_TABLE, new String[] { 
					DataConstants.TAGTEXT }, 
					DataConstants.TAG_ID + " = ?", 
					new String[] { String.valueOf(id) }, null, null, null, "1");
		if (c.moveToFirst()) {
			tag = new Tag();
			tag.id = (id);
			tag.text = (c.getString(0));
		}
		if (!c.isClosed()) {
			c.close();
		}
		return tag;
	}

	public Tag select(final String name) {
		Tag tag = null;
		Cursor c =
			db.query(DataConstants.TAG_TABLE, new String[] { DataConstants.TAG_ID }, DataConstants.TAGTEXT
					+ " = ?", new String[] { name }, null, null, null, "1");
		if (c.moveToFirst()) {
			tag = select(c.getLong(0));
		}
		if (!c.isClosed()) {
			c.close();
		}
		return tag;
	}

	@Override
	public ArrayList<Tag> selectAll() {
		ArrayList<Tag> set = new ArrayList<Tag>();
		Cursor c =
			db.query(DataConstants.TAG_TABLE, new String[] { DataConstants.TAG_ID, 
					 DataConstants.TAGTEXT}, 
					 null, null, null, null, DataConstants.TAGTEXT + " desc", null);
		if (c.moveToFirst()) {
			do {
				Tag s = new Tag();
				s.id = (c.getLong(0));
				s.text = (c.getString(1));
				set.add(s);
			} while (c.moveToNext());
		}
		if (!c.isClosed()) {
			c.close();
		}
		return set;
	}


	@Override
	public long insert(final Tag tag) {
		tagInsertStmt.clearBindings();
		tagInsertStmt.bindString(1, tag.text);
		try {
			return tagInsertStmt.executeInsert();
		} catch (SQLiteConstraintException e) {
			return 0;
		}
	}

	@Override
	public void update(Tag tag) {
		if ((tag != null) && (tag.id != 0)) {
			Tag exists = select(tag.id);
			if (exists == null) {
				throw new IllegalArgumentException("Cannot update entity that does not already exist.");
			}

			// use transaction
			db.beginTransaction();
			try {
				// update book
				final ContentValues values = new ContentValues();
				values.put(DataConstants.TAGTEXT, tag.text);
				db.update(DataConstants.TAG_TABLE, values, DataConstants.TAG_ID + " = ?", new String[] { String
						.valueOf(tag.id) });

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
		Tag tag = select(id);
		if (tag != null) {
			db.delete(DataConstants.TAG_TABLE, DataConstants.TAG_ID + " = ?", new String[] { String.valueOf(id) });
		}
	}

	public void delete(final String name) {
		Tag tag = select(name);
		if (tag != null) {
			db.delete(DataConstants.TAG_TABLE, DataConstants.TAGTEXT + " = ?", new String[] { name });
		}
	}	
}

package com.totsp.bookworm.data.dao;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import com.totsp.bookworm.Constants;
import com.totsp.bookworm.R;
import com.totsp.bookworm.data.DataConstants;
import com.totsp.bookworm.model.Tag;

// TODO: Extract the tagbooks methods into an abstract tagItemsDAO and a sub-class extending it for books.

/**
 *  DAO for item tag DB tables.
 *  As well as implementing the DAO methods for maintaining the tag entries this class also includes static methods to 
 *  be used by the SQL open helper to create and upgrade the tag tables.  
 */
public class TagDAO implements DAO<Tag>{

	private static final String QUERY_CURSOR_PREFIX =
		"select tags.tid as _id, tags.ttext from tags";


	private static final String QUERY_TAGS_BY_BOOK_ID =
		"select tags.ttext from tags join tagbooks on (tagbooks.tid=tags.tid)" 
		+ " join book on (book.bid = tagbooks.bid) where book.bid=? order by tags.ttext asc";


	private static final String QUERY_TAG_ID_BY_BOOK_ID =
		"select tagbooks.tid from tagbooks where tagbooks.bid=?";


	
	private static final String QUERY_BOOK_POSITION =
		"select tagbooks.tbid from tagbooks where (tagbooks.bid=? and tagbooks.tid=?)";

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
	 * Queries the tags with a calculated column indicating whether each tag is linked to the specified book.
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
	 * Returns a string containing all of the tags which are linked against the specified book.
	 * 
	 * @param bookId  Book to query
	 * @param separator Tag separator text
	 * @return        String containing comma separated tag text
	 */
	public String getTagsString(final long bookId, CharSequence separator) {
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
	
	
	/**
	 * Queries the ID of all tags linked to the specified book.
	 * 
	 * @param bookId
	 * 
	 * @return A list of tag ID's representing tags that are linked to the specified book
	 */
	public ArrayList<Long> getLinkedTagIds(long bookId) {
		ArrayList<Long> result = new ArrayList<Long>();
		boolean moreTags;
		
		try {
			Cursor c = db.rawQuery(QUERY_TAG_ID_BY_BOOK_ID, new String[] { String.valueOf(bookId) });	

			moreTags = c.moveToFirst();
			while (moreTags) {
				result.add(c.getLong(0));
				moreTags = c.moveToNext();
			}

			if (!c.isClosed()) {
				c.close();
			}
		} catch (SQLException e) {
			Log.d(Constants.LOG_TAG, "TagDAO", e);
		}
		
		return result;		
	}
	
	
	/**
	 * Swaps the order of two books in the tag books table.
	 * 
	 * @param tagId    ID of tag collection to be updated
	 * @param bookId1  ID of first book
	 * @param bookId2  ID of second book
	 */
	public void swapBooks(long tagId, long bookId1, long bookId2) {
		int position1;
		int position2;
		
		position1 = getTagBookKey(tagId, bookId1);		
		position2 = getTagBookKey(tagId, bookId2);
		
		if ((position1 > 0) && (position2 > 0)) {

			// use transaction
			db.beginTransaction();
			try {
				// Swap books by updating each entry with the ID of the other book
				final ContentValues values = new ContentValues();
				
				values.put(DataConstants.TAG_ID, tagId);
				values.put(DataConstants.BOOKID, bookId1);
				db.update(DataConstants.TAG_BOOKS_TABLE, values, DataConstants.TAG_BOOK_ID + " = ?", 
						new String[] { String.valueOf(position2) });
				values.clear();
				values.put(DataConstants.TAG_ID, tagId);
				values.put(DataConstants.BOOKID, bookId2);
				db.update(DataConstants.TAG_BOOKS_TABLE, values, DataConstants.TAG_BOOK_ID + " = ?", 
						new String[] { String.valueOf(position1) });
				
				db.setTransactionSuccessful();
			} catch (SQLException e) {
				Log.e(Constants.LOG_TAG, "Error updating TAG_BOOKS_TABLE.", e);
			} finally {
				db.endTransaction();
			}
		} else {
			throw new IllegalArgumentException("One or both books are not linked to tag");
		}
	}

	
	/**
	 * Returns the tagbooks table key for the specified tag and book
	 * 
	 * @param tagId
	 * @param bookId
	 */
	private int getTagBookKey(long tagId, long bookId) {
		int position = -1;
		Cursor c;
		
		c = db.rawQuery(QUERY_BOOK_POSITION, new String[] { String.valueOf(bookId),  String.valueOf(tagId) });	
		if (c.moveToFirst()) {
			position = c.getInt(0);
		}
		if (!c.isClosed()) {
			c.close();
		}
		return position;
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
			db.delete(DataConstants.TAG_BOOKS_TABLE, DataConstants.TAG_ID + " = ?", new String[]{ String.valueOf(id) });
			db.delete(DataConstants.TAG_TABLE, DataConstants.TAG_ID + " = ?", new String[] { String.valueOf(id) });
		}
	}

	
	/**
	 * Factory method to create a new tag selector dialog builder.
	 *    
	 * @param context  Context for the new dialog
	 * @param bookId   ID of book to which tags are linked
	 * 
	 * @return A new tag selector dialog builder instance
	 */
	public SelectorDialogBuilder getSelectorDialogBuilder(Context context, long bookId) {
		return new SelectorDialogBuilder(context, bookId); 
	}	

	
	/**
	 * Tag selector dialog builder utility class.<br>
	 * Creates a new tag selector dialog builder for a specific book.
	 * The dialog consists of a multi-selection list of all tags with tags linked to the specified book selected. 
	 * This class handles the management of the cursor and DB updates on selection changes.
	 */
	public class SelectorDialogBuilder extends AlertDialog.Builder {
		private Cursor tagCursor;
		private Activity activity;
		private DialogInterface.OnClickListener clientListener;
		private long bookId =-1;

		public SelectorDialogBuilder(Context context, long bookId) {
			super(context);
			activity = (Activity)context;
			this.bookId = bookId;
		}

		@Override
		public AlertDialog create() {
			tagCursor = getSelectorCursor(bookId);
			setTitle(activity.getString(R.string.titleTagSelector));
			if ((tagCursor != null) && (tagCursor.getCount() > 0)) {
				activity.startManagingCursor(tagCursor);

				setMultiChoiceItems(tagCursor, new String("tagged"), new String("taggedText"),
						new OnMultiChoiceClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which, boolean isChecked) {
						tagCursor.moveToPosition(which);

						if (isChecked) {
							insertBook(tagCursor.getLong(0), bookId);
						}
						else
						{
							deleteBook(tagCursor.getLong(0), bookId);
						}

						tagCursor.requery();

						if (clientListener != null) {
							clientListener.onClick(dialog, which);
						}					
					}								
				});		   		   
			}
			else
			{
				setMessage(R.string.msgNoTagsFound);
			}

			return super.create();
		}

		public SelectorDialogBuilder setOnClickListener(DialogInterface.OnClickListener listener) {
			clientListener = listener;
			return this;
		}

	}
	
}

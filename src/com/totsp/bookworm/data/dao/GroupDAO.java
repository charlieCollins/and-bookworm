package com.totsp.bookworm.data.dao;

import java.util.ArrayList;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import com.totsp.bookworm.Constants;
import com.totsp.bookworm.data.DataConstants;
import com.totsp.bookworm.data.DataManager;
import com.totsp.bookworm.model.Group;


/**
 *  DAO for item group DB tables.
 *  As well as implementing the DAO methods for maintaining the group entries
 *  this class also includes static methods to be used by the SQL open helper to 
 *  create and upgrade the group tables.  
 */
public class GroupDAO implements DAO<Group>{

	private static final String QUERY_CURSOR_PREFIX =
		"select groups.gid as _id, groups.name from groups";


	private static final String QUERY_GROUP_BY_BOOK_ID_PREFIX =
		"select groups.name from group join book on book.gbid = groups.gid";

	/*
	 * Prefix for a query to return all books in the specified groups.
	 * The group entry in the group table is found by group ID, which is used to look up all entries
	 * in the groupbooks table with a matching group ID.
	 */
	private static final String QUERY_BOOKS_BY_GROUP_ID_PREFIX =
		"select book.bid as _id, book.tit, book.subtit, "
		+ "group_concat(author.name) as authors "
		+ "from book "
		+ "left outer join bookauthor on bookauthor.bid = book.bid left outer join author on author.aid = bookauthor.aid";



	private final SQLiteStatement groupInsertStmt;
	private static final String GROUP_INSERT =
		"insert into " + DataConstants.GROUP_TABLE + "(" + DataConstants.NAME + ") values (?)";

	private SQLiteDatabase db;

	public GroupDAO(SQLiteDatabase db) {
		this.db = db;

		// statements
		groupInsertStmt = db.compileStatement(GroupDAO.GROUP_INSERT);
	}

	/**
	 * Create group and group books tables in DB
	 * @param db  SQLite database 
	 */
	public static void onCreate(final SQLiteDatabase db) {
		// using StringBuilder here because it is easier to read/reuse lines
		StringBuilder sb = new StringBuilder();

		// group table
		sb.setLength(0);
		sb.append("CREATE TABLE " + DataConstants.GROUP_TABLE + " (");
		sb.append(DataConstants.GROUP_ID + " INTEGER PRIMARY KEY, ");
		sb.append(DataConstants.NAME + " TEXT, ");
		sb.append(DataConstants.DESCRIPTION + " TEXT");
		sb.append(");");
		db.execSQL(sb.toString());

		// group table
		sb.setLength(0);
		sb.append("CREATE TABLE " + DataConstants.GROUP_ITEMS_TABLE + " (");
		sb.append(DataConstants.GROUP_BOOK_ID + " INTEGER PRIMARY KEY, ");
		sb.append(DataConstants.GROUP_ID + " INTEGER, ");
		sb.append(DataConstants.BOOKID + " INTEGER, ");
		sb.append(DataConstants.GROUP_BOOK_NUM + " INTEGER, ");

		sb.append("FOREIGN KEY(" + DataConstants.BOOKID + ") REFERENCES " + DataConstants.BOOK_TABLE + "("
				+ DataConstants.BOOKID + "), ");
		sb.append("FOREIGN KEY(" + DataConstants.GROUP_ID + ") REFERENCES " + DataConstants.GROUP_TABLE + "("
				+ DataConstants.GROUP_ID + ") ");
		sb.append(");");
		db.execSQL(sb.toString());
	}


	public static void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
		if (oldVersion < 12) 
		{
			onCreate(db);
		}
	}

	/**
	 * Queries whether the book is a member of the specified groups.
	 * 
	 * @param groupId ID of the group
	 * @param bookId   ID of the book
	 * 
	 * @return  True if the book is found in the group, false otherwise
	 */
	public boolean isInGroup(final long groupId, final long bookId) {
		boolean result;
		Cursor c =
			db.query(DataConstants.GROUP_ITEMS_TABLE, new String[] {"*"}, 
					DataConstants.GROUP_ID + " = ? AND " + DataConstants.BOOKID + " = ?", 
					new String[] { String.valueOf(groupId), String.valueOf(bookId) }, null, null, null, "1");
		result = (c.getCount() > 0);
		if (!c.isClosed()) {
			c.close();
		}
		return result;
		
	}
	
	public void insertBook(final long groupId, final long bookId, final long bookNum) {
		if (!isInGroup(groupId, bookId)) {
			ContentValues entry = new ContentValues();
			entry.put(DataConstants.GROUP_ID, groupId);
			entry.put(DataConstants.BOOKID, bookId);
			entry.put(DataConstants.GROUP_BOOK_NUM, bookNum);
			db.insert(DataConstants.GROUP_ITEMS_TABLE, null, entry);
		}
	}
	
	public void deleteBook(final long groupId, final long bookId) {

		db.delete(DataConstants.GROUP_ITEMS_TABLE, 
				  DataConstants.GROUP_ID + " = ? AND " + DataConstants.BOOKID + " = ?", 
				  new String[] { String.valueOf(groupId), String.valueOf(bookId) });
	}

	
	
	public void deleteTable() {
        db.delete(DataConstants.GROUP_TABLE, null, null);
        db.delete(DataConstants.GROUP_ITEMS_TABLE, null, null);
	}
	

	@Override
	public Cursor getCursor(final String orderBy, final String whereClauseLimit) {
		// note that query MUST have a column named _id
		StringBuilder sb = new StringBuilder();
		sb.append(GroupDAO.QUERY_CURSOR_PREFIX);
		if ((whereClauseLimit != null) && (whereClauseLimit.length() > 0)) {
			sb.append(" " + whereClauseLimit);
		}
		sb.append(" group by groups.gid");
		if ((orderBy != null) && (orderBy.length() > 0)) {
			sb.append(" order by " + orderBy);
		}

		return db.rawQuery(sb.toString(), null);
	}

	@Override
	public Group select(final long id) {
		Group s = null;
		Cursor c =
			db.query(DataConstants.GROUP_TABLE, new String[] { 
					DataConstants.NAME, DataConstants.DESCRIPTION }, 
					DataConstants.GROUP_ID + " = ?", 
					new String[] { String.valueOf(id) }, null, null, null, "1");
		if (c.moveToFirst()) {
			s = new Group();
			s.id = (id);
			s.name = (c.getString(0));
			s.description = (c.getString(1));
		}
		if (!c.isClosed()) {
			c.close();
		}
		return s;
	}

	public Group select(final String name) {
		Group s = null;
		Cursor c =
			db.query(DataConstants.GROUP_TABLE, new String[] { DataConstants.GROUP_ID }, DataConstants.NAME
					+ " = ?", new String[] { name }, null, null, null, "1");
		if (c.moveToFirst()) {
			s = select(c.getLong(0));
		}
		if (!c.isClosed()) {
			c.close();
		}
		return s;
	}

	@Override
	public ArrayList<Group> selectAll() {
		ArrayList<Group> set = new ArrayList<Group>();
		Cursor c =
			db.query(DataConstants.GROUP_TABLE, new String[] { DataConstants.GROUP_ID, 
					 DataConstants.NAME, DataConstants.DESCRIPTION}, 
					 null, null, null, null, DataConstants.NAME + " desc", null);
		if (c.moveToFirst()) {
			do {
				Group s = new Group();
				s.id = (c.getLong(0));
				s.name = (c.getString(1));
				s.description = (c.getString(2));
				set.add(s);
			} while (c.moveToNext());
		}
		if (!c.isClosed()) {
			c.close();
		}
		return set;
	}


	//	public ArrayList<Group> selectByGroupId(final long groupId) {
	//		ArrayList<Book> books = new ArrayList<Book>();
	//		// TODO string.format this with final String, faster?
	//		StringBuilder sb = new StringBuilder();
	//		sb.append(GroupDAO.QUERY_BOOKS_BY_GROUP_ID_PREFIX);
	//		sb.append(" where groups.gid = " + groupId);
	//		sb.append(" order by groupbooks.gbnum asc");
	//		Cursor c = db.rawQuery(sb.toString(), null);
	//
	//		if (c.moveToFirst()) {
	//			do {
	//				Book b = new Book();
	//				b.name = c.getString(0);
	//				books.add(b);
	//			} while (c.moveToNext());
	//		}
	//		if (!c.isClosed()) {
	//			c.close();
	//		}
	//		return books;
	//	}


	@Override
	public long insert(final Group s) {
		groupInsertStmt.clearBindings();
		groupInsertStmt.bindString(1, s.name);
		return groupInsertStmt.executeInsert();
	}

	@Override
	public void update(Group s) {
		if ((s != null) && (s.id != 0)) {
			Group exists = select(s.id);
			if (exists == null) {
				throw new IllegalArgumentException("Cannot update entity that does not already exist.");
			}

			// use transaction
			db.beginTransaction();
			try {
				// update book
				final ContentValues values = new ContentValues();
				values.put(DataConstants.NAME, s.name);
				values.put(DataConstants.DESCRIPTION, s.description);
				db.update(DataConstants.GROUP_TABLE, values, DataConstants.GROUP_ID + " = ?", new String[] { String
						.valueOf(s.id) });

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
		Group s = select(id);
		if (s != null) {
			db.delete(DataConstants.GROUP_TABLE, DataConstants.GROUP_ID + " = ?", new String[] { String.valueOf(id) });
		}
	}

	public void delete(final String name) {
		Group s = select(name);
		if (s != null) {
			db.delete(DataConstants.GROUP_TABLE, DataConstants.NAME + " = ?", new String[] { name });
		}
	}	
}

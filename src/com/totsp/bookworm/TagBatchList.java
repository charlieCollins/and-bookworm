package com.totsp.bookworm;

import com.totsp.bookworm.data.DataConstants;
import com.totsp.bookworm.model.Book;
import com.totsp.bookworm.util.StringUtil;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CursorAdapter;
import android.widget.FilterQueryProvider;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

public class TagBatchList extends Activity {
	
	private static final long NO_BOOK_SELECTED = 0;
	
	private BookWormApplication application;
	private SharedPreferences prefs;

	//! ArrayAdapter connects the spinner widget to array-based data.
	private CursorAdapter tagAdapter;
	private CursorAdapter booksAdapter;

	private Spinner tagSelector;
	private ListView bookListView;
	private Bitmap coverImageMissing;
	private Cursor tagCursor;
	private Cursor booksCursor;

	private ImageView sortTagImage;
	private ImageView filterTagImage;
	private ImageView addGroupImage;
	private ImageView editTagImage;
	protected long selectedBookId;
	protected boolean onlyShowTagged;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tagbatchlist);
		setTitle(R.string.titleTagBatchList);
		
		selectedBookId = NO_BOOK_SELECTED;
		onlyShowTagged = false;

		application = (BookWormApplication) getApplication();
		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		coverImageMissing = BitmapFactory.decodeResource(getResources(), R.drawable.book_cover_missing);
		tagSelector = (Spinner) findViewById(R.id.tagselector);
				
		bookListView = (ListView) findViewById(R.id.bookfilterview);
	    bookListView.setEmptyView(findViewById(R.id.empty));
		bookListView.setTextFilterEnabled(true);
				
		setupActionBar();
		bindAdapters();
	}

	/**
	 * Configures the on-screen quick-action bar and connects listeners to the 
	 * action buttons.
	 */
	private void setupActionBar() {
		sortTagImage = (ImageView) findViewById(R.id.tagactionsort);
		
		filterTagImage = (ImageView) findViewById(R.id.tagactionfilter);
		filterTagImage.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				onlyShowTagged = !onlyShowTagged;				
			}
		});
		
		addGroupImage = (ImageView) findViewById(R.id.tagadd);
		addGroupImage.setOnClickListener(new OnClickListener() {		
			public void onClick(View v) {
				application.selectedTag = null;
				startActivity(new Intent(TagBatchList.this, TagEditor.class));			
			}
		});

		editTagImage = (ImageView) findViewById(R.id.tagedit);
		editTagImage.setOnClickListener(new OnClickListener() {		
			public void onClick(View v) {
				startActivity(new Intent(TagBatchList.this, TagEditor.class));			
			}
		});
	}


	/**
	 * Bind cursor adapters for tag dropdown and book list views
	 */
	private void bindAdapters() {
		String orderBy = DataConstants.ORDER_BY_TAG_TEXT_ASC;
		tagCursor = application.dataManager.getTagCursor(orderBy, null);
		if ((tagCursor != null) && (tagCursor.getCount() > 0)) {
			startManagingCursor(tagCursor);
			tagAdapter = new TagCursorAdapter(tagCursor);
			tagSelector.setAdapter(tagAdapter);
	        OnItemSelectedListener tagOnItemSelectedListener = new TagOnItemSelectedListener();
	        tagSelector.setOnItemSelectedListener(tagOnItemSelectedListener);
		}

		orderBy = prefs.getString(Constants.DEFAULT_SORT_ORDER, DataConstants.ORDER_BY_TITLE_ASC);
		booksCursor = application.dataManager.getBookCursor(orderBy, null);
		if ((booksCursor != null) && (booksCursor.getCount() > 0)) {
			startManagingCursor(booksCursor);
			booksAdapter = new BookCursorAdapter(booksCursor);
			bookListView.setAdapter(booksAdapter);

			int lastMainPos = application.lastMainListPosition;
			if ((lastMainPos - 1) < booksAdapter.getCount()) {
				bookListView.setSelection(application.lastMainListPosition - 1);
			}
		}

	}


	// static and package access as an Android optimization 
	// (used in inner class)
	static class TagViewHolder {
		TextView name;
	}

	private class TagCursorAdapter extends CursorAdapter {

		public TagCursorAdapter(final Cursor c) {
			super(TagBatchList.this, c, true);
		}

		@Override
		public void bindView(final View v, final Context context, final Cursor c) {
			populateView(v, c);
		}

		@Override
		public View newView(final Context context, final Cursor c, final ViewGroup parent) {
			// use ViewHolder pattern to avoid extra trips to findViewById
			View v = new TextView(context);

			TagViewHolder holder = new TagViewHolder();
			holder.name = (TextView) v;
			holder.name.setLines(1);
			holder.name.setTextColor(android.graphics.Color.BLACK);
			holder.name.setText("Add Group");
			holder.name.setTextSize(20);
			v.setTag(holder);
			populateView(v, c);
			return v;
		}

		private void populateView(final View v, final Cursor c) {
			// use ViewHolder pattern to avoid extra trips to findViewById
			TagViewHolder holder = (TagViewHolder) v.getTag();

			if ((c != null) && !c.isClosed()) {
				long id = c.getLong(0);

				String name = c.getString(1);
				if (application.debugEnabled) {
					Log.d(Constants.LOG_TAG, "book (id|title) from cursor - " + id + "|" + name);
				}            

				holder.name.setText(name);

			}
		}
	}    


	// static and package access as an Android optimization 
	// (used in inner class)
	static class BookViewHolder {
		ImageView coverImage;
		TextView title;
		TextView authors;
		CheckBox isTagged;
	}

	//
	// BookCursorAdapter
	//
	private class BookCursorAdapter extends CursorAdapter implements FilterQueryProvider {

		LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		public BookCursorAdapter(final Cursor c) {
			super(TagBatchList.this, c, true);
			setFilterQueryProvider(this);
		}

		// FilterQueryProvider impl
		public Cursor runQuery(CharSequence constraint) {
			Cursor c = null;
			if ((constraint == null) || (constraint.length() == 0)) {
				c = getCursor();
			} else {
				String pattern = "'%" + constraint + "%'";
				String orderBy = prefs.getString(Constants.DEFAULT_SORT_ORDER, DataConstants.ORDER_BY_TITLE_ASC);
				c = application.dataManager.getBookCursor(orderBy, "where book.tit like " + pattern);
			}
			booksCursor = c;
			return c;
		}

		@Override
		public void bindView(final View v, final Context context, final Cursor c) {
			populateView(v, c);
		}

		@Override
		public View newView(final Context context, final Cursor c, final ViewGroup parent) {
			// use ViewHolder pattern to avoid extra trips to findViewById
			View v = vi.inflate(R.layout.tag_list_items, parent, false);
			BookViewHolder holder = new BookViewHolder();
			holder.coverImage = (ImageView) v.findViewById(R.id.tag_list_items_image);
			holder.title = (TextView) v.findViewById(R.id.tag_list_items_title);
			holder.authors = (TextView) v.findViewById(R.id.tag_list_items_authors);
			holder.isTagged = (CheckBox) v.findViewById(R.id.tag_list_items_tagged);
			v.setTag(holder);
			populateView(v, c);
			return v;
		}

		private void populateView(final View v, final Cursor c) {
			// use ViewHolder pattern to avoid extra trips to findViewById
			BookViewHolder holder = (BookViewHolder) v.getTag();

			if ((c != null) && !c.isClosed()) {
				long id = c.getLong(0);

				// TODO investigate, may need to file Android/SQLite bug
				// Log.i(Constants.LOG_TAG, "COLUMN INDEX rating - " +
				// c.getColumnIndex(DataConstants.RATING));
				// as soon as query has group by or group_concat the
				// getColumnIndex fails? (explicit works)
				/*
				 * bid = 0 tit = 1 subtit = 2 subject = 3 pub = 4 datepub = 5
				 * format = 6 rstat = 7 rat = 8 blurb = 9 authors = 10
				 */

				boolean inGroup = false;
				if (application.selectedTag != null) {
					inGroup = application.dataManager.isTagged(application.selectedTag.id, id);
				}
				String title = c.getString(1);
				String authors = c.getString(10);

				if (application.debugEnabled) {
					Log.d(Constants.LOG_TAG, "book (id|title) from cursor - " + id + "|" + title);
				}

				ImageView coverImage = holder.coverImage;
				Bitmap coverImageBitmap = application.imageManager.retrieveBitmap(title, id, true);
				if (coverImageBitmap != null) {
					coverImage.setImageBitmap(coverImageBitmap);
				} else {
					coverImage.setImageBitmap(coverImageMissing);
				}
				coverImage.setTag(new Long(id));
				coverImage.setOnLongClickListener(new OnLongClickListener() {
					
					@Override
					public boolean onLongClick(View v) {
						// TODO Implement drag&drop group order editting here
						if (TagBatchList.this.onlyShowTagged && 
								TagBatchList.this.selectedBookId == TagBatchList.NO_BOOK_SELECTED) {							
							Toast.makeText(TagBatchList.this, getString(R.string.msgReorderTag), 
									Toast.LENGTH_LONG).show();
							TagBatchList.this.selectedBookId = (Long) v.getTag();
						}
						if (application.debugEnabled) {
							Log.d(Constants.LOG_TAG, "Selected Id: " + String.valueOf(TagBatchList.this.selectedBookId));
							Log.d(Constants.LOG_TAG, "Filtered = " + String.valueOf(onlyShowTagged));
						}
						return false;
					}
				});
				coverImage.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						// TODO Implement drag&drop group order editting here
						if (TagBatchList.this.onlyShowTagged && 
								TagBatchList.this.selectedBookId != TagBatchList.NO_BOOK_SELECTED) {							

							if (application.debugEnabled) {
								Log.d(Constants.LOG_TAG, "Moved book: " + String.valueOf(TagBatchList.this.selectedBookId));
								Log.d(Constants.LOG_TAG, "to: " + String.valueOf((Long) v.getTag()));
								Log.d(Constants.LOG_TAG, "Filtered = " + String.valueOf(onlyShowTagged));
							}

							TagBatchList.this.selectedBookId = NO_BOOK_SELECTED;
						}
					}
				});
				
				holder.title.setText(title);
				holder.authors.setText(StringUtil.addSpacesToCSVString(authors));

				holder.isTagged.setChecked(inGroup);
				holder.isTagged.setId((int) id);
				holder.isTagged.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView,	boolean isChecked) {
						application.dataManager.setBookTagged(buttonView.getId(), application.selectedTag.id, 
								isChecked);
						
					}					
				});
			}
		}
	}  
	
	
    /**
     *  Callback listener that implements the
     *  {@link android.widget.AdapterView.OnItemSelectedListener} interface for
     *  the group selection spinner
     */
    public class TagOnItemSelectedListener implements OnItemSelectedListener {

        /**
         * Callback triggered when the user selects an item in the group spinner.
         *
         * @see android.widget.AdapterView.OnItemSelectedListener#onItemSelected(
         *  android.widget.AdapterView, android.view.View, int, long)
         *  
         * @param parent  The AdapterView where the selection happened
         * @param view    The view within the AdapterView that was clicked
         * @param pos     The position of the view in the adapter
         * @param id      The row id of the item that is selected 
         */
    	@Override
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            // TODO not sure why change listener fires when onCreate is init, but does
            Cursor cursor = (Cursor) parent.getItemAtPosition(pos);
            long groupId = cursor.getLong(0);
            application.selectedTag = application.dataManager.selectTag(groupId);
            booksAdapter.notifyDataSetChanged();
        }

        // Required implementation of abstract method
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }
    

 
    
	
}

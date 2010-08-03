package com.totsp.bookworm;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.FilterQueryProvider;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.totsp.bookworm.data.DataConstants;
import com.totsp.bookworm.util.PrefListDialogBuilder;
import com.totsp.bookworm.util.StringUtil;


/**
 * Tag batch mode editing activity.
 * Allows tags to be assigned or removed from multiple books simultaneously as well as providing the entry point to 
 * tag creation and editing activities.
 * <br>
 * Displays a list of books similar to the main activity, but with a CheckBox controlling whether the currently selected
 * tag is linked to each book. The CheckBox is implemented as an actual CheckBox in the ListView adapter rather than 
 * using the ListView multi-select mode to avoid item selection changing when any part of the book row is touched, which
 * is error prone due to false positives when scrolling. This approach is slightly less efficient due to requiring a
 * separate click listener for each displayed book, but since the listener is only created once for each view in the 
 * list, the overhead should be minimal.
 * 
 */
public class TagBatchList extends Activity {
	
	private static final long NO_BOOK_SELECTED = 0;
	
	private BookWormApplication application;
	private SharedPreferences prefs;

	//! ArrayAdapter connects the spinner widget to array-based data.
	private CursorAdapter tagAdapter;
	private BookCursorAdapter booksAdapter;
	
	private PrefListDialogBuilder sortDialog;
	private PrefListDialogBuilder filterDialog;

	private Spinner tagSelector;
	private ListView bookListView;
	private Bitmap coverImageMissing;
	private Cursor tagCursor;
	private Cursor booksCursor;

	private ImageView sortTagImage;
	private ImageView filterTagImage;
	private ImageView addTagImage;
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
		
		setupDialogs();
		setupActionBar();
		bindAdapters();
	}

	/**
	 * Configures the on-screen quick-action bar and connects listeners to the 
	 * action buttons.
	 */
	private void setupActionBar() {
		sortTagImage = (ImageView) findViewById(R.id.tagactionsort);
		sortTagImage.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				sortDialog.show();				
			}
		});
		
		filterTagImage = (ImageView) findViewById(R.id.tagactionfilter);
		filterTagImage.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				filterDialog.show();
				onlyShowTagged = !onlyShowTagged;				
			}
		});
		
		addTagImage = (ImageView) findViewById(R.id.tagadd);
		addTagImage.setOnClickListener(new OnClickListener() {		
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

		orderBy = prefs.getString(Constants.TAG_SORT_ORDER, DataConstants.ORDER_BY_TITLE_ASC);
		booksCursor = application.dataManager.getBookCursor(orderBy, null);
		if ((booksCursor != null) && (booksCursor.getCount() > 0)) {
			startManagingCursor(booksCursor);
			booksAdapter = new BookCursorAdapter(booksCursor);
			bookListView.setAdapter(booksAdapter);
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
		long bookId;
		ImageView coverImage;
		TextView title;
		TextView authors;
		CheckBox checkedTag;
	}

	//
	// BookCursorAdapter
	//
	private class BookCursorAdapter extends CursorAdapter implements FilterQueryProvider {

		LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		boolean suppressClick = false;

		public BookCursorAdapter(final Cursor c) {
			super(TagBatchList.this, c, true);
			setFilterQueryProvider(this);
		}

		// FilterQueryProvider impl
		public Cursor runQuery(CharSequence constraint) {
			Cursor c = null;
			String orderBy = prefs.getString(Constants.TAG_SORT_ORDER, DataConstants.ORDER_BY_TITLE_ASC);
			if ((constraint == null) || (constraint.length() == 0)) {
				c = application.dataManager.getBookCursor(orderBy, null);
			} else {
				String filter = prefs.getString(Constants.TAG_FILTER, DataConstants.FILTER_BY_TITLE);
				Log.d(Constants.LOG_TAG, "filter text = '" + String.format(filter, constraint) + "'");
				c = application.dataManager.getBookCursor(orderBy, String.format(filter, constraint));
			}
			startManagingCursor(c);
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
			holder.checkedTag = (CheckBox) v.findViewById(R.id.tag_list_items_tagged);
			
			holder.coverImage.setOnLongClickListener(new OnLongClickListener() {
				
				@Override
				public boolean onLongClick(View v) {
					// TODO Implement drag&drop group order editing here
					if (TagBatchList.this.onlyShowTagged && 
							TagBatchList.this.selectedBookId == TagBatchList.NO_BOOK_SELECTED) {							
						Toast.makeText(TagBatchList.this, getString(R.string.msgReorderTag), 
								Toast.LENGTH_LONG).show();
						TagBatchList.this.selectedBookId = (Long) v.getTag();
						BookCursorAdapter.this.notifyDataSetChanged();
						// Suppress the spurious click event received on long click.
						suppressClick = true;
					}
					if (application.debugEnabled) {
						Log.d(Constants.LOG_TAG, "Selected Id: " + String.valueOf(TagBatchList.this.selectedBookId));
						Log.d(Constants.LOG_TAG, "Filtered = " + String.valueOf(onlyShowTagged));
					}
					return false;
				}
			});
			
			holder.coverImage.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Implement drag&drop group order editting here
					if (!suppressClick && TagBatchList.this.onlyShowTagged && 
							TagBatchList.this.selectedBookId != TagBatchList.NO_BOOK_SELECTED) {							

						if (application.debugEnabled) {
							Log.d(Constants.LOG_TAG, "Moved book: " + String.valueOf(TagBatchList.this.selectedBookId));
							Log.d(Constants.LOG_TAG, "to: " + String.valueOf((Long) v.getTag()));
							Log.d(Constants.LOG_TAG, "Filtered = " + String.valueOf(onlyShowTagged));
						}
						TagBatchList.this.selectedBookId = NO_BOOK_SELECTED;
						BookCursorAdapter.this.notifyDataSetChanged();
					}
					// Only ignore first click after long click since it is always for the same item 
					suppressClick = false;
				}
			});
			
			holder.checkedTag.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View view) {
					long bookId = (Long) ((CheckBox)view).getTag();
					if (application.debugEnabled) {
						Log.d(Constants.LOG_TAG, String.format("Clicked - Book ID: %d", bookId));
					}
					application.dataManager.toggleBookTagged(bookId, application.selectedTag.id);
					
				}
				
			});
			
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

				Bitmap coverImageBitmap = application.imageManager.retrieveBitmap(title, id, true);
				if (coverImageBitmap != null) {
					holder.coverImage.setImageBitmap(coverImageBitmap);
				} else {
					holder.coverImage.setImageBitmap(coverImageMissing);
				}
				holder.coverImage.setTag(new Long(id));
				// Hi-lite book selected for re-ordering
				if (TagBatchList.this.selectedBookId == id) { 
					holder.coverImage.setBackgroundDrawable(application.getResources()
							.getDrawable(R.drawable.selected_border));
				}
				else
				{
					holder.coverImage.setBackgroundDrawable(application.getResources()
							.getDrawable(R.drawable.border1));
				}

				
				holder.title.setText(title);
				holder.authors.setText(StringUtil.addSpacesToCSVString(authors));

				holder.checkedTag.setChecked(inGroup);
				// TODO: Consider creating view tag Long object when holder is instanciated and just assigning a value
				//       here as a performance optimization.
				holder.checkedTag.setTag(new Long(id));
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
            Cursor cursor = (Cursor) parent.getItemAtPosition(pos);
            long tagId = cursor.getLong(0);
            application.selectedTag = application.dataManager.selectTag(tagId);
            booksAdapter.notifyDataSetChanged();
        }

        // Required implementation of abstract method
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }
    
    private void setupDialogs() {
    	sortDialog = new PrefListDialogBuilder(this, prefs, Constants.TAG_SORT_ORDER);
    	sortDialog.setTitle(getString(R.string.btnSortBy))
    		.addEntry(getString(R.string.labelTitle), DataConstants.ORDER_BY_TITLE_ASC)
    		.addEntry(getString(R.string.labelAuthorsShort), DataConstants.ORDER_BY_AUTHORS_ASC)
    		.addEntry(getString(R.string.labelRating), DataConstants.ORDER_BY_RATING_DESC)
    		.addEntry(getString(R.string.labelReadstatus), DataConstants.ORDER_BY_READ_DESC)
    		.addEntry(getString(R.string.labelSubject),DataConstants.ORDER_BY_SUBJECT_ASC)
    		.addEntry(getString(R.string.labelDatepub), DataConstants.ORDER_BY_DATE_PUB_DESC)
    		.addEntry(getString(R.string.labelPublisher), DataConstants.ORDER_BY_PUB_ASC)
    		.setOnClickListener(new DialogInterface.OnClickListener() {		
				@Override
				public void onClick(DialogInterface dialog, int which) {
					TagBatchList.this.booksAdapter.runQuery(null);
				}
			});
    	sortDialog.create();
    	  	
    	filterDialog = new PrefListDialogBuilder(this, prefs, Constants.TAG_FILTER);
    	filterDialog.setTitle(getString(R.string.btnFilterBy))
			.addEntry(getString(R.string.labelTitle), DataConstants.FILTER_BY_TITLE)
			.addEntry(getString(R.string.labelAuthorsShort), DataConstants.FILTER_BY_AUTHOR)
			.addEntry(getString(R.string.labelSubject),DataConstants.FILTER_BY_SUBJECT)
			.addEntry(getString(R.string.labelPublisher), DataConstants.FILTER_BY_PUBLISHER)
			.setOnClickListener(new DialogInterface.OnClickListener() {		
				@Override
				public void onClick(DialogInterface dialog, int which) {
					TagBatchList.this.booksAdapter.runQuery(null);
				}
			});
		filterDialog.create();
    }
    
	
}

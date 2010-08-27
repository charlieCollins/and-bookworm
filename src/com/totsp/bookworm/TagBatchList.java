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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.FilterQueryProvider;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.totsp.bookworm.data.DataConstants;
import com.totsp.bookworm.model.Book;
import com.totsp.bookworm.util.PrefListDialogBuilder;
import com.totsp.bookworm.util.StringUtil;


/**
 * Tag batch mode editing activity.
 * Allows tags to be assigned or removed from multiple books simultaneously, books sharing a common tag to be 
 * re-ordered, as well as providing the entry point to tag creation and editing activities.
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
	private ImageView reorderTagsImage;
	private ImageView addTagImage;
	private ImageView editTagImage;
	private ImageView moveUpImage;
	private ImageView moveDownImage;
	private LinearLayout defaultActionBar;
	private LinearLayout reorderActionBar;
	
	protected long selectedBookId;
	private int selectedBookPosition;
	
	
	private boolean sortDialogIsShowing;
	private boolean filterDialogIsShowing;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tagbatchlist);
		setTitle(R.string.titleTagBatchList);
		
		selectedBookId = NO_BOOK_SELECTED;

		application = (BookWormApplication) getApplication();
		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		coverImageMissing = BitmapFactory.decodeResource(getResources(), R.drawable.book_cover_missing);
		tagSelector = (Spinner) findViewById(R.id.tagselector);
				
		bookListView = (ListView) findViewById(R.id.bookfilterview);
	    bookListView.setEmptyView(findViewById(R.id.empty));
		bookListView.setTextFilterEnabled(true);

		bookListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(final AdapterView<?> parent, final View v, final int index, final long id) {
				booksCursor.moveToPosition(index);
				// NOTE - this is tricky, table doesn't have _id, but CursorAdapter requires it
				// in the query we used "book.bid as _id" so here we have to use _id too
				int bookId = booksCursor.getInt(booksCursor.getColumnIndex("_id"));
				Book book = application.dataManager.selectBook(bookId);
				if (book != null) {
					if (application.debugEnabled) {
						Log.d(Constants.LOG_TAG, "book selected - " + book.title);
					}
					application.lastMainListPosition = index;
					application.selectedBook = book;
					startActivity(new Intent(TagBatchList.this, BookDetail.class));
				} else {
					Toast.makeText(TagBatchList.this, getString(R.string.msgSelectBookError), Toast.LENGTH_SHORT).show();
				}
			}
		});
		
		defaultActionBar = (LinearLayout) findViewById(R.id.tagactionbar);
		reorderActionBar = (LinearLayout) findViewById(R.id.tagreorderactionbar);
		
		setupDialogs();
		setupActionBar();
		bindAdapters();
	}

	@Override
	protected void onPause() {
		CharSequence filterText; 

		// Preserve filter text
		filterText = bookListView.getTextFilter();
		if (filterText == null)
		{
			application.lastTagEditorFilter = null;   	  
		} else {
			application.lastTagEditorFilter = String.valueOf(filterText);
		}
		super.onPause();
	}

	@Override
	protected void onResume() {
		// Reapply previous filter
		if (application.lastTagEditorFilter != null) {
			bookListView.setFilterText(application.lastTagEditorFilter);
		}

		super.onResume();
	}
	
	@Override
	public boolean onKeyDown(final int keyCode, final KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK) && application.tagReorderingEnabled) {
			application.tagReorderingEnabled = false;			
			updateActionBar();
			updateFilter();
			
			return true;
		}
		
		// Use search key to toggle on-screen keyboard for filtering
		if (keyCode == KeyEvent.KEYCODE_SEARCH) {
			InputMethodManager  imeMgr = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
			imeMgr.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
			return true;
		}

		return super.onKeyDown(keyCode, event);
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
	            if (!sortDialogIsShowing) {
	                sortDialogIsShowing = true;
	                sortDialog.show();
	             }
			}
		});
		
		filterTagImage = (ImageView) findViewById(R.id.tagactionfilter);
		filterTagImage.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
	            if (!filterDialogIsShowing) {
	                filterDialogIsShowing = true;
	                filterDialog.show();
	            }
			}
		});

		reorderTagsImage = (ImageView) findViewById(R.id.tagactionreorder);
		reorderTagsImage.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				application.tagReorderingEnabled = !application.tagReorderingEnabled;	
				
				updateActionBar();
				updateFilter();
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
		
		// TODO: Make books list scroll to the selected book on move to keep it visible.
		moveUpImage = (ImageView) findViewById(R.id.tagactionmoveup);
		moveUpImage.setOnClickListener(new OnClickListener() {		
			public void onClick(View v) {
				if (application.tagReorderingEnabled && (selectedBookId != TagBatchList.NO_BOOK_SELECTED) &&
						(selectedBookPosition > 0)){
					int newSelectionPosition = selectedBookPosition - 1;
					Long newSelectedBookId = (Long) booksAdapter.getItemId(newSelectionPosition);;

					application.dataManager.swapBooksInTagTable(application.selectedTag.id, 
							selectedBookId, newSelectedBookId);

					selectedBookPosition = newSelectionPosition;
					booksCursor.requery();
					booksAdapter.notifyDataSetChanged();
				}
			}		
		});
		
		moveDownImage = (ImageView) findViewById(R.id.tagactionmovedown);
		moveDownImage.setOnClickListener(new OnClickListener() {		
			public void onClick(View v) {
				if (application.tagReorderingEnabled && (selectedBookId != TagBatchList.NO_BOOK_SELECTED) &&
						(selectedBookPosition < booksAdapter.getCount()-1)){
					int newSelectionPosition = selectedBookPosition + 1;
					Long newSelectedBookId = (Long) booksAdapter.getItemId(newSelectionPosition);;

					application.dataManager.swapBooksInTagTable(application.selectedTag.id, 
							selectedBookId, newSelectedBookId);

					selectedBookPosition = newSelectionPosition;
					booksCursor.requery();
					booksAdapter.notifyDataSetChanged();

				}
			}
		});
		
		updateActionBar();

	}

	/**
	 * Updates the enabled state of buttons, based on whether re-ordering mode is currently active
	 */
	private void updateActionBar() {
		defaultActionBar.setVisibility(application.tagReorderingEnabled ? View.GONE : View.VISIBLE);
		reorderActionBar.setVisibility(application.tagReorderingEnabled ? View.VISIBLE : View.GONE);
		
		setTitle(application.tagReorderingEnabled ? R.string.titleTagReorder : R.string.titleTagBatchList);
	}

	/**
	 * Updates filter results.
	 */
	private void updateFilter() {
		// Null check required due to null adapter when no books are present
		if (booksAdapter != null) {
			booksAdapter.runQuery(null);
			
			// Only needed when re-ordering is active or after it is disabled, but it's easier and more consistent to 
			// just do it every time 
			booksAdapter.changeCursor(booksCursor);
		}
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
			holder.name.setText("Select Tag");
			holder.name.setTextSize(20);
			// TODO: Re-evaluate this padding: a vertical padding of 15 would be closer to the standard list view, but
			//       makes the spinner button too large.
			holder.name.setPadding(10, 10, 10, 10);
			v.setTag(holder);
			populateView(v, c);
			return v;
		}

		private void populateView(final View v, final Cursor c) {
			// use ViewHolder pattern to avoid extra trips to findViewById
			TagViewHolder holder = (TagViewHolder) v.getTag();

			if ((c != null) && !c.isClosed()) {
				holder.name.setText(c.getString(1));

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
		TextView positionNum;
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
			String whereClause = null;
			String orderBy;
			String filter;
			
			if (application.tagReorderingEnabled) {
				orderBy = String.format(DataConstants.ORDER_BY_TAG_POSITION_ASC, application.selectedTag.id);
				whereClause = String.format(DataConstants.FILTER_BY_CURRENT_TAG, application.selectedTag.id);
			} else {
				orderBy = prefs.getString(Constants.TAG_SORT_ORDER, DataConstants.ORDER_BY_TITLE_ASC);
				
				if ((constraint != null) && (constraint.length() != 0)) {
					filter = prefs.getString(Constants.TAG_FILTER, DataConstants.FILTER_BY_TITLE);
					whereClause = String.format(filter, constraint);
				}					
			}
			
			if (application.debugEnabled) {
				Log.d(Constants.LOG_TAG, "filter text = '" + whereClause + "'");
			}
			
			c = application.dataManager.getBookCursor(orderBy, whereClause);
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
			holder.positionNum = (TextView) v.findViewById(R.id.tag_books_position);
			
			// Implement pseudo drag&drop editting by using click to select book and long click to drop it
			holder.coverImage.setOnLongClickListener(new OnLongClickListener() {
				// TODO: Instanciate listeners once for adapter and reuse them for each items as an optimisation
				
				@Override
				public boolean onLongClick(View v) {					
					Long newSelectedBookId;
					int newSelectionPosition;
					if (application.tagReorderingEnabled && selectedBookId != TagBatchList.NO_BOOK_SELECTED) {							
						newSelectionPosition = (Integer) v.getTag();
						newSelectedBookId = (Long) BookCursorAdapter.this.getItemId(newSelectionPosition);
						if (application.debugEnabled) {
							Log.d(Constants.LOG_TAG, "Moved book: " + String.valueOf(selectedBookId));
							Log.d(Constants.LOG_TAG, "to: " + String.valueOf(newSelectedBookId));
						}
						application.dataManager.swapBooksInTagTable(application.selectedTag.id, 
								 			selectedBookId, newSelectedBookId);
						
						selectedBookId = NO_BOOK_SELECTED;
						booksCursor.requery();
						BookCursorAdapter.this.notifyDataSetChanged();
					}
					
					return true;
				}
			});
			
			holder.coverImage.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Long newSelectedBookId;
					if (application.tagReorderingEnabled) {	
						selectedBookPosition = (Integer) v.getTag();
						newSelectedBookId = (Long) BookCursorAdapter.this.getItemId(selectedBookPosition);
						if (selectedBookId == newSelectedBookId) {
							selectedBookId = NO_BOOK_SELECTED;
						} else {
						  selectedBookId = newSelectedBookId;							
						}
						
						// Notify the adapter so that it can highlight the select book
						BookCursorAdapter.this.notifyDataSetChanged();
					}
					if (application.debugEnabled) {
						Log.d(Constants.LOG_TAG, "Selected Id: " + String.valueOf(selectedBookId));
						Log.d(Constants.LOG_TAG, "Selected item id " + String.valueOf(BookCursorAdapter.this.getItemId((Integer) v.getTag()) ));
					}
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
				long bookId = c.getLong(0);

				// TODO investigate, may need to file Android/SQLite bug
				// Log.i(Constants.LOG_TAG, "COLUMN INDEX rating - " +
				// c.getColumnIndex(DataConstants.RATING));
				// as soon as query has group by or group_concat the
				// getColumnIndex fails? (explicit works)
				/*
				 * bid = 0 tit = 1 subtit = 2 subject = 3 pub = 4 datepub = 5
				 * format = 6 rstat = 7 rat = 8 blurb = 9 authors = 10
				 */

				boolean isTagged = false;
				if (application.selectedTag != null) {
					isTagged = application.dataManager.isTagged(application.selectedTag.id, bookId);
				}
				String title = c.getString(1);
				String authors = c.getString(10);

				Bitmap coverImageBitmap = application.imageManager.retrieveBitmap(title, bookId, true);
				if (coverImageBitmap != null) {
					holder.coverImage.setImageBitmap(coverImageBitmap);
				} else {
					holder.coverImage.setImageBitmap(coverImageMissing);
				}
				holder.coverImage.setTag(new Integer(c.getPosition()));
				// High-light book selected for re-ordering
				if (selectedBookId == bookId) { 
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

				holder.checkedTag.setChecked(isTagged);
				// TODO: Consider creating view tag Long object when holder is instanciated and just assigning a value
				//       here as a performance optimisation.
				holder.checkedTag.setTag(new Long(bookId));
				holder.positionNum.setText(String.valueOf(c.getPosition()+1));
				
				// Display either checkbox or position number based on the re-ordering enabled state
				if (application.tagReorderingEnabled) {
					holder.checkedTag.setVisibility(View.INVISIBLE);
					holder.positionNum.setVisibility(View.VISIBLE);
				} else {
					holder.checkedTag.setVisibility(View.VISIBLE);
					holder.positionNum.setVisibility(View.INVISIBLE);					
				}
			}
		}
	}  
	
	
    /**
     *  Callback listener that implements the
     *  {@link android.widget.AdapterView.OnItemSelectedListener} interface for the group selection spinner
     */
    public class TagOnItemSelectedListener implements OnItemSelectedListener {

        /**
         * Callback triggered when the user selects an item in the tags spinner.
         *
         * @see android.widget.AdapterView.OnItemSelectedListener#onItemSelected(
         *  android.widget.AdapterView, android.view.View, int, long)
         *  
         * @param parent  The AdapterView where the selection occured
         * @param view    The view within the AdapterView that was clicked
         * @param pos     The position of the view in the adapter
         * @param id      The row id of the item that is selected 
         */
    	@Override
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            Cursor cursor = (Cursor) parent.getItemAtPosition(pos);
            long tagId = cursor.getLong(0);
            application.selectedTag = application.dataManager.selectTag(tagId);
            
            // Null check required due to null adapter when no books are present
            if (booksAdapter != null) {
            	booksAdapter.notifyDataSetChanged();
            }
            if (application.tagReorderingEnabled) {
            	updateFilter();
            }
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
    		.addEntry(getString(R.string.labelSubject),DataConstants.ORDER_BY_SUBJECT_ASC)
    		.addEntry(getString(R.string.labelDatepub), DataConstants.ORDER_BY_DATE_PUB_DESC)
    		.addEntry(getString(R.string.labelPublisher), DataConstants.ORDER_BY_PUB_ASC)
    		.setOnClickListener(new DialogInterface.OnClickListener() {		
				@Override
				public void onClick(DialogInterface dialog, int which) {
					sortDialogIsShowing = false;
					// Null check required due to null adapter when no books are present
					if (booksAdapter != null) {
						booksAdapter.runQuery(null);
						// Must call changeCursor for sort since it isn't called automatically as it is for filter 
						booksAdapter.changeCursor(booksCursor);
					}
				}
			});
    	
    	sortDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
    		public void onCancel(DialogInterface d) {
    			sortDialogIsShowing = false;
    		}
    	});

    	sortDialog.create();
    	  	
    	filterDialog = new PrefListDialogBuilder(this, prefs, Constants.TAG_FILTER);
    	filterDialog.setTitle(getString(R.string.btnFilterBy))
			.addEntry(getString(R.string.labelTag), DataConstants.FILTER_BY_TAG)
			.addEntry(getString(R.string.labelTitle), DataConstants.FILTER_BY_TITLE)
			.addEntry(getString(R.string.labelAuthorsShort), DataConstants.FILTER_BY_AUTHOR)
			.addEntry(getString(R.string.labelSubject),DataConstants.FILTER_BY_SUBJECT)
			.addEntry(getString(R.string.labelPublisher), DataConstants.FILTER_BY_PUBLISHER)
			.addEntry(getString(R.string.labelRating), DataConstants.FILTER_BY_RATING)
			.setOnClickListener(new DialogInterface.OnClickListener() {		
				@Override
				public void onClick(DialogInterface dialog, int which) {
					filterDialogIsShowing = false;
					updateFilter();
				}
			});
    	
    	filterDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
    		public void onCancel(DialogInterface d) {
    			filterDialogIsShowing = false;
    		}
    	});

    	filterDialog.create();
    }

	
}

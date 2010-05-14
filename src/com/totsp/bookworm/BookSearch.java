package com.totsp.bookworm;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.totsp.bookworm.data.GoogleBookDataSource;
import com.totsp.bookworm.model.Book;

import java.net.URLEncoder;
import java.util.ArrayList;

public class BookSearch extends Activity {

   public static final String FROM_SEARCH = "FROM_SEARCH";

   BookWormApplication application;

   private EditText searchInput;
   private Button searchButton;
   private ListView searchResults;
   TextView getMoreData;
   ArrayList<Book> parsedBooks;
   int selectorPosition = 0;

   private ArrayAdapter<Book> adapter;

   private SearchTask searchTask;

   private boolean footerViewShown;
   private boolean fromEntryResult;

   @Override
   public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.booksearch);
      application = (BookWormApplication) getApplication();

      searchTask = null;
      parsedBooks = null;
      adapter = null;

      searchInput = (EditText) findViewById(R.id.bookentrysearchinput);
      searchButton = (Button) findViewById(R.id.bookentrysearchbutton);

      searchResults = (ListView) findViewById(R.id.bookentrysearchresultlist);
      searchResults.setTextFilterEnabled(true);
      searchResults.setOnItemClickListener(new OnItemClickListener() {
         public void onItemClick(final AdapterView<?> parent, final View v,
                  final int index, final long id) {
            Book selected = BookSearch.this.parsedBooks.get(index);
            BookSearch.this.selectorPosition = index - 1;
            Intent intent = new Intent(BookSearch.this, BookEntryResult.class);
            // favor isbn 10, but use 13 if 10 missing
            if ((selected.isbn10 != null) && !selected.isbn10.equals("")) {
               intent.putExtra(Constants.ISBN, selected.isbn10);
            } else if ((selected.isbn13 != null) && !selected.isbn13.equals("")) {
               intent.putExtra(Constants.ISBN, selected.isbn13);
            }
            intent.putExtra(BookSearch.FROM_SEARCH, true);
            BookSearch.this.startActivity(intent);
         }
      });

      searchButton.setOnClickListener(new OnClickListener() {
         public void onClick(final View v) {
            BookSearch.this.parsedBooks = new ArrayList<Book>();
            BookSearch.this.searchTask = new SearchTask();
            BookSearch.this.searchTask.execute(BookSearch.this.searchInput
                     .getText().toString(), "1");
         }
      });

      LayoutInflater li = getLayoutInflater();
      getMoreData =
               (TextView) li.inflate(R.layout.search_listview_footer, null);
      getMoreData.setOnClickListener(new OnClickListener() {
         public void onClick(final View v) {
            int startIndex = BookSearch.this.parsedBooks.size() + 1;
            new SearchTask().execute(BookSearch.this.searchInput.getText()
                     .toString(), String.valueOf(startIndex));
            v.setBackgroundResource(R.color.red1);
         }
      });

      // if returning to search from search result reload prev search data
      fromEntryResult =
               getIntent().getBooleanExtra(BookEntryResult.FROM_RESULT, false);
      if (fromEntryResult) {
         restoreFromCache();
      }

      // if search data exists after an orientation change, reload it
      Object lastNonConfig = getLastNonConfigurationInstance();
      if ((lastNonConfig != null) && (lastNonConfig instanceof Boolean)) {
         restoreFromCache();
      }
   }

   @Override
   public Object onRetainNonConfigurationInstance() {
      // never pass a View/Drawable/Adapter etc here or will leak Activity
      return true;
   }

   @Override
   public void onPause() {
      if ((searchTask != null) && searchTask.dialog.isShowing()) {
         searchTask.dialog.dismiss();
      }
      if (parsedBooks != null) {
         application.bookCacheList = parsedBooks;
      }
      if (searchInput != null) {
         application.lastSearchTerm = searchInput.getText().toString();
      }
      if (selectorPosition > 0) {
         application.lastSearchListPosition = selectorPosition;
      }
      super.onPause();
   }

   // go back to Main on back from here
   @Override
   public boolean onKeyDown(final int keyCode, final KeyEvent event) {
      if ((keyCode == KeyEvent.KEYCODE_BACK) && (event.getRepeatCount() == 0)) {
         startActivity(new Intent(BookSearch.this, Main.class));
         return true;
      }
      return super.onKeyDown(keyCode, event);
   }

   private void bindAdapter() {
      // add footer view BEFORE setting adapter
      if (!parsedBooks.isEmpty() && !footerViewShown) {
         searchResults.addFooterView(getMoreData);
         footerViewShown = true;
      }

      adapter =
               new ArrayAdapter<Book>(BookSearch.this,
                        R.layout.simple_list_item_1,
                        BookSearch.this.parsedBooks);
      searchResults.setAdapter(adapter);
      if (selectorPosition > 2) {
         searchResults.setSelection(selectorPosition - 1);
      }
   }

   private void restoreFromCache() {
      // use application object as quick/dirty cache for state      
      if (application.bookCacheList != null) {
         selectorPosition = application.lastSearchListPosition;
         parsedBooks = application.bookCacheList;
         bindAdapter();
      }
      if (application.lastSearchTerm != null) {
         searchInput.setText(application.lastSearchTerm);
      }
   }

   //
   // AsyncTasks
   //
   private class SearchTask extends AsyncTask<String, Void, ArrayList<Book>> {
      private final ProgressDialog dialog = new ProgressDialog(BookSearch.this);

      // TODO hard coded to GoolgeBookDataSource for now
      private final GoogleBookDataSource gbs = new GoogleBookDataSource();

      public SearchTask() {
         gbs.setDebugEnabled(BookSearch.this.application.debugEnabled);
      }

      @Override
      protected void onPreExecute() {
         dialog.setMessage(BookSearch.this.getString(R.string.msgSearching));
         dialog.show();
      }

      @Override
      protected ArrayList<Book> doInBackground(final String... args) {
         String searchTerm = args[0];
         int startIndex = Integer.valueOf(args[1]);
         if (searchTerm != null) {
            searchTerm = URLEncoder.encode(searchTerm);
            return gbs.getBooks(searchTerm, startIndex);
         }
         return null;
      }

      @Override
      protected void onPostExecute(final ArrayList<Book> books) {
         if (dialog.isShowing()) {
            dialog.dismiss();
         }

         if ((books != null) && !books.isEmpty()) {
            BookSearch.this.selectorPosition =
                     BookSearch.this.parsedBooks.size();
            for (Book b : books) {
               if (!BookSearch.this.parsedBooks.contains(b)) {
                  BookSearch.this.parsedBooks.add(b);
               }
            }

            BookSearch.this.bindAdapter();
            BookSearch.this.getMoreData
                     .setBackgroundResource(android.R.color.transparent);
         }
      }
   }
}
package com.totsp.bookworm;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.totsp.bookworm.data.GoogleBookDataSource;
import com.totsp.bookworm.model.Book;
import com.totsp.bookworm.util.StringUtil;

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
      // if user hits "enter" on keyboard, go ahead and submit, no need for newlines in the search box
      searchInput.setOnKeyListener(new OnKeyListener() {
         public boolean onKey(View v, int keyCode, KeyEvent event) {
            if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
               BookSearch.this.parsedBooks = new ArrayList<Book>();
               BookSearch.this.searchTask = new SearchTask();
               BookSearch.this.searchTask.execute(BookSearch.this.searchInput.getText().toString(), "1");
               return true;
            }
            return false;
         }
      });

      searchButton = (Button) findViewById(R.id.bookentrysearchbutton);

      searchResults = (ListView) findViewById(R.id.bookentrysearchresultlist);
      searchResults.setTextFilterEnabled(true);
      searchResults.setOnItemClickListener(new OnItemClickListener() {
         public void onItemClick(final AdapterView<?> parent, final View v, final int index, final long id) {
            // don't redo the search, you have the BOOK itself (don't pass the ISBN, use the Book)
            application.selectedBook = BookSearch.this.parsedBooks.get(index);
            ;
            BookSearch.this.selectorPosition = index - 1;
            Intent intent = new Intent(BookSearch.this, BookEntryResult.class);
            intent.putExtra(BookSearch.FROM_SEARCH, true);
            BookSearch.this.startActivity(intent);
         }
      });

      searchButton.setOnClickListener(new OnClickListener() {
         public void onClick(final View v) {
            BookSearch.this.parsedBooks = new ArrayList<Book>();
            BookSearch.this.searchTask = new SearchTask();
            BookSearch.this.searchTask.execute(BookSearch.this.searchInput.getText().toString(), "1");
         }
      });

      LayoutInflater li = getLayoutInflater();
      getMoreData = (TextView) li.inflate(R.layout.search_listview_footer, null);
      getMoreData.setOnClickListener(new OnClickListener() {
         public void onClick(final View v) {
            int startIndex = BookSearch.this.parsedBooks.size() + 1;
            new SearchTask().execute(BookSearch.this.searchInput.getText().toString(), String.valueOf(startIndex));
            v.setBackgroundResource(R.color.red1);
         }
      });

      // if returning to search from search result reload prev search data
      fromEntryResult = getIntent().getBooleanExtra(BookEntryResult.FROM_RESULT, false);
      if (fromEntryResult) {
         restoreFromCache();
      }

      // if search data exists after an orientation change, reload it
      Object lastNonConfig = getLastNonConfigurationInstance();
      if ((lastNonConfig != null) && (lastNonConfig instanceof Boolean)) {
         restoreFromCache();
      }

      // do not enable the soft keyboard unless user explicitly selects textedit
      // Android seems to have an IMM bug concerning this on devices with only soft keyboard
      // http://code.google.com/p/android/issues/detail?id=7115
      getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
   }

   @Override
   public void onStart() {
      super.onStart();
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

      // because we add and remove footer view dynamically
      // and because list set here is small, we just rebind the list
      // rather than calling notifyDataSetChanged, etc.

      // add footer view BEFORE setting adapter
      if (!parsedBooks.isEmpty() && !footerViewShown) {
         searchResults.addFooterView(getMoreData);
         footerViewShown = true;
      }

      adapter = new BookSearchAdapter(BookSearch.this.parsedBooks);

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

   // static and package access as an Android optimization (used in inner class)
   static class ViewHolder {
      TextView text1;
      TextView text2;
   }

   // BookSearchAdapter
   private class BookSearchAdapter extends ArrayAdapter<Book> {
      private ArrayList<Book> books;

      LayoutInflater vi = (LayoutInflater) BookSearch.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

      BookSearchAdapter(ArrayList<Book> books) {
         super(BookSearch.this, R.layout.search_list_item, books);
         this.books = books;
      }

      @Override
      public View getView(int position, View convertView, ViewGroup parent) {

         View item = convertView;
         ViewHolder holder = null;

         if (item == null) {
            item = vi.inflate(R.layout.search_list_item, parent, false);
            // use ViewHolder pattern to avoid extra trips to findViewById         
            holder = new ViewHolder();
            holder.text1 = (TextView) item.findViewById(R.id.search_item_text_1);
            holder.text2 = (TextView) item.findViewById(R.id.search_item_text_2);
            item.setTag(holder);
         }

         holder = (ViewHolder) item.getTag();
         holder.text1.setText(books.get(position).title);
         holder.text2.setText(StringUtil.contractAuthors(books.get(position).authors));
         return item;
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
            BookSearch.this.selectorPosition = BookSearch.this.parsedBooks.size();
            for (int i = 0; i < books.size(); i++) {
               Book b = books.get(i);
               if (!BookSearch.this.parsedBooks.contains(b)) {
                  BookSearch.this.parsedBooks.add(b);
               }
            }

            BookSearch.this.bindAdapter();
            BookSearch.this.getMoreData.setBackgroundResource(android.R.color.transparent);
         }
      }
   }
}
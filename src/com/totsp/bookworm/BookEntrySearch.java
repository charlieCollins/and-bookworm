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

public class BookEntrySearch extends Activity {

   public static final String FROM_SEARCH = "FROM_SEARCH";

   private BookWormApplication application;

   private int startIndex = 1;

   private EditText searchInput;
   private Button searchButton;
   private ListView searchResults;
   private TextView getMoreData;

   private ArrayList<Book> parsedBooks;
   private ArrayAdapter<Book> adapter;

   private SearchTask searchTask;

   private boolean footerViewShown;
   private boolean fromEntryResult;

   @Override
   public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      this.setContentView(R.layout.bookentrysearch);
      this.application = (BookWormApplication) this.getApplication();

      this.searchTask = null;
      this.parsedBooks = null;
      this.adapter = null;

      this.searchInput = (EditText) this.findViewById(R.id.bookentrysearchinput);
      this.searchButton = (Button) this.findViewById(R.id.bookentrysearchbutton);

      this.searchResults = (ListView) this.findViewById(R.id.bookentrysearchresultlist);
      this.searchResults.setTextFilterEnabled(true);
      this.searchResults.setOnItemClickListener(new OnItemClickListener() {
         public void onItemClick(final AdapterView<?> parent, final View v, final int index, final long id) {
            Book selected = BookEntrySearch.this.parsedBooks.get(index);
            Intent intent = new Intent(BookEntrySearch.this, BookEntryResult.class);
            // favor isbn 10, but use 13 if 10 missing
            if ((selected.isbn10 != null) && !selected.isbn10.equals("")) {
               intent.putExtra(Constants.ISBN, selected.isbn10);
            } else if ((selected.isbn13 != null) && !selected.isbn13.equals("")) {
               intent.putExtra(Constants.ISBN, selected.isbn13);
            }
            intent.putExtra(BookEntrySearch.FROM_SEARCH, true);
            BookEntrySearch.this.startActivity(intent);
         }
      });

      this.searchButton.setOnClickListener(new OnClickListener() {
         public void onClick(final View v) {
            BookEntrySearch.this.parsedBooks = new ArrayList<Book>();
            BookEntrySearch.this.searchTask = new SearchTask();
            BookEntrySearch.this.searchTask.execute(BookEntrySearch.this.searchInput.getText().toString(), "1");
         }
      });

      LayoutInflater li = this.getLayoutInflater();
      this.getMoreData = (TextView) li.inflate(R.layout.search_listview_footer, null);
      this.getMoreData.setOnClickListener(new OnClickListener() {
         public void onClick(final View v) {
            BookEntrySearch.this.startIndex += 15;
            new SearchTask().execute(BookEntrySearch.this.searchInput.getText().toString(), String
                     .valueOf(BookEntrySearch.this.startIndex));
            ///BookEntrySearch.this.searchResults.setSelection(BookEntrySearch.this.startIndex  - 15);
            v.setBackgroundResource(R.color.red1);
         }
      });

      this.fromEntryResult = this.getIntent().getBooleanExtra(BookEntryResult.FROM_RESULT, false);
      if (this.fromEntryResult) {
         this.restoreFromCache();
      }
   }

   @Override
   public void onPause() {
      if ((this.searchTask != null) && this.searchTask.dialog.isShowing()) {
         this.searchTask.dialog.dismiss();
      }
      if (this.parsedBooks != null) {
         this.application.setBookCacheList(this.parsedBooks);
      }
      if (this.searchInput != null) {
         this.application.setLastSearchTerm(this.searchInput.getText().toString());
      }
      super.onPause();
   }

   // go back to Main on back from here
   @Override
   public boolean onKeyDown(final int keyCode, final KeyEvent event) {
      if ((keyCode == KeyEvent.KEYCODE_BACK) && (event.getRepeatCount() == 0)) {
         this.startActivity(new Intent(BookEntrySearch.this, Main.class));
         return true;
      }
      return super.onKeyDown(keyCode, event);
   }

   private void bindAdapter() {
      // add footer view BEFORE setting adapter
      if (!this.parsedBooks.isEmpty() && !this.footerViewShown) {
         this.searchResults.addFooterView(this.getMoreData);
         this.footerViewShown = true;
      }

      this.adapter =
               new ArrayAdapter<Book>(BookEntrySearch.this, R.layout.simple_list_item_1,
                        BookEntrySearch.this.parsedBooks);
      this.searchResults.setAdapter(this.adapter);
   }

   private void restoreFromCache() {
      // use application object as quick/dirty cache for state      
      if (this.application.getBookCacheList() != null) {
         this.parsedBooks = this.application.getBookCacheList();
         this.bindAdapter();
      }
      if (this.application.getLastSearchTerm() != null) {
         this.searchInput.setText(this.application.getLastSearchTerm());
      }
   }

   //
   // AsyncTasks
   //
   private class SearchTask extends AsyncTask<String, Void, ArrayList<Book>> {
      private final ProgressDialog dialog = new ProgressDialog(BookEntrySearch.this);

      private final GoogleBookDataSource gbs = new GoogleBookDataSource();

      protected void onPreExecute() {
         this.dialog.setMessage("Searching...");
         this.dialog.show();
      }

      protected ArrayList<Book> doInBackground(final String... args) {
         String searchTerm = args[0];
         int startIndex = Integer.valueOf(args[1]);
         if (searchTerm != null) {
            searchTerm = URLEncoder.encode(searchTerm);
            return this.gbs.getBooks(searchTerm, startIndex);
         }
         return null;
      }

      protected void onPostExecute(final ArrayList<Book> books) {
         if (this.dialog.isShowing()) {
            this.dialog.dismiss();
         }

         if ((books != null) && !books.isEmpty()) {
            for (Book b : books) {
               if (((b.isbn10 != null) && !b.isbn10.equals("")) || ((b.isbn13 != null) && !b.isbn13.equals(""))) {
                  BookEntrySearch.this.parsedBooks.add(b);
               }
            }

            BookEntrySearch.this.bindAdapter();
            BookEntrySearch.this.getMoreData.setBackgroundResource(android.R.color.transparent);
         }
      }
   }
}
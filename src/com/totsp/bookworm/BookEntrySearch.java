package com.totsp.bookworm;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

import com.totsp.bookworm.data.GoogleBookDataSource;
import com.totsp.bookworm.model.Book;

import java.util.ArrayList;

public class BookEntrySearch extends Activity {

   ///private BookWormApplication application;

   private EditText searchInput;
   private Button searchButton;
   private ListView searchResults;

   @Override
   public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      ///this.application = (BookWormApplication) this.getApplication();

      this.setContentView(R.layout.bookentrysearch);

      this.searchInput = (EditText) this.findViewById(R.id.bookentrysearchinput);
      this.searchButton = (Button) this.findViewById(R.id.bookentrysearchbutton);

      this.searchResults = (ListView) this.findViewById(R.id.bookentrysearchresultlist);

      this.searchButton.setOnClickListener(new OnClickListener() {
         public void onClick(final View v) {
            new SearchTask().execute(BookEntrySearch.this.searchInput.getText().toString());
         }
      });
   }

   @Override
   protected void onRestoreInstanceState(final Bundle savedInstanceState) {
      super.onRestoreInstanceState(savedInstanceState);
   }

   @Override
   protected void onSaveInstanceState(final Bundle saveState) {
      super.onSaveInstanceState(saveState);
   }

   private class SearchTask extends AsyncTask<String, Void, Void> {
      private final ProgressDialog dialog = new ProgressDialog(BookEntrySearch.this);

      private ArrayList<Book> books = new ArrayList<Book>();

      private final GoogleBookDataSource gbs = new GoogleBookDataSource();

      // can use UI thread here
      protected void onPreExecute() {
         this.dialog.setMessage("Searching...");
         this.dialog.show();
      }

      // automatically done on worker thread (separate from UI thread)
      protected Void doInBackground(final String... args) {
         String searchTerm = args[0];
         this.books = this.gbs.getBooks(searchTerm);
         return null;
      }

      // can use UI thread here
      protected void onPostExecute(final Void unused) {
         if (this.dialog.isShowing()) {
            this.dialog.dismiss();
         }

         if ((this.books != null) && !this.books.isEmpty()) {
            Log.d(Constants.LOG_TAG, "book list size - " + this.books.size());
            ArrayAdapter<Book> adapter =
                     new ArrayAdapter<Book>(BookEntrySearch.this, android.R.layout.simple_list_item_1, this.books);

            BookEntrySearch.this.searchResults.setAdapter(adapter);
            BookEntrySearch.this.searchResults.setTextFilterEnabled(true);
            BookEntrySearch.this.searchResults.setOnItemClickListener(new OnItemClickListener() {
               public void onItemClick(final AdapterView<?> parent, final View v, final int index, final long id) {
                  Book selected = SearchTask.this.books.get(index);
                  Intent scanIntent = new Intent(BookEntrySearch.this, BookEntryResult.class);
                  scanIntent.putExtra(Constants.ISBN, selected.getIsbn());
                  BookEntrySearch.this.startActivity(scanIntent);
               }
            });
         }
      }
   }
}
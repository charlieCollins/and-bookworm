package com.totsp.bookworm;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
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

import java.net.URLEncoder;
import java.util.ArrayList;

public class BookEntrySearch extends Activity {

   private static final int MENU_NEXT_PAGE = 0;
   private static final int MENU_PREV_PAGE = 1;
   private int startIndex = 1;

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
            new SearchTask().execute(BookEntrySearch.this.searchInput.getText().toString(), "1");
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

   @Override
   public boolean onCreateOptionsMenu(final Menu menu) {
      menu.add(0, MENU_PREV_PAGE, 0, "Prev page of results").setIcon(android.R.drawable.ic_menu_search);
      menu.add(0, MENU_NEXT_PAGE, 1, "Next page of results").setIcon(android.R.drawable.ic_menu_search);
      return super.onCreateOptionsMenu(menu);
   }

   @Override
   public boolean onOptionsItemSelected(final MenuItem item) {
      switch (item.getItemId()) {
      case MENU_NEXT_PAGE:
         this.startIndex += 20;
         new SearchTask().execute(BookEntrySearch.this.searchInput.getText().toString(), String
                  .valueOf(BookEntrySearch.this.startIndex));
         return true;
      case MENU_PREV_PAGE:
         if (this.startIndex > 1) {
            this.startIndex -= 20;
            new SearchTask().execute(BookEntrySearch.this.searchInput.getText().toString(), String
                     .valueOf(BookEntrySearch.this.startIndex));
         }
         return true;
      default:
         return super.onOptionsItemSelected(item);
      }
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
         int startIndex = Integer.valueOf(args[1]);
         if (searchTerm != null) {
            searchTerm = URLEncoder.encode(searchTerm);
            this.books = this.gbs.getBooks(searchTerm, startIndex);
         }
         return null;
      }

      // can use UI thread here
      protected void onPostExecute(final Void unused) {
         if (this.dialog.isShowing()) {
            this.dialog.dismiss();
         }

         final ArrayList<Book> parsedBooks = new ArrayList<Book>();
         if ((this.books != null) && !this.books.isEmpty()) {
            // try to strip out results that don't have ISBNs
            for (Book b : this.books) {
               if ((b.getIsbn10() != null && !b.getIsbn10().equals(""))
                        || (b.getIsbn13() != null && !b.getIsbn13().equals(""))) {
                  parsedBooks.add(b);
               }
            }

            ArrayAdapter<Book> adapter =
                     new ArrayAdapter<Book>(BookEntrySearch.this, android.R.layout.simple_list_item_1, parsedBooks);

            BookEntrySearch.this.searchResults.setAdapter(adapter);
            BookEntrySearch.this.searchResults.setTextFilterEnabled(true);
            BookEntrySearch.this.searchResults.setOnItemClickListener(new OnItemClickListener() {
               public void onItemClick(final AdapterView<?> parent, final View v, final int index, final long id) {
                  Book selected = parsedBooks.get(index);
                  Intent scanIntent = new Intent(BookEntrySearch.this, BookEntryResult.class);
                  // favor isbn 10, but use 13 if 10 missing
                  if (selected.getIsbn10() != null && !selected.getIsbn10().equals("")) {
                     scanIntent.putExtra(Constants.ISBN, selected.getIsbn10());
                  } else if (selected.getIsbn13() != null && !selected.getIsbn13().equals("")) {
                     scanIntent.putExtra(Constants.ISBN, selected.getIsbn10());
                  }
                  BookEntrySearch.this.startActivity(scanIntent);
               }
            });
         }
      }
   }
}
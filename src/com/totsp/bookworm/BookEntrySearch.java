package com.totsp.bookworm;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.totsp.bookworm.data.GoogleBookSearch;
import com.totsp.bookworm.model.Book;

import java.util.HashSet;

public class BookEntrySearch extends Activity {

   private BookWormApplication application;

   private EditText searchInput;
   private Button searchButton;

   private TextView searchOutput;

   ///private ListView searchResultsListView;

   @Override
   public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      this.application = (BookWormApplication) this.getApplication();

      this.setContentView(R.layout.bookentrysearch);

      this.searchInput = (EditText) this.findViewById(R.id.bookentrysearchinput);
      this.searchButton = (Button) this.findViewById(R.id.bookentrysearchbutton);

      this.searchButton.setOnClickListener(new OnClickListener() {
         public void onClick(final View v) {
            new SearchTask().execute(BookEntrySearch.this.searchInput.getText().toString());
         }
      });

      this.searchOutput = (TextView) this.findViewById(R.id.bookentrysearchoutputtemp);
   }

   @Override
   public void onStart() {
      super.onStart();
   }

   @Override
   public void onPause() {
      super.onPause();
   }

   @Override
   protected void onStop() {
      super.onStop();
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
      ///menu.add(0, MENU_HELP, 0, "Help").setIcon(android.R.drawable.ic_menu_help);
      return super.onCreateOptionsMenu(menu);
   }

   @Override
   public boolean onOptionsItemSelected(final MenuItem item) {
      /*
      switch (item.getItemId()) {
      case MENU_HELP:
         this.startActivity(new Intent(Main.this, Help.class));
         return true;
      }
      */
      return super.onOptionsItemSelected(item);
   }

   private class SearchTask extends AsyncTask<String, Void, Void> {
      private final ProgressDialog dialog = new ProgressDialog(BookEntrySearch.this);

      private HashSet<Book> books;

      private final GoogleBookSearch gbs = new GoogleBookSearch();

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
            StringBuilder sb = new StringBuilder();
            for (Book b : this.books) {
               sb.append(b.toString() + "\n");
            }
            BookEntrySearch.this.searchOutput.setText(sb.toString());
         } else {
            BookEntrySearch.this.searchOutput.setText("no books found");
         }
      }
   }
}
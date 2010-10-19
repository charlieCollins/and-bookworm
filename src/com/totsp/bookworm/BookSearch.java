package com.totsp.bookworm;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;

import com.totsp.bookworm.model.Book;
import com.totsp.bookworm.util.BookUtil;
import com.totsp.bookworm.util.StringUtil;

import java.net.URLEncoder;
import java.util.ArrayList;

public class BookSearch extends Activity {

   public static final String FROM_SEARCH = "FROM_SEARCH";

   BookWormApplication application;

   EditText searchInput;
   Button searchButton;
   ListView searchResults;

   int selectorPosition;
   int searchPosition;

   String currSearchTerm = "";
   String prevSearchTerm = "";

   BookListAdapter adapter;

   boolean allowSearchContinue;
   boolean prevSearchResultCount;
   
   private ProgressDialog progressDialog;

   @Override
   public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      setContentView(R.layout.booksearch);
      application = (BookWormApplication) getApplication();
      
      progressDialog = new ProgressDialog(this);
      progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
      progressDialog.setCancelable(false);

      adapter = new BookListAdapter(this, new ArrayList<Book>());

      searchInput = (EditText) findViewById(R.id.bookentrysearchinput);
      // if user hits "enter" on keyboard, go ahead and submit, no need for newlines in the search box
      searchInput.setOnKeyListener(new OnKeyListener() {
         public boolean onKey(View v, int keyCode, KeyEvent event) {
            if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
               // if the "enter" key is pressed start over (diff from "get more results")
               String searchTerm = searchInput.getText().toString();
               if ((searchTerm != null) && !searchTerm.equals("")) {
                  newSearch(searchTerm);
               }
               return true;
            }
            return false;
         }
      });

      searchButton = (Button) findViewById(R.id.bookentrysearchbutton);
      searchButton.setOnClickListener(new OnClickListener() {
         public void onClick(final View v) {
            // if the "search" button is pressed start over (diff from "get more results")
            String searchTerm = searchInput.getText().toString();
            if ((searchTerm != null) && !searchTerm.equals("")) {
               newSearch(searchTerm);
            }
         }
      });

      searchResults = (ListView) findViewById(R.id.bookentrysearchresultlist);
      searchResults.setTextFilterEnabled(true);
      searchResults.setOnItemClickListener(new OnItemClickListener() {
         public void onItemClick(final AdapterView<?> parent, final View v, final int index, final long id) {
            // don't redo the search, you have the BOOK itself (don't pass the ISBN, use the Book)
            application.selectedBook = adapter.getItem(index);
            selectorPosition = index;
            Intent intent = new Intent(BookSearch.this, BookEntryResult.class);
            intent.putExtra(BookSearch.FROM_SEARCH, true);
            startActivity(intent);
         }
      });
      searchResults.setOnScrollListener(new OnScrollListener() {
         public void onScroll(AbsListView v, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            String searchTerm = searchInput.getText().toString();
            if (totalItemCount > 0 && (firstVisibleItem + visibleItemCount == totalItemCount)
                     && (searchTerm != null && !searchTerm.equals("")) && allowSearchContinue) {
               allowSearchContinue = false;
               selectorPosition = totalItemCount - 5;
               if (application.debugEnabled) {
                  Log.d(Constants.LOG_TAG, "search for term " + searchTerm + " starting at position " + searchPosition);
               }
               new SearchTask().execute(searchTerm, String.valueOf(searchPosition));
            }
         }

         public void onScrollStateChanged(AbsListView v, int scrollState) {
         }
      });
      searchResults.setAdapter(adapter);

      // do not enable the soft keyboard unless user explicitly selects textedit
      // Android seems to have an IMM bug concerning this on devices with only soft keyboard
      // http://code.google.com/p/android/issues/detail?id=7115
      getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

      // if coming from the search entry result page, try to re-establish prev adapter contents and positions
      if (getIntent().getBooleanExtra(BookEntryResult.FROM_RESULT, false)) {
         restoreFromStateBean(application.bookSearchStateBean);
      }
   }

   private void newSearch(final String searchTerm) {
      searchPosition = 0;
      selectorPosition = 0;
      prevSearchTerm = "";
      adapter.clear();
      adapter.notifyDataSetChanged();
      if (application.debugEnabled) {
         Log.i(Constants.LOG_TAG, "new search for term " + searchTerm + " starting at pos 0");
      }
      new SearchTask().execute(searchTerm, "0");
   }

   @Override
   public void onStart() {
      super.onStart();
   }

   @Override
   public void onPause() {
      if (progressDialog.isShowing()) {
         progressDialog.dismiss();
      }
      application.bookSearchStateBean = createStateBean();
      super.onPause();
   }

   @Override
   public void onSaveInstanceState(Bundle outState) {
      super.onSaveInstanceState(outState);
   }

   @Override
   public void onRestoreInstanceState(Bundle inState) {
      super.onRestoreInstanceState(inState);
      restoreFromStateBean((BookSearchStateBean) getLastNonConfigurationInstance());
   }

   @Override
   public Object onRetainNonConfigurationInstance() {
      return createStateBean();
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

   // use application object as quick/dirty cache for state
   // onRetainNonConfigurationInstance uses this to quickly save state on config changes
   // onPause uses this to save state longer term, when user "adds" book and then comes BACK to search   
   // NOTE document this pattern 
   // onRetainNonConfigurationInstance, onSaveInstanceState/onRestoreInstanceState, createStateBean/restoreFromStateBean

   // restore from state bean (called from onRestoreInstanceState (using lastNonConfigurationInstance) and from onCreate (using application))
   private void restoreFromStateBean(BookSearchStateBean bean) {
      if (bean != null) {
         selectorPosition = bean.lastSelectorPosition;
         searchPosition = bean.lastSearchPosition;
         currSearchTerm = bean.lastSearchTerm;
         searchInput.setText(currSearchTerm);

         if (bean.books != null && !bean.books.isEmpty()) {
            for (Book b : bean.books) {
               boolean dupe = false;
               // this is very inefficient, but should be relatively small collections here, and must prevent dupes
               for (int j = 0; j < adapter.getCount(); j++) {
                  Book ab = adapter.getItem(j);
                  if (BookUtil.areBooksEffectiveDupes(ab, b)) {
                     if (application.debugEnabled) {
                        Log.d(Constants.LOG_TAG,
                                 "duplicate book detected on BookSearch restoreFromStateBean, it will not be added - "
                                          + b.title + " " + StringUtil.contractAuthors(b.authors));
                     }
                     dupe = true;
                     break;
                  }
               }
               if (!dupe) {
                  adapter.add(b);
               }
            }

            adapter.notifyDataSetChanged();
            if (adapter.getCount() >= selectorPosition) {
               searchResults.setSelection(selectorPosition);
            }
         }
         // any time we restore state, we can assume allowSearchContinue true (we don't want to prevent more data)
         allowSearchContinue = true;
      }
   }

   private BookSearchStateBean createStateBean() {
      BookSearchStateBean bean = new BookSearchStateBean();
      bean.lastSearchPosition = searchPosition;
      bean.lastSearchTerm = currSearchTerm;
      bean.lastSelectorPosition = selectorPosition;

      // store the current adapter contents
      ArrayList<Book> cacheList = new ArrayList<Book>();
      // again, should be able to get/put all from adapter? (and not just brute local access)
      for (int i = 0; i < adapter.getCount(); i++) {
         cacheList.add(adapter.getItem(i));
      }
      bean.books = cacheList;
      return bean;
   }  

   //
   // AsyncTasks
   //
   private class SearchTask extends AsyncTask<String, String, ArrayList<Book>> {

      @Override
      protected void onPreExecute() {
         if (progressDialog.isShowing()) {
            progressDialog.dismiss();
         }
         // keep screen on, and prevent orientation change, during potentially long running task
         getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
         setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
      }

      @Override
      protected ArrayList<Book> doInBackground(final String... args) {         
         String searchTerm = args[0];
         publishProgress(new String[] { searchTerm });
         prevSearchTerm = currSearchTerm;
         currSearchTerm = searchTerm;
         int startIndex = Integer.valueOf(args[1]);
         if (searchTerm != null) {
            searchTerm = URLEncoder.encode(searchTerm);
            return application.bookDataSource.getBooks(searchTerm, startIndex, 10);
         }
         return null;
      }
      
      @Override
      protected void onProgressUpdate(String... progress) {
         progressDialog.setMessage(getString(R.string.msgSearching) + " " + progress[0]);
         if (!progressDialog.isShowing()) {
            progressDialog.show();
         }
      }

      @Override
      protected void onPostExecute(final ArrayList<Book> searchBooks) {
         if (!prevSearchTerm.equals(currSearchTerm)) {
            adapter.clear();
         }

         int dupeCount = 0;
         int addCount = 0;
         if (searchBooks != null && !searchBooks.isEmpty()) {
            if (application.debugEnabled) {
               Log.d(Constants.LOG_TAG, "Books parsed from data source:");
            }
            for (int i = 0; i < searchBooks.size(); i++) {
               Book b = searchBooks.get(i);
               boolean dupe = false;
               // this is very inefficient, need to figure out logical error (why dupes to begin with at this point)
               for (int j = 0; j < adapter.getCount(); j++) {
                  Book ab = adapter.getItem(j);
                  if (BookUtil.areBooksEffectiveDupes(ab, b)) {
                     if (application.debugEnabled) {
                        Log.i(Constants.LOG_TAG,
                                 "duplicate book detected on BookSearch searchTask, it will not be added - " + b.title
                                          + " " + StringUtil.contractAuthors(b.authors));
                     }
                     dupe = true;
                     dupeCount++;
                     break;
                  }
               }
               if (!dupe) {
                  addCount++;
                  adapter.add(b);
                  if (application.debugEnabled) {
                     Log.d(Constants.LOG_TAG, "  Book(" + i + "): " + b.title + " "
                              + StringUtil.contractAuthors(b.authors));
                  }
               }
            }
         }

         searchPosition = adapter.getCount() + 1;
         //searchPosition += 10;         

         adapter.notifyDataSetChanged();
         if (addCount > 0) {
            allowSearchContinue = true;
         } else {
            Toast.makeText(BookSearch.this, "No more results found for this term, please try another search term.",
                     Toast.LENGTH_LONG).show();
         }
         
         if (progressDialog.isShowing()) {
            progressDialog.dismiss();
         }
         // reset screen and orientation params
         getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
         setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
      }
   }

   // state bean
   class BookSearchStateBean {
      ArrayList<Book> books;
      String lastSearchTerm;
      int lastSearchPosition;
      int lastSelectorPosition;
   }
}
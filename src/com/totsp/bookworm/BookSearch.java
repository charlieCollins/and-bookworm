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
   int selectorPosition;
   int searchPosition;

   private BookSearchAdapter adapter;

   private SearchTask searchTask;

   private boolean footerViewEnabled;
   
   private String currSearchTerm = "";
   private String prevSearchTerm = "";
   
   private OnClickListener getMoreDataClickListener = new OnClickListener() {
      public void onClick(final View v) {        
         BookSearch.this.selectorPosition = BookSearch.this.adapter.getCount();
         BookSearch.this.searchTask = new SearchTask();
         BookSearch.this.searchTask.execute(BookSearch.this.searchInput.getText().toString(), String
                  .valueOf(searchPosition));
         v.setBackgroundResource(R.color.red1);
      }
   };

   // TODO data gone after ADD book (after onCreate invoked)
   // need to clean this up a lot, too convoluted right now
   
   @Override
   public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      System.out.println("ONCREATE INVOKED");
      
      setContentView(R.layout.booksearch);
      application = (BookWormApplication) getApplication();

      searchTask = null;
      parsedBooks = new ArrayList<Book>();
      adapter = new BookSearchAdapter(parsedBooks);

      LayoutInflater li = getLayoutInflater();
      getMoreData = (TextView) li.inflate(R.layout.search_listview_footer, null);      
      getMoreData.setVisibility(View.INVISIBLE);
      
      searchInput = (EditText) findViewById(R.id.bookentrysearchinput);
      // if user hits "enter" on keyboard, go ahead and submit, no need for newlines in the search box
      searchInput.setOnKeyListener(new OnKeyListener() {
         public boolean onKey(View v, int keyCode, KeyEvent event) {
            if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
               // if the "search" button is repressed start over (diff from "get more results")
               BookSearch.this.searchTask = new SearchTask();               
               BookSearch.this.searchPosition = 0;
               BookSearch.this.selectorPosition = 0;
               BookSearch.this.prevSearchTerm = "";
               BookSearch.this.parsedBooks = new ArrayList<Book>();
               BookSearch.this.adapter.clear();
               BookSearch.this.adapter.notifyDataSetChanged();
               BookSearch.this.searchTask.execute(BookSearch.this.searchInput.getText().toString(), "0");
               return true;
            }
            return false;
         }
      });

      searchButton = (Button) findViewById(R.id.bookentrysearchbutton);
      searchButton.setOnClickListener(new OnClickListener() {
         public void onClick(final View v) {
            // if the "search" button is repressed start over (diff from "get more results")
            BookSearch.this.searchTask = new SearchTask();               
            BookSearch.this.searchPosition = 0;
            BookSearch.this.selectorPosition = 0;
            BookSearch.this.prevSearchTerm = "";
            BookSearch.this.parsedBooks = new ArrayList<Book>();
            BookSearch.this.adapter.clear();
            BookSearch.this.adapter.notifyDataSetChanged();
            BookSearch.this.searchTask.execute(BookSearch.this.searchInput.getText().toString(), "0");
         }
      });    

      searchResults = (ListView) findViewById(R.id.bookentrysearchresultlist);
      searchResults.setTextFilterEnabled(true);
      searchResults.setOnItemClickListener(new OnItemClickListener() {
         public void onItemClick(final AdapterView<?> parent, final View v, final int index, final long id) {
            // don't redo the search, you have the BOOK itself (don't pass the ISBN, use the Book)
            application.selectedBook = adapter.getItem(index);
            BookSearch.this.selectorPosition = index;           
            Intent intent = new Intent(BookSearch.this, BookEntryResult.class);
            intent.putExtra(BookSearch.FROM_SEARCH, true);
            BookSearch.this.startActivity(intent);
         }
      });
      // note you can't dynamically add/remove header/footer views -- must be there before setAdapter (by design?)
      searchResults.addFooterView(getMoreData);
      searchResults.setAdapter(adapter);
      

      // do not enable the soft keyboard unless user explicitly selects textedit
      // Android seems to have an IMM bug concerning this on devices with only soft keyboard
      // http://code.google.com/p/android/issues/detail?id=7115
      getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

      //this.restoreFromCache();
      this.bindAdapter();
   }

   @Override
   public void onStart() {
      super.onStart();
   }

   @Override
   public void onPause() {
      System.out.println("ONPAUSE INVOKED");
      if ((searchTask != null) && searchTask.dialog.isShowing()) {
         searchTask.dialog.dismiss();
      }
      
      if (searchInput != null) {
         application.lastSearchTerm = searchInput.getText().toString();
      }
      if (searchPosition > 0) {
         application.lastSearchListPosition = searchPosition;
      }
      /*
       if (parsedBooks != null) {
         ArrayList<Book> cacheList = new ArrayList<Book>();
         // again, should be able to get/put all from adapter? (and not just brute local access)
         for (int i = 0; i < adapter.getCount(); i++) {
            Book b = adapter.getItem(i);
            cacheList.add(adapter.getItem(i));
         }
         application.bookCacheList = cacheList;
      }
       */
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

   private void enableFooterView() {
      if (!footerViewEnabled) {         
         footerViewEnabled = true;
      }
      getMoreData.setVisibility(View.VISIBLE);
      getMoreData.setOnClickListener(getMoreDataClickListener);
   }
   
   private void disableFooterView() {
      if (footerViewEnabled) {
         footerViewEnabled = false;
      }
      getMoreData.setVisibility(View.INVISIBLE);
      getMoreData.setOnClickListener(null);
      getMoreData.setBackgroundResource(android.R.color.transparent);
   }
   
   private void bindAdapter() {
      if (!parsedBooks.isEmpty() && !footerViewEnabled) {
         this.enableFooterView();         
      }
      // TODO why can't I add all? must be doing this wrong
      if (!parsedBooks.isEmpty()) {
         if (!prevSearchTerm.equals(currSearchTerm)) {
            adapter.clear();
         }
         for (Book book : parsedBooks) {
            adapter.add(book);
         }
      } else {
         adapter.clear();
      }
      
      adapter.notifyDataSetChanged();

      if (selectorPosition > 2) {
         searchResults.setSelection(selectorPosition - 1);
      }     
   }

   private void restoreFromCache() {
      System.out.println("RESTORE FROM CACHE INVOKED");
      
      // use application object as quick/dirty cache for state      
      if (application.bookCacheList != null) {
         selectorPosition = application.lastSearchListPosition;
         searchPosition = application.lastSearchListPosition;
         parsedBooks = application.bookCacheList;
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
         BookSearch.this.prevSearchTerm = BookSearch.this.currSearchTerm;
         BookSearch.this.currSearchTerm = searchTerm;
         int startIndex = Integer.valueOf(args[1]); 
         System.out.println("STARTINDEX - " + startIndex);
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

         BookSearch.this.parsedBooks.clear();
         if ((books != null) && !books.isEmpty()) {  
            BookSearch.this.searchPosition += books.size() + 1;
            for (int i = 0; i < books.size(); i++) {
               Book b = books.get(i);
               if (!BookSearch.this.parsedBooks.contains(b)) {
                  BookSearch.this.parsedBooks.add(b);
               }
            }            
         }
         
         BookSearch.this.disableFooterView();
         BookSearch.this.bindAdapter();         
      }
   }
}
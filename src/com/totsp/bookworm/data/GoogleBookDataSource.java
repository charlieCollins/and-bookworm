package com.totsp.bookworm.data;

import android.util.Log;
import android.util.Xml;

import com.totsp.bookworm.BookWormApplication;
import com.totsp.bookworm.Constants;
import com.totsp.bookworm.model.Book;
import com.totsp.bookworm.util.NetworkUtil;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class GoogleBookDataSource implements BookDataSource {

   // web identifier search url http://books.google.com/books?isbn=
   private static final String GDATA_BOOK_URL_PREFIX =
            "http://books.google.com/books/feeds/volumes?as_pt=BOOKS&q=isbn:";
   // web search term url
   private static final String GDATA_BOOK_SEARCH_PREFIX = "http://books.google.com/books/feeds/volumes?q=%22";
   private static final String GDATA_BOOK_SEARCH_SUFFIX_PRE = "%22&start-index=";
   private static final String GDATA_BOOK_SEARCH_SUFFIX_POST = "&max-results=";

   // google books uses X FORWARDED FOR header to determine location and what book stuff user can "see"
   private static final String X_FORWARDED_FOR = "X-Forwarded-For";

   private final BookWormApplication application;   
   private final GoogleBooksHandler saxHandler;
   private final HttpHelper httpHelper;

   private boolean debugEnabled;

   public GoogleBookDataSource(final BookWormApplication application) {
      this.application = application;
      // application is unused here - but ctor with application is required
      saxHandler = new GoogleBooksHandler();
      httpHelper = new HttpHelper();
   }

   public void setDebugEnabled(final boolean debugEnabled) {
      this.debugEnabled = debugEnabled;
   }

   public Book getBook(final String isbn) {
      return getSingleBook(isbn);
   }

   public ArrayList<Book> getBooks(final String searchTerm, int startIndex, int numResults) {
      if (startIndex < 1) {
         // don't allow zero or neg, just set to 1
         startIndex = 1;
      }
      if (numResults < 1) {
         numResults = 1;
      }
      
      // remove any added quotes from search term
      // TODO create util inside HttpHelper that cleans up any "string" url passed in
      // (because HttpClient, which should do this, doesn't)
      // uri = new URI("http", null, "www.google.com", 80, "/path/path and path/", URLEncoder.encode("query crap here", "UTF-8"), null);
      String polishedTerm = searchTerm.replace('"', ' ');      
      return getBooksFromSearch(polishedTerm, startIndex, numResults);
   }

   private Book getSingleBook(final String isbn) {
      String url = GoogleBookDataSource.GDATA_BOOK_URL_PREFIX + isbn;
      HashMap<String, String> headers = new HashMap<String, String>();
      headers.put(GoogleBookDataSource.X_FORWARDED_FOR, NetworkUtil.getIpAddress());
      String response = httpHelper.performGet(url, null, null, headers);
      if (debugEnabled) {
         Log.d(Constants.LOG_TAG, "HTTP request to URL " + url);
         Log.d(Constants.LOG_TAG, "HTTP response\n" + response);
      }
      if ((response == null) || response.contains(HttpHelper.HTTP_RESPONSE_ERROR)) {
         Log.w(Constants.LOG_TAG, "HTTP request returned no data (null) - " + url);
         return null;
      }

      ArrayList<Book> books = parseResponse(response);
      if ((books != null) && !books.isEmpty()) {
         return books.get(0);
      }
      return null;
   }

   private ArrayList<Book> getBooksFromSearch(final String searchTerm, final int startIndex, final int numResults) {
      String url =
               GoogleBookDataSource.GDATA_BOOK_SEARCH_PREFIX + searchTerm
                        + GoogleBookDataSource.GDATA_BOOK_SEARCH_SUFFIX_PRE + startIndex
                        + GoogleBookDataSource.GDATA_BOOK_SEARCH_SUFFIX_POST + numResults;
      if (debugEnabled) {
         Log.d(Constants.LOG_TAG, "book search URL - " + url);
      }
      HashMap<String, String> headers = new HashMap<String, String>();
      headers.put(GoogleBookDataSource.X_FORWARDED_FOR, NetworkUtil.getIpAddress());
      String response = httpHelper.performGet(url, null, null, headers);
      if (debugEnabled) {
         Log.d(Constants.LOG_TAG, "HTTP request to URL " + url);
         Log.d(Constants.LOG_TAG, "HTTP response\n" + response);
      }
      if ((response == null) || response.contains(HttpHelper.HTTP_RESPONSE_ERROR)) {
         Log.w(Constants.LOG_TAG, "HTTP request returned no data (null) - " + url);
         return null;
      }
      return parseResponse(response);
   }

   private ArrayList<Book> parseResponse(final String response) {
      try {
         Xml.parse(new ByteArrayInputStream(response.getBytes("UTF-8")), Xml.Encoding.UTF_8, saxHandler);
      } catch (Exception e) {
         Log.e(Constants.LOG_TAG, "Error parsing book XML result", e);
      }

      ArrayList<Book> books = saxHandler.getBooks();
      return books;
   }
}

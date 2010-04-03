package com.totsp.bookworm.data;

import android.util.Log;
import android.util.Xml;

import com.totsp.bookworm.Constants;
import com.totsp.bookworm.model.Book;
import com.totsp.bookworm.util.NetworkUtil;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class GoogleBookDataSource implements IBookDataSource {

   // web identifier search url http://books.google.com/books?isbn=
   private static final String GDATA_BOOK_URL_PREFIX = "http://books.google.com/books/feeds/volumes?as_pt=BOOKS&q=isbn:";
   // web search term url
   private static final String GDATA_BOOK_SEARCH_PREFIX = "http://books.google.com/books/feeds/volumes?q=%22";
   private static final String GDATA_BOOK_SEARCH_SUFFIX_PRE = "%22&start-index=";
   private static final String GDATA_BOOK_SEARCH_SUFFIX_POST = "&max-results=10";

   // google books uses X FORWARDED FOR header to determine location and what book stuff user can "see"
   private static final String X_FORWARDED_FOR = "X-Forwarded-For";

   private final GoogleBooksHandler saxHandler;
   private final HttpHelper httpHelper;
   
   private boolean debugEnabled;

   public GoogleBookDataSource() {
      this.saxHandler = new GoogleBooksHandler();
      this.httpHelper = new HttpHelper();
   }
   
   public void setDebugEnabled(boolean debugEnabled) {
      this.debugEnabled = debugEnabled;
   }

   public Book getBook(final String isbn) {
      return this.getSingleBook(isbn);
   }

   public ArrayList<Book> getBooks(final String searchTerm, int startIndex) {
      if (startIndex < 1) {
         // don't allow zero or neg, just set to 1
         startIndex = 1;
      }
      return this.getBooksFromSearch(searchTerm, startIndex);
   }

   private Book getSingleBook(final String isbn) {
      String url = GoogleBookDataSource.GDATA_BOOK_URL_PREFIX + isbn;
      HashMap<String, String> headers = new HashMap<String, String>();
      headers.put(GoogleBookDataSource.X_FORWARDED_FOR, NetworkUtil.getIpAddress());
      String response = this.httpHelper.performGet(url, null, null, headers);
      if (this.debugEnabled) {
         Log.d(Constants.LOG_TAG, "HTTP request to URL " + url);
         Log.d(Constants.LOG_TAG, "HTTP response\n" + response);
      }
      if ((response == null) || response.contains(HttpHelper.HTTP_RESPONSE_ERROR)) {
         Log.w(Constants.LOG_TAG, "HTTP request returned no data (null) - " + url);
         return null;
      }

      ArrayList<Book> books = this.parseResponse(response);
      if ((books != null) && !books.isEmpty()) {
         return books.get(0);
      }
      return null;
   }

   private ArrayList<Book> getBooksFromSearch(final String searchTerm, final int startIndex) {
      String url =
               GoogleBookDataSource.GDATA_BOOK_SEARCH_PREFIX + searchTerm
                        + GoogleBookDataSource.GDATA_BOOK_SEARCH_SUFFIX_PRE + startIndex
                        + GoogleBookDataSource.GDATA_BOOK_SEARCH_SUFFIX_POST;
      if (this.debugEnabled) {
         Log.d(Constants.LOG_TAG, "book search URL - " + url);
      }
      HashMap<String, String> headers = new HashMap<String, String>();
      headers.put(GoogleBookDataSource.X_FORWARDED_FOR, NetworkUtil.getIpAddress());
      String response = this.httpHelper.performGet(url, null, null, headers);
      if (this.debugEnabled) {
         Log.d(Constants.LOG_TAG, "HTTP request to URL " + url);
         Log.d(Constants.LOG_TAG, "HTTP response\n" + response);
      }
      if ((response == null) || response.contains(HttpHelper.HTTP_RESPONSE_ERROR)) {
         Log.w(Constants.LOG_TAG, "HTTP request returned no data (null) - " + url);
         return null;
      }
      return this.parseResponse(response);
   }

   private ArrayList<Book> parseResponse(final String response) {
      try {
         Xml.parse(new ByteArrayInputStream(response.getBytes("UTF-8")), Xml.Encoding.UTF_8, this.saxHandler);
      } catch (Exception e) {
         Log.e(Constants.LOG_TAG, "Error parsing book XML result", e);
      }

      ArrayList<Book> books = this.saxHandler.getBooks();
      return books;
   }
}

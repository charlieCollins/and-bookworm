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
   private static final String GDATA_BOOK_URL_PREFIX = "http://books.google.com/books/feeds/volumes?q=isbn:";
   // web search term url
   private static final String GDATA_BOOK_SEARCH_PREFIX = "http://books.google.com/books/feeds/volumes?q=%22";
   private static final String GDATA_BOOK_SEARCH_SUFFIX_PRE = "%22&start-index=";
   private static final String GDATA_BOOK_SEARCH_SUFFIX_POST = "&max-results=20";

   // google books uses X FORWARDED FOR header to determine location and what book stuff user can "see"
   private static final String X_FORWARDED_FOR = "X-Forwarded-For";

   private GoogleBooksHandler saxHandler;
   private HttpHelper httpHelper;

   public GoogleBookDataSource() {
      this.saxHandler = new GoogleBooksHandler();
      this.httpHelper = new HttpHelper();
   }
   
   public String getProviderLoginUrl() {
      return "https://www.google.com/accounts/ClientLogin";
   }
   
   public Book getBook(String isbn) {
      // TODO validate isbn
      return this.getSingleBook(isbn);
   }

   public ArrayList<Book> getBooks(String searchTerm, int startIndex) {
      if (startIndex < 1) {
         // don't allow zero or neg, just set to 1
         startIndex = 1;
      }
      return this.getBooksFromSearch(searchTerm, startIndex);
   }

   private Book getSingleBook(String isbn) {
      String url = GDATA_BOOK_URL_PREFIX + isbn;
      HashMap<String, String> headers = new HashMap<String, String>();
      headers.put(X_FORWARDED_FOR, NetworkUtil.getIpAddress());
      String response = this.httpHelper.performGet(url, null, null, headers);
      if (Constants.LOCAL_LOGV) {
         Log.v(Constants.LOG_TAG, "HTTP response\n" + response);
      }
      if (response == null || response.contains(HttpHelper.HTTP_RESPONSE_ERROR)) {
         return null; // TODO better error handling
      }
      return this.parseResponse(response).get(0);
   }

   private ArrayList<Book> getBooksFromSearch(String searchTerm, int startIndex) {
      String url =
               GDATA_BOOK_SEARCH_PREFIX + searchTerm + GDATA_BOOK_SEARCH_SUFFIX_PRE + startIndex
                        + GDATA_BOOK_SEARCH_SUFFIX_POST;
      if (Constants.LOCAL_LOGD) {
         Log.d(Constants.LOG_TAG, "book search URL - " + url);
      }
      HashMap<String, String> headers = new HashMap<String, String>();
      headers.put(X_FORWARDED_FOR, NetworkUtil.getIpAddress());
      String response = this.httpHelper.performGet(url, null, null, headers);
      if (Constants.LOCAL_LOGV) {
         Log.v(Constants.LOG_TAG, "HTTP response\n" + response);
      }
      if (response == null || response.contains(HttpHelper.HTTP_RESPONSE_ERROR)) {
         return null; // TODO better error handling
      }
      return this.parseResponse(response);
   }

   private ArrayList<Book> parseResponse(String response) {
      try {
         Xml.parse(new ByteArrayInputStream(response.getBytes("UTF-8")), Xml.Encoding.UTF_8, this.saxHandler);
      } catch (Exception e) {
         throw new RuntimeException(e);
      }

      ArrayList<Book> books = this.saxHandler.getBooks();
      return books;
   }
}

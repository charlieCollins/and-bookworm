package com.totsp.bookworm.data;

import android.util.Log;

import com.totsp.bookworm.BookWormApplication;
import com.totsp.bookworm.Constants;
import com.totsp.bookworm.model.Author;
import com.totsp.bookworm.model.Book;
import com.totsp.bookworm.util.DateUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;

/**
 * Data source for openlibrary.org
 * 
 * @author Simon McLaughlin
 */
public class OpenLibraryDataSource implements BookDataSource {

   private static final String OL_ISBN_SEARCH_PREFIX =
            "http://openlibrary.org/api/books?callback=processOLBooks&details=true&bibkeys=ISBN:";
   private static final String OL_BOOK_SEARCH_PREFIX = "http://openlibrary.org/api/search?q=";
   private static final String OL_BOOK_DATA_PREFIX = "http://openlibrary.org/api/get?key=";

   private final BookWormApplication application;
   private final HttpHelper httpHelper;

   private JSONObject jObject;

   public OpenLibraryDataSource(final BookWormApplication application) {
      this.application = application;
      this.httpHelper = new HttpHelper();
   }

   public Book getBook(String isbn) {
      String url = OpenLibraryDataSource.OL_ISBN_SEARCH_PREFIX + isbn;
      String response = httpHelper.performGet(url, null, null, null);
      if (application.debugEnabled) {
         Log.d(Constants.LOG_TAG, "HTTP request to URL " + url);
         Log.d(Constants.LOG_TAG, "HTTP response\n" + response);
      }
      if ((response == null) || response.contains(HttpHelper.HTTP_RESPONSE_ERROR)) {
         Log.w(Constants.LOG_TAG, "HTTP request returned no data (null) - " + url);
         return null;
      }

      Book book = null;
      try {
         book = parseResponse(response, isbn);
      } catch (JSONException e) {
         Log.e(Constants.LOG_TAG, e.getMessage(), e);
      }
      return book;
   }

   private Book parseResponse(String response, String isbn) throws JSONException {

      int stop = response.length() - 2;
      String jsonString = response.substring(15, stop);

      if ((jsonString.length() == 2)) {
         Log.w(Constants.LOG_TAG, "HTTP request returned no data (null) - "
                  + OpenLibraryDataSource.OL_ISBN_SEARCH_PREFIX + isbn);
         return null;
      }

      jObject = new JSONObject(jsonString);
      Book book = new Book();
      String key = "ISBN:" + isbn;

      JSONObject jsonBook = jObject.getJSONObject(key);

      book.title = (jsonBook.getJSONObject("details").getString("title"));

      if (jsonBook.getJSONObject("details").has("isbn_10")) {
         book.isbn10 = (jsonBook.getJSONObject("details").getJSONArray("isbn_10").getString(0));
      }

      if (jsonBook.getJSONObject("details").has("isbn_13")) {
         book.isbn13 = (jsonBook.getJSONObject("details").getJSONArray("isbn_13").getString(0));
      }

      if (jsonBook.getJSONObject("details").has("subtitle")) {
         book.subTitle = (jsonBook.getJSONObject("details").getString("subtitle"));
      }

      if (jsonBook.getJSONObject("details").has("publish_date")) {
         Date d = DateUtil.parse(jsonBook.getJSONObject("details").getString("publish_date"));
         if (d != null) {
            book.datePubStamp = (d.getTime());
         }
      }

      if (jsonBook.getJSONObject("details").has("authors")) {
         book.authors.add(new Author(jsonBook.getJSONObject("details").getJSONArray("authors").getJSONObject(0)
                  .getString("name")));
      }

      if (jsonBook.getJSONObject("details").has("publishers")) {
         book.publisher = (jsonBook.getJSONObject("details").getJSONArray("publishers").getString(0));
      }

      if (jsonBook.getJSONObject("details").has("subjects")) {
         JSONArray resultArray = jsonBook.getJSONObject("details").getJSONArray("subjects");
         String subjects = "";

         for (int i = 0; i < resultArray.length(); i++) {
            subjects += resultArray.getString(i);
         }
         book.subject = subjects;
      }

      if (jsonBook.getJSONObject("details").has("physical_format")) {
         book.format = (jsonBook.getJSONObject("details").getString("physical_format"));
      }

      return book;
   }

   public ArrayList<Book> getBooks(String searchTerm, int startIndex) {
      ArrayList<Book> books = new ArrayList<Book>();
      if (startIndex == 0) {
         String url = OpenLibraryDataSource.OL_BOOK_SEARCH_PREFIX;
         try {
            url = url + URLEncoder.encode("{\"query\":\"" + searchTerm + "\"}", "UTF-8");
         } catch (UnsupportedEncodingException e) {
            Log.e(Constants.LOG_TAG, e.getMessage(), e);
         }

         url = url.replaceAll("%2B", "%20");

         String response = httpHelper.performGet(url, null, null, null);

         if (application.debugEnabled) {
            Log.d(Constants.LOG_TAG, "HTTP request to URL " + url);
            Log.d(Constants.LOG_TAG, "HTTP response\n" + response);
         }

         if ((response == null) || response.contains(HttpHelper.HTTP_RESPONSE_ERROR)) {
            Log.w(Constants.LOG_TAG, "HTTP request returned no data (null) - " + url);
            return books;
         }

         try {
            books = getBookDetails(response);
         } catch (JSONException e) {
            Log.e(Constants.LOG_TAG, e.getMessage(), e);
         }

         return books;
      }

      return books;
   }

   private ArrayList<Book> getBookDetails(String jsonString) throws JSONException {
      ArrayList<Book> books = new ArrayList<Book>();
      jObject = new JSONObject(jsonString);
      JSONArray resultArray = jObject.getJSONArray("result");

      for (int i = 0; i < resultArray.length(); i++) {
         String url = OpenLibraryDataSource.OL_BOOK_DATA_PREFIX + resultArray.getString(i);
         String response = httpHelper.performGet(url, null, null, null);

         if (application.debugEnabled) {
            Log.d(Constants.LOG_TAG, "HTTP request to URL " + url);
            Log.d(Constants.LOG_TAG, "HTTP response\n" + response);
         }

         JSONObject jsonBook = new JSONObject(response);
         Book book = new Book();
         String title = "";
         if (jsonBook.getJSONObject("result").has("title_prefix")) {
            title += jsonBook.getJSONObject("result").getString("title_prefix");
         }
         title += jsonBook.getJSONObject("result").getString("title");
         book.title = (title);

         if (jsonBook.getJSONObject("result").has("isbn_10")) {
            book.isbn10 = (jsonBook.getJSONObject("result").getJSONArray("isbn_10").getString(0));
         }

         if (jsonBook.getJSONObject("result").has("isbn_13")) {
            book.isbn13 = (jsonBook.getJSONObject("result").getJSONArray("isbn_13").getString(0));
         }

         if (jsonBook.getJSONObject("result").has("subtitle")) {
            book.subTitle = (jsonBook.getJSONObject("result").getString("subtitle"));
         }

         if (jsonBook.getJSONObject("result").has("publish_date")) {
            Date d = DateUtil.parse(jsonBook.getJSONObject("result").getString("publish_date"));
            if (d != null) {
               book.datePubStamp = (d.getTime());
            }
         }

         if (jsonBook.getJSONObject("result").has("authors")) {
            String url2 =
                     OpenLibraryDataSource.OL_BOOK_DATA_PREFIX
                              + jsonBook.getJSONObject("result").getJSONArray("authors").getJSONObject(0).getString(
                                       "key");
            String response2 = httpHelper.performGet(url2, null, null, null);

            if (application.debugEnabled) {
               Log.d(Constants.LOG_TAG, "HTTP request to URL " + url2);
               Log.d(Constants.LOG_TAG, "HTTP response\n" + response2);
            }

            JSONObject jsonAuthor = new JSONObject(response2);
            book.authors.add(new Author(jsonAuthor.getJSONObject("result").getString("name")));
         }

         if (jsonBook.getJSONObject("result").has("publishers")) {
            book.publisher = (jsonBook.getJSONObject("result").getJSONArray("publishers").getString(0));
         }

         if (jsonBook.getJSONObject("result").has("subjects")) {
            JSONArray resultArray2 = jsonBook.getJSONObject("result").getJSONArray("subjects");
            String subjects = "";

            for (int j = 0; j < resultArray2.length(); j++) {
               subjects += resultArray2.getString(j);
            }
            book.subject = subjects;
         }

         if (jsonBook.getJSONObject("result").has("physical_format")) {
            book.format = (jsonBook.getJSONObject("result").getString("physical_format"));
         }

         books.add(book);
      }

      return books;
   }
}

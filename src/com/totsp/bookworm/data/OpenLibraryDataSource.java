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

import java.util.ArrayList;
import java.util.Date;

/**
 * Data source for openlibrary.org.
 * 
 * @author Simon McLaughlin
 */
public class OpenLibraryDataSource implements BookDataSource {

   private static final String OL_ISBN_SEARCH_PREFIX =
            "http://openlibrary.org/api/books?callback=processOLBooks&details=true&bibkeys=ISBN:";
   private static final String OL_BOOK_SEARCH_PREFIX = "http://openlibrary.org/search.json?title=";
   //private static final String OL_BOOK_DATA_PREFIX = "http://openlibrary.org/api/get?key=";

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

   public ArrayList<Book> getBooks(String searchTerm, int startIndex, int numResults) {
      ArrayList<Book> books = new ArrayList<Book>();
      if (startIndex < 1) {
         // don't allow zero or neg, just set to 1
         startIndex = 1;
      }
      if (numResults < 1) {
         numResults = 1;
      }
      
      String url = OpenLibraryDataSource.OL_BOOK_SEARCH_PREFIX + searchTerm + "&offset=" + startIndex + "&limit=" + numResults;

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

   private ArrayList<Book> getBookDetails(String jsonString) throws JSONException {
      ArrayList<Book> books = new ArrayList<Book>();
      jObject = new JSONObject(jsonString);
      JSONArray resultArray = jObject.getJSONArray("docs");

      for (int i = 0; i < resultArray.length(); i++) {

         JSONObject jsonBook = resultArray.getJSONObject(i);
         Book book = new Book();

         if (jsonBook.has("title")) {
            book.title = jsonBook.getString("title");
         }

         if (jsonBook.has("isbn")) {
            JSONArray jsonISBN = jsonBook.getJSONArray("isbn");
            for (int k = 0; k < jsonISBN.length(); k++) {
               if (jsonISBN.getString(k).length() == 10) {
                  book.isbn10 = jsonISBN.getString(k);
               } else if (jsonISBN.getString(k).length() == 13) {
                  book.isbn13 = jsonISBN.getString(k);
               }
            }
         }

         if (jsonBook.has("subtitle")) {
            book.subTitle = (jsonBook.getString("subtitle"));
         }

         if (jsonBook.has("first_publish_year")) {
            Date d = DateUtil.parse(jsonBook.getString("first_publish_year"));
            if (d != null) {
               book.datePubStamp = (d.getTime());
            }
         }

         if (jsonBook.has("author_name")) {
            JSONArray jsonAuthor = jsonBook.getJSONArray("author_name");
            for (int j = 0; j < jsonAuthor.length(); j++) {
               book.authors.add(new Author(jsonAuthor.getString(j)));
            }
         }

         if (jsonBook.has("publisher")) {
            book.publisher = (jsonBook.getJSONArray("publisher").getString(0));
         }

         if (jsonBook.has("subject")) {
            JSONArray resultArray2 = jsonBook.getJSONArray("subject");
            String subjects = "";

            for (int j = 0; j < resultArray2.length(); j++) {
               subjects += resultArray2.getString(j) + ", ";
            }
            book.subject = subjects;
         }

         if (jsonBook.has("physical_format")) {
            book.format = (jsonBook.getString("physical_format"));
         }

         books.add(book);
      }
      
      return books;
   }
}

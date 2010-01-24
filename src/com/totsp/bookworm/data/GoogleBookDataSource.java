package com.totsp.bookworm.data;

import android.util.Xml;

import com.totsp.bookworm.model.Book;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;

public class GoogleBookDataSource implements IBookDataSource {

   // web url http://books.google.com/books?isbn=
   private static final String GDATA_BOOK_URL_PREFIX = "http://books.google.com/books/feeds/volumes?q=isbn:";

   private GoogleBooksHandler handler;
   private HttpHelper httpHelper;

   public GoogleBookDataSource() {
      this.handler = new GoogleBooksHandler();
      this.httpHelper = new HttpHelper();
   }  

   public void getBook(String isbn, IAsyncCallback<Book> callback) {
      // TODO validate isbn
      this.getBookData(isbn, callback);      
   }
   
   // TODO make request in separate thread here? (like HttpHelperAndroid)
   private void getBookData(String isbn, IAsyncCallback<Book> callback) {
      String url = GDATA_BOOK_URL_PREFIX + isbn;      
      String response = this.httpHelper.performGet(url);
      if (response == null || response.contains(HttpHelper.HTTP_RESPONSE_ERROR)) {
         // TODO better exception
         callback.onFailure(new Exception(response));
      } else {
         Book book = this.parseResponse(response);
         callback.onSuccess(book);
      }      
   }   
   
   private Book parseResponse(String response) {
      Book book = null;
      try {
         Xml.parse(new ByteArrayInputStream(response.getBytes("UTF-8")), Xml.Encoding.UTF_8, this.handler);
      } catch (Exception e) {
         throw new RuntimeException(e);
      }

      ArrayList<Book> books = this.handler.getBooks();
      if (books != null && !books.isEmpty()) {        
         book = books.get(0);         
      } else {
         ///
      }
      return book;
   }

}

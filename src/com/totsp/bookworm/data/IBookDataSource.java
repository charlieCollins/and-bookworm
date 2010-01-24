package com.totsp.bookworm.data;

import com.totsp.bookworm.model.Book;

public interface IBookDataSource {

   void getBook(String isbn, IAsyncCallback<Book> callback);   
   
}

package com.totsp.bookworm.data;

import com.totsp.bookworm.model.Book;

public interface IBookDataSource {

   Book getBook(String isbn);   
   
}

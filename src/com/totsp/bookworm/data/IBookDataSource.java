package com.totsp.bookworm.data;

import com.totsp.bookworm.model.Book;

import java.util.ArrayList;

public interface IBookDataSource {

   String getProviderName();
   
   Book getBook(String identifier); 
   
   ArrayList<Book> getBooks(String searchTerm, int startIndex);
   
}

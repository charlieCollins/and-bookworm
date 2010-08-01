package com.totsp.bookworm.data;

import com.totsp.bookworm.BookWormApplication;
import com.totsp.bookworm.model.Book;

import java.util.ArrayList;

public interface BookDataSource {

   Book getBook(String identifier);

   ArrayList<Book> getBooks(String searchTerm, int startIndex);

   void setContext(BookWormApplication bookWormApplication);

   void setDebugEnabled(boolean debugEnabled);

}

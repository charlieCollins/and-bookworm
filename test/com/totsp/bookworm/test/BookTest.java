package com.totsp.bookworm.test;

import com.totsp.bookworm.model.Author;
import com.totsp.bookworm.model.Book;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;

// gotta use Junit3 and extend TestCase - to run this in Eclipse without extra config

public class BookTest extends TestCase {

   public void testBookList() {
      
      Author a1 = new Author("author1");      
      
      Book b1 = new Book("1231", "title1", null, new Date());
      Book b2 = new Book("1232", "title2", null, new Date());      
      Book b3 = new Book("1233", "title3", null, new Date());
      List<Book> books1 = new ArrayList<Book>();
      books1.add(b1);
      books1.add(b2);
      books1.add(b3);     
      
      System.out.println("b1 - " + b1);
      
   }
   
}
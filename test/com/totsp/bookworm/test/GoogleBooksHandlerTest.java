package com.totsp.bookworm.test;

import com.totsp.bookworm.data.GoogleBooksHandler;
import com.totsp.bookworm.model.Book;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import junit.framework.TestCase;

// gotta use Junit3 and extend TestCase - to run this in Eclipse without extra config
// remove android lib and add JRE and junit to run config

public class GoogleBooksHandlerTest extends TestCase {

   public void testParse() throws Exception {
      
      GoogleBooksHandler handler = new GoogleBooksHandler();

      File file = new File("/home/ccollins/projects/BookWorm/docs/book_search_response.xml");
      InputStream is = new FileInputStream(file);
      
      SAXParserFactory factory = SAXParserFactory.newInstance();
      SAXParser p = factory.newSAXParser();
      p.parse(is, handler);
      
      System.out.println("getBooks");
      List<Book> books = handler.getBooks();
      for (Book book : books) {
         System.out.println("book - " + book);
      }     
   }   
}
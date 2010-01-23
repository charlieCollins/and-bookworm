package com.totsp.bookworm.test;

import com.totsp.bookworm.data.GoogleBooksHandler;
import com.totsp.bookworm.model.Book;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import junit.framework.TestCase;

// gotta use Junit3 and extend TestCase - to run this in Eclipse without extra config
// remove android lib and add JRE and junit to run config

public class GoogleBooksHandlerTest extends TestCase {

   public void testParse() throws Exception {

      InputStream is = null;
      try {
         GoogleBooksHandler handler = new GoogleBooksHandler();

         File file = new File("/home/ccollins/projects/BookWorm/docs/book_search_response.xml");
         is = new FileInputStream(file);

         XMLReader r = XMLReaderFactory.createXMLReader();
         r.setContentHandler(handler);
         r.parse(new InputSource(is));

         System.out.println("getBooks");
         List<Book> books = handler.getBooks();
         for (Book book : books) {
            System.out.println("book - " + book);
         }
      } finally {
         if (is != null) {
            is.close();
         }
      }
   }
}
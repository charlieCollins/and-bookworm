package com.totsp.bookworm.test;

import com.totsp.bookworm.data.GoogleBooksHandler;

import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import junit.framework.TestCase;

// gotta use Junit3 and extend TestCase - to run this in Eclipse without extra config

public class GoogleBooksHandlerTest extends TestCase {

   public void testParse() throws Exception {
      
      DefaultHandler handler = new GoogleBooksHandler();

      File file = new File("/home/ccollins/projects/BookWorm/docs/book_search_response.xml");
      InputStream is = new FileInputStream(file);
      
      SAXParserFactory factory = SAXParserFactory.newInstance();
      SAXParser p = factory.newSAXParser();
      p.parse(is, handler);
     
   }   
}
package com.totsp.bookworm.data;

import com.totsp.bookworm.model.Author;
import com.totsp.bookworm.model.Book;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class GoogleBooksHandler extends DefaultHandler {

   private static final String ENTRY = "entry";

   private List<Book> books;

   private Book book;

   private boolean inEntry;
   private boolean inTitle;
   private boolean inDate;
   private boolean inCreator;

   public GoogleBooksHandler() {
      this.books = new ArrayList<Book>();
   }   
   
   /*
   public static void main(String[] args) throws Exception {
      DefaultHandler handler = new DefaultHandler();

      SAXParserFactory factory = SAXParserFactory.newInstance();
      SAXParser p = factory.newSAXParser();
      p.parse(GoogleBooksHandler.class.getResourceAsStream("./docs/book_search_response.xml"), handler);
   }
   */

   @Override
   public void startDocument() throws SAXException {
      System.out.println("startDocument");
   }

   @Override
   public void endDocument() throws SAXException {
      System.out.println("endDocument");
   }

   @Override
   public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
      
      System.out.println("startElement - " + localName);
      
      if (localName.equals(GoogleBooksHandler.ENTRY)) {
         this.inEntry = true;
         this.book = new Book();
      }

      if (this.inEntry && localName.equals("title")) {
         this.inTitle = true;
      } else if (this.inEntry && localName.equals("date")) {
         this.inDate = true;
      } else if (this.inEntry && localName.equals("create")) {
         this.inCreator = true;
      }
   }

   @Override
   public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
      
      System.out.println("endElement - " + localName);
      
      if (localName.equals(GoogleBooksHandler.ENTRY)) {
         if (this.inEntry) {
            this.inEntry = false;
         }

         if (this.inEntry && localName.equals("title")) {
            this.inTitle = false;
         } else if (this.inEntry && localName.equals("date")) {
            this.inDate = false;
         } else if (this.inEntry && localName.equals("create")) {
            this.inCreator = false;
         }
      }
   }

   @Override
   public void characters(char ch[], int start, int length) {

      System.out.println("characters - " + new String(ch));
      
      if (this.inEntry && this.inTitle) {
         book.setTitle(new String(ch));
      } else if (this.inEntry && this.inDate) {
         // parse date
      } else if (this.inEntry && this.inCreator) {
         book.getAuthors().add(new Author(new String(ch)));
      }

   }

   private String getAttributeValue(String attName, Attributes atts) {
      String result = null;
      for (int i = 0; i < atts.getLength(); i++) {
         String thisAtt = atts.getLocalName(i);
         if (attName.equals(thisAtt)) {
            result = atts.getValue(i);
            break;
         }
      }
      return result;
   }

   public List<Book> getBooks() {
      return this.books;
   }
}

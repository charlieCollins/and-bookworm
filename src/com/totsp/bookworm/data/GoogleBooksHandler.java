package com.totsp.bookworm.data;

import com.totsp.bookworm.model.Author;
import com.totsp.bookworm.model.Book;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * SAX DefaultHandler impl for Google Books feed.
 * 
 * TODO - compare with XMLPullParser impl - though direct SAX may still be faster?
 * http://www.developer.com/xml/article.php/3824221/Android-XML-Parser-Performance.htm 
 *
 * @author ccollins
 */
public class GoogleBooksHandler extends DefaultHandler {

   private static final String ENTRY = "entry";

   private SimpleDateFormat dateFormat;
   private ArrayList<Book> books;
   private Book book;

   private boolean inEntry;
   private boolean inTitle;
   private boolean inDate;
   private boolean inCreator;
   private boolean inIdentifier;
   private boolean inSubject;
   private boolean inPublisher;
   private boolean inFormat;
   private boolean inDescription;
   private boolean inLink;

   public GoogleBooksHandler() {
      this.books = new ArrayList<Book>();
      this.dateFormat = new SimpleDateFormat("yyyy-MM-dd");
   }

   @Override
   public void startDocument() throws SAXException {
   }

   @Override
   public void endDocument() throws SAXException {
   }

   @Override
   public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
      if (localName.equals(GoogleBooksHandler.ENTRY)) {
         this.inEntry = true;
         this.book = new Book();
      }

      if (this.inEntry && localName.equals("title")) {
         this.inTitle = true;
      } else if (this.inEntry && localName.equals("date")) {
         this.inDate = true;
      } else if (this.inEntry && localName.equals("creator")) {
         this.inCreator = true;
      } else if (this.inEntry && localName.equals("identifier")) {
         this.inIdentifier = true;
      } else if (this.inEntry && localName.equals("publisher")) {
         this.inPublisher = true;
      } else if (this.inEntry && localName.equals("subject")) {
         this.inSubject = true;
      } else if (this.inEntry && localName.equals("description")) {
         this.inDescription = true;
      } else if (this.inEntry && localName.equals("format")) {
         this.inFormat = true;
      } else if (this.inEntry && localName.equals("link")) {
         this.inLink = true;         
         // parse/process the links (attributes), find gBooks page, find images, etc
         // use rel attribute = "http://schemas.google.com/books/2008/thumbnail" for image
         // use rel attribute = "http://schemas.google.com/books/2008/info" for overview web page
         // others are available, preview, etc, but not all books have such features (have to cross check with other feed items)
         // TODO only authenticated users can use this stuff?
         /*
         String rel = this.getAttributeValue("rel", atts);
         if (rel.equalsIgnoreCase("http://schemas.google.com/books/2008/thumbnail")) {
            book.setImageUrl(this.getAttributeValue("href", atts));
         } else if (rel.equalsIgnoreCase("http://schemas.google.com/books/2008/info")) {
            book.setOverviewUrl(this.getAttributeValue("href", atts));
         } 
         */         
      }
   }

   @Override
   public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
      if (localName.equals(GoogleBooksHandler.ENTRY)) {
         if (this.inEntry) {
            this.books.add(this.book);
            this.inEntry = false;
         }
      }

      if (this.inEntry && localName.equals("title")) {
         this.inTitle = false;
      } else if (this.inEntry && localName.equals("date")) {
         this.inDate = false;
      } else if (this.inEntry && localName.equals("creator")) {
         this.inCreator = false;
      } else if (this.inEntry && localName.equals("identifier")) {
         this.inIdentifier = false;
      } else if (this.inEntry && localName.equals("publisher")) {
         this.inPublisher = false;
      } else if (this.inEntry && localName.equals("subject")) {
         this.inSubject = false;
      } else if (this.inEntry && localName.equals("description")) {
         this.inDescription = false;
      } else if (this.inEntry && localName.equals("format")) {
         this.inFormat = false;
      } else if (this.inEntry && localName.equals("link")) {
         this.inLink = false;
      }
   }

   @Override
   public void characters(char ch[], int start, int length) {
      if (this.inEntry && this.inTitle) {
         this.book.setTitle(new String(ch, start, length));
      } else if (this.inEntry && this.inDate) {
         try {
            Date d = this.dateFormat.parse(new String(ch, start, length));
            book.setDatePubStamp(d.getTime());
         } catch (ParseException e) {
         }
      } else if (this.inEntry && this.inCreator) {
         this.book.getAuthors().add(new Author(new String(ch, start, length)));
      } else if (this.inEntry && this.inIdentifier) {
         String existingId = book.getIsbn();
         if (existingId == null || existingId.length() < 13) {
            String id = new String(ch, start, length);
            if (id.startsWith("ISBN")) {
               book.setIsbn(id.substring(5, id.length()));
            }
         }
      } else if (this.inEntry && this.inPublisher) {
         book.setPublisher(new String(ch, start, length));
      } else if (this.inEntry && this.inSubject) {
         book.setSubject(new String(ch, start, length));
      } else if (this.inEntry && this.inDescription) {
         book.setDescription(new String(ch, start, length));
      } else if (this.inEntry && this.inFormat) {
         String existingFormat = book.getFormat();
         if (existingFormat == null) {
            book.setFormat(new String(ch, start, length));
         } else {
            book.setFormat(existingFormat + " " + new String(ch, start, length));
         }
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

   public ArrayList<Book> getBooks() {
      return this.books;
   }
}

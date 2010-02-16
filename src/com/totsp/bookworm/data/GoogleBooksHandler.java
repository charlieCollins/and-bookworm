package com.totsp.bookworm.data;

import android.util.Log;

import com.totsp.bookworm.Constants;
import com.totsp.bookworm.model.Author;
import com.totsp.bookworm.model.Book;
import com.totsp.bookworm.util.DateUtil;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

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

   private final ArrayList<Book> books;
   private Book book;

   private boolean inEntry;
   private int titleLevel = 1;
   StringBuilder sb;

   public GoogleBooksHandler() {
      this.books = new ArrayList<Book>();
      this.sb = new StringBuilder();
   }

   @Override
   public void startDocument() throws SAXException {
   }

   @Override
   public void endDocument() throws SAXException {
   }

   @Override
   public void startElement(final String namespaceURI, final String localName, final String qName, final Attributes atts)
      throws SAXException {

      if (localName.equals(GoogleBooksHandler.ENTRY)) {
         this.inEntry = true;
         this.book = new Book();
      }

      if (this.inEntry && localName.equals("title")) {
         this.sb = new StringBuilder();
      } else if (this.inEntry && localName.equals("date")) {
         this.sb = new StringBuilder();
      } else if (this.inEntry && localName.equals("creator")) {
         this.sb = new StringBuilder();
      } else if (this.inEntry && localName.equals("identifier")) {
         this.sb = new StringBuilder();
      } else if (this.inEntry && localName.equals("publisher")) {
         this.sb = new StringBuilder();
      } else if (this.inEntry && localName.equals("subject")) {
         this.sb = new StringBuilder();
      } else if (this.inEntry && localName.equals("description")) {
         this.sb = new StringBuilder();
      } else if (this.inEntry && localName.equals("format")) {
         this.sb = new StringBuilder();
      } else if (this.inEntry && localName.equals("link")) {
         this.sb = new StringBuilder();
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
   public void endElement(final String namespaceURI, final String localName, final String qName) throws SAXException {
      if (localName.equals(GoogleBooksHandler.ENTRY)) {
         if (this.inEntry) {
            this.books.add(this.book);
            this.inEntry = false;
            this.titleLevel = 1;
         }
      }

      String bufferContents = sb.toString().replaceAll("\\s+", " ");

      if (this.inEntry && localName.equals("title")) {
         // there is an unqualified title and 0-n dc:title (s) so title is complicated  
         // and qName seems to be NULL on Android, namespace-prefixes feature may not be enabled?
         // whatever the cause, have to rely on this to work WITHOUT qNames
         if (book.getTitle() == null || book.getTitle().equals("")) {
            book.setTitle(bufferContents);
         } else if (book.getSubTitle() == null || book.getSubTitle().equals("")) {
            if (!book.getTitle().equals(bufferContents)) {
               book.setSubTitle(bufferContents);
            }
         }
         // don't set pass sub

      } else if (this.inEntry && localName.equals("date")) {
         Date d = DateUtil.parse(bufferContents);
         if (d != null) {
            this.book.setDatePubStamp(d.getTime());
         }
      } else if (this.inEntry && localName.equals("creator")) {
         this.book.getAuthors().add(new Author(bufferContents));
      } else if (this.inEntry && localName.equals("identifier")) { 
         String existingId = this.book.getIsbn();
         if ((existingId.equals("")) || (existingId.length() > 10)) {
            // prefer the ISBN 10 for now - need to eventually store both
            String id = bufferContents;
            if (id.startsWith("ISBN")) {
               this.book.setIsbn(id.substring(5, id.length()).trim());
            }
         }
      } else if (this.inEntry && localName.equals("publisher")) {
         this.book.setPublisher(bufferContents);
      } else if (this.inEntry && localName.equals("subject")) {
         this.book.setSubject(bufferContents);
      } else if (this.inEntry && localName.equals("description")) {
         this.book.setDescription(bufferContents);
      } else if (this.inEntry && localName.equals("format")) {
         if (this.book.getFormat() != null) {
            this.book.setFormat(new String(this.book.getFormat() + " " + bufferContents).trim());
         } else {
            this.book.setFormat(bufferContents);
         }
      } else if (this.inEntry && localName.equals("link")) {
      }

   }

   @Override
   public void characters(final char ch[], final int start, final int length) {
      this.sb.append(new String(ch, start, length));
   }

   /*
   private String getAttributeValue(final String attName, final Attributes atts) {
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
   */

   public ArrayList<Book> getBooks() {
      return this.books;
   }
}

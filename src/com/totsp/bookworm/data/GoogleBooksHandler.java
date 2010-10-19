package com.totsp.bookworm.data;

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
 * @author ccollins
 */
public class GoogleBooksHandler extends DefaultHandler {

   // NOTE compare with XMLPullParser impl - though direct SAX may still be faster?
   // http://www.developer.com/xml/article.php/3824221/Android-XML-Parser-Performance.htm 
   
   private static final String ENTRY = "entry";

   private ArrayList<Book> books;
   private Book book;

   private boolean inEntry;
   StringBuilder sb;

   public GoogleBooksHandler() {
   }

   @Override
   public void startDocument() throws SAXException {
      books = new ArrayList<Book>();
      sb = new StringBuilder();
   }

   @Override
   public void endDocument() throws SAXException {
   }

   @Override
   public void startElement(final String namespaceURI, final String localName, final String qName, final Attributes atts)
      throws SAXException {

      if (localName.equals(GoogleBooksHandler.ENTRY)) {
         inEntry = true;
         book = new Book();
      }

      if (inEntry && localName.equals("title")) {
         sb = new StringBuilder();
      } else if (inEntry && localName.equals("date")) {
         sb = new StringBuilder();
      } else if (inEntry && localName.equals("creator")) {
         sb = new StringBuilder();
      } else if (inEntry && localName.equals("identifier")) {
         sb = new StringBuilder();
      } else if (inEntry && localName.equals("publisher")) {
         sb = new StringBuilder();
      } else if (inEntry && localName.equals("subject")) {
         sb = new StringBuilder();
      } else if (inEntry && localName.equals("description")) {
         sb = new StringBuilder();
      } else if (inEntry && localName.equals("format")) {
         sb = new StringBuilder();
      } else if (inEntry && localName.equals("link")) {
         sb = new StringBuilder();
         // parse/process the links (attributes), find gBooks page, find images, etc
         // use rel attribute = "http://schemas.google.com/books/2008/thumbnail" for image
         // use rel attribute = "http://schemas.google.com/books/2008/info" for overview web page
         // others are available, preview, etc, but not all books have such features (have to cross check with other feed items)

         /*String rel = getAttributeValue("rel", atts);
         if (rel.equalsIgnoreCase("http://schemas.google.com/books/2008/thumbnail")) {
            book.coverImageURL = (getAttributeValue("href", atts));
         }*/
      }
   }

   @Override
   public void endElement(final String namespaceURI, final String localName, final String qName) throws SAXException {
      if (localName.equals(GoogleBooksHandler.ENTRY)) {
         if (inEntry) {
            books.add(book);
            inEntry = false;
         }
      }

      String bufferContents = sb.toString().replaceAll("\\s+", " ");

      if (inEntry && localName.equals("title")) {
         // there is an unqualified title and 0-n dc:title (s) so title is complicated  
         // and qName seems to be NULL on Android, namespace-prefixes feature may not be enabled?
         // whatever the cause, have to rely on this to work WITHOUT qNames
         if ((book.title == null) || book.title.equals("")) {
            book.title = (bufferContents);
         } else if ((book.subTitle == null) || book.subTitle.equals("")) {
            if (!book.title.equals(bufferContents)) {
               book.subTitle = (bufferContents);
            }
         }
         // don't set past sub
      } else if (inEntry && localName.equals("date")) {
         Date d = DateUtil.parse(bufferContents);
         if (d != null) {
            book.datePubStamp = (d.getTime());
         }
      } else if (inEntry && localName.equals("creator")) {
         book.authors.add(new Author(bufferContents));
      } else if (inEntry && localName.equals("identifier")) {
         String id = bufferContents;
         if ((id != null) && id.startsWith("ISBN")) {
            id = id.substring(5, id.length()).trim();
            if (id.length() == 10) {
               book.isbn10 = (id);
            } else if (id.length() == 13) {
               book.isbn13 = (id);
            }
         }
      } else if (inEntry && localName.equals("publisher")) {
         book.publisher = (bufferContents);
      } else if (inEntry && localName.equals("subject")) {
         book.subject = (bufferContents);
      } else if (inEntry && localName.equals("description")) {
         book.description = (bufferContents);
      } else if (inEntry && localName.equals("format")) {
         if (book.format != null) {
            book.format = book.format + " " + bufferContents.trim();
         } else {
            book.format = (bufferContents);
         }
      } else if (inEntry && localName.equals("link")) {
      }
   }

   @Override
   public void characters(final char ch[], final int start, final int length) {
      sb.append(new String(ch, start, length));
   }

   /*private String getAttributeValue(final String attName, final Attributes atts) {
      String result = null;
      for (int i = 0; i < atts.getLength(); i++) {
         String thisAtt = atts.getLocalName(i);
         if (attName.equals(thisAtt)) {
            result = atts.getValue(i);
            break;
         }
      }
      return result;
   }*/

   public ArrayList<Book> getBooks() {
      return books;
   }
}

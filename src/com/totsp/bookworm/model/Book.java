package com.totsp.bookworm.model;

import android.graphics.Bitmap;

import java.util.ArrayList;


/**
 * Defines a single book entry in database.
 * As well as the publisher data for the book, each entry also contains a  
 * user-specific metadata object to hold info such as personal ratings.
 *
 */
public final class Book {

   // NOTE - no accessors/mutators by design, Android optimization

   // give things explicit defaults, easier than null checks later for SQLite
   public long id = 0L;
   public String isbn10 = "";
   public String isbn13 = "";
   public String title = "";
   public String subTitle = "";
   public String publisher = "";
   public String description = "";
   public String format = "";
   public String subject = "";
   public long datePubStamp = 0L;
   public ArrayList<Author> authors;

   public BookUserData bookUserData;

   // not stored in db or serialized
   // (optionally returned from parser, but not stored, image Ids are stored after processing)
   public transient Bitmap coverImage;

   public Book() {
      authors = new ArrayList<Author>();
      bookUserData = new BookUserData();
   }

   public Book(final String title) {
      if ((title == null) || (title.length() < 1)) {
         throw new IllegalArgumentException("Error, book must have a title (minimum size 1)");
      }
      authors = new ArrayList<Author>();
      bookUserData = new BookUserData();
      this.title = title;
   }

   @Override
   public String toString() {
      // this is the default display in a ListView, also used by Filter, etc, so make it significant/short/sweet
      return title;
   }

   @Override
   public boolean equals(final Object obj) {
      if (obj == this) {
         return true;
      }
      if (obj instanceof Book) {
         Book lhs = (Book) obj;
         if ((lhs.id == id) && (lhs.isbn10.equals(isbn10)) && (lhs.isbn13.equals(isbn13)) && (lhs.title.equals(title))
                  && (lhs.subTitle.equals(subTitle)) && (lhs.authors.equals(authors))
                  && (lhs.publisher.equals(publisher)) && (lhs.description.equals(description))
                  && (lhs.format.equals(format)) && (lhs.subject.equals(subject)) && (lhs.datePubStamp == datePubStamp)
                  && (lhs.bookUserData == bookUserData)) {
            return true;
         }
      }
      return false;
   }

   @Override
   public int hashCode() {
      int result = 31;
      result += id;
      if (isbn10 != null) {
         result += isbn10.hashCode();
      }
      if (isbn13 != null) {
         result += isbn13.hashCode();
      }
      if (title != null) {
         result += title.hashCode();
      }
      if (subTitle != null) {
         result += subTitle.hashCode();
      }
      if (authors != null) {
         result += authors.hashCode();
      }
      if (publisher != null) {
         result += publisher.hashCode();
      }
      if (description != null) {
         result += description.hashCode();
      }
      if (format != null) {
         result += format.hashCode();
      }
      if (subject != null) {
         result += subject.hashCode();
      }
      if (bookUserData != null) {
         result += bookUserData.hashCode();
      }
      result += datePubStamp;

      return result;
   }

   public String toStringFull() {
      StringBuilder sb = new StringBuilder();
      sb.append("Book-");
      sb.append("\n id:" + id);
      sb.append("\n title:" + title);
      sb.append("\n subTitle:" + subTitle);
      sb.append("\n isbn10:" + isbn10);
      sb.append("\n isbn13:" + isbn13);
      sb.append("\n authors:" + authors);
      sb.append("\n publisher:" + publisher);
      sb.append("\n description:" + description);
      sb.append("\n format:" + format);
      sb.append("\n subject:" + subject);
      sb.append("\n datePubStamp:" + datePubStamp);
      return sb.toString();
   }
}
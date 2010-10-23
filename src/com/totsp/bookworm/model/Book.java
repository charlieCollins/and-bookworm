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

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((this.authors == null) ? 0 : this.authors.hashCode());
      result = prime * result + ((this.bookUserData == null) ? 0 : this.bookUserData.hashCode());
      result = prime * result + (int) (this.datePubStamp ^ (this.datePubStamp >>> 32));
      result = prime * result + ((this.description == null) ? 0 : this.description.hashCode());
      result = prime * result + ((this.format == null) ? 0 : this.format.hashCode());
      result = prime * result + (int) (this.id ^ (this.id >>> 32));
      result = prime * result + ((this.isbn10 == null) ? 0 : this.isbn10.hashCode());
      result = prime * result + ((this.isbn13 == null) ? 0 : this.isbn13.hashCode());
      result = prime * result + ((this.publisher == null) ? 0 : this.publisher.hashCode());
      result = prime * result + ((this.subTitle == null) ? 0 : this.subTitle.hashCode());
      result = prime * result + ((this.subject == null) ? 0 : this.subject.hashCode());
      result = prime * result + ((this.title == null) ? 0 : this.title.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      if (!(obj instanceof Book)) {
         return false;
      }
      Book other = (Book) obj;
      if (this.authors == null) {
         if (other.authors != null) {
            return false;
         }
      } else if (!this.authors.equals(other.authors)) {
         return false;
      }
      if (this.bookUserData == null) {
         if (other.bookUserData != null) {
            return false;
         }
      } else if (!this.bookUserData.equals(other.bookUserData)) {
         return false;
      }
      if (this.datePubStamp != other.datePubStamp) {
         return false;
      }
      if (this.description == null) {
         if (other.description != null) {
            return false;
         }
      } else if (!this.description.equals(other.description)) {
         return false;
      }
      if (this.format == null) {
         if (other.format != null) {
            return false;
         }
      } else if (!this.format.equals(other.format)) {
         return false;
      }
      if (this.id != other.id) {
         return false;
      }
      if (this.isbn10 == null) {
         if (other.isbn10 != null) {
            return false;
         }
      } else if (!this.isbn10.equals(other.isbn10)) {
         return false;
      }
      if (this.isbn13 == null) {
         if (other.isbn13 != null) {
            return false;
         }
      } else if (!this.isbn13.equals(other.isbn13)) {
         return false;
      }
      if (this.publisher == null) {
         if (other.publisher != null) {
            return false;
         }
      } else if (!this.publisher.equals(other.publisher)) {
         return false;
      }
      if (this.subTitle == null) {
         if (other.subTitle != null) {
            return false;
         }
      } else if (!this.subTitle.equals(other.subTitle)) {
         return false;
      }
      if (this.subject == null) {
         if (other.subject != null) {
            return false;
         }
      } else if (!this.subject.equals(other.subject)) {
         return false;
      }
      if (this.title == null) {
         if (other.title != null) {
            return false;
         }
      } else if (!this.title.equals(other.title)) {
         return false;
      }
      return true;
   }
}
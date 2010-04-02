package com.totsp.bookworm.model;

import android.graphics.Bitmap;

import java.util.LinkedHashSet;
import java.util.Set;

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
   public Set<Author> authors;

   // user data (stored sep from book itself, not modeled sep though, for now)
   public boolean read;
   public int rating;
   ///public String blurb;

   // not stored in db or serialized
   // (optionally returned from parser, but not stored, image Ids are stored after processing)
   public transient Bitmap coverImage;

   public Book() {
      this.authors = new LinkedHashSet<Author>();
   }

   public Book(final String title) {
      if ((title == null) || (title.length() < 1)) {
         throw new IllegalArgumentException("Error, book must have a title (minimum size 1)");
      }
      this.authors = new LinkedHashSet<Author>();
      this.title = title;
   }

   @Override
   public String toString() {
      // this is the default display in a ListView, also used by Filter, etc, so make it significant/short/sweet
      return this.title;
   }

   @Override
   public boolean equals(final Object obj) {
      if (obj == this) {
         return true;
      }
      if (obj instanceof Book) {
         Book lhs = (Book) obj;
         if ((lhs.id == this.id) && (lhs.isbn10.equals(this.isbn10)) && (lhs.isbn13.equals(this.isbn13))
                  && (lhs.title.equals(this.title)) && (lhs.subTitle.equals(this.subTitle))
                  && (lhs.authors.equals(this.authors)) && (lhs.publisher.equals(this.publisher))
                  && (lhs.description.equals(this.description)) && (lhs.format.equals(this.format))
                  && (lhs.subject.equals(this.subject)) && (lhs.datePubStamp == this.datePubStamp)
                  && (lhs.rating == this.rating) && (lhs.read == this.read)) {
            return true;
         }
      }
      return false;
   }

   @Override
   public int hashCode() {
      int result = 31;
      result += this.id;
      if (this.isbn10 != null) {
         result += this.isbn10.hashCode();
      }
      if (this.isbn13 != null) {
         result += this.isbn13.hashCode();
      }
      if (this.title != null) {
         result += this.title.hashCode();
      }
      if (this.subTitle != null) {
         result += this.subTitle.hashCode();
      }
      if (this.authors != null) {
         result += this.authors.hashCode();
      }
      if (this.publisher != null) {
         result += this.publisher.hashCode();
      }
      if (this.description != null) {
         result += this.description.hashCode();
      }
      if (this.format != null) {
         result += this.format.hashCode();
      }
      if (this.subject != null) {
         result += this.subject.hashCode();
      }
      result += this.datePubStamp;
      result += this.rating;
      result += this.read ? 1 : 0;

      return result;
   }

   public String toStringFull() {
      StringBuilder sb = new StringBuilder();
      sb.append("Book-");
      sb.append("\n id:" + this.id);
      sb.append("\n title:" + this.title);
      sb.append("\n subTitle:" + this.subTitle);
      sb.append("\n isbn10:" + this.isbn10);
      sb.append("\n isbn13:" + this.isbn13);
      sb.append("\n authors:" + this.authors);
      sb.append("\n publisher:" + this.publisher);
      sb.append("\n description:" + this.description);
      sb.append("\n format:" + this.format);
      sb.append("\n subject:" + this.subject);
      sb.append("\n datePubStamp:" + this.datePubStamp);
      sb.append("\n read:" + this.read);
      sb.append("\n rating:" + this.rating);
      return sb.toString();
   }
}
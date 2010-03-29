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
   public String blurb;

   // not stored in db or serialized
   // (optionally returned from parser, but not stored, image Ids are stored after processing)
   public transient Bitmap coverImage;

   public Book() {
      this.authors = new LinkedHashSet<Author>();
   }

   public Book(String title) {
      if (title == null || title.length() < 1) {
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
      return sb.toString();
   }   
}
package com.totsp.bookworm.model;

import java.util.LinkedHashSet;
import java.util.Set;

// TODO save isbn 10 and isbn 13?
public final class Book {

   // give things explicit defaults, easier than null checks later for SQLite
   private long id = 0L;
   private String isbn = "";
   private String title = "";
   private long coverImageId = 0L;   
   private String publisher = ""; 
   private String description = ""; 
   private String format = ""; 
   private String subject = ""; 
   private String overviewUrl = "";    
   private long datePubStamp = 0L;
   private Set<Author> authors;

   public Book() {
      this.authors = new LinkedHashSet<Author>();
   }

   public Book(String isbn, String title) {
      if (isbn == null || isbn.length() < 4) {
         throw new IllegalArgumentException("Error, book must have an ISBN (minimum size 4)");
      } 
      if (title == null || title.length() < 1) {
         throw new IllegalArgumentException("Error, book must have a title (minimum size 1)");
      }
      this.authors = new LinkedHashSet<Author>();
      this.isbn = isbn;
      this.title = title;
   }

   @Override
   public String toString() {
      // this is the default display in a ListView, etc, so make it short/sweet
      return this.title;
   }
   
   public String toStringFull() {
      StringBuilder sb = new StringBuilder();
      sb.append("Book-");
      sb.append("\n id:" + this.id);
      sb.append("\n title:" + this.title);
      sb.append("\n isbn:" + this.isbn);
      sb.append("\n authors:" + this.authors);
      sb.append("\n publisher:" + this.publisher);
      sb.append("\n description:" + this.description);
      sb.append("\n format:" + this.format);
      sb.append("\n subject:" + this.subject);
      sb.append("\n coverImageId:" + this.coverImageId);
      sb.append("\n overviewUrl:" + this.overviewUrl);
      sb.append("\n datePubStamp:" + this.datePubStamp);
      return sb.toString();
   }

   public long getId() {
      return this.id;
   }

   public void setId(long id) {
      this.id = id;
   }

   public String getIsbn() {
      return this.isbn;
   }

   public void setIsbn(String isbn) {
      this.isbn = isbn;
   }

   public String getTitle() {
      return this.title;
   }

   public void setTitle(String title) {
      this.title = title;
   }

   public Set<Author> getAuthors() {
      return this.authors;
   }

   public void setAuthors(Set<Author> authors) {
      this.authors = authors;
   }

   public long getDatePubStamp() {
      return this.datePubStamp;
   }

   public void setDatePubStamp(long datePub) {
      this.datePubStamp = datePub;
   }

   public String getPublisher() {
      return this.publisher;
   }

   public void setPublisher(String publisher) {
      this.publisher = publisher;
   }

   public String getDescription() {
      return this.description;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   public String getFormat() {
      return this.format;
   }

   public void setFormat(String format) {
      this.format = format;
   }

   public String getSubject() {
      return this.subject;
   }

   public void setSubject(String subject) {
      this.subject = subject;
   }

   public String getOverviewUrl() {
      return this.overviewUrl;
   }

   public void setOverviewUrl(String overviewUrl) {
      this.overviewUrl = overviewUrl;
   }

   public long getCoverImageId() {
      return this.coverImageId;
   }

   public void setCoverImageId(long coverImageId) {
      this.coverImageId = coverImageId;
   }
}
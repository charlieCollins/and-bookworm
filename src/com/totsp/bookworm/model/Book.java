package com.totsp.bookworm.model;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public final class Book {

   private long id;
   private long coverImageId;  // not in DB
   private long coverImageThumbId;  // not in DB
   private String isbn;
   private String title;
   private Set<Author> authors;
   private String publisher; // not in DB
   private String description; // not in DB
   private String format; // not in DB
   private String subject; // not in DB
   private String overviewUrl; // not in DB
   private Date datePub;

   public Book() {
      this.authors = new HashSet<Author>();
   }

   public Book(String isbn, String title, Set<Author> authors, Date datePub) {
      this.id = 0L;
      this.isbn = isbn;
      this.title = title;
      this.authors = authors;
      this.datePub = datePub;
   }

   public String toString() {
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
      sb.append("\n coverImageThumbId:" + this.coverImageThumbId);
      sb.append("\n overviewUrl:" + this.overviewUrl);
      sb.append("\n datepub:" + this.datePub);
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

   public Date getDatePub() {
      return this.datePub;
   }

   public void setDatePub(Date datePub) {
      this.datePub = datePub;
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

   public long getCoverImageThumbId() {
      return this.coverImageThumbId;
   }

   public void setCoverImageThumbId(long coverImageThumbId) {
      this.coverImageThumbId = coverImageThumbId;
   }   
}
package com.totsp.bookworm.model;

import android.graphics.Bitmap;

import java.util.LinkedHashSet;
import java.util.Set;

public final class Book {

   // TODO get rid of getters/setters
   
   // give things explicit defaults, easier than null checks later for SQLite
   private long id = 0L;
   private String isbn10 = "";
   private String isbn13 = "";
   private String title = "";
   private String subTitle = "";   
   private String publisher = "";
   private String description = "";
   private String format = "";
   private String subject = "";
   private long datePubStamp = 0L;
   private Set<Author> authors;

   // user data (stored sep from book itself, not modeled sep though, for now)
   private boolean read;
   private int rating;
   private String blurb;

   // not stored in db or serialized
   // (optionally returned from parser, but not stored, image Ids are stored after processing)
   private transient String coverImageURL = "";
   private transient Bitmap coverImage;

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

   public long getId() {
      return this.id;
   }

   public void setId(long id) {
      this.id = id;
   }

   public String getIsbn10() {
      return this.isbn10;
   }

   public void setIsbn10(String isbn10) {
      if (isbn10 != null && isbn10.length() != 10) {
         throw new IllegalArgumentException("isbn10 must be 10 characters in length");
      }
      this.isbn10 = isbn10;
   }

   public String getIsbn13() {
      return this.isbn13;
   }

   public void setIsbn13(String isbn13) {
      if (isbn13 != null && isbn13.length() != 13) {
         throw new IllegalArgumentException("isbn13 must be 13 characters in length");
      }
      this.isbn13 = isbn13;
   }

   public String getTitle() {
      return this.title;
   }

   public void setTitle(String title) {
      this.title = title;
   }

   public String getSubTitle() {
      return this.subTitle;
   }

   public void setSubTitle(String subTitle) {
      this.subTitle = subTitle;
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

   public boolean isRead() {
      return this.read;
   }

   public void setRead(boolean read) {
      this.read = read;
   }

   public int getRating() {
      return this.rating;
   }

   public void setRating(int rating) {
      this.rating = rating;
   }

   public String getBlurb() {
      return this.blurb;
   }

   public void setBlurb(String blurb) {
      this.blurb = blurb;
   }

   // transient
   public String getCoverImageURL() {
      return this.coverImageURL;
   }

   public void setCoverImageURL(String coverImageURL) {
      this.coverImageURL = coverImageURL;
   }

   public Bitmap getCoverImage() {
      return this.coverImage;
   }

   public void setCoverImage(Bitmap coverImage) {
      this.coverImage = coverImage;
   }
}
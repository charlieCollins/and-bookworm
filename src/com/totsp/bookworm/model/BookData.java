package com.totsp.bookworm.model;

public final class BookData {
   
   private long id;
   private long bookId;
   private long rating;
   private String blurb;
   
   public BookData() {      
   }

   public long getId() {
      return this.id;
   }

   public void setId(long id) {
      this.id = id;
   }

   public long getBookId() {
      return this.bookId;
   }

   public void setBookId(long bookId) {
      this.bookId = bookId;
   }

   public long getRating() {
      return this.rating;
   }

   public void setRating(long rating) {
      this.rating = rating;
   }

   public String getBlurb() {
      return this.blurb;
   }

   public void setBlurb(String blurb) {
      this.blurb = blurb;
   }
}
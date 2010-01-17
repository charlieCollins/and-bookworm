package com.totsp.bookworm.model;

public final class BookData {
   
   private int id;
   private int bookId;
   private int rating;
   private String blurb;
   
   public BookData() {      
   }

   public int getId() {
      return this.id;
   }

   public void setId(int id) {
      this.id = id;
   }

   public int getBookId() {
      return this.bookId;
   }

   public void setBookId(int bookId) {
      this.bookId = bookId;
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
}
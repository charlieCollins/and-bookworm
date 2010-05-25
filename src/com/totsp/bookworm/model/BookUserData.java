package com.totsp.bookworm.model;

public final class BookUserData {

   // NOTE - no accessors/mutators by design, Android optimization

   public long id;
   public long bookId;
   public long rating;
   public boolean read;
   public String blurb;

   public BookUserData() {
   }

   public BookUserData(final long bookId, final long rating, final boolean read, final String blurb) {
      this.bookId = bookId;
      this.rating = rating;
      this.read = read;
      this.blurb = blurb;
   }

   @Override
   public boolean equals(final Object obj) {
      if (obj == this) {
         return true;
      }
      if (obj instanceof BookUserData) {
         BookUserData lhs = (BookUserData) obj;
         if ((lhs.id == id) && (lhs.bookId == bookId) && (lhs.rating == rating) && (lhs.read == read)
                  && (lhs.blurb.equals(blurb))) {
            return true;
         }
      }
      return false;
   }

   @Override
   public int hashCode() {
      int result = 31;
      result += id;
      result += bookId;
      result += rating;
      result += read ? 1 : 0;
      if (blurb != null) {
         result += blurb.hashCode();
      }
      return result;
   }
}
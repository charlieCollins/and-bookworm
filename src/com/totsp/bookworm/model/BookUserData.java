package com.totsp.bookworm.model;

/**
 * Defines user metadata structure for a single book.
 *
 */
public final class BookUserData {

   // NOTE - no accessors/mutators by design, Android optimization

   public long id;
   public long bookId;
   public long rating;
   public boolean own;
   public boolean lent;
   public boolean read;
   public String blurb;

   public BookUserData() {
   }

   public BookUserData(final long bookId, final long rating, final boolean own,
		               final boolean lent, final boolean read, final String blurb) {
      this.bookId = bookId;
      this.rating = rating;
      this.own = own;
      this.lent = lent;
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
         // id and bookId uniquely identify instance
         if ((lhs.id == id) && (lhs.bookId == bookId)) {
            return true;
         }
      }
      return false;
   }

   @Override
   public int hashCode() {
      int result = 31;
      
      // id and bookId uniquely identify instance
      // Note that the hashcode is only unique for 65534 books
      result += id << 16;
      result += bookId;

      return result;
   }
}
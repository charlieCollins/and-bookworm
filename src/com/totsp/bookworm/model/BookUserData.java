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
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((this.blurb == null) ? 0 : this.blurb.hashCode());
      result = prime * result + (int) (this.bookId ^ (this.bookId >>> 32));
      result = prime * result + (int) (this.id ^ (this.id >>> 32));
      result = prime * result + (int) (this.rating ^ (this.rating >>> 32));
      result = prime * result + (this.read ? 1231 : 1237);
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
      if (!(obj instanceof BookUserData)) {
         return false;
      }
      BookUserData other = (BookUserData) obj;
      if (this.blurb == null) {
         if (other.blurb != null) {
            return false;
         }
      } else if (!this.blurb.equals(other.blurb)) {
         return false;
      }
      if (this.bookId != other.bookId) {
         return false;
      }
      if (this.id != other.id) {
         return false;
      }
      if (this.rating != other.rating) {
         return false;
      }
      if (this.read != other.read) {
         return false;
      }
      return true;
   }
}
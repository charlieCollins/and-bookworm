package com.totsp.bookworm.model;

public final class Author {

   // NOTE - no accessors/mutators by design, Android optimization

   public long id;
   public String name;

   public Author() {
   }

   public Author(final String name) {
      id = 0L;
      this.name = name;
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("Author-");
      sb.append(" name:" + name);
      return sb.toString();
   }

   @Override
   public boolean equals(final Object obj) {
      if (obj == this) {
         return true;
      }
      if (obj instanceof Author) {
         Author lhs = (Author) obj;
         if ((lhs.id == id) && (lhs.name.equals(this.name))) {
            return true;
         }
      }
      return false;
   }

   @Override
   public int hashCode() {
      int result = 31;
      result += id;
      if (name != null) {
         result += name.hashCode();
      }
      return result;
   }
}
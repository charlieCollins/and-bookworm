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
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (int) (this.id ^ (this.id >>> 32));
      result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
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
      if (!(obj instanceof Author)) {
         return false;
      }
      Author other = (Author) obj;
      if (this.id != other.id) {
         return false;
      }
      if (this.name == null) {
         if (other.name != null) {
            return false;
         }
      } else if (!this.name.equals(other.name)) {
         return false;
      }
      return true;
   }
}
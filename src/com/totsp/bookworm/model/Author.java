package com.totsp.bookworm.model;

public final class Author {

   // NOTE - no accessors/mutators by design, Android optimization

   public long id;
   public String name;

   public Author() {
   }

   public Author(final String name) {
      this.id = 0L;
      this.name = name;
   }

   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("Author-");
      sb.append(" name:" + this.name);
      return sb.toString();
   }
}
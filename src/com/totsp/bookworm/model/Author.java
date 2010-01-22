package com.totsp.bookworm.model;


public final class Author {

   private long id;
   private String name;
   
   public Author() {
   }
   
   public Author(String name) {
      this.id = 0L;
      this.name = name;      
   }
   
   public String toString() {
      StringBuilder sb = new StringBuilder() ;
      sb.append("Author-");
      sb.append(" name:" + this.getName());
      return sb.toString();
   }

   public long getId() {
      return this.id;
   }

   public void setId(long id) {
      this.id = id;
   }

   public String getName() {
      return this.name;
   }

   public void setName(String name) {
      this.name = name;
   }   
}
package com.totsp.bookworm.model;


public final class Author {

   private long id;
   private String name;
   
   public Author() {
   }
   
   public Author(long id, String name) {
      this.id = id;
      this.name = name;      
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
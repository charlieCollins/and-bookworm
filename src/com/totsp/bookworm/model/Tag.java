package com.totsp.bookworm.model;

/**
 * Markup tag class.
 * Provides a simple text tag that can be applied to  multiple items.
 * @author Jason Liick
 */
public final class Tag {

   // NOTE - no accessors/mutators by design, Android optimization
   public long id;
   public String text;

   public Tag() {
   }

   public Tag(final String name) {
      id = 0L;
      this.text = name;
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("Tag-");
      sb.append(" name:" + text);
      return sb.toString();
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (int) (this.id ^ (this.id >>> 32));
      result = prime * result + ((this.text == null) ? 0 : this.text.hashCode());
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
      if (!(obj instanceof Tag)) {
         return false;
      }
      Tag other = (Tag) obj;
      if (this.id != other.id) {
         return false;
      }
      if (this.text == null) {
         if (other.text != null) {
            return false;
         }
      } else if (!this.text.equals(other.text)) {
         return false;
      }
      return true;
   }
}

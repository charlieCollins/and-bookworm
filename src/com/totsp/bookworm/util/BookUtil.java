package com.totsp.bookworm.util;

import com.totsp.bookworm.model.Author;
import com.totsp.bookworm.model.Book;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class BookUtil {

   private BookUtil() {
   }

   private static final Comparator<Author> AUTHOR_COMP = new Comparator<Author>() {
      public int compare(Author a, Author b) {
         return a.name.compareTo(b.name);
      }
   };
   
   public static boolean areBooksEffectiveDupes(final Book b1, final Book b2) {

      if (b1 != null && b2 == null) {
         return false;
      } else if (b1 == null && b2 != null) {
         return false;
      } else if (b1 != null && b2 != null) {

         String b1Title = b1.title;
         String b2Title = b2.title;
         List<Author> b1Authors = b1.authors;
         List<Author> b2Authors = b2.authors;
         // sort authors before we compare them
         // AbstractList.equals will check elements, but must be in same order
         Collections.sort(b1Authors, AUTHOR_COMP);        
         Collections.sort(b2Authors, AUTHOR_COMP);        

         if (b1Title != null && b2Title == null) {
            return false;
         } else if (b1Title == null && b2Title != null) {
            return false;
         } else if (b1Title != null && b2Title != null) {
            if (!b1Title.equals(b2Title)) {
               return false;
            } else {
               if (b1Authors != null && b2Authors == null) {
                  return false;
               } else if (b1Authors == null && b2Authors != null) {
                  return false;
               } else if (b1Authors != null && b2Authors != null) {
                  if (!b1Authors.equals(b2Authors)) {
                     return false;
                  } else {
                     return true;
                  }
               }
            }
         }
      }
      return false;
   }
}

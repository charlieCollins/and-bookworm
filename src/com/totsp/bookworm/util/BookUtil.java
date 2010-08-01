package com.totsp.bookworm.util;

import com.totsp.bookworm.model.Book;

public final class BookUtil {

   private BookUtil() {
   }

   public static boolean areBooksEffectiveDupes(final Book b1, final Book b2) {

      if ((b1 != null) && (b2 == null)) {
         return false;
      } else if ((b1 == null) && (b2 != null)) {
         return false;
      } else if ((b1 != null) && (b2 != null)) {

         String b1Title = b1.title;
         String b2Title = b2.title;
         String b1Authors = StringUtil.contractAuthors(b1.authors);
         String b2Authors = StringUtil.contractAuthors(b2.authors);

         if ((b1Title != null) && (b2Title == null)) {
            return false;
         } else if ((b1Title == null) && (b2Title != null)) {
            return false;
         } else if ((b1Title != null) && (b2Title != null)) {
            if (!b1Title.equals(b2Title)) {
               return false;
            } else {
               if ((b1Authors != null) && (b2Authors == null)) {
                  return false;
               } else if ((b1Authors == null) && (b2Authors != null)) {
                  return false;
               } else if ((b1Authors != null) && (b2Authors != null)) {
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

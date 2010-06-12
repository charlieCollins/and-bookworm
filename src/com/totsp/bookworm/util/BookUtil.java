package com.totsp.bookworm.util;

import com.totsp.bookworm.model.Book;

public final class BookUtil {

   private BookUtil() {
   }

   public static boolean areBooksEffectiveDupes(final Book b1, final Book b2) {
      boolean result = false;
      if (b1.title != null && b2.title != null && b1.title.equals(b2.title)
               && (b1.isbn10 != null && b1.isbn10.equals(b2.isbn10))
               || (b1.isbn13 != null && b1.isbn13.equals(b2.isbn13))) {
         result = true;
      } else if (b1.title != null && b2.title != null && b1.title.equals(b2.title) && b1.authors != null
               && b2.authors != null
               && StringUtil.contractAuthors(b1.authors).equals(StringUtil.contractAuthors(b2.authors))) {
         result = true;
      }
      return result;
   }
}

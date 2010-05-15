package com.totsp.bookworm.util;

import com.totsp.bookworm.model.Author;

import java.util.LinkedHashSet;
import java.util.Set;

public final class AuthorsStringUtil {

   public static LinkedHashSet<Author> expandAuthors(final String in) {
      LinkedHashSet<Author> authors = new LinkedHashSet<Author>();
      if (in.contains(",")) {
         String[] authorsArray = in.split(",\\s*");
         for (int i = 0; i < authorsArray.length; i++) {
            authors.add(new Author(authorsArray[i]));
         }
      } else {
         authors.add(new Author(in));
      }
      return authors;
   }

   public static String contractAuthors(final Set<Author> authors) {
      String result = null;
      if (authors.size() == 1) {
         result = authors.iterator().next().name;
      } else {
         int count = 0;
         for (Author a : authors) {
            if (count == 0) {
               result = a.name;
            } else {
               result += ", " + a.name;
            }
            count++;
         }
      }
      return result;
   }
   
   public static String addSpacesToCSVString(final String in) {
      StringBuilder sb = null;
      if (in.contains(",")) {
         sb = new StringBuilder();
         String[] authorsArray = in.split(",\\s*");
         for (int i = 0; i < authorsArray.length; i++) {
            if (i == 0) {
               sb.append(authorsArray[i]);
            } else {
               sb.append(" ," + authorsArray[i]);
            }
         }
      } else {
         return in;
      }
      return sb.toString();
   }

}

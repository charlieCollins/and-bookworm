package com.totsp.bookworm.util;

public final class OpenLibraryUtil {

   private static final String URL_PREFIX = "http://covers.openlibrary.org/b/isbn/"; 
   private static final String URL_SUFFIX = ".jpg";
   private static final String SMALL = "-S";
   private static final String MED = "-M";
   private static final String LARGE = "-L";
   
   // Because google books seems to not allow cover links unauthenticated, use another source.
   // OpenLibrary seems great - http://openlibrary.org/ - make them an Android app too ;).
   
   public static final String getCoverUrlSmall(String isbn) {
      return URL_PREFIX + isbn + SMALL + URL_SUFFIX;
   }
   
   public static final String getCoverUrlMedium(String isbn) {
      return URL_PREFIX + isbn + MED + URL_SUFFIX;
   }
   
   public static final String getCoverUrlLarge(String isbn) {
      return URL_PREFIX + isbn + LARGE + URL_SUFFIX;
   }  
}

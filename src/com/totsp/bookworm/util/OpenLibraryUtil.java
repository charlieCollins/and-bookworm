package com.totsp.bookworm.util;

public final class OpenLibraryUtil {

   private static final String URL_PREFIX = "http://covers.openlibrary.org/b/isbn/";
   private static final String URL_SUFFIX = ".jpg";
   private static final String SMALL = "-S";
   private static final String MED = "-M";
   private static final String LARGE = "-L";

   // Because google books seems to not allow cover links unauthenticated, use another source.
   // OpenLibrary seems great - http://openlibrary.org/ - make them an Android app too ;).

   public static final String getCoverUrlSmall(final String isbn) {
      return OpenLibraryUtil.URL_PREFIX + isbn + OpenLibraryUtil.SMALL + OpenLibraryUtil.URL_SUFFIX;
   }

   public static final String getCoverUrlMedium(final String isbn) {
      return OpenLibraryUtil.URL_PREFIX + isbn + OpenLibraryUtil.MED + OpenLibraryUtil.URL_SUFFIX;
   }

   public static final String getCoverUrlLarge(final String isbn) {
      return OpenLibraryUtil.URL_PREFIX + isbn + OpenLibraryUtil.LARGE + OpenLibraryUtil.URL_SUFFIX;
   }
}

package com.totsp.bookworm.util;

public final class CoverImageURLUtil {

   private static final String AZ_URL_PREFIX = "http://images.amazon.com/images/P/";
   private static final String AZ_URL_SUFFIX = ".01";
   private static final String OL_URL_PREFIX = "http://covers.openlibrary.org/b/isbn/";
   private static final String OL_URL_SUFFIX = ".jpg";
   //private static final String SMALL = "-S";
   private static final String MED = "-M";

   //private static final String LARGE = "-L";

   // Because google books seems to not allow cover links unauthenticated, use another source.
   // OpenLibrary seems great - http://openlibrary.org/ - make them an Android app too ;).

   private CoverImageURLUtil() {
   }

   public static final String getCoverUrlMedium(final String isbn, final int providerKey) {
      switch (providerKey) {
         case CoverImageUtil.COVER_IMAGE_PROVIDER_AMAZON:
            return CoverImageURLUtil.AZ_URL_PREFIX + isbn + CoverImageURLUtil.AZ_URL_SUFFIX;
         case CoverImageUtil.COVER_IMAGE_PROVIDER_OPENLIBRARY:
            return CoverImageURLUtil.OL_URL_PREFIX + isbn + CoverImageURLUtil.MED + CoverImageURLUtil.OL_URL_SUFFIX;
         default:
            return null;
      }
   }

}

package com.totsp.bookworm;

public final class Constants {

   public static final String LOG_TAG = "BookWorm";
   public static final String ISBN = "ISBN";
   public static final String TITLE = "TITLE";
   public static final String BOOK_ID = "BOOK_ID";
   public static final String DEFAULT_SORT_ORDER = "def_sort_order";
   
   private static boolean debugEnabled = false;

   private Constants() {
   }   

   public static boolean isDebugEnabled() {
      return debugEnabled;
   }
   
   static void setDebugEnabled(boolean value) {
      Constants.debugEnabled = value;
   }   
}

package com.totsp.bookworm.data;

import android.os.Environment;

public class DataConstants {

   private static final String APP_PACKAGE_NAME = "com.totsp.bookworm";
   private static final String EXTERNAL_DATA_DIR_NAME = "bookwormdata";
   public static final String DATABASE_NAME = "bookworm.db";
   public static final String DATABASE_PATH =
            Environment.getDataDirectory() + "/data/"
                     + DataConstants.APP_PACKAGE_NAME + "/databases/"
                     + DataConstants.DATABASE_NAME;
   public static final String EXTERNAL_DATA_PATH =
            Environment.getExternalStorageDirectory() + "/"
                     + DataConstants.EXTERNAL_DATA_DIR_NAME;

   public static final String BOOK_TABLE = "book";
   public static final String BOOKUSERDATA_TABLE = "bookuserdata";
   public static final String BOOKAUTHOR_TABLE = "bookauthor";
   public static final String AUTHOR_TABLE = "author";

   public static final String BOOKID = "bid";
   public static final String BOOKUSERDATAID = "budid";
   public static final String BOOKAUTHORID = "baid";
   public static final String BOOKLISTID = "blid";
   public static final String AUTHORID = "aid";
   public static final String ISBN10 = "isbn10";
   public static final String ISBN13 = "isbn13";
   public static final String TITLE = "tit";
   public static final String SUBTITLE = "subtit";
   public static final String DATEPUB = "datepub";
   public static final String NAME = "name";
   public static final String RATING = "rat";
   public static final String READSTATUS = "rstat";
   public static final String BLURB = "blurb";
   public static final String DESCRIPTION = "desc";
   public static final String PUBLISHER = "pub";
   public static final String FORMAT = "format";
   public static final String SUBJECT = "subject";

   private DataConstants() {
   }
}

package com.totsp.bookworm.util;

import android.os.Environment;

public final class ExternalStorageUtil {

   public static boolean isExternalStorageAvail() {
      return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
   }

   private ExternalStorageUtil() {
   }
}

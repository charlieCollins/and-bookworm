package com.totsp.bookworm.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public final class DateUtil {

   // repeat the DateFormat objects on purpose, they are not thread safe?
   private DateUtil() {
   }

   public static final Date parse(String s) {
      for (SimpleDateFormat format : DateUtil.getDateFormats()) {
         try {
            return format.parse(s);
         } catch (ParseException e) {
         }
      }
      return null;
   }

   public static final String format(Date d) {
      if (d != null) {
         for (SimpleDateFormat format : DateUtil.getDateFormats()) {
            String dateString = format.format(d);
            if (dateString != null && dateString.length() > 0) {
               return dateString;
            }
         }
      }
      return null;
   }

   private static final ArrayList<SimpleDateFormat> getDateFormats() {
      // add more formats here if needed, and yes, create every time, not thread safe
      ArrayList<SimpleDateFormat> formats = new ArrayList<SimpleDateFormat>(2);
      SimpleDateFormat dateFormat1 = new SimpleDateFormat("yyyy-MM-dd");
      SimpleDateFormat dateFormat2 = new SimpleDateFormat("yyyy");
      formats.add(dateFormat1);
      formats.add(dateFormat2);
      return formats;
   }

}

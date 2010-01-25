package com.totsp.bookworm.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class DateUtil {

   // repeat the DateFormat objects on purpose, they are not thread safe?
   private DateUtil() {      
   }
   
   public static final Date parse(String s) {
      SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
      try {
         return dateFormat.parse(s);
      } catch (ParseException e) {
      }
      return null;
   }

   public static final String format(Date d) {
      SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
      return dateFormat.format(d);
   }

}

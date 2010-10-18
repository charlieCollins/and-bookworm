package com.totsp.bookworm.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * DateUtils.
 * 
 * @author ccollins
 *
 */
public final class DateUtil {

   // NOTE - be careful with this, probably not thread safe
   private static final ArrayList<SimpleDateFormat> DATE_FORMATS = new ArrayList<SimpleDateFormat>(2);
   static {
      // add more formats here if needed, and yes, create every time, not thread safe      
      SimpleDateFormat dateFormat1 = new SimpleDateFormat("yyyy-MM-dd");
      SimpleDateFormat dateFormat2 = new SimpleDateFormat("yyyy");
      DateUtil.DATE_FORMATS.add(dateFormat1);
      DateUtil.DATE_FORMATS.add(dateFormat2);
   }

   private DateUtil() {
   }

   public static final Date parse(final String s) {
      for (SimpleDateFormat format : DateUtil.DATE_FORMATS) {
         try {
            return format.parse(s);
         } catch (ParseException e) {
         }
      }
      return null;
   }

   public static final String format(final Date d) {
      if (d != null) {
         for (SimpleDateFormat format : DateUtil.DATE_FORMATS) {
            String dateString = format.format(d);
            if ((dateString != null) && (dateString.length() > 0)) {
               return dateString;
            }
         }
      }
      return null;
   }
}

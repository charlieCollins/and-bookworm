package com.totsp.bookworm.util;

import android.util.Log;

import com.totsp.bookworm.Constants;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.channels.FileChannel;

/**
 * FileUtils. 
 * 
 * @author ccollins
 *
 */
public final class FileUtil {

   // Object for intrinsic lock (per docs 0 length array "lighter" than a normal Object
   public static final Object[] DATA_LOCK = new Object[0];
   
   private FileUtil() {
   }

   /**
    * Copy file.
    * 
    * @param src
    * @param dst
    * @throws IOException
    */
   public static void copyFile(final File src, final File dst) throws IOException {
      FileChannel inChannel = new FileInputStream(src).getChannel();
      FileChannel outChannel = new FileOutputStream(dst).getChannel();
      try {
         inChannel.transferTo(0, inChannel.size(), outChannel);
      } finally {
         if (inChannel != null) {
            inChannel.close();
         }
         if (outChannel != null) {
            outChannel.close();
         }
      }
   }
   
   /**
    * Replace entire File with contents of String.
    * 
    * @param fileContents
    * @param file
    * @return
    */
   public static boolean writeStringAsFile(final String fileContents, final File file) {
      boolean result = false;
      try {
         synchronized (DATA_LOCK) {
            if (file != null) {
               file.createNewFile(); // ok if returns false, overwrite
               Writer out = new BufferedWriter(new FileWriter(file), 1024);
               out.write(fileContents);
               out.close();   
               result = true;
            }
         }
      } catch (IOException e) {
         Log.e(Constants.LOG_TAG, "Error writing string data to file " + e.getMessage(), e);
      }
      return result;
   }
   
   /**
    * Append String to end of File.
    * 
    * @param appendContents
    * @param file
    * @return
    */
   public static boolean appendStringToFile(final String appendContents, final File file) {
      boolean result = false;
      try {
         synchronized (DATA_LOCK) {
            if (file != null && file.canWrite()) {
               file.createNewFile(); // ok if returns false, overwrite
               Writer out = new BufferedWriter(new FileWriter(file, true), 1024);
               out.write(appendContents);
               out.close();   
               result = true;
            }
         }
      } catch (IOException e) {
         Log.e(Constants.LOG_TAG, "Error appending string data to file " + e.getMessage(), e);
      }
      return result;
   }
}

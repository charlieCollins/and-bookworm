package com.totsp.bookworm.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.os.Environment;
import android.util.Log;

import com.totsp.bookworm.Constants;
import com.totsp.bookworm.model.Book;
import com.totsp.bookworm.util.CoverImageUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Util class to use Android external storage to store and retrieve images with
 * a key (title and id), and maintain and optional cache.
 * 
 * @author ccollins
 *
 */
public class ImageManager {

   private static final String IMAGES_LOCATION = "bookwormdata/images/";   
   
   public static BitmapFactory.Options options = new BitmapFactory.Options();
   static {      
      options.inInputShareable = true;
      options.inPurgeable = true;
      //options.inSampleSize = 16;
   }   

   public ImageManager(final Context context) {
   }

   public final Bitmap retrieveBitmap(final String title, final Long id, final boolean thumb) {
      String name = getNameKey(title, id);

      Bitmap bitmap = null;

      File exportDir = new File(Environment.getExternalStorageDirectory(), ImageManager.IMAGES_LOCATION);
      File file = null;
      if (!thumb) {
         file = new File(exportDir, name + ".jpg");
      } else {
         file = new File(exportDir, name + "-t.jpg");
      }

      if (file != null) {
         bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), ImageManager.options);
      }

      return bitmap;
   }

   public final void storeBitmap(final Bitmap source, final String title, final Long id) {
      String name = getNameKey(title, id);

      // M from OpenLibrary is about 180x225
      // I scale to 120x150      
      Bitmap bitmap = ImageManager.resizeBitmap(source, 120, 150);
      Bitmap bitmapThumb = ImageManager.resizeBitmap(source, 55, 70);

      try {
         File exportDir = new File(Environment.getExternalStorageDirectory(), ImageManager.IMAGES_LOCATION);
         if (!exportDir.exists()) {
            exportDir.mkdirs();
         }

         File noMedia = new File(exportDir.getAbsolutePath() + "/.nomedia");
         if (!noMedia.exists()) {
            noMedia.createNewFile();
         }

         FileOutputStream fos = null;

         File file = new File(exportDir, name + ".jpg");
         file.createNewFile(); // ok if returns false, overwrite
         fos = new FileOutputStream(file);
         bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
         fos.close();

         File fileThumb = new File(exportDir, name + "-t.jpg");
         fileThumb.createNewFile(); // ok if returns false, overwrite
         fos = new FileOutputStream(fileThumb);
         bitmapThumb.compress(Bitmap.CompressFormat.JPEG, 100, fos);
         fos.close();
      } catch (FileNotFoundException e) {
         // don't fail fast here, just swallow and log?
         e.printStackTrace();
      } catch (IOException e) {
         // don't fail fast here, just swallow and log?
         e.printStackTrace();
      }
   }

   public final void deleteBitmapSourceFile(final String title, final Long id) {
      String name = getNameKey(title, id);
      File exportDir = new File(Environment.getExternalStorageDirectory(), ImageManager.IMAGES_LOCATION);
      File file = new File(exportDir, name + ".jpg");
      File thumbFile = new File(exportDir, name + "-t.jpg");
      if ((file != null) && file.exists() && file.canWrite()) {
         file.delete();
      }
      if ((thumbFile != null) && thumbFile.exists() && thumbFile.canWrite()) {
         thumbFile.delete();
      }
   }

   public final void renameBitmapSourceFile(final String oldTitle, final String newTitle, final Long id) {
      String oldName = getNameKey(oldTitle, id);
      String newName = getNameKey(newTitle, id);
      File exportDir = new File(Environment.getExternalStorageDirectory(), ImageManager.IMAGES_LOCATION);
      File file = new File(exportDir, oldName + ".jpg");
      File thumbFile = new File(exportDir, oldName + "-t.jpg");
      if ((file != null) && file.exists() && file.canWrite()) {
         file.renameTo(new File(exportDir, newName + ".jpg"));
      }
      if ((thumbFile != null) && thumbFile.exists() && thumbFile.canWrite()) {
         thumbFile.renameTo(new File(exportDir, newName + "-t.jpg"));
      }
   }

   public final void clearAllBitmapSourceFiles() {
      File exportDir = new File(Environment.getExternalStorageDirectory(), ImageManager.IMAGES_LOCATION);
      if (exportDir.exists() && exportDir.canWrite()) {
         for (File f : exportDir.listFiles()) {
            f.delete();
         }
      }
   }

   public Bitmap getOrCreateCoverImage(final Book b) {
      Bitmap coverImageBitmap = null;

      String isbn = b.isbn10;
      if ((isbn == null) || isbn.equals("")) {
         isbn = b.isbn13;
      }

      // for now hard code cover image providers (later will be pref)
      // nwo it's OL, then AZ, then generate (in that order)
      if (isbn != null) {
         coverImageBitmap =
                  CoverImageUtil.getCoverImageFromNetwork(isbn, CoverImageUtil.COVER_IMAGE_PROVIDER_OPENLIBRARY);
         if (coverImageBitmap == null) {
            coverImageBitmap =
                     CoverImageUtil.getCoverImageFromNetwork(isbn, CoverImageUtil.COVER_IMAGE_PROVIDER_AMAZON);
         }
         if (coverImageBitmap == null) {
            coverImageBitmap = createCoverImage(b.title);
         }
      } else {
         coverImageBitmap = createCoverImage(b.title);
      }
      return coverImageBitmap;
   }

   public void resetCoverImage(final Book b) {
      this.deleteBitmapSourceFile(b.title, b.id);
      Bitmap coverImageBitmap = getOrCreateCoverImage(b);
      if (coverImageBitmap != null) {
         storeBitmap(coverImageBitmap, b.title, b.id);
      }
   }

   public Bitmap createCoverImage(final String title) {
      String[] words = title.split(" ");
      String line1 = parseLine(0, 15, words);
      String line2 = parseLine(line1.split(" ").length - 1, 15, words);
      String line3 = parseLine(line1.split(" ").length + line2.split(" ").length - 2, 15, words);
      String line4 =
               parseLine(line1.split(" ").length + line2.split(" ").length + line3.split(" ").length - 3, 12, words);
      if ((line1.split(" ").length + line2.split(" ").length + line3.split(" ").length + line4.split(" ").length) < words.length) {
         line4 += "...";
      }

      Bitmap bitmap = Bitmap.createBitmap(120, 183, Bitmap.Config.RGB_565);
      Canvas canvas = new Canvas(bitmap);
      canvas.drawARGB(100, 165, 42, 42);

      Paint paint = new Paint();
      paint.setTextSize(13);
      paint.setColor(Color.WHITE);
      paint.setAntiAlias(true);
      paint.setStyle(Style.FILL);

      canvas.drawText(line1, 3, 70, paint);
      if (line2.length() > 0) {
         canvas.drawText(line2, 3, 85, paint);
      }
      if (line3.length() > 0) {
         canvas.drawText(line3, 3, 100, paint);
      }
      if (line4.length() > 0) {
         canvas.drawText(line4, 3, 115, paint);
      }

      canvas.save();
      return bitmap;
   }

   private static final Bitmap resizeBitmap(final Bitmap source, final int width, final int height) {
      // create the matrix to scale it
      /*
      Matrix matrix = new Matrix();     
      float scaleX = width / source.getWidth();
      float scaleY = height / source.getHeight();
      matrix.setScale(scaleX, scaleY);
      return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
      */

      final int bitmapWidth = source.getWidth();
      final int bitmapHeight = source.getHeight();
      final float scale = Math.min((float) width / (float) bitmapWidth, (float) height / (float) bitmapHeight);
      final int scaledWidth = (int) (bitmapWidth * scale);
      final int scaledHeight = (int) (bitmapHeight * scale);
      return Bitmap.createScaledBitmap(source, scaledWidth, scaledHeight, true);
   }

   private String getNameKey(final String title, final Long id) {
      String key = title.replaceAll("\\W+", "_");
      key += "_" + id;
      return key;
   }

   // TODO clean up title line break util, make it more robust, this is fugly
   private String parseLine(final int wordStart, final int maxLineLength, final String[] words) {
      String line = "";
      try {
         if (((words != null) && (words.length > 0)) && (wordStart < words.length)) {
            for (int i = wordStart; i < words.length; i++) {
               try {
                  if (line == null) {
                     line = words[i];
                  } else {
                     line += " " + words[i];
                  }
                  if (line.length() >= maxLineLength - 2) {
                     break;
                  }
               } catch (ArrayIndexOutOfBoundsException e) {
                  // very rare error on line 232 above reported in market (has happened 4 times)
                  // must be some strange titles that are causing this (I have never been able to repro)
                  // if it happens, log it, and just return empty String to avoid FC?
                  Log
                           .i(Constants.LOG_TAG,
                                    "Error parsing title string into words, returning line up to this point.", e);
                  return line;
               }
            }
         }
      } catch (Exception e) {
         Log.e(Constants.LOG_TAG, "Error parsing title line, will return empty string (generated title will be wrong).");
      }
      return line;
   }
}

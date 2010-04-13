package com.totsp.bookworm.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.os.Environment;

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
public class DataImageHelper {

   private static final String IMAGES_LOCATION = "bookwormdata/images/";

   public DataImageHelper(final Context context) {
   }

   // TODO could be used (with change path name to .images above) to remove old images dir
   // in order to hide from gallery - but gallery 1.5+ expects it and deals with it
   // and logs errors about files missing if files deleted without also removing from ContentProvider
   // might be more trouble than worth to "hide" 
   /*
   public void copyOverImages() {
      // this method is used to copy over images from bookwormdata/images 
      // into bookwormdata/.images
      // this is needed to because 1.0.5 is changing the images path in order to hide them
      try {
         File newDir = new File(Environment.getExternalStorageDirectory(), DataImageHelper.IMAGES_LOCATION);
         if (!newDir.exists()) {
            newDir.mkdirs();
         }

         File oldDir = new File(Environment.getExternalStorageDirectory(), "bookwormdata/images/");
         if (oldDir.exists() && oldDir.canRead()) {
            for (File s : oldDir.listFiles()) {
               File d = new File(newDir.getAbsolutePath() + File.separator + s.getName());
               FileUtil.copyFile(s, d);
               s.delete();
            }
         }

         // delete old dir itself too
         oldDir.delete();

      } catch (IOException e) {
         Log.e(Constants.LOG_TAG, e.getMessage(), e);
      }
   }
   */

   public final Bitmap retrieveBitmap(final String title, final Long id, final boolean thumb) {
      String name = this.getNameKey(title, id);

      Bitmap bitmap = null;

      File exportDir = new File(Environment.getExternalStorageDirectory(), DataImageHelper.IMAGES_LOCATION);
      File file = null;
      if (!thumb) {
         file = new File(exportDir, name + ".jpg");
      } else {
         file = new File(exportDir, name + "-t.jpg");
      }

      if (file != null) {
         bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
      }

      return bitmap;
   }

   public final void storeBitmap(final Bitmap source, final String title, final Long id) {
      String name = this.getNameKey(title, id);

      // M from OpenLibrary is about 180x225
      // I scale to 120x150      
      Bitmap bitmap = DataImageHelper.resizeBitmap(source, 120, 150);
      Bitmap bitmapThumb = DataImageHelper.resizeBitmap(source, 55, 70);

      try {
         File exportDir = new File(Environment.getExternalStorageDirectory(), DataImageHelper.IMAGES_LOCATION);
         if (!exportDir.exists()) {
            exportDir.mkdirs();
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
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public final void deleteBitmapSourceFile(final String title, final Long id) {
      String name = this.getNameKey(title, id);
      File exportDir = new File(Environment.getExternalStorageDirectory(), DataImageHelper.IMAGES_LOCATION);
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
      String oldName = this.getNameKey(oldTitle, id);
      String newName = this.getNameKey(newTitle, id);
      File exportDir = new File(Environment.getExternalStorageDirectory(), DataImageHelper.IMAGES_LOCATION);
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
      File exportDir = new File(Environment.getExternalStorageDirectory(), DataImageHelper.IMAGES_LOCATION);
      if (exportDir.exists() && exportDir.canWrite()) {
         for (File f : exportDir.listFiles()) {
            f.delete();
         }
      }
   }

   public void resetCoverImage(final DataHelper dataHelper, final Book b) {
      // for now hard code provider to 2, OpenLibrary (future use pref here, etc, to establish)
      Bitmap coverImageBitmap = CoverImageUtil.retrieveCoverImage("2", b.isbn10);
      if (coverImageBitmap != null) {
         this.storeBitmap(coverImageBitmap, b.title, b.id);
      }
   }

   public Bitmap createCoverImage(final String title) {
      String[] words = title.split(" ");
      String line1 = this.parseLine(0, 15, words);
      String line2 = this.parseLine(line1.split(" ").length - 1, 15, words);
      String line3 = this.parseLine(line1.split(" ").length + line2.split(" ").length - 2, 15, words);
      String line4 =
               this.parseLine(line1.split(" ").length + line2.split(" ").length + line3.split(" ").length - 3, 12,
                        words);
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

   private String parseLine(final int wordStart, final int maxLineLength, final String[] words) {
      String line = "";
      if ((words != null) && (wordStart < words.length)) {
         for (int i = wordStart; i < words.length; i++) {
            if (line == null) {
               line = words[i];
            } else {
               line += " " + words[i];
            }
            if (line.length() >= maxLineLength - 2) {
               break;
            }
         }
      }
      return line;
   }
}

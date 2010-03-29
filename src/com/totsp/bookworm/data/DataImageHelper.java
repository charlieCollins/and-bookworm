package com.totsp.bookworm.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Environment;

import com.totsp.bookworm.R;
import com.totsp.bookworm.model.Book;
import com.totsp.bookworm.util.CacheMap;
import com.totsp.bookworm.util.CoverImageUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

/**
 * Util class to use Android external storage to store and retrieve images with
 * a key (title and id), and maintain and optional cache.
 * 
 * @author ccollins
 *
 */
public class DataImageHelper {

   private final HashMap<String, Bitmap> imageCache = new CacheMap<String, Bitmap>(250);
   private final Context context;

   private final boolean cacheEnabled;

   public DataImageHelper(final Context context, final boolean cacheEnabled) {
      this.context = context;
      this.cacheEnabled = cacheEnabled;
   }

   public void clearCache() {
      this.imageCache.clear();
   }

   public void clearCache(final String item) {
      this.imageCache.remove(item);
   }

   public final Bitmap retrieveBitmap(final String title, final Long id, final boolean thumb) {
      String name = this.getNameKey(title, id);

      if (this.cacheEnabled && this.imageCache.containsKey(name)) {
         return this.imageCache.get(name);
      }

      Bitmap bitmap = null;

      File exportDir = new File(Environment.getExternalStorageDirectory(), "bookwormdata/images/");
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
         File exportDir = new File(Environment.getExternalStorageDirectory(), "bookwormdata/images/");
         if (!exportDir.exists()) {
            exportDir.mkdirs();
         }

         FileOutputStream fos = null;

         File file = new File(exportDir, name + ".jpg");
         boolean created = file.createNewFile();
         if (created) {
            fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();
         }

         File fileThumb = new File(exportDir, name + "-t.jpg");
         boolean thumbCreated = fileThumb.createNewFile();
         if (thumbCreated) {
            fos = new FileOutputStream(fileThumb);
            bitmapThumb.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();
         }

         if (this.cacheEnabled && bitmap != null) {
            this.imageCache.put(name, bitmap);
         }

      } catch (FileNotFoundException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public final void deleteBitmapSourceFile(final String title, final Long id) {
      String name = this.getNameKey(title, id);
      File exportDir = new File(Environment.getExternalStorageDirectory(), "bookwormdata/images/");
      File file = new File(exportDir, name + ".jpg");
      File thumbFile = new File(exportDir, name + "-t.jpg");
      if (file != null && file.exists() && file.canWrite()) {
         file.delete();
      }
      if (thumbFile != null && thumbFile.exists() && thumbFile.canWrite()) {
         thumbFile.delete();
      }
   }

   public final void clearAllBitmapSourceFiles() {
      File exportDir = new File(Environment.getExternalStorageDirectory(), "bookwormdata/images/");
      if (!exportDir.exists()) {
         exportDir.mkdirs();
      }

      for (File f : exportDir.listFiles()) {
         f.delete();
      }
   }

   public void resetCoverImage(final DataHelper dataHelper, final String coverImageProviderKey, final Book b) {
      Bitmap coverImageBitmap = CoverImageUtil.retrieveCoverImage(coverImageProviderKey, b.isbn10);
      if (coverImageBitmap != null) {
         this.storeBitmap(coverImageBitmap, b.title, b.id);
      }
   }

   public Bitmap createCoverImage(final String title) {
      Bitmap bitmap = Bitmap.createBitmap(120, 183, Bitmap.Config.ARGB_4444);
      Canvas canvas = new Canvas(bitmap);
      Bitmap bkgrnd = BitmapFactory.decodeResource(this.context.getResources(), R.drawable.book_bgrnd_small);
      canvas.drawBitmap(bkgrnd, new Matrix(), null);
      Paint paint = new Paint();
      paint.setTextSize(13);
      paint.setAlpha(255);
      paint.setAntiAlias(true);

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

      canvas.drawText(line1, 3, 70, new Paint());
      if (line2.length() > 0) {
         canvas.drawText(line2, 3, 85, new Paint());
      }
      if (line3.length() > 0) {
         canvas.drawText(line3, 3, 100, new Paint());
      }
      if (line4.length() > 0) {
         canvas.drawText(line4, 3, 115, new Paint());
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

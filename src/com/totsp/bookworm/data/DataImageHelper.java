package com.totsp.bookworm.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import com.totsp.bookworm.Constants;
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
 * a String (title) key (and optional cache).
 * 
 * @author ccollins
 *
 */
public class DataImageHelper {

   private final HashMap<String, Bitmap> imageCache = new CacheMap<String, Bitmap>(200);
   private final Context context;

   private boolean cacheEnabled;

   public DataImageHelper(Context context, boolean cacheEnabled) {
      this.context = context;      
      this.cacheEnabled = cacheEnabled;
   }
   
   public void clearCache() {
      this.imageCache.clear();
   }

   public void clearCache(String item) {
      this.imageCache.remove(item);
   }

   // TODO we are keyed on title here - won't allow multiple books with same title, bad
   
   public final Bitmap retrieveBitmap(String nameInput, boolean thumb) {      
      String name = nameInput.replaceAll("\\s+", "_");  
      Log.d(Constants.LOG_TAG, "retrieving images using name (derived from title) " + name);
      
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
   
   public final void storeBitmap(Bitmap source, String nameInput) {      
      String name = nameInput.replaceAll("\\s+", "_");  
      Log.d(Constants.LOG_TAG, "storing images using name (derived from title) " + name);
      
      // M from OpenLibrary is about 180x225
      // I scale to 120x150      
      Bitmap bitmap = DataImageHelper.resizeBitmap(source, 120, 150);      
      Bitmap bitmapThumb = DataImageHelper.resizeBitmap(source, 55, 70);

      try {
         File exportDir = new File(Environment.getExternalStorageDirectory(), "bookwormdata/images/");
         if (!exportDir.exists()) {
            exportDir.mkdirs();
         }
         
         File file = new File(exportDir, name + ".jpg");         
         boolean created = file.createNewFile();         
         if (!created) {
            // file already exists, should we delete it and recreate it?
         }         
         FileOutputStream fos = new FileOutputStream(file);
         bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
         fos.close();
         
         File fileThumb = new File(exportDir, name + "-t.jpg");
         boolean createdThumb = fileThumb.createNewFile(); 
         if (!createdThumb) {
            // file already exists, should we delete it and recreate it?
         }    
         fos = new FileOutputStream(fileThumb);
         bitmapThumb.compress(Bitmap.CompressFormat.JPEG, 100, fos);
         fos.close();
         
         if (this.cacheEnabled) {
            this.imageCache.put(name, bitmap);
         }
         
      } catch (FileNotFoundException e) {
         e.printStackTrace();         
      } catch (IOException e) {
         e.printStackTrace();         
      }
   }
   
   private static final Bitmap resizeBitmap(Bitmap source, final int width, final int height) {
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


   public void resetCoverImage(DataHelper dataHelper, String coverImageProviderKey, Book b) {      
      Bitmap coverImageBitmap = CoverImageUtil.retrieveCoverImage(coverImageProviderKey, b.getIsbn10());      
      if (coverImageBitmap != null) {
         // TODO remove OLD images first?
         this.storeBitmap(coverImageBitmap, b.getTitle());         
      }
   }
}

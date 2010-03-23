package com.totsp.bookworm.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.Images.Media;
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
 * Util class to use Android built in ContentProvider to
 * store and retrieve images.
 * 
 * @author ccollins
 *
 */
public class DataImageHelper {

   private static final Uri IMAGES_URI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

   private HashMap<Integer, Bitmap> imageCache = new CacheMap<Integer, Bitmap>(200);

   private final Context context;
   private String bucketId;
   private String bucketDisplayName;
   private boolean privateStore;

   private boolean cacheEnabled;

   public DataImageHelper(Context context, String bucketId, String bucketDisplayName, boolean privateStore,
            boolean cacheEnabled) {
      this.context = context;
      this.bucketId = bucketId;
      this.bucketDisplayName = bucketDisplayName;
      this.privateStore = privateStore;
      this.cacheEnabled = cacheEnabled;
   }

   /*
   public Bitmap getBitmap(final int id) {

      if (this.cacheEnabled && this.imageCache.containsKey(id)) {
         return this.imageCache.get(id);
      }

      String[] projection = { MediaStore.MediaColumns.DATA };
      String selection = MediaStore.Images.Media._ID + "=" + id;
      Cursor c = null;
      String filePath = null;
      try {
         c = this.context.getContentResolver().query(IMAGES_URI, projection, selection, null, null);
         if (c != null) {
            c.moveToFirst();
            try {
               filePath = c.getString(0);
            } catch (Exception e) {
               // if user manually deletes images from SD, can cause exceptions
               Log.e(Constants.LOG_TAG, e.getMessage());
            }
         }
      } finally {
         if (c != null && !c.isClosed()) {
            c.close();
         }
      }

      Bitmap bitmap = null;
      if (filePath != null) {
         bitmap = BitmapFactory.decodeFile(filePath);
      }

      if (this.cacheEnabled) {
         this.imageCache.put(id, bitmap);
      }

      return bitmap;
   }
   */

   public void clearCache() {
      this.imageCache.clear();
   }

   public void clearCache(int id) {
      this.imageCache.remove(id);
   }

   /*
   public int saveBitmap(final String title, final Bitmap bitmap) {
      ContentValues values = new ContentValues();
      values.put(MediaColumns.TITLE, title);
      values.put(ImageColumns.BUCKET_DISPLAY_NAME, this.bucketDisplayName);
      values.put(ImageColumns.BUCKET_ID, this.bucketId);
      if (this.privateStore == true) {
         values.put(ImageColumns.IS_PRIVATE, 1);
      } else {
         values.put(ImageColumns.IS_PRIVATE, 0);
      }
      Uri uri = this.context.getContentResolver().insert(DataImageHelper.IMAGES_URI, values);
      int id = Integer.parseInt(uri.toString().substring(Media.EXTERNAL_CONTENT_URI.toString().length() + 1));

      ///this.saveStream(this.context, uri, bitmap);

      if (this.cacheEnabled) {
         this.imageCache.put(id, bitmap);
      }

      return id;
   }
   */

   // TODO we are keyed on title here - won't allow multiple books with same title, bad
   
   public static final Bitmap retrieveBitmap(Context context, String nameInput, boolean thumb) {
      
      String name = nameInput.replaceAll("\\s+", "_");  
      System.out.println("retrieving images using name (derived from title) " + name);
      
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
   
   public static final void storeBitmap(Context context, Bitmap source, String nameInput) {    
      
      String name = nameInput.replaceAll("\\s+", "_");  
      System.out.println("storing images using name (derived from title) " + name);
      
      // M from OpenLibrary is about 180x225
      // I scale to 120x150
      System.out.println("********** source - " + source + " width:" + source.getWidth() + " height:" + source.getHeight());
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

   /*
   public void resetCoverImage(DataHelper dataHelper, String coverImageProviderKey, Book b) {
      Bitmap coverImageBitmap = CoverImageUtil.retrieveCoverImage(coverImageProviderKey, b.getIsbn10());
      if (coverImageBitmap != null) {
         // TODO remove OLD images first?

         int imageId = this.saveBitmap(b.getTitle(), coverImageBitmap);
         b.setCoverImageId(imageId);

         // also save one really small for use in ListView - rather than scaling later
         Bitmap scaledBookCoverImage = CoverImageUtil.scaleAndFrame(coverImageBitmap, 55, 70);
         imageId = this.saveBitmap(b.getTitle() + "-T", scaledBookCoverImage);
         b.setCoverImageTinyId(imageId);
         dataHelper.updateBook(b);
      }
   }
   */
}

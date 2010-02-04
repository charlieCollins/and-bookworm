package com.totsp.bookworm.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.Images.Media;
import android.util.Log;

import com.totsp.bookworm.Constants;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
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

   private HashMap<Integer, Bitmap> imageCache = new HashMap<Integer, Bitmap>(1000);
   
   private final Context context;
   private String bucketId;
   private String bucketDisplayName;
   private boolean privateStore;

   public DataImageHelper(Context context, String bucketId, String bucketDisplayName, boolean privateStore) {
      this.context = context;
      this.bucketId = bucketId;
      this.bucketDisplayName = bucketDisplayName;
      this.privateStore = privateStore;
   }

   public Bitmap getBitmap(final int id) {
      
      if (this.imageCache.containsKey(id)) {
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
            filePath = c.getString(0);
            Log.d(Constants.LOG_TAG, "found image filePath - " + filePath);
         }
      } finally {
         if (c != null && !c.isClosed()) {
            c.close();
         }
      }

      Bitmap bitmap = null;
      if (filePath != null) {
         try {
            FileInputStream fis = new FileInputStream(filePath);
            bitmap = BitmapFactory.decodeStream(fis);
         } catch (IOException e) {
            Log.e("Constants.LOG_TAG", "", e);
         }
      }
      
      this.imageCache.put(id, bitmap);
      
      return bitmap;
   }
   
   public void clearCache() {
      this.imageCache.clear();
   }
   
   public void clearCache(int id) {
      this.imageCache.remove(id);
   }

   public int saveBitmap(final String title, final Bitmap bitmap) {
      ContentValues values = new ContentValues();
      values.put(MediaColumns.TITLE, title);
      values.put(ImageColumns.BUCKET_DISPLAY_NAME, this.bucketDisplayName);
      values.put(ImageColumns.BUCKET_ID, this.bucketId);
      if (this.privateStore == true) {
         values.put(ImageColumns.IS_PRIVATE, 0);
      } else {
         values.put(ImageColumns.IS_PRIVATE, 1);
      }
      Uri uri = this.context.getContentResolver().insert(DataImageHelper.IMAGES_URI, values);
      int imageId = Integer.parseInt(uri.toString().substring(Media.EXTERNAL_CONTENT_URI.toString().length() + 1));

      this.saveStream(this.context, uri, bitmap);
      return imageId;
   }

   private void saveStream(final Context context, final Uri uri, final Bitmap bitmap) {
      OutputStream os = null;
      try {
         os = context.getContentResolver().openOutputStream(uri);
         bitmap.compress(Bitmap.CompressFormat.JPEG, 50, os);
         os.close();
      } catch (FileNotFoundException e) {
         Log.e(Constants.LOG_TAG, e.toString());
      } catch (IOException e) {
         Log.e(Constants.LOG_TAG, e.toString());
      }
   }
}

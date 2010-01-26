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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

/**
 * 
 * 
 * @author ccollins
 *
 */
public class DataImageHelper {

   public static final String TAG = "DataImageHelper";

   private static final Uri IMAGES_URI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

   private final Context context;

   public DataImageHelper(final Context context) {
      this.context = context;
   }
   
   // TODO clean all this up - make sure it's private, etc
   
   public Bitmap getImage(final int id) {
      String[] projection = { MediaStore.MediaColumns.DATA };
      String selection = MediaStore.Images.Media._ID + "=" + id;
      Cursor c = null;
      String filePath = null;
      try {
         c = this.context.getContentResolver().query(IMAGES_URI, projection, selection, null, null);
         if (c != null) {
            c.moveToFirst();
            filePath = c.getString(0);
            Log.d(TAG, "found image filePath - " + filePath);
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
            Log.e("TotspTEST", "", e);
         }
      }
      return bitmap;
   }

   public int saveImage(final String title, final Bitmap bitmap) {

      // save full size bitmap
      ContentValues values = new ContentValues();
      values.put(MediaColumns.TITLE, title);
      values.put(ImageColumns.BUCKET_DISPLAY_NAME, "BookWorm-" + "ID");
      values.put(ImageColumns.BUCKET_ID, "BookWorm");
      values.put(ImageColumns.IS_PRIVATE, 0);
      values.put(MediaColumns.MIME_TYPE, "image/jpeg");
      Uri uri = this.context.getContentResolver().insert(DataImageHelper.IMAGES_URI, values);
      int imageId = Integer.parseInt(uri.toString().substring(Media.EXTERNAL_CONTENT_URI.toString().length() + 1));

      this.saveStream(this.context, uri, bitmap);

      return imageId;
   }

   private void saveStream(final Context context, final Uri uri, final Bitmap bitmap) {
      OutputStream os = null;
      try {
         os = context.getContentResolver().openOutputStream(uri);
         bitmap.compress(Bitmap.CompressFormat.JPEG, 70, os);
         os.close();
      } catch (FileNotFoundException e) {
         Log.e(DataImageHelper.TAG, e.toString());
      } catch (IOException e) {
         Log.e(DataImageHelper.TAG, e.toString());
      }
   }
}

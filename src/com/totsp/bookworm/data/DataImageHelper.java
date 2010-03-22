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
import com.totsp.bookworm.ManageData;
import com.totsp.bookworm.model.Book;
import com.totsp.bookworm.util.CacheMap;
import com.totsp.bookworm.util.CoverImageUtil;
import com.totsp.bookworm.util.FileUtil;

import java.io.File;
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

   /*
   public static final void insertImage(String imagePath, String name, String description) throws FileNotFoundException {
      FileInputStream stream = new FileInputStream(imagePath);
      try {
         insertImage(BitmapFactory.decodeFile(imagePath), name, description);
      } finally {
         try {
            stream.close();
         } catch (IOException e) {
         }
      }
   }

   private static final Bitmap storeThumbnail(Bitmap source, long id, float width, float height, int kind) {
      // create the matrix to scale it
      Matrix matrix = new Matrix();

      float scaleX = width / source.getWidth();
      float scaleY = height / source.getHeight();

      matrix.setScale(scaleX, scaleY);

      Bitmap thumb = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);

      ///Uri url = cr.insert(Images.Thumbnails.EXTERNAL_CONTENT_URI, values);

      
      try {
         ///OutputStream thumbOut = cr.openOutputStream(url);

         ///thumb.compress(Bitmap.CompressFormat.JPEG, 100, thumbOut);
         ///thumbOut.close();
         return thumb;
      } catch (FileNotFoundException ex) {
         return null;
      } catch (IOException ex) {
         return null;
      }
      
   }

   public static final void insertImage(Context context, Bitmap source, long bookId) {

      
      ContentValues values = new ContentValues();
      values.put(Images.Media.TITLE, title);
      values.put(Images.Media.DESCRIPTION, description);
      values.put(Images.Media.MIME_TYPE, "image/jpeg");
     

      File imageFile = new File(Environment.getExternalStorageDirectory() + "/bookwormdata/images/" + bookId + ".jpg");
      if (imageFile.exists()) {
         imageFile.delete();
      }

      try {
         imageFile.createNewFile();
         FileUtil.copyFile(dbBackupFile, dbFile);
         ManageData.this.application.getDataHelper().resetDbConnection();
         return null;
      } catch (IOException e) {
         Log.e(Constants.LOG_TAG, e.getMessage(), e);
         return e.getMessage();
      }

      try {
         /// url = cr.insert(EXTERNAL_CONTENT_URI, values);

         if (source != null) {
            
            OutputStream imageOut = cr.openOutputStream(url);
            try {
               source.compress(Bitmap.CompressFormat.JPEG, 50, imageOut);
            } finally {
               imageOut.close();
            }
           

            ///long id = ContentUris.parseId(url);
            ///Bitmap miniThumb = StoreThumbnail(source, id, 320F, 240F, Images.Thumbnails.MINI_KIND);
            ///Bitmap microThumb = StoreThumbnail(miniThumb, id, 50F, 50F, Images.Thumbnails.MICRO_KIND);
         } else {
           
            Log.e(TAG, "Failed to create thumbnail, removing original");
            cr.delete(url, null, null);
            url = null;
           
         }
      } catch (Exception e) {
         
         Log.e(TAG, "Failed to insert image", e);
         if (url != null) {
            cr.delete(url, null, null);
            url = null;
         }
         
         e.printStackTrace();
      }

      if (url != null) {
         stringUrl = url.toString();
      }

   }
   */

}

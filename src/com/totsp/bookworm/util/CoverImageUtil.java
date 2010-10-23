package com.totsp.bookworm.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.totsp.bookworm.Constants;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;

public final class CoverImageUtil {

   // note -- gbooks not implemented yet, needs OAuth login to allow cover images
   public static final int COVER_IMAGE_PROVIDER_GOOGLEBOOKS = 1;
   public static final int COVER_IMAGE_PROVIDER_OPENLIBRARY = 2;
   public static final int COVER_IMAGE_PROVIDER_AMAZON = 3;

   private CoverImageUtil() {
   }

   // FUTURE - pass in coverImageProvider key and use specified provider
   // right now hard coded to us OL first and fall through to OZ
   public static Bitmap getCoverImageFromNetwork(final String isbn, final int providerKey) {
      Bitmap coverImageBitmap = null;
      String imageUrl = CoverImageURLUtil.getCoverUrlMedium(isbn, providerKey);

      // TODO implement via HttpHelper and not URLConnection
      // NOTE - make sure this is called outside UI Thread
      BufferedInputStream bis = null;
      if ((imageUrl != null) && !imageUrl.equals("")) {
         try {
            URL url = new URL(imageUrl);
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            conn.connect();
            bis = new BufferedInputStream(conn.getInputStream(), 8192);
            coverImageBitmap = CoverImageUtil.decodeStream(bis);
            if ((coverImageBitmap != null) && (coverImageBitmap.getWidth() < 10)) {
               coverImageBitmap = null;
            }
         } catch (IOException e) {
            if (e instanceof SocketTimeoutException) {
               Log.i(Constants.LOG_TAG, "SocketTimeoutException retrieving cover image for URL:" + imageUrl);
            } else {
               Log.e(Constants.LOG_TAG, " ", e);
            }
         } finally {
            if (bis != null) {
               try {
                  bis.close();
               } catch (IOException e) {
                  // swallow
               }
            }
         }
      }
      return coverImageBitmap;
   }

   // taken from apps-for-android examples (not sure if this is ideal or not) 
   public static Bitmap scaleAndFrame(final Bitmap bitmap, final int width, final int height) {
      final int bitmapWidth = bitmap.getWidth();
      final int bitmapHeight = bitmap.getHeight();

      final float scale = Math.min((float) width / (float) bitmapWidth, (float) height / (float) bitmapHeight);

      final int scaledWidth = (int) (bitmapWidth * scale);
      final int scaledHeight = (int) (bitmapHeight * scale);

      final Bitmap decored = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true);

      /*
      final Canvas canvas = new Canvas(decored);
      final int offset = (int) (PHOTO_BORDER_WIDTH / 2);
      STROKE_PAINT.setAntiAlias(false);
      canvas.drawRect(offset, offset, scaledWidth - offset - 1, scaledHeight - offset - 1, STROKE_PAINT);
      STROKE_PAINT.setAntiAlias(true);
      */
      return decored;
   }

   // modified from http://stackoverflow.com/questions/477572/android-strange-out-of-memory-issue
   //decodes image and scales it to reduce memory consumption
   public static Bitmap decodeStream(InputStream is) {

      //Decode image size
      BitmapFactory.Options o = new BitmapFactory.Options();
      o.inJustDecodeBounds = true;
      o.inInputShareable = true;
      o.inPurgeable = true;
      BitmapFactory.decodeStream(is, null, o);

      //The new size we want to scale to
      final int REQUIRED_SIZE = 70;

      //Find the correct scale value. It should be the power of 2.
      int width_tmp = o.outWidth, height_tmp = o.outHeight;
      int scale = 1;
      while (true) {
         if (width_tmp / 2 < REQUIRED_SIZE || height_tmp / 2 < REQUIRED_SIZE)
            break;
         width_tmp /= 2;
         height_tmp /= 2;
         scale++;
      }

      //Decode with inSampleSize
      BitmapFactory.Options o2 = new BitmapFactory.Options();
      o2.inSampleSize = scale;
      return BitmapFactory.decodeStream(is, null, o2);
   }

   public static Bitmap decodeFile(File f) {
      FileInputStream fis = null;
      Bitmap bitmap = null;
      try {
         fis = new FileInputStream(f);
         bitmap = CoverImageUtil.decodeStream(fis);
      } catch (IOException e) {

      } finally {
         try {
            fis.close();
         } catch (IOException e) {            
         }
      }
      return bitmap;
   }
}

package com.totsp.bookworm.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.totsp.bookworm.Constants;
import com.totsp.bookworm.data.ImageManager;

import java.io.BufferedInputStream;
import java.io.IOException;
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
      if (imageUrl != null && !imageUrl.equals("")) {
         try {
            URL url = new URL(imageUrl);
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            conn.connect();
            bis = new BufferedInputStream(conn.getInputStream(), 8192);            
            coverImageBitmap = BitmapFactory.decodeStream(bis, null, ImageManager.options);
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
}

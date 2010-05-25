package com.totsp.bookworm.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.totsp.bookworm.Constants;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

public class CoverImageUtil {

   public static Bitmap retrieveCoverImage(final String coverImageProviderKey, final String isbn) {

      Bitmap coverImageBitmap = null;

      // TODO better book cover get stuff (HttpHelper binary)
      // book cover image
      String imageUrl = null;
      if (coverImageProviderKey.equals("1")) {
         // 1 should equal "default for handler" or such - regardless of current handler
         // 1 = Google Books 
         // 1 not supported right now, only using OpenLibrary, which doesn't require login
      } else if (coverImageProviderKey.equals("2")) {
         // 2 = OpenLibrary
         // M is about 180x225
         // I scale to 120x150
         imageUrl = OpenLibraryUtil.getCoverUrlMedium(isbn);
      }

      if ((imageUrl != null) && !imageUrl.equals("")) {
         try {
            URL url = new URL(imageUrl);
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(10000);
            conn.connect();
            BufferedInputStream bis = new BufferedInputStream(conn.getInputStream(), 8192);
            coverImageBitmap = BitmapFactory.decodeStream(bis);
            if ((coverImageBitmap != null) && (coverImageBitmap.getWidth() < 10)) {
               coverImageBitmap = null;
            }
         } catch (IOException e) {
            Log.e(Constants.LOG_TAG, " ", e);
         }
      }

      return coverImageBitmap;
   }

   ///private static final float PHOTO_BORDER_WIDTH = 3.0f;
   ///private static final Paint STROKE_PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);

   // taken from apps-for-android examples 
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

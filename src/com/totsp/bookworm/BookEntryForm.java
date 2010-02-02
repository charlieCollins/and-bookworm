package com.totsp.bookworm;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.totsp.bookworm.model.Author;
import com.totsp.bookworm.model.Book;

import java.io.IOException;
import java.util.LinkedHashSet;

// have to force landscape mode for Camera preview (see manifest)
// http://code.google.com/p/android/issues/detail?id=1193

public class BookEntryForm extends Activity {

   private BookWormApplication application;

   private EditText titleInput;
   private EditText authorInput;
   private Button saveButton;
   private android.view.SurfaceHolder surfaceHolder;
   private SurfaceView surfaceView;
   private Camera camera;
   private boolean previewRunning;
   private Bitmap picBitmap;

   Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
      @Override
      public void onPictureTaken(byte[] arg0, Camera arg1) {
         picBitmap = BitmapFactory.decodeByteArray(arg0, 0, arg0.length);
         Log.d(Constants.LOG_TAG, "picBitmap after camera jpegCallback - " + picBitmap);
         Log.d(Constants.LOG_TAG, "picBitmap size - " + picBitmap.getWidth() + " " + picBitmap.getHeight());
         new InsertBookTask().execute(titleInput.getText().toString(), authorInput.getText().toString());
      }
   };
   Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
      @Override
      public void onShutter() {
         // after image is captured, do sound here
      }
   };

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      this.application = (BookWormApplication) this.getApplication();

      setContentView(R.layout.bookentryform);

      this.titleInput = (EditText) this.findViewById(R.id.bookentryformtitleinput);
      this.authorInput = (EditText) this.findViewById(R.id.bookentryformauthorinput);
      this.saveButton = (Button) this.findViewById(R.id.bookentryformsavebutton);
      this.surfaceView = (SurfaceView) this.findViewById(R.id.bookentryformcamera);

      this.saveButton.setOnClickListener(new OnClickListener() {
         public void onClick(View v) {
            String title = titleInput.getText().toString();
            String authors = authorInput.getText().toString();
            if (title.length() < 1 || authors.length() < 1) {
               Log.d(Constants.LOG_TAG, "should show toast here?");
               Toast.makeText(BookEntryForm.this, "Title and author(s) are required", Toast.LENGTH_SHORT).show();
            } else {
               // on camera callback use local AsyncTask - see jpegCallback
               BookEntryForm.this.camera.takePicture(shutterCallback, null, jpegCallback);
            }
         }
      });

      this.surfaceHolder = this.surfaceView.getHolder();

      this.surfaceHolder.addCallback(new android.view.SurfaceHolder.Callback() {
         public void surfaceCreated(SurfaceHolder holder) {
            BookEntryForm.this.camera = Camera.open();            
         }

         public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            if (previewRunning) {
               camera.stopPreview();
            }
            
            try {
               camera.setPreviewDisplay(holder);
            } catch (IOException e) {
               Toast.makeText(BookEntryForm.this, "Error - " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
            

            Camera.Parameters params = camera.getParameters();
            params.setPreviewSize(480, 320); // required on G1 regardless?
            ///params.setPictureFormat(PixelFormat.YCbCr_420_SP);
            params.setPictureFormat(PixelFormat.JPEG);
            params.setPictureSize(240, 360);

            /*
            jpeg-thumbnail-width=512;
            antibanding-values=off;
            preview-frame-rate=15;
            preview-size=180x180;
            picture-format=jpeg;
            antibanding=off;
            jpeg-thumbnail-height=384;
            picture-size=42x60;
            effect=none;
            whitebalance=auto;
            jpeg-thumbnail-quality=90;
            jpeg-quality=100;
            whitebalance-values=auto,incandescent,florescent,daylight,cloudy,twilight,shade;
            preview-format=yuv420sp;
            effect-values=none,mono,negative,solarize,sepia,posterize,whiteboard,blackboard,aqua;
            picture-size-values=2048x1536,1600x1200,1024x768
            */
            ///params.set("jpeg-thumbnail-height", 60);
            ///params.set("jpeg-thumbnail-quality", 70);
            ///params.set("jpeg-quality", 70);
            ///params.set("picture-size-values", "42x60, 42x60, 42x60");

            camera.setParameters(params);
            camera.startPreview();
            previewRunning = true;
         }

         public void surfaceDestroyed(SurfaceHolder holder) {
            camera.stopPreview();            
            camera.release();
            camera = null;
            previewRunning = false;
         }
      });

      this.surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
   }

   @Override
   public void onStart() {
      super.onStart();
   }

   @Override
   public void onPause() {
      super.onPause();
   }

   @Override
   protected void onStop() {
      super.onStop();
   }

   @Override
   protected void onRestoreInstanceState(Bundle savedInstanceState) {
      super.onRestoreInstanceState(savedInstanceState);
   }

   @Override
   protected void onSaveInstanceState(Bundle saveState) {
      super.onSaveInstanceState(saveState);
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      ///menu.add(0, MENU_HELP, 0, "Help").setIcon(android.R.drawable.ic_menu_help);
      return super.onCreateOptionsMenu(menu);
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      /*
      switch (item.getItemId()) {
      case MENU_HELP:
         this.startActivity(new Intent(Main.this, Help.class));
         return true;
      }
      */
      return super.onOptionsItemSelected(item);
   }

   private class InsertBookTask extends AsyncTask<String, Void, Void> {
      private final ProgressDialog dialog = new ProgressDialog(BookEntryForm.this);

      private Book book;
      String title;
      LinkedHashSet<Author> authors;

      // can use UI thread here
      protected void onPreExecute() {
         this.dialog.setMessage("Saving book..");
         this.dialog.show();
      }

      // automatically done on worker thread (separate from UI thread)
      protected Void doInBackground(String... args) {
         book = new Book();
         book.setTitle(args[0]);

         authors = new LinkedHashSet<Author>();
         String[] authorsArray = args[1].split(",\\s*");
         for (int i = 0; i < authorsArray.length; i++) {
            Log.d(Constants.LOG_TAG, "authors i - " + authorsArray[i]);
            authors.add(new Author(authorsArray[i]));
         }
         book.setAuthors(authors);

         if (picBitmap != null) {
            Log.d(Constants.LOG_TAG, "picBitmap present in task, attempt image save");
            int imageId = application.getDataImageHelper().saveImage(title, picBitmap);
            Log.d(Constants.LOG_TAG, "imageId - " + imageId);
            book.setCoverImageId(imageId);
         } else {
            Log.d(Constants.LOG_TAG, "picBitmap NULL - async?");
         }

         // TODO check title already exists?
         long bookId = application.getDataHelper().insertBook(book);
         Log.d(Constants.LOG_TAG, "bookId - " + bookId);
         return null;
      }

      // can use UI thread here
      protected void onPostExecute(final Void unused) {
         this.dialog.dismiss();
         startActivity(new Intent(BookEntryForm.this, Main.class));
      }
   }
}
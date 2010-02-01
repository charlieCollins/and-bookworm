package com.totsp.bookworm;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import java.io.IOException;

// have to force landscape mode for Camera preview (see manifest)
// http://code.google.com/p/android/issues/detail?id=1193

public class BookEntryForm extends Activity {

   private EditText titleInput;
   private Button saveButton;
   private android.view.SurfaceHolder surfaceHolder;
   private SurfaceView surfaceView;
   private Camera camera;
   private boolean previewRunning;

   Camera.PictureCallback rawCallback = new Camera.PictureCallback() {
      @Override
      public void onPictureTaken(byte[] arg0, Camera arg1) {

      }
   };
   Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
      @Override
      public void onPictureTaken(byte[] arg0, Camera arg1) {

      }
   };
   Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
      @Override
      public void onShutter() {
         ///Camera.Parameters p = camera.getParameters();
         ///p.set("rotation", "90");
         ///camera.setParameters(p);
      }
   };

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.bookentryform);

      this.titleInput = (EditText) this.findViewById(R.id.bookentryformtitleinput);
      this.saveButton = (Button) this.findViewById(R.id.bookentryformsavebutton);
      this.surfaceView = (SurfaceView) this.findViewById(R.id.bookentryformcamera);

      //getWindow().setFormat(PixelFormat.TRANSLUCENT);
      ///requestWindowFeature(Window.FEATURE_NO_TITLE);
      ///getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
      ///WindowManager.LayoutParams.FLAG_FULLSCREEN);

      this.saveButton.setOnClickListener(new OnClickListener() {
         public void onClick(View v) {
            BookEntryForm.this.camera.takePicture(shutterCallback, rawCallback, jpegCallback);
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

            Camera.Parameters params = camera.getParameters();
            params.setPreviewSize(w, h);
            params.setPictureFormat(PixelFormat.JPEG);
            ///params.set("orientation", "portrait");
            camera.setParameters(params);

            try {
               camera.setPreviewDisplay(holder);
            } catch (IOException e) {
               e.printStackTrace();
            }

            camera.startPreview();
            previewRunning = true;
         }

         public void surfaceDestroyed(SurfaceHolder holder) {
            camera.stopPreview();
            previewRunning = false;
            camera.release();

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
}
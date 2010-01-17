package com.totsp.bookworm;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;

public class Splash extends Activity {

   protected static final String APP_NAME = "BookWorm";  

   /*
   private final Handler handler = new Handler() {
      public void handleMessage(final Message msg) {
         startActivity(new Intent(Splash.this, Main.class));
      }
   };
   */

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.splash);
   }

   @Override
   public void onStart() {
      super.onStart();
      
      /*
      new Thread() {
         @Override
         public void run() {
            handler.sendMessageDelayed(handler.obtainMessage(), 2000);
         };
      }.start();
      */
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
   public boolean onTouchEvent(MotionEvent e) {
      // any touch, move along
      if (e.getAction() == MotionEvent.ACTION_DOWN) {
         this.startActivity(new Intent(Splash.this, Main.class));
      }
      return true;
   }
}
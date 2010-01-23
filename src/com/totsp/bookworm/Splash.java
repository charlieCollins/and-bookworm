package com.totsp.bookworm;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;

public class Splash extends Activity {

   public static final String APP_NAME = "BookWorm";  

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.splash);
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
   public boolean onTouchEvent(MotionEvent e) {
      if (e.getAction() == MotionEvent.ACTION_DOWN) {
         this.startActivity(new Intent(Splash.this, Main.class));
      }
      return true;
   }
}
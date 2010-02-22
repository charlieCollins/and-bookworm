package com.totsp.bookworm;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MotionEvent;

public class Splash extends Activity {   

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.splash);
      this.checkSkip();      
   }
   
   private void checkSkip() {
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
      boolean skipSplash = prefs.getBoolean("splashcheckpref", false);
      if (skipSplash) {
         this.startActivity(new Intent(Splash.this, Main.class));
      }
   }
   
   @Override
   public boolean onTouchEvent(MotionEvent e) {
      if (e.getAction() == MotionEvent.ACTION_DOWN) {
         this.startActivity(new Intent(Splash.this, Main.class));
      }
      return true;
   }
}
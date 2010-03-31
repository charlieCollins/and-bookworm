package com.totsp.bookworm;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MotionEvent;

public class Splash extends Activity {

   @Override
   public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      this.setContentView(R.layout.splash);
      this.checkSkip();
   }

   private void checkSkip() {
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
      
      boolean splashSeenOnce = prefs.getBoolean("splashseenonce", false);
      if (!splashSeenOnce) {
         // set splash seen once so that default will skip the splash but user can still override
         Editor editor = prefs.edit();
         editor.putBoolean("splashseenonce", true);
         editor.commit();
      }
      
      boolean skipSplash = prefs.getBoolean("splashcheckpref", false);
      if (skipSplash && splashSeenOnce) {         
         this.startActivity(new Intent(Splash.this, Main.class));
      }            
   }

   @Override
   public boolean onTouchEvent(final MotionEvent e) {
      if (e.getAction() == MotionEvent.ACTION_DOWN) {
         this.startActivity(new Intent(Splash.this, Main.class));
      }
      return true;
   }
}
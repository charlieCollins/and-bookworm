package com.totsp.bookworm;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MotionEvent;

public class Splash extends Activity {

   private BookWormApplication application;

   @Override
   public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.splash);
      application = (BookWormApplication) getApplication();
      initPrefs();
   }

   private void initPrefs() {
      SharedPreferences prefs =
               PreferenceManager.getDefaultSharedPreferences(this);

      boolean debugEnabled = prefs.getBoolean("debugenabled", false);
      application.debugEnabled = debugEnabled;

      boolean splashSeenOnce = prefs.getBoolean("splashseenonce", false);
      if (!splashSeenOnce) {
         Editor editor = prefs.edit();
         editor.putBoolean("splashseenonce", true);
         editor.commit();
      }

      boolean showSplash = prefs.getBoolean("showsplash", false);
      if (!showSplash && splashSeenOnce) {
         startActivity(new Intent(Splash.this, Main.class));
      }
   }

   @Override
   public boolean onTouchEvent(final MotionEvent e) {
      if (e.getAction() == MotionEvent.ACTION_DOWN) {
         startActivity(new Intent(Splash.this, Main.class));
      }
      return true;
   }
}
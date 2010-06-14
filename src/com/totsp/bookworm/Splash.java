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
   private SharedPreferences prefs;

   @Override
   public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.splash);
      application = (BookWormApplication) getApplication();
      prefs = PreferenceManager.getDefaultSharedPreferences(this);
   }

   @Override
   public void onStart() {
      super.onStart();
      initPrefs();
   }

   private void initPrefs() {
      boolean debugEnabled = prefs.getBoolean("debugenabled", false);
      application.debugEnabled = debugEnabled;

      boolean splashSeenOnce = prefs.getBoolean("splashseenonce", false);
      if (!splashSeenOnce) {
         Editor editor = prefs.edit();
         editor.putBoolean("splashseenonce", true);
         editor.commit();
      }

      // TODO debug this, the JIT might somehow roll on past even when splash enabled?
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
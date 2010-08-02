package com.totsp.bookworm;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

import com.totsp.bookworm.data.CompoundDataSource;

public class Preferences extends PreferenceActivity {

   private BookWormApplication application;
   
   @Override
   public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      
      application = (BookWormApplication) getApplication();
      
      addPreferencesFromResource(R.layout.preferences);

      // listen to see if user changes data provider pref, if so reset provider class
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
      prefs.registerOnSharedPreferenceChangeListener(new OnSharedPreferenceChangeListener() {
         public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            if (key.equals("dataproviderpref")) {               
               String value = prefs.getString("dataproviderpref", CompoundDataSource.class.getCanonicalName());
               Log.i(Constants.LOG_TAG, "Data provider preference changed - " + value);
               if (!value.equals(application.bookDataSource.getClass().getCanonicalName())) {
                  application.establishBookDataSourceFromProvider();
               }
            }
         }
      });
   }

}
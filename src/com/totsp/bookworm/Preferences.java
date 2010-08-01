package com.totsp.bookworm;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class Preferences extends PreferenceActivity {

   @Override
   public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      addPreferencesFromResource(R.layout.preferences);
      
      
   }
   
   /*
    * Updates application preferences variables when the preference screen is closed.
    * (non-Javadoc)
    * @see android.preference.PreferenceActivity#onStop()
    */
   @Override
   public void onStop() {
	   ((BookWormApplication) getApplication()).updatePreferences();
	   super.onStop();
   }
}
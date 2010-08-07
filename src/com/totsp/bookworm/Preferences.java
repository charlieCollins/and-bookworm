package com.totsp.bookworm;

import com.totsp.bookworm.data.DataManager;
import com.totsp.bookworm.data.DataManager.TagSelectorBuilder;

import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

public class Preferences extends PreferenceActivity {

   Preference defaultTags;
   private TagSelectorBuilder tagDialog;		// Pop-up dialog to select default tags 
   
	
   @Override
   public void onCreate(final Bundle savedInstanceState) {
	   BookWormApplication application = (BookWormApplication)getApplication();

	   super.onCreate(savedInstanceState);
	   addPreferencesFromResource(R.layout.preferences);

	   // Must keep the reference to the builder rather than the dialog since the builder maintains the cursor which is
	   // used to populate the list.
	   tagDialog = application.dataManager.getTagSelectorBuilder(this, DataManager.DEFAULT_BOOK_TAGS_ID);

	   tagDialog.create();

	   defaultTags = (Preference) findPreference("prefdefaulttags");
	   defaultTags.setOnPreferenceClickListener(new OnPreferenceClickListener() {

		   @Override
		   public boolean onPreferenceClick(Preference preference) {
			   tagDialog.show();
			   return true;
		   }
	   });
      
      
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
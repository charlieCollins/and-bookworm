package com.totsp.bookworm;

import java.util.ArrayList;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.totsp.bookworm.BookSearch.BookSearchStateBean;
import com.totsp.bookworm.data.BookDataSource;
import com.totsp.bookworm.data.DataManager;
import com.totsp.bookworm.data.GoogleBookDataSource;
import com.totsp.bookworm.data.ImageManager;
import com.totsp.bookworm.model.Book;
import com.totsp.bookworm.model.Tag;

public class BookWormApplication extends Application {

   final static int ADD_MODE_SCAN = 0;
   final static int ADD_MODE_SEARCH = 1;
   final static int ADD_MODE_MANUAL = 2;
	
   boolean debugEnabled;
   boolean fastScanEnabled;
   int     defaultAddMode;
   boolean defaultReadEnabled;
   boolean scanOnCameraButton;        // Controls whether the camera button starts a scan
   ArrayList<Long> defaultTagIds;     // List of tags to be linked to new books by default

   
   SharedPreferences prefs;
   BookDataSource bookDataSource;
   DataManager dataManager;
   ImageManager imageManager;

   Book selectedBook;
   Tag selectedTag;
   boolean tagReorderingEnabled = false;       // Maintain re-ordering state for TagBatchList over orientation changes

   int lastMainListPosition;

   //ArrayList<Book> bookCacheList;
   // TODO use onRetainNonConfigurationInstance for quick config state/cache
   // for longer term cache use state bean relative to Activity referenced via application
   BookSearchStateBean bookSearchStateBean;
   

   @Override
   public void onCreate() {
      super.onCreate();
      if (debugEnabled) {
         Log.d(Constants.LOG_TAG, "APPLICATION onCreate");
      }

      prefs = PreferenceManager.getDefaultSharedPreferences(this);
      dataManager = new DataManager(this);
      imageManager = new ImageManager(this);

      establishBookDataSourceFromProvider();
   }

   private void establishBookDataSourceFromProvider() {
      // hard coded to one provider for now
      String className = prefs.getString("dataproviderpref", GoogleBookDataSource.class.getCanonicalName());
      Log.i(Constants.LOG_TAG, "establishing book data provider using class name - " + className);
      try {
         Class<?> clazz = Class.forName(className);
         // NOTE - validate that clazz is of BookDataSource type?
         bookDataSource = (BookDataSource) clazz.newInstance();
      } catch (ClassNotFoundException e) {
         Log.e(Constants.LOG_TAG, e.getMessage(), e);
         throw new RuntimeException("Error, umable to establish data provider. " + e.getMessage());
      } catch (IllegalAccessException e) {
         Log.e(Constants.LOG_TAG, e.getMessage(), e);
         throw new RuntimeException("Error, umable to establish data provider. " + e.getMessage());
      } catch (InstantiationException e) {
         Log.e(Constants.LOG_TAG, e.getMessage(), e);
         throw new RuntimeException("Error, umable to establish data provider. " + e.getMessage());
      }
   }
   
   /**
    * Updates all application preference variables on application startup or when
    * preferences are changed.
    */
   public void updatePreferences() {
	   debugEnabled = prefs.getBoolean("debugenabled", false);
	   fastScanEnabled = prefs.getBoolean("fastscanenabled", false);
	   defaultReadEnabled = prefs.getBoolean("defaultreadenabled", false);
	   defaultAddMode = Integer.parseInt(prefs.getString("addmodepref", String.valueOf(ADD_MODE_SCAN)));
	   scanOnCameraButton = prefs.getBoolean("camerabuttonscanenabled", false);
	   
	   // Default tags are stored in DB to ensure they stay synced with tags table.
	   defaultTagIds = dataManager.getDefaultTags();
   }
   
   // so that onSaveInstanceState/onRestoreInstanceState can use with just saved id
   public void establishSelectedBook(final long id) {
      selectedBook = dataManager.selectBook(id);
   }

      
   // so that onSaveInstanceState/onRestoreInstanceState can use with just saved id
   public void establishSelectedTag(final long id) {
      selectedTag = dataManager.selectTag(id);
   }

   
   @Override
   public void onTerminate() {
      // not guaranteed to be called
      dataManager.closeDb();
      selectedBook = null;
      super.onTerminate();
   }
}

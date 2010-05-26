package com.totsp.bookworm;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.totsp.bookworm.data.BookDataSource;
import com.totsp.bookworm.data.DataManager;
import com.totsp.bookworm.data.GoogleBookDataSource;
import com.totsp.bookworm.data.ImageManager;
import com.totsp.bookworm.model.Book;

import java.util.ArrayList;

public class BookWormApplication extends Application {

   boolean debugEnabled;

   SharedPreferences prefs;
   BookDataSource bookDataSource;
   DataManager dataManager;
   ImageManager imageManager;

   Book selectedBook;

   String lastSearchTerm;
   int lastSearchListPosition;
   int lastMainListPosition;
   ArrayList<Book> bookCacheList;

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

   // so that onSaveInstanceState/onRestoreInstanceState can use with just saved id
   public void establishSelectedBook(final long id) {
      selectedBook = dataManager.selectBook(id);
   }

   @Override
   public void onTerminate() {
      // not guaranteed to be called
      dataManager.closeDb();
      selectedBook = null;
      super.onTerminate();
   }
}

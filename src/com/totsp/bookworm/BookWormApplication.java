package com.totsp.bookworm;

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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class BookWormApplication extends Application {

   public boolean debugEnabled;

   SharedPreferences prefs;
   BookDataSource bookDataSource;
   DataManager dataManager;
   ImageManager imageManager;

   Book selectedBook;

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

   void establishBookDataSourceFromProvider() {
      // hard coded default provider (can change via prefs)
      String className = prefs.getString("dataproviderpref", GoogleBookDataSource.class.getCanonicalName());
      Log.i(Constants.LOG_TAG, "Establishing book data provider using class name - " + className);
      try {
         Class<?> clazz = Class.forName(className);
         // NOTE - validate that clazz is of BookDataSource type?
         Constructor<?> ctor = clazz.getConstructor(new Class[] { BookWormApplication.class });
         bookDataSource = (BookDataSource) ctor.newInstance(this);
      } catch (ClassNotFoundException e) {
         Log.e(Constants.LOG_TAG, e.getMessage(), e);
         throw new RuntimeException("Error, unable to establish data provider. " + e.getMessage());
      } catch (InvocationTargetException e) {
         Log.e(Constants.LOG_TAG, e.getMessage(), e);
         throw new RuntimeException("Error, unable to establish data provider. " + e.getMessage());
      } catch (NoSuchMethodException e) {
         Log.e(Constants.LOG_TAG, e.getMessage(), e);
         throw new RuntimeException("Error, unable to establish data provider. " + e.getMessage());
      } catch (IllegalAccessException e) {
         Log.e(Constants.LOG_TAG, e.getMessage(), e);
         throw new RuntimeException("Error, unable to establish data provider. " + e.getMessage());
      } catch (InstantiationException e) {
         Log.e(Constants.LOG_TAG, e.getMessage(), e);
         throw new RuntimeException("Error, unable to establish data provider. " + e.getMessage());
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

package com.totsp.bookworm;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.totsp.bookworm.data.DataHelper;
import com.totsp.bookworm.data.DataImageHelper;
import com.totsp.bookworm.data.IBookDataSource;
import com.totsp.bookworm.model.Book;

public class BookWormApplication extends Application {

   public static final String APP_NAME = "BookWorm";

   private static final boolean IMAGE_STORE_PRIVATE = true;
   private static final boolean IMAGE_CACHE_ENABLED = true;

   private IBookDataSource bookDataSource;
   private DataHelper dataHelper;
   private DataImageHelper dataImageHelper;

   private Book selectedBook;

   @Override
   public void onCreate() {
      super.onCreate();
      if (Constants.LOCAL_LOGV) {
         Log.v(Constants.LOG_TAG, "APPLICATION onCreate");
      }
      this.establishBookDataSourceFromProvider();
      this.dataHelper = new DataHelper(this);
      this.dataImageHelper =
               new DataImageHelper(this, "BookWorm", "BookWorm Cover Images", IMAGE_STORE_PRIVATE, IMAGE_CACHE_ENABLED);
   }

   private void establishBookDataSourceFromProvider() {
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
      String className = prefs.getString("dataproviderpref", "com.totsp.bookworm.data.GoogleBookDataSource");
      Log.i(Constants.LOG_TAG, "establishing book data provider using class name - " + className);
      try {
         Class<?> clazz = Class.forName(className);
         // TODO validate that clazz is of IBookDataSource type?
         this.bookDataSource = (IBookDataSource) clazz.newInstance();
      } catch (ClassNotFoundException e) {
         Log.e(Constants.LOG_TAG, e.getMessage(), e);
      } catch (IllegalAccessException e) {
         Log.e(Constants.LOG_TAG, e.getMessage(), e);
      } catch (InstantiationException e) {
         Log.e(Constants.LOG_TAG, e.getMessage(), e);
      }
   }

   @Override
   public void onTerminate() {
      // not guaranteed to be called?
      this.dataHelper.cleanup();
      this.selectedBook = null;
      super.onTerminate();
   }

   public IBookDataSource getBookDataSource() {
      return this.bookDataSource;
   }

   public void setBookDataSource(IBookDataSource bookDataSource) {
      this.bookDataSource = bookDataSource;
   }

   public DataHelper getDataHelper() {
      return this.dataHelper;
   }

   public void setDataHelper(DataHelper dataHelper) {
      this.dataHelper = dataHelper;
   }

   public DataImageHelper getDataImageHelper() {
      return this.dataImageHelper;
   }

   public void setDataImageHelper(DataImageHelper dataImageHelper) {
      this.dataImageHelper = dataImageHelper;
   }

   public Book getSelectedBook() {
      return this.selectedBook;
   }

   public void setSelectedBook(Book selectedBook) {
      this.selectedBook = selectedBook;
   }

   // so that onSaveInstanceState/onRestoreInstanceState can use with just saved title
   public void establishSelectedBook(String title) {
      this.selectedBook = this.dataHelper.selectBook(title);
   }
}

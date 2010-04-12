package com.totsp.bookworm;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.totsp.bookworm.data.DataHelper;
import com.totsp.bookworm.data.DataImageHelper;
import com.totsp.bookworm.data.IBookDataSource;
import com.totsp.bookworm.model.Book;

import java.util.ArrayList;

public class BookWormApplication extends Application {

   public static final String APP_NAME = "BookWorm";

   private static final boolean IMAGE_CACHE_ENABLED = true;

   private boolean debugEnabled;

   private SharedPreferences prefs;
   private IBookDataSource bookDataSource;
   private DataHelper dataHelper;
   private DataImageHelper dataImageHelper;

   private Book selectedBook;

   private String lastSearchTerm;
   private int lastSearchListPosition;
   private ArrayList<Book> bookCacheList;

   @Override
   public void onCreate() {
      super.onCreate();
      if (this.debugEnabled) {
         Log.d(Constants.LOG_TAG, "APPLICATION onCreate");
      }

      this.prefs = PreferenceManager.getDefaultSharedPreferences(this);
      this.dataHelper = new DataHelper(this);
      this.dataImageHelper = new DataImageHelper(this, BookWormApplication.IMAGE_CACHE_ENABLED);

      this.establishBookDataSourceFromProvider();
      
      // NOTE - after sufficient time REMOVE this
      this.dataImageHelper.copyOverImages();
   }

   private void establishBookDataSourceFromProvider() {

      String className = this.prefs.getString("dataproviderpref", "com.totsp.bookworm.data.GoogleBookDataSource");
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
      // not guaranteed to be called
      this.dataHelper.cleanup();
      this.selectedBook = null;
      super.onTerminate();
   }

   public IBookDataSource getBookDataSource() {
      return this.bookDataSource;
   }

   public void setBookDataSource(final IBookDataSource bookDataSource) {
      this.bookDataSource = bookDataSource;
   }

   public DataHelper getDataHelper() {
      return this.dataHelper;
   }

   public void setDataHelper(final DataHelper dataHelper) {
      this.dataHelper = dataHelper;
   }

   public DataImageHelper getDataImageHelper() {
      return this.dataImageHelper;
   }

   public void setDataImageHelper(final DataImageHelper dataImageHelper) {
      this.dataImageHelper = dataImageHelper;
   }

   public Book getSelectedBook() {
      return this.selectedBook;
   }

   public void setSelectedBook(final Book selectedBook) {
      this.selectedBook = selectedBook;
   }

   // so that onSaveInstanceState/onRestoreInstanceState can use with just saved title
   public void establishSelectedBook(final long id) {
      this.selectedBook = this.dataHelper.selectBook(id);
   }

   public ArrayList<Book> getBookCacheList() {
      return this.bookCacheList;
   }

   public void setBookCacheList(final ArrayList<Book> list) {
      this.bookCacheList = list;
   }

   public String getLastSearchTerm() {
      return this.lastSearchTerm;
   }

   public void setLastSearchTerm(final String lastSearchTerm) {
      this.lastSearchTerm = lastSearchTerm;
   }

   public int getLastSearchListPosition() {
      return this.lastSearchListPosition;
   }

   public void setLastSearchListPosition(final int lastSearchListPosition) {
      this.lastSearchListPosition = lastSearchListPosition;
   }

   public boolean isDebugEnabled() {
      return this.debugEnabled;
   }

   public void setDebugEnabled(final boolean value) {
      this.debugEnabled = value;
   }
}

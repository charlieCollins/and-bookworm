package com.totsp.bookworm;

import android.app.Application;
import android.util.Log;

import com.totsp.bookworm.data.DataHelper;
import com.totsp.bookworm.data.DataImageHelper;
import com.totsp.bookworm.model.Book;

public class BookWormApplication extends Application {

   public static final String APP_NAME = "BookWorm";  
   
   private DataHelper dataHelper;
   private DataImageHelper dataImageHelper;
   
   private Book selectedBook;   
   
   @Override
   public void onCreate() {
      super.onCreate();
      Log.d(Constants.LOG_TAG, "APPLICATION onCreate");
      this.dataHelper = new DataHelper(this);
      this.dataImageHelper = new DataImageHelper(this, "BookWorm", "BookWorm Cover Images", true);
   }
   
   @Override
   public void onTerminate() {
      Log.d(Constants.LOG_TAG, "APPLICATION onTerminate");
      // not guaranteed to be called?
      this.dataHelper.cleanup(); 
      this.selectedBook = null;
      super.onTerminate();      
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
   
   // so that onSaveInstanceState/onRestoreInstanceState can use with just saved isbn
   public void establishSelectedBook(String isbn) {
      this.selectedBook = this.dataHelper.selectBook(isbn);
   }
}

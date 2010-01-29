package com.totsp.bookworm;

import android.app.Application;

import com.totsp.bookworm.data.DataHelper;
import com.totsp.bookworm.data.DataImageHelper;
import com.totsp.bookworm.model.Book;

public class BookWormApplication extends Application {

   private DataHelper dataHelper;
   private DataImageHelper dataImageHelper;
   
   private Book selectedBook;
   
   @Override
   public void onCreate() {
      super.onCreate();
      this.dataHelper = new DataHelper(this);
      this.dataImageHelper = new DataImageHelper(this, "BookWorm", "BookWorm Cover Images", true);
   }
   
   @Override
   public void onTerminate() {
      // not guaranteed to be called?
      this.dataHelper.cleanup();      
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
}

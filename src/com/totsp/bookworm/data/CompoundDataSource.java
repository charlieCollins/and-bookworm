package com.totsp.bookworm.data;

import android.content.res.Resources.NotFoundException;
import android.util.Log;

import com.totsp.bookworm.BookWormApplication;
import com.totsp.bookworm.Constants;
import com.totsp.bookworm.R;
import com.totsp.bookworm.model.Book;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

/**
 * The Compound Data Source loops through available 
 * data sources to return matching books.
 * 
 * @author Simon McLaughlin
 */
public class CompoundDataSource implements BookDataSource {

   private final BookWormApplication application;
   private final ArrayList<BookDataSource> dataSources = new ArrayList<BookDataSource>(2);

   public CompoundDataSource(final BookWormApplication application) {
      this.application = application;

      String[] dataProviders = null;
      try {
         dataProviders = this.application.getResources().getStringArray(R.array.bookdataproviderkeys);
      } catch (NotFoundException e) {
         Log.e(Constants.LOG_TAG, e.getMessage(), e);
      }

      for (int i = 0; i < dataProviders.length; i++) {
         String className = dataProviders[i].toString();
         if (!className.equalsIgnoreCase(CompoundDataSource.class.getCanonicalName())) {
            Log.i(Constants.LOG_TAG, "establishing sub book data provider for compound source using class name - "
                     + className);
            try {
               Class<?> clazz = Class.forName(className);
               Constructor<?> ctor = clazz.getConstructor(new Class[] { BookWormApplication.class });
               // NOTE - validate that clazz is of BookDataSource type?              
               BookDataSource dataSource = (BookDataSource) ctor.newInstance(this.application);
               dataSources.add(dataSource);
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
      }
   }

   public Book getBook(final String identifier) {
      Book book = null;
      for (BookDataSource dataSource : dataSources) {
         book = dataSource.getBook(identifier);
         if (book != null) {
            break;
         }
      }
      return book;
   }

   public ArrayList<Book> getBooks(final String searchTerm, final int startIndex) {
      ArrayList<Book> books = new ArrayList<Book>();
      for (BookDataSource dataSource : dataSources) {
         ArrayList<Book> sourceBooks = dataSource.getBooks(searchTerm, startIndex);
         if ((sourceBooks != null) && !sourceBooks.isEmpty()) {
            for (Book book : sourceBooks) {
               if (!books.contains(book)) {
                  books.add(book);
               }
            }
         }
      }
      return books;
   }
}

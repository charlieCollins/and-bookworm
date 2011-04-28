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
import java.util.HashMap;
import java.util.Map;

/**
 * The Compound Data Source loops through available 
 * data sources to return matching books.
 * 
 * @author Simon McLaughlin
 */
public class CompoundDataSource implements BookDataSource {

   private final BookWormApplication application;
   private final ArrayList<BookDataSource> dataSources = new ArrayList<BookDataSource>();

   public CompoundDataSource(final BookWormApplication application) {
      this.application = application;

      String[] dataProvidersArray = null;
      try {
         dataProvidersArray = this.application.getResources().getStringArray(R.array.bookdataproviderkeys);
      } catch (NotFoundException e) {
         Log.e(Constants.LOG_TAG, e.getMessage(), e);
      }

      for (int i = 0; i < dataProvidersArray.length; i++) {
         String className = dataProvidersArray[i].toString();
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
   
   public ArrayList<Book> getBooks(final String searchTerm, final int startIndex, final int numResults) {      
      // for each data source, get up to startIndex/num data sources results (so overall total is correct)
      int numResultsPerDataSource = (int) numResults/dataSources.size();
      int startIndexPerDataSource = 0;
      if (startIndex > 0) {
         startIndexPerDataSource = (int) startIndex/dataSources.size();
      }      
      // store results in map, per data source, so we can put them together in relevant order at end
      // (overall we want result 1 from each provider, then result 2 from each provider, etc, not all from 1, then all from 2)
      // TODO not sure this logic is perfect, if dupes are found startIndex/etc might not be correct, empirically works, but needs review
      HashMap<String, ArrayList<Book>> resultsMap = new HashMap<String, ArrayList<Book>>();      
      for (BookDataSource dataSource : dataSources) {
         ArrayList<Book> books = dataSource.getBooks(searchTerm, startIndexPerDataSource, numResultsPerDataSource);
         resultsMap.put(dataSource.getClass().getCanonicalName(), books);
      }      
      return this.flattenResultsMap(resultsMap);
   }
   
   private ArrayList<Book> flattenResultsMap(final HashMap<String, ArrayList<Book>> resultsMap) {
      ArrayList<Book> books = new ArrayList<Book>();
      // get the largest size of list from any provider, use that as starting point
      int size = 0;
      for (Map.Entry<String, ArrayList<Book>> entry : resultsMap.entrySet()) {
         if (entry.getValue() != null && entry.getValue().size() > size) {
            size = entry.getValue().size();
         }
      }      
      // loop over size and pull element from each List in Map
      // (this could be optimized, for instance just use map.values and split at size, but for now this is simple and effective
      // and not a bottleneck)
      for (int i = 0; i < size; i++) {
         for (Map.Entry<String, ArrayList<Book>> entry : resultsMap.entrySet()) {
            if (entry.getValue() != null && entry.getValue().size() >= size) {
               books.add(entry.getValue().get(i));
            }
         }
      }      
      return books;
   }
}

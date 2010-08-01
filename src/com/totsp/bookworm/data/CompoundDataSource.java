package com.totsp.bookworm.data;

import java.util.ArrayList;
import java.util.Iterator;

import android.content.res.Resources.NotFoundException;
import android.util.Log;

import com.totsp.bookworm.BookWormApplication;
import com.totsp.bookworm.Constants;
import com.totsp.bookworm.R;
import com.totsp.bookworm.model.Book;

/**
 * The Compound Data Source loops through available 
 * data sources to return matching books.
 * 
 * @author Simon McLaughlin
 */
public class CompoundDataSource implements BookDataSource {
	
	public BookWormApplication appContext;
	private boolean debugEnabled;
	private ArrayList<Book> masterBooks = new ArrayList<Book>();
	
	public CompoundDataSource() {
		
	}

	public Book getBook(String identifier) {
		String[] dataProviders = null;
		try {
			dataProviders = appContext.getResources().getStringArray(R.array.bookdataproviderkeys);
		} catch (NotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for(int i = 0; i < dataProviders.length; i++)
		{
			String className = dataProviders[i].toString();
			BookDataSource bookDataSource;
			if(!className.equalsIgnoreCase("com.totsp.bookworm.data.CompoundDataSource"))
			{
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
			      
			      bookDataSource.setDebugEnabled(debugEnabled);
			      Book book = bookDataSource.getBook(identifier);
			      if(book != null)
			      {
			    	  return book;
			      }
			}
		}
		
		return null;
	}

	public ArrayList<Book> getBooks(String searchTerm, int startIndex) {
		String[] dataProviders = null;
		try {
			dataProviders = appContext.getResources().getStringArray(R.array.bookdataproviderkeys);
		} catch (NotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for(int i = 0; i < dataProviders.length; i++)
		{
			String className = dataProviders[i].toString();
			BookDataSource bookDataSource;
			if(!className.equalsIgnoreCase("com.totsp.bookworm.data.CompoundDataSource"))
			{
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
			      
			      bookDataSource.setDebugEnabled(debugEnabled);
			      ArrayList<Book> books = bookDataSource.getBooks(searchTerm, startIndex);
			      if ((books != null) && !books.isEmpty()) {
				      Iterator booksIterator = books.iterator();
				      while(booksIterator.hasNext()) {
					      Book book = (Book) booksIterator.next();
					      if(!masterBooks.contains(book)) {
					    	  masterBooks.add(book);	
					      }
				      }
			      }
			}
			
		}
		
		if ((masterBooks != null) && !masterBooks.isEmpty()) {
			return masterBooks;
		}
		return null;
	}

	public void setDebugEnabled(boolean debugEnabled) {
		this.debugEnabled = debugEnabled;

	}

	public void setContext(BookWormApplication appContext) {
		this.appContext = appContext;
		
	}

}

package com.totsp.bookworm.data.dao;

import android.database.Cursor;

import java.util.List;

/**
 * Interface to enforce DAO pattern. This is *not* intended to look
 * or feel like server side DAOs, rather it's a simple pattern 
 * for use on Android that helps separate local database operations
 * into classes for separate entities (abstractions) for consitency and 
 * maintainability, etc. 
 
 * Note, implementation references should be used directly, not the 
 * interface, as an Android optimization.
 * 
 * @author ccollins
 *
 * @param <T>
 */
public interface DAO<T> {

   // return a Cursor that can be used for Android ListViews
   public Cursor getCursor(final String orderBy, final String whereClauseLimit);
   
   public T select(final long id);

   // TODO prefer array over collections
   public List<T> selectAll();

   public long insert(final T entity);

   public void update(final T entity);

   public void delete(final long id);
}

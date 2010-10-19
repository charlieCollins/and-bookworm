package com.totsp.bookworm.data.dao;

import android.database.Cursor;

import java.util.List;

/**
 * Interface to enforce DAO pattern. This is *not* intended to look
 * or feel like server side DAOs, rather it's a simple pattern 
 * for use on Android that helps separate local database operations
 * into classes for separate entities (abstractions) for consistency and 
 * maintainability, etc. 
 
 * Note, implementation references should be used directly, not the 
 * interface, as an Android optimization.
 * 
 * @author ccollins
 *
 * @param <T>
 */
public interface DAO<T> {

   /**
    * Invoked when database is created, should create the table
    * structure for this DAO. 
    * 
    * @param db
    */
   ///void static onCreate(final SQLiteDatabase db);

   /**
    * Invoked when the database is upgraded, should migrate the 
    * data structure (if necessary) for this DAO from one version
    * to the next.
    * 
    * @param db
    * @param oldVersion
    * @param newVersion
    */
   ///void static onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion);

   /**
    * Delete all data from the underlying table(s).
    * 
    */
   void deleteAll();

   /**
    * Return a Cursor representing the data underlying this DAO 
    * that can be used by Android ListView widgets.
    * 
    * @param orderBy
    * @param whereClauseLimit
    * @return
    */
   Cursor getCursor(final String orderBy, final String whereClauseLimit);

   /**
    * Select entity.
    * 
    * @param id
    * @return
    */
   T select(final long id);

   /**
    * Select all entities.
    * 
    * @return
    */
   List<T> selectAll();

   /**
    * Insert entity.
    * 
    * @param entity
    * @return
    */
   long insert(final T entity);

   /**
    * Update entity.
    * 
    * @param entity
    */
   void update(final T entity);

   /**
    * Delete entity.
    * 
    * @param id
    */
   void delete(final long id);
}

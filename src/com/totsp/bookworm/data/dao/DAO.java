package com.totsp.bookworm.data.dao;

import java.util.List;

/**
 * Interface to enforce DAO pattern.
 * Note, implementation references should be used directly, not the 
 * interface, as an Android optimization.
 * 
 * @author ccollins
 *
 * @param <T>
 */
public interface DAO<T> {

   public T select(final long id);

   // TODO prefer array over collections
   public List<T> selectAll();

   public long insert(final T entity);

   public void update(final T entity);

   public void delete(final long id);

}

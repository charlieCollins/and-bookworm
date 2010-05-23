package com.totsp.bookworm.data.dao;

import java.util.List;

public interface DAO<T> {

	public T select(final long id);
	// TODO prefer array over collections
	public List<T> selectAll();
	public long insert(final T entity);
	public void update(final T entity);
	public void delete(final long id);
	
}

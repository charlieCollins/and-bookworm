package com.totsp.bookworm.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Private extension of LinkedHashMap that overrides removeEldestEntry
 * to allow the cache to operate in LRU fashion.
 *
 * @author ccollins
 *
 * @param <T>
 * @param <K>
 */
public class CacheMap<T, K> extends LinkedHashMap<T, K> {

   private static final long serialVersionUID = 1L;
   private final int cacheSize;

   public CacheMap(final int cacheSize) {
      super();
      this.cacheSize = cacheSize;
   }

   @Override
   protected boolean removeEldestEntry(final Map.Entry<T, K> arg) {
      return this.size() > this.cacheSize;
   }
}

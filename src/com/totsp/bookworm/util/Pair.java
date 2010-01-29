package com.totsp.bookworm.util;

/**
 * 
 * @author ccollins
 *
 * @param <F>
 * @param <S>
 */
public class Pair<F, S> {

   private F first;
   private S second;

   public Pair(F first, S second) {
      this.first = first;
      this.second = second;
   }

   public F getFirst() {
      return this.first;
   }

   public S getSecond() {
      return this.second;
   }

   @Override
   public int hashCode() {
      if (first == null) {
         return (second == null) ? 0 : second.hashCode() + 1;
      }
      else if (second == null) {
         return first.hashCode() + 2;
      }
      return first.hashCode() * 17 + second.hashCode();
   }

   private boolean equals(Object x, Object y) {
      return (x == null && y == null) || (x != null && x.equals(y));
   }

   @Override
   public boolean equals(Object o) {
      return o instanceof Pair && equals(this.first, ((Pair) o).first) && equals(this.second, ((Pair) o).second);
   }
}

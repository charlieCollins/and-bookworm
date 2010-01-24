package com.totsp.bookworm.data;

public interface IAsyncCallback<T> {

   public void onSuccess(T result);
   
   public void onFailure(Throwable t);
   
}

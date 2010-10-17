package com.totsp.bookworm.util;

import android.app.Dialog;
import android.os.AsyncTask;
import android.util.Log;

import com.totsp.bookworm.Constants;

@Deprecated
public final class TaskUtil {
   
   private TaskUtil() {      
   }
   
   // TODO remove this and use progress dialog correctly from tasks
   // see Main
   
   public static void dismissDialog(final Dialog d) {
      if (d != null && d.isShowing()) {
         d.dismiss();
      }
   }
   
   public static void pauseTask(final AsyncTask<?,?,?> t) {     
      if (t != null) {
         if (!t.isCancelled() && !t.getStatus().equals(AsyncTask.Status.FINISHED)) {
            Log.w(Constants.LOG_TAG, "AsyncTask not finished at onPause (status " + t.getStatus() + "), trying to cancel.");
            t.cancel(true);            
         }         
      }      
   }

}

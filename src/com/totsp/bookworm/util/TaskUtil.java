package com.totsp.bookworm.util;

import android.app.Dialog;
import android.os.AsyncTask;
import android.util.Log;

import com.totsp.bookworm.Constants;

public final class TaskUtil {

   private TaskUtil() {
   }

   // AsyncTasks are odd beasts, very helpful, but can also be confusing
   // you need to keep a reference to an instance, so you can cleanup in onPause, etc (or leak windows)
   // but you can only "execute" once, so you have to create new instances for your reference
   // if an old instance is still around (orientation change, fast clicks, etc., problems ensue
   // this is an attempt to consolidate handling around AsyncTask

   public static void dismissDialog(final Dialog d) {
      if ((d != null) && d.isShowing()) {
         d.dismiss();
      }
   }

   public static void pauseTask(final AsyncTask<?, ?, ?> t) {
      if (t != null) {
         if (!t.isCancelled() && !t.getStatus().equals(AsyncTask.Status.FINISHED)) {
            Log.w(Constants.LOG_TAG, "AsyncTask not finished at onPause (status " + t.getStatus()
                     + "), trying to cancel.");
            t.cancel(true);
         }
      }
   }

}

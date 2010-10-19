package com.totsp.bookworm;

import android.app.backup.BackupManager;
import android.content.Context;

public class BackupManagerWrapper {
   private BackupManager instance;

   static {
      try {
         Class.forName("android.app.backup.BackupManager");
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }
   
   public static void isAvailable() {
   }

   public BackupManagerWrapper(final Context context) {
      instance = new BackupManager(context);
   }   

   public void dataChanged() {
      instance.dataChanged();
   }  
}

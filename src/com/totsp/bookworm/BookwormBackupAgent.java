package com.totsp.bookworm;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.FileBackupHelper;
import android.app.backup.SharedPreferencesBackupHelper;
import android.os.ParcelFileDescriptor;

import com.totsp.bookworm.data.DataConstants;
import com.totsp.bookworm.util.FileUtil;

import java.io.IOException;

public class BookwormBackupAgent extends BackupAgentHelper {
   
   private static final String DEFAULT_SHARED_PREFS_KEY = "com.totsp.bookworm_preferences";   
   
   private static final String PREFS_BACKUP_KEY = "defaultprefs";
   private static final String CSV_FILE_BACKUP_KEY = "csvfile";
   
   public void onCreate() {
      SharedPreferencesBackupHelper prefsHelper =  new SharedPreferencesBackupHelper(this, DEFAULT_SHARED_PREFS_KEY);
      addHelper(PREFS_BACKUP_KEY, prefsHelper);
      
      FileBackupHelper csvFileHelper = new FileBackupHelper(this, DataConstants.EXPORT_FILENAME);
      addHelper(CSV_FILE_BACKUP_KEY, csvFileHelper);
  }
   
   @Override
   public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
             ParcelFileDescriptor newState) throws IOException {
       // Hold the lock while the FileBackupHelper performs backup
       synchronized (FileUtil.DATA_LOCK) {
           super.onBackup(oldState, data, newState);
       }
   }

   @Override
   public void onRestore(BackupDataInput data, int appVersionCode,
           ParcelFileDescriptor newState) throws IOException {
       // Hold the lock while the FileBackupHelper restores the file
       synchronized (FileUtil.DATA_LOCK) {
           super.onRestore(data, appVersionCode, newState);
       }
   }   
}

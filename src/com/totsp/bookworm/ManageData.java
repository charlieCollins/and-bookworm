package com.totsp.bookworm;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.totsp.bookworm.data.DataXmlExporter;
import com.totsp.bookworm.util.FileUtil;

import java.io.File;
import java.io.IOException;

public class ManageData extends Activity {

   private BookWormApplication application;

   private Button exportDbToSdButton;
   private Button importDbFromSdButton;
   private Button clearDbButton;

   private ImportDatabaseTask importDatabaseTask;
   private ExportDatabaseTask exportDatabaseTask;

   @Override
   public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      this.setContentView(R.layout.managedata);
      this.application = (BookWormApplication) this.getApplication();

      this.importDatabaseTask = null;
      this.exportDatabaseTask = null;

      this.exportDbToSdButton =
               (Button) this.findViewById(R.id.exportdbtosdbutton);
      this.exportDbToSdButton.setOnClickListener(new OnClickListener() {
         public void onClick(final View v) {
            new AlertDialog.Builder(ManageData.this).setMessage(
                     ManageData.this
                              .getString(R.string.msgReplaceExistingExport))
                     .setPositiveButton(
                              ManageData.this.getString(R.string.btnYes),
                              new DialogInterface.OnClickListener() {
                                 public void onClick(
                                          final DialogInterface arg0,
                                          final int arg1) {
                                    Log
                                             .i(Constants.LOG_TAG,
                                                      "exporting database to external storage");
                                    ManageData.this.exportDatabaseTask =
                                             new ExportDatabaseTask();
                                    ManageData.this.exportDatabaseTask
                                             .execute();
                                    ManageData.this.startActivity(new Intent(
                                             ManageData.this, Main.class));
                                 }
                              }).setNegativeButton(
                              ManageData.this.getString(R.string.btnNo),
                              new DialogInterface.OnClickListener() {
                                 public void onClick(
                                          final DialogInterface arg0,
                                          final int arg1) {
                                 }
                              }).show();
         }
      });

      this.importDbFromSdButton =
               (Button) this.findViewById(R.id.importdbfromsdbutton);
      this.importDbFromSdButton.setOnClickListener(new OnClickListener() {
         public void onClick(final View v) {
            new AlertDialog.Builder(ManageData.this)
                     .setMessage(
                              ManageData.this
                                       .getString(R.string.msgReplaceExistingData))
                     .setPositiveButton(
                              ManageData.this.getString(R.string.btnYes),
                              new DialogInterface.OnClickListener() {
                                 public void onClick(
                                          final DialogInterface arg0,
                                          final int arg1) {
                                    if (DataXmlExporter
                                             .isExternalStorageAvail()) {
                                       Log
                                                .i(Constants.LOG_TAG,
                                                         "importing database from external storage");
                                       ManageData.this.importDatabaseTask =
                                                new ImportDatabaseTask();
                                       ManageData.this.importDatabaseTask
                                                .execute("bookworm",
                                                         "bookwormdata");
                                       // reset the db (else Main shows no data)
                                       ManageData.this.application.dataManager.resetDb();
                                       ManageData.this
                                                .startActivity(new Intent(
                                                         ManageData.this,
                                                         Main.class));
                                    } else {
                                       Toast
                                                .makeText(
                                                         ManageData.this,
                                                         ManageData.this
                                                                  .getString(R.string.msgExternalStorageNAError),
                                                         Toast.LENGTH_SHORT)
                                                .show();
                                    }
                                 }
                              }).setNegativeButton(
                              ManageData.this.getString(R.string.btnNo),
                              new DialogInterface.OnClickListener() {
                                 public void onClick(
                                          final DialogInterface arg0,
                                          final int arg1) {
                                 }
                              }).show();
         }
      });

      this.clearDbButton = (Button) this.findViewById(R.id.cleardbutton);
      this.clearDbButton.setOnClickListener(new OnClickListener() {
         public void onClick(final View v) {
            new AlertDialog.Builder(ManageData.this).setMessage(
                     ManageData.this.getString(R.string.msgDeleteAllData))
                     .setPositiveButton(
                              ManageData.this.getString(R.string.btnYes),
                              new DialogInterface.OnClickListener() {
                                 public void onClick(
                                          final DialogInterface arg0,
                                          final int arg1) {
                                    Log.i(Constants.LOG_TAG,
                                             "deleting database");
                                    ManageData.this.application.dataManager
                                             .deleteAllDataYesIAmSure();
                                    ManageData.this.application.dataManager
                                             .resetDb();
                                    Toast
                                             .makeText(
                                                      ManageData.this,
                                                      ManageData.this
                                                               .getString(R.string.msgDataDeleted),
                                                      Toast.LENGTH_SHORT)
                                             .show();
                                    ManageData.this.startActivity(new Intent(
                                             ManageData.this, Main.class));
                                 }
                              }).setNegativeButton(
                              ManageData.this.getString(R.string.btnNo),
                              new DialogInterface.OnClickListener() {
                                 public void onClick(
                                          final DialogInterface arg0,
                                          final int arg1) {
                                 }
                              }).show();
         }
      });
   }

   @Override
   public void onPause() {
      if ((this.exportDatabaseTask != null)
               && this.exportDatabaseTask.dialog.isShowing()) {
         this.exportDatabaseTask.dialog.dismiss();
      }
      if ((this.importDatabaseTask != null)
               && this.importDatabaseTask.dialog.isShowing()) {
         this.importDatabaseTask.dialog.dismiss();
      }
      super.onPause();
   }

   // TODO don't need param types on these, don't use the params?
   // could pass in the param strings for data dirs though
   private class ExportDatabaseTask extends AsyncTask<String, Void, Boolean> {
      private final ProgressDialog dialog = new ProgressDialog(ManageData.this);

      @Override
      protected void onPreExecute() {
         this.dialog.setMessage(ManageData.this
                  .getString(R.string.msgExportingData));
         this.dialog.show();
      }

      @Override
      protected Boolean doInBackground(final String... args) {

         File dbFile =
                  new File(Environment.getDataDirectory()
                           + "/data/com.totsp.bookworm/databases/bookworm.db");

         File exportDir =
                  new File(Environment.getExternalStorageDirectory(),
                           "bookwormdata");
         if (!exportDir.exists()) {
            exportDir.mkdirs();
         }
         File file = new File(exportDir, dbFile.getName());

         try {
            file.createNewFile();
            FileUtil.copyFile(dbFile, file);
            return true;
         } catch (IOException e) {
            Log.e(Constants.LOG_TAG, e.getMessage(), e);
            return false;
         }
      }

      @Override
      protected void onPostExecute(final Boolean success) {
         if (this.dialog.isShowing()) {
            this.dialog.dismiss();
         }
         if (success) {
            Toast.makeText(ManageData.this,
                     ManageData.this.getString(R.string.msgExportSuccess),
                     Toast.LENGTH_SHORT).show();
         } else {
            Toast.makeText(ManageData.this,
                     ManageData.this.getString(R.string.msgExportError),
                     Toast.LENGTH_SHORT).show();
         }
      }
   }

   private class ImportDatabaseTask extends AsyncTask<String, Void, String> {
      private final ProgressDialog dialog = new ProgressDialog(ManageData.this);

      @Override
      protected void onPreExecute() {
         this.dialog.setMessage(ManageData.this
                  .getString(R.string.msgImportingData));
         this.dialog.show();
      }

      @Override
      protected String doInBackground(final String... args) {

         File dbBackupFile =
                  new File(Environment.getExternalStorageDirectory()
                           + "/bookwormdata/bookworm.db");
         if (!dbBackupFile.exists()) {
            return ManageData.this
                     .getString(R.string.msgImportFileMissingError);
         } else if (!dbBackupFile.canRead()) {
            return ManageData.this
                     .getString(R.string.msgImportFileNonReadableError);
         }

         File dbFile =
                  new File(Environment.getDataDirectory()
                           + "/data/com.totsp.bookworm/databases/bookworm.db");
         if (dbFile.exists()) {
            dbFile.delete();
         }

         try {
            dbFile.createNewFile();
            FileUtil.copyFile(dbBackupFile, dbFile);
            ///ManageData.this.application.dataManager.resetDbConnection();
            return null;
         } catch (IOException e) {
            Log.e(Constants.LOG_TAG, e.getMessage(), e);
            return e.getMessage();
         }
      }

      @Override
      protected void onPostExecute(final String errMsg) {
         if (this.dialog.isShowing()) {
            this.dialog.dismiss();
         }
         if (errMsg == null) {
            Toast.makeText(ManageData.this,
                     ManageData.this.getString(R.string.msgImportSuccess),
                     Toast.LENGTH_SHORT).show();
         } else {
            Toast.makeText(
                     ManageData.this,
                     ManageData.this.getString(R.string.msgImportError) + ": "
                              + errMsg, Toast.LENGTH_SHORT).show();
         }
      }
   }

}
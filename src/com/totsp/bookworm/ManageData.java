package com.totsp.bookworm;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
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

      this.exportDbToSdButton = (Button) this.findViewById(R.id.exportdbtosdbutton);
      this.exportDbToSdButton.setOnClickListener(new OnClickListener() {
         public void onClick(final View v) {
            new AlertDialog.Builder(ManageData.this).setMessage(
                     "Are you sure (this will replace any existing prior export)?").setPositiveButton("Yes",
                     new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface arg0, final int arg1) {
                           Log.i(Constants.LOG_TAG, "exporting database to external storage");
                           ManageData.this.exportDatabaseTask = new ExportDatabaseTask();
                           ManageData.this.exportDatabaseTask.execute();
                           ManageData.this.startActivity(new Intent(ManageData.this, Main.class));
                        }
                     }).setNegativeButton("No", new DialogInterface.OnClickListener() {
               public void onClick(final DialogInterface arg0, final int arg1) {
               }
            }).show();
         }
      });

      this.importDbFromSdButton = (Button) this.findViewById(R.id.importdbfromsdbutton);
      this.importDbFromSdButton.setOnClickListener(new OnClickListener() {
         public void onClick(final View v) {
            new AlertDialog.Builder(ManageData.this).setMessage(
                     "Are you sure (this will overwrite any existing current data)?").setPositiveButton("Yes",
                     new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface arg0, final int arg1) {
                           if (DataXmlExporter.isExternalStorageAvail()) {
                              Log.i(Constants.LOG_TAG, "importing database from external storage");
                              ManageData.this.importDatabaseTask = new ImportDatabaseTask();
                              ManageData.this.importDatabaseTask.execute("bookworm", "bookwormdata");
                              // sleep momentarily so that database reset stuff has time to take place (else Main shows no data)
                              try {
                                 Thread.sleep(1000);
                              } catch (InterruptedException e) {

                              }
                              ManageData.this.startActivity(new Intent(ManageData.this, Main.class));
                           } else {
                              Toast.makeText(ManageData.this,
                                       "External storage is not available, unable to export data.", Toast.LENGTH_SHORT)
                                       .show();
                           }
                        }
                     }).setNegativeButton("No", new DialogInterface.OnClickListener() {
               public void onClick(final DialogInterface arg0, final int arg1) {
               }
            }).show();
         }
      });

      this.clearDbButton = (Button) this.findViewById(R.id.cleardbutton);
      this.clearDbButton.setOnClickListener(new OnClickListener() {
         public void onClick(final View v) {
            new AlertDialog.Builder(ManageData.this).setMessage(
                     "Are you sure (this will delete all data from database)?").setPositiveButton("Yes",
                     new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface arg0, final int arg1) {
                           Log.i(Constants.LOG_TAG, "deleting database");
                           ManageData.this.application.getDataHelper().deleteAllDataYesIAmSure();
                           ManageData.this.application.getDataHelper().resetDbConnection();
                           Toast.makeText(ManageData.this, "Data deleted", Toast.LENGTH_SHORT).show();
                           ManageData.this.startActivity(new Intent(ManageData.this, Main.class));
                        }
                     }).setNegativeButton("No", new DialogInterface.OnClickListener() {
               public void onClick(final DialogInterface arg0, final int arg1) {
               }
            }).show();
         }
      });
   }

   @Override
   public void onPause() {
      if ((this.exportDatabaseTask != null) && this.exportDatabaseTask.dialog.isShowing()) {
         this.exportDatabaseTask.dialog.dismiss();
      }
      if ((this.importDatabaseTask != null) && this.importDatabaseTask.dialog.isShowing()) {
         this.importDatabaseTask.dialog.dismiss();
      }
      super.onPause();
   }

   // TODO don't need param types on these, don't use the params?
   // could pass in the param strings for data dirs though
   private class ExportDatabaseTask extends AsyncTask<String, Void, Boolean> {
      private final ProgressDialog dialog = new ProgressDialog(ManageData.this);

      protected void onPreExecute() {
         this.dialog.setMessage("Exporting database...");
         this.dialog.show();
      }

      protected Boolean doInBackground(final String... args) {

         File dbFile = new File(Environment.getDataDirectory() + "/data/com.totsp.bookworm/databases/bookworm.db");

         File exportDir = new File(Environment.getExternalStorageDirectory(), "bookwormdata");
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

      protected void onPostExecute(final Boolean success) {
         if (this.dialog.isShowing()) {
            this.dialog.dismiss();
         }
         if (success) {
            Toast.makeText(ManageData.this, "Export successful!", Toast.LENGTH_SHORT).show();
         } else {
            Toast.makeText(ManageData.this, "Export failed", Toast.LENGTH_SHORT).show();
         }
      }
   }

   private class ImportDatabaseTask extends AsyncTask<String, Void, String> {
      private final ProgressDialog dialog = new ProgressDialog(ManageData.this);

      protected void onPreExecute() {
         this.dialog.setMessage("Importing database...");
         this.dialog.show();
      }

      protected String doInBackground(final String... args) {

         File dbBackupFile = new File(Environment.getExternalStorageDirectory() + "/bookwormdata/bookworm.db");
         if (!dbBackupFile.exists()) {
            return "Database backup file does not exist, cannot import.";
         } else if (!dbBackupFile.canRead()) {
            return "Database backup file exists, but is not readable, cannot import.";
         }

         File dbFile = new File(Environment.getDataDirectory() + "/data/com.totsp.bookworm/databases/bookworm.db");
         if (dbFile.exists()) {
            dbFile.delete();
         }

         try {
            dbFile.createNewFile();
            FileUtil.copyFile(dbBackupFile, dbFile);
            ManageData.this.application.getDataHelper().resetDbConnection();
            return null;
         } catch (IOException e) {
            Log.e(Constants.LOG_TAG, e.getMessage(), e);
            return e.getMessage();
         }
      }

      protected void onPostExecute(final String errMsg) {
         if (this.dialog.isShowing()) {
            this.dialog.dismiss();
         }
         if (errMsg == null) {
            Toast.makeText(ManageData.this, "Import successful!", Toast.LENGTH_SHORT).show();
         } else {
            Toast.makeText(ManageData.this, "Import failed - " + errMsg, Toast.LENGTH_SHORT).show();
         }
      }
   }

}
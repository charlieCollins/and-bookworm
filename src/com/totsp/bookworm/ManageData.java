package com.totsp.bookworm;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.totsp.bookworm.data.DataXmlExporter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class ManageData extends Activity {

   private BookWormApplication application;

   private Button exportDbToSdButton;
   private Button exportDbXmlToSdButton;

   // TODO complete all this - not tested AT ALL yet

   @Override
   public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      this.application = (BookWormApplication) this.getApplication();

      this.setContentView(R.layout.managedata);

      this.exportDbToSdButton = (Button) this.findViewById(R.id.exportdbtosdbutton);
      this.exportDbToSdButton.setOnClickListener(new OnClickListener() {
         public void onClick(final View v) {
            new ExportDatabaseFileTask().execute();
         }
      });

      this.exportDbXmlToSdButton = (Button) this.findViewById(R.id.exportdbxmltosdbutton);
      this.exportDbXmlToSdButton.setOnClickListener(new OnClickListener() {
         public void onClick(final View v) {
            if (DataXmlExporter.isExternalStorageAvail()) {
               new ExportDataAsXmlTask().execute("bookworm", "bookwormdata");
            } else {
               Toast.makeText(ManageData.this, "External storage is not available, unable to export data.",
                        Toast.LENGTH_SHORT).show();
            }
         }
      });
   }

   @Override
   public void onPause() {
      super.onPause();
   }

   @Override
   protected void onRestoreInstanceState(final Bundle savedInstanceState) {
      super.onRestoreInstanceState(savedInstanceState);
   }

   @Override
   protected void onSaveInstanceState(final Bundle saveState) {
      super.onSaveInstanceState(saveState);
   }

   private class ExportDatabaseFileTask extends AsyncTask<String, Void, Boolean> {
      private final ProgressDialog dialog = new ProgressDialog(ManageData.this);

      private String errMsg;

      // can use UI thread here
      protected void onPreExecute() {
         this.dialog.setMessage("Exporting database...");
         this.dialog.show();
      }

      // automatically done on worker thread (separate from UI thread)
      protected Boolean doInBackground(final String... args) {

         File dbFile = new File(Environment.getDataDirectory() + "/data/databases/bookworm.db");

         File exportDir = new File(Environment.getExternalStorageDirectory(), "bookwormdata");
         if (!exportDir.exists()) {
            exportDir.mkdirs();
         }
         File file = new File(exportDir, dbFile.getName());

         try {
            file.createNewFile();
            this.copyFile(dbFile, file);
            return true;
         } catch (IOException e) {
            Log.e(Constants.LOG_TAG, e.getMessage(), e);
            return false;
         }         
      }

      // can use UI thread here
      protected void onPostExecute(final Boolean success) {
         if (this.dialog.isShowing()) {
            this.dialog.dismiss();
         }
         if (success) {
            Toast.makeText(ManageData.this, "Export successful!", Toast.LENGTH_LONG).show();            
         } else {
            Toast.makeText(ManageData.this, "Export failed", Toast.LENGTH_LONG).show();
         }
      }

      void copyFile(File src, File dst) throws IOException {
         FileChannel inChannel = new FileInputStream(src).getChannel();
         FileChannel outChannel = new FileOutputStream(dst).getChannel();
         try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
         } finally {
            if (inChannel != null)
               inChannel.close();
            if (outChannel != null)
               outChannel.close();
         }
      }

   }

   private class ExportDataAsXmlTask extends AsyncTask<String, Void, String> {
      private final ProgressDialog dialog = new ProgressDialog(ManageData.this);

      // can use UI thread here
      protected void onPreExecute() {
         this.dialog.setMessage("Exporting database as XML...");
         this.dialog.show();
      }

      // automatically done on worker thread (separate from UI thread)
      protected String doInBackground(final String... args) {
         DataXmlExporter dm = new DataXmlExporter(ManageData.this.application.getDataHelper().getDb());
         try {
            String dbName = args[0];
            String exportFileName = args[1];
            dm.export(dbName, exportFileName);
         } catch (IOException e) {
            Log.e(Constants.LOG_TAG, e.getMessage(), e);
            return e.getMessage();
         }
         return null;
      }

      // can use UI thread here
      protected void onPostExecute(final String errMsg) {
         if (this.dialog.isShowing()) {
            this.dialog.dismiss();
         }
         if (errMsg == null) {
            Toast.makeText(ManageData.this, "Export successful!", Toast.LENGTH_LONG).show();            
         } else {
            Toast.makeText(ManageData.this, "Export failed - " + errMsg, Toast.LENGTH_LONG).show();
         }
      }
   }
}
package com.totsp.bookworm;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.totsp.bookworm.data.DataExporter;

import java.io.IOException;

public class Manage extends Activity {

   private BookWormApplication application;

   private Button exportDbButton;
   private Button importDbButton;

   // TODO complete all this - not tested AT ALL yet
   
   @Override
   public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      this.application = (BookWormApplication) this.getApplication();

      this.setContentView(R.layout.manage);

      this.exportDbButton = (Button) this.findViewById(R.id.exportdbbutton);
      this.exportDbButton.setOnClickListener(new OnClickListener() {
         public void onClick(final View v) {
            new ExportDataTask().execute();
         }
      });

      this.importDbButton = (Button) this.findViewById(R.id.importdbbutton);
      this.importDbButton.setOnClickListener(new OnClickListener() {
         public void onClick(final View v) {
            Toast.makeText(Manage.this, "Importing Data (TODO)", Toast.LENGTH_SHORT).show();
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

   private class ExportDataTask extends AsyncTask<String, Void, Void> {
      private final ProgressDialog dialog = new ProgressDialog(Manage.this);

      private String errMsg;

      // can use UI thread here
      protected void onPreExecute() {
         this.dialog.setMessage("Exporting database...");
         this.dialog.show();
      }

      // automatically done on worker thread (separate from UI thread)
      protected Void doInBackground(final String... args) {
         DataExporter dm = new DataExporter(Manage.this, Manage.this.application.getDataHelper().getDb());
         try {
            // TODO pass in params
            dm.export("exportfilename", false);
         } catch (IOException e) {
            Log.e(Constants.LOG_TAG, e.getMessage(), e);
            this.errMsg = e.getMessage();
         }
         return null;
      }

      // can use UI thread here
      protected void onPostExecute(final Void unused) {
         if (this.dialog.isShowing()) {
            this.dialog.dismiss();
         }
         if (errMsg != null) {
            Toast.makeText(Manage.this, "Export failed - " + this.errMsg, Toast.LENGTH_LONG).show();
         } else {
            Toast.makeText(Manage.this, "Export successful!", Toast.LENGTH_LONG).show();
         }
      }
   }
}
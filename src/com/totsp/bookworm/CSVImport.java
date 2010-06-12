package com.totsp.bookworm;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.totsp.bookworm.data.CsvManager;
import com.totsp.bookworm.data.DataConstants;
import com.totsp.bookworm.model.Book;
import com.totsp.bookworm.util.ExternalStorageUtil;

import java.io.File;
import java.util.ArrayList;

public class CSVImport extends Activity {

   BookWormApplication application;

   TextView data;
   Button parseButton;
   Button importButton;

   ArrayList<Book> books;
   
   ImportTask importTask;

   @Override
   public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      setContentView(R.layout.csvimport);
      application = (BookWormApplication) getApplication();
      
      importTask = new ImportTask();

      data = (TextView) this.findViewById(R.id.bookimportdata);
      data.setText("");

      parseButton = (Button) this.findViewById(R.id.bookimportparsebutton);
      importButton = (Button) this.findViewById(R.id.bookimportbutton);
      importButton.setEnabled(false);

      if (!ExternalStorageUtil.isExternalStorageAvail()) {
         Toast.makeText(this, "External storage (SD card) not available, cannot import.", Toast.LENGTH_LONG).show();
         parseButton.setEnabled(false);
      }

      parseButton.setOnClickListener(new OnClickListener() {
         public void onClick(View v) {
            File f = new File(DataConstants.EXTERNAL_DATA_PATH + File.separator + "bookworm.csv");
            if (f == null || !f.exists() || !f.canRead()) {
               Toast.makeText(CSVImport.this, "File /sdcard/bookwormdata/bookworm.csv not available to import.",
                        Toast.LENGTH_LONG);
            }
            // TODO AsyncTask this too
            CsvManager importer = new CsvManager();
            ArrayList<Book> parsedBooks = importer.parseCSVFile(f);
            if (parsedBooks == null || parsedBooks.isEmpty()) {
               Toast.makeText(CSVImport.this, "Unable to parse any data from file for import.", Toast.LENGTH_LONG);
            } else {
               CSVImport.this.books = parsedBooks;
               CSVImport.this.populateData();
            }
         }
      });

      importButton.setOnClickListener(new OnClickListener() {
         public void onClick(View v) {
            if (books != null && !books.isEmpty()) {
               importTask.execute(books);
            }
            Toast.makeText(CSVImport.this, "Imported book data from CSV file.", Toast.LENGTH_LONG);
            reset();
         }
      });

   }
   
   @Override
   public void onPause() {
      if ((importTask != null) && importTask.dialog.isShowing()) {
         importTask.dialog.dismiss();
      }      
      super.onPause();
   }

   private void reset() {
      books = null;
      data.setText("");
      importButton.setEnabled(false);
      parseButton.setEnabled(true);
   }

   private void populateData() {
      StringBuilder sb = new StringBuilder();
      sb.append("Parsed " + books.size()
               + " books from import file. Click import (below) to continue and import to database.\n\n");
      for (Book b : books) {
         System.out.println("book - " + b);
         sb.append("Title: " + b.title);
         sb.append("\n");
      }
      this.data.setText(sb.toString());
      this.importButton.setEnabled(true);
      this.parseButton.setEnabled(false);
   }

   //
   // AsyncTasks
   //
   private class ImportTask extends AsyncTask<ArrayList<Book>, String, Void> {
      private final ProgressDialog dialog = new ProgressDialog(CSVImport.this);

      // TODO don't import books that are dupes!
      
      public ImportTask() {
      }

      @Override
      protected void onPreExecute() {
         dialog.setMessage("Importing Books");
         dialog.show();
         getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
      }

      @Override
      protected void onProgressUpdate(final String... args) {
         dialog.setMessage(args[0]);
      }

      @Override
      protected Void doInBackground(final ArrayList<Book>... args) {
         ArrayList<Book> taskBooks = args[0];
         for (Book b : taskBooks) {
            Log.i(Constants.LOG_TAG, "Importing book: " + b.title);
            publishProgress("Importing book:\n" + b.title);
            application.dataManager.insertBook(b);
         }
         return null;
      }

      @Override
      protected void onPostExecute(final Void arg) {
         if (dialog.isShowing()) {
            dialog.dismiss();
         }
         
         application.dataManager.resetDb();
         reset();
         getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
         startActivity(new Intent(CSVImport.this, Main.class));
      }
   }
}
package com.totsp.bookworm;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.totsp.bookworm.data.CsvManager;
import com.totsp.bookworm.data.DataConstants;
import com.totsp.bookworm.model.Book;
import com.totsp.bookworm.util.BookUtil;
import com.totsp.bookworm.util.ExternalStorageUtil;

import java.io.File;
import java.util.ArrayList;

public class CSVImport extends Activity {

   private static final int MENU_CSV_HELP = 1;

   private BookWormApplication application;

   private TextView metaData;
   private TextView data;
   private Button parseButton;
   private Button importButton;

   private ProgressDialog progressDialog;
   
   ArrayList<Book> books;   

   @Override
   public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      setContentView(R.layout.csvimport);
      application = (BookWormApplication) getApplication();

      progressDialog = new ProgressDialog(this);
      progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
      progressDialog.setCancelable(false);

      metaData = (TextView) findViewById(R.id.bookimportmetadata);
      metaData.setText("");
      data = (TextView) findViewById(R.id.bookimportdata);
      data.setText("");

      parseButton = (Button) findViewById(R.id.bookimportparsebutton);
      importButton = (Button) findViewById(R.id.bookimportbutton);
      importButton.setEnabled(false);

      if (!ExternalStorageUtil.isExternalStorageAvail()) {
         Toast.makeText(this, getString(R.string.msgExternalStorageNAError), Toast.LENGTH_LONG).show();
         parseButton.setEnabled(false);
      }

      parseButton.setOnClickListener(new OnClickListener() {
         public void onClick(View v) {
            File f = new File(DataConstants.EXTERNAL_DATA_PATH + File.separator + "bookworm.csv");
            if ((f == null) || !f.exists() || !f.canRead()) {
               Toast.makeText(CSVImport.this, getString(R.string.msgCsvFileNotFound), Toast.LENGTH_LONG).show();
            }
            // potentially AsyncTask this too? (could be an FC here with perfect timing, though this is very quick)           
            ArrayList<Book> parsedBooks =  CsvManager.parseCSVFile(application.bookDataSource, f);
            if (parsedBooks == null || parsedBooks.isEmpty()) {
               Toast.makeText(CSVImport.this, getString(R.string.msgCsvUnableToParse), Toast.LENGTH_LONG).show();
            } else {
               books = parsedBooks;
               populateData();
            }
         }
      });

      importButton.setOnClickListener(new OnClickListener() {
         @SuppressWarnings("unchecked")
         public void onClick(View v) {
            if ((books != null) && !books.isEmpty()) {
               new ImportTask().execute(books);
            }
            reset();
         }
      });

   }

   @Override
   public void onPause() {
      if (progressDialog.isShowing()) {
         progressDialog.dismiss();
      }
      super.onPause();
   }

   @Override
   public boolean onCreateOptionsMenu(final Menu menu) {
      menu.add(0, CSVImport.MENU_CSV_HELP, 1, getString(R.string.menuCsvHelp)).setIcon(
               android.R.drawable.ic_menu_info_details);
      return super.onCreateOptionsMenu(menu);
   }

   @Override
   public boolean onOptionsItemSelected(final MenuItem item) {
      switch (item.getItemId()) {
         case MENU_CSV_HELP:
            new AlertDialog.Builder(CSVImport.this).setTitle(getString(R.string.menuCsvHelp)).setMessage(
                     Html.fromHtml(getString(R.string.msgCsvHelp))).setNeutralButton(getString(R.string.btnDismiss),
                     new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface d, final int i) {
                        }
                     }).show();
            return true;
         default:
            return super.onOptionsItemSelected(item);
      }
   }

   private void reset() {
      books = null;
      data.setText("");
      metaData.setText("");
      importButton.setEnabled(false);
      parseButton.setEnabled(true);
   }

   private void populateData() {
      metaData.setText(String.format(getString(R.string.msgCsvMetaData), books.size()));

      String title = getString(R.string.labelTitle);
      StringBuilder sb = new StringBuilder();
      for (Book b : books) {
         sb.append(title + ": " + b.title);
         sb.append("\n");
      }
      data.setText(sb.toString());

      importButton.setEnabled(true);
      parseButton.setEnabled(false);
   }

   //
   // AsyncTasks
   //
   private class ImportTask extends AsyncTask<ArrayList<Book>, String, Void> {      

      public ImportTask() {
      }

      @Override
      protected void onPreExecute() {
         if (progressDialog.isShowing()) {
            progressDialog.dismiss();
         }
         // keep screen on, and prevent orientation change, during potentially long running task
         getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
         setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
      }

      @Override
      protected Void doInBackground(final ArrayList<Book>... args) {
         ArrayList<Book> taskBooks = args[0];
         String[] progress = new String[3];
         progress[2] = Integer.toString(taskBooks.size());
         for (int i = 0; i < taskBooks.size(); i++) {
            Book b = taskBooks.get(i);
            boolean dupe = false;
            ArrayList<Book> potentialDupes = application.dataManager.selectAllBooksByTitle(b.title);
            if (potentialDupes != null) {
               // very poor algorithm to check dupes here, just re-linear search
               for (int j = 0; j < potentialDupes.size(); j++) {
                  Book b2 = potentialDupes.get(j);
                  if (BookUtil.areBooksEffectiveDupes(b, b2)) {
                     dupe = true;
                     break;
                  }
               }
            }
            if (dupe) {
               Log.i(Constants.LOG_TAG, "NOT Importing book: " + b.title + " because it appears to be a duplicate.");
               progress[0] = String.format(getString(R.string.msgCsvSkippingBook, b.title));
               progress[1] = Integer.toString(i);
               publishProgress(progress);
               // sleep because loop is too fast to see messages
               SystemClock.sleep(500);
            } else {
               Log.i(Constants.LOG_TAG, "Importing book: " + b.title);
               progress[0] = String.format(getString(R.string.msgCsvImportingBook, b.title));
               progress[1] = Integer.toString(i);
               publishProgress(progress);
               // sleep because loop is too fast to see messages
               SystemClock.sleep(500);
               b.id = application.dataManager.insertBook(b);
               application.imageManager.resetCoverImage(b);
            }
         }
         return null;
      }
      
      @Override
      protected void onProgressUpdate(String... progress) {
         progressDialog.setMessage(progress[0]);
         if ((progress[1].equals("1")) && !progressDialog.isShowing()) {
            //Toast.makeText(Main.this, R.string.msgResetCoverImagesWarnTime, Toast.LENGTH_SHORT).show();
            progressDialog.setMax(Integer.valueOf(progress[2]));
            progressDialog.show();
         } else if (progress[1].equals(progress[2]) && progressDialog.isShowing()) {
            progressDialog.dismiss();
         }
         progressDialog.setProgress(Integer.valueOf(progress[1]));
      }

      @Override
      protected void onPostExecute(final Void arg) {
         if (progressDialog.isShowing()) {
            progressDialog.dismiss();
         }

         reset();

         // reset screen and orientation params
         getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
         setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

         startActivity(new Intent(CSVImport.this, Main.class));
      }
   }
}
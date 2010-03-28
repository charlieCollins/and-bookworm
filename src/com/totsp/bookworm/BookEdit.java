package com.totsp.bookworm;

import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.totsp.bookworm.model.Book;
import com.totsp.bookworm.util.AuthorsStringUtil;
import com.totsp.bookworm.util.DateUtil;

import java.util.Date;

public class BookEdit extends TabActivity {

   private BookWormApplication application;

   private TabHost tabHost;

   private ImageView bookCover;
   private TextView bookTitleCoverTab;
   private EditText bookTitleFormTab;
   private EditText bookSubTitle;
   private EditText bookAuthors;
   private EditText bookSubject;
   private EditText bookDatePub;
   private EditText bookPublisher;

   private Button saveButton;
   private Button resetCoverButton;
   private Button generateCoverButton;

   // TODO allow use to select an image from gallery/sdcard to use as cover
   ///private Button selectCoverButton;

   @Override
   public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      this.application = (BookWormApplication) this.getApplication();

      this.setContentView(R.layout.bookedit);

      this.tabHost = this.getTabHost();

      this.tabHost.addTab(this.tabHost.newTabSpec("tabs").setIndicator("Edit Book Details").setContent(
               R.id.bookedittab1));
      this.tabHost.addTab(this.tabHost.newTabSpec("tabs").setIndicator("Manage Cover Image").setContent(
               R.id.bookedittab2));

      this.tabHost.setCurrentTab(0);

      this.bookCover = (ImageView) this.findViewById(R.id.bookcover);
      this.bookTitleFormTab = (EditText) this.findViewById(R.id.booktitleform);
      this.bookTitleCoverTab = (TextView) this.findViewById(R.id.booktitlecover);
      this.bookSubTitle = (EditText) this.findViewById(R.id.booksubtitle);
      this.bookAuthors = (EditText) this.findViewById(R.id.bookauthors);
      this.bookSubject = (EditText) this.findViewById(R.id.booksubject);
      this.bookDatePub = (EditText) this.findViewById(R.id.bookdatepub);
      this.bookPublisher = (EditText) this.findViewById(R.id.bookpublisher);

      this.saveButton = (Button) this.findViewById(R.id.bookeditsavebutton);
      this.saveButton.setOnClickListener(new OnClickListener() {
         public void onClick(final View v) {
            BookEdit.this.saveEdits();
         }
      });

      this.resetCoverButton = (Button) this.findViewById(R.id.bookeditresetcoverbutton);
      this.resetCoverButton.setOnClickListener(new OnClickListener() {
         public void onClick(final View v) {
            new ResetCoverImageTask().execute(BookEdit.this.application.getSelectedBook());
         }
      });

      this.generateCoverButton = (Button) this.findViewById(R.id.bookeditgeneratecoverbutton);
      this.generateCoverButton.setOnClickListener(new OnClickListener() {
         public void onClick(final View v) {
            new GenerateCoverImageTask().execute(BookEdit.this.application.getSelectedBook());
         }
      });

      this.setViewData();
   }

   @Override
   public void onPause() {
      this.bookTitleFormTab = null;
      super.onPause();
   }

   /*
   // go back to main if back pressed here, in case path is multiple tabs, etc
   @Override
   public boolean onKeyDown(final int keyCode, final KeyEvent event) {
      if ((keyCode == KeyEvent.KEYCODE_BACK) && (event.getRepeatCount() == 0)) {
         BookEdit.this.startActivity(new Intent(BookEdit.this, Main.class));
         return true;
      }
      return super.onKeyDown(keyCode, event);
   }
   */

   private void saveEdits() {
      Book book = this.application.getSelectedBook();
      if (book != null) {
         Book newBook = new Book();
         newBook.setTitle(this.bookTitleFormTab.getText().toString());
         newBook.setSubTitle(this.bookSubTitle.getText().toString());
         newBook.setAuthors(AuthorsStringUtil.expandAuthors(this.bookAuthors.getText().toString()));
         newBook.setSubject(this.bookSubject.getText().toString());
         newBook.setDatePubStamp(DateUtil.parse(this.bookDatePub.getText().toString()).getTime());
         newBook.setPublisher(this.bookPublisher.getText().toString());

         // TODO properties not yet editable, but should be         
         newBook.setBlurb(book.getBlurb());
         newBook.setDescription(book.getDescription());
         newBook.setFormat(book.getFormat());
         newBook.setIsbn13(book.getIsbn13());
         newBook.setIsbn10(book.getIsbn10());

         // properties editable on display page and not on edit page
         newBook.setRating(book.getRating());
         newBook.setRead(book.isRead());

         newBook.setId(book.getId());
         new UpdateBookTask().execute(newBook);
      }
   }

   private void setViewData() {
      Book book = this.application.getSelectedBook();
      if (book != null) {
         Bitmap coverImage = this.application.getDataImageHelper().retrieveBitmap(book.getTitle(), book.getId(), false);
         if (coverImage != null) {
            this.bookCover.setImageBitmap(coverImage);
         } else {
            this.bookCover.setImageResource(R.drawable.book_cover_missing);
         }

         this.bookTitleFormTab.setText(book.getTitle());
         this.bookTitleCoverTab.setText(book.getTitle());
         this.bookSubTitle.setText(book.getSubTitle());
         this.bookAuthors.setText(AuthorsStringUtil.contractAuthors(book.getAuthors()));
         this.bookSubject.setText(book.getSubject());
         this.bookDatePub.setText(DateUtil.format(new Date(book.getDatePubStamp())));
         this.bookPublisher.setText(book.getPublisher());
      }
   }

   @Override
   protected void onRestoreInstanceState(final Bundle savedInstanceState) {
      super.onRestoreInstanceState(savedInstanceState);
      if (this.application.getSelectedBook() == null) {
         this.application.establishSelectedBook(savedInstanceState.getLong(Constants.BOOK_ID));
         this.setViewData();
      }
   }

   @Override
   protected void onSaveInstanceState(final Bundle saveState) {
      saveState.putLong(Constants.BOOK_ID, this.application.getSelectedBook().getId());
      super.onSaveInstanceState(saveState);
   }

   @Override
   public boolean onCreateOptionsMenu(final Menu menu) {

      return super.onCreateOptionsMenu(menu);
   }

   @Override
   public boolean onOptionsItemSelected(final MenuItem item) {
      switch (item.getItemId()) {

      default:
         return super.onOptionsItemSelected(item);
      }
   }

   private class UpdateBookTask extends AsyncTask<Book, Void, Boolean> {
      private final ProgressDialog dialog = new ProgressDialog(BookEdit.this);

      protected void onPreExecute() {
         this.dialog.setMessage("Saving updated book info...");
         this.dialog.show();
      }

      protected Boolean doInBackground(final Book... args) {
         Book book = args[0];
         if ((book != null) && (book.getId() > 0)) {
            BookEdit.this.application.getDataHelper().updateBook(book);
            BookEdit.this.application.establishSelectedBook(book.getId());
            return true;
         }
         return false;
      }

      protected void onPostExecute(final Boolean b) {
         if (this.dialog.isShowing()) {
            this.dialog.dismiss();
         }        
         if (!b) {
            Toast.makeText(BookEdit.this, "Error updating book, book information not present, or ID null",
                     Toast.LENGTH_LONG).show();
         } else {
            Toast.makeText(BookEdit.this, "Book updated", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(BookEdit.this, BookDetail.class);
            intent.putExtra("RELOAD_AFTER_EDIT", true);
            BookEdit.this.startActivity(intent);
         }
      }
   }

   private class ResetCoverImageTask extends AsyncTask<Book, Void, Boolean> {
      private final ProgressDialog dialog = new ProgressDialog(BookEdit.this);

      protected void onPreExecute() {
         this.dialog.setMessage("Resetting cover image...");
         this.dialog.show();
      }

      protected Boolean doInBackground(final Book... args) {
         Book book = args[0];
         if ((book != null) && (book.getId() > 0)) {
            BookEdit.this.application.getDataImageHelper().resetCoverImage(BookEdit.this.application.getDataHelper(),
                     "2", book);
            return true;
         }
         return false;
      }

      protected void onPostExecute(final Boolean b) {
         if (this.dialog.isShowing()) {
            this.dialog.dismiss();
         }
         if (!b) {
            Toast.makeText(BookEdit.this, "Error updating book, book information not present, or ID null.",
                     Toast.LENGTH_LONG).show();
         } else {
            Toast.makeText(BookEdit.this, "Book updated", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(BookEdit.this, BookDetail.class);
            BookEdit.this.startActivity(intent);
         }
      }
   }

   private class GenerateCoverImageTask extends AsyncTask<Book, Void, Boolean> {
      private final ProgressDialog dialog = new ProgressDialog(BookEdit.this);

      protected void onPreExecute() {
         this.dialog.setMessage("Generating cover image...");
         this.dialog.show();
      }

      protected Boolean doInBackground(final Book... args) {
         Book book = args[0];
         if ((book != null) && (book.getId() > 0)) {
            BookEdit.this.application.getDataImageHelper().createAndStoreCoverImage(book.getTitle(), book.getId());
            return true;
         }
         return false;
      }

      protected void onPostExecute(final Boolean b) {
         if (this.dialog.isShowing()) {
            this.dialog.dismiss();
         }
         if (!b) {
            Toast.makeText(BookEdit.this, "Error generating cover image, book information not present, or ID null.",
                     Toast.LENGTH_LONG).show();
         } else {            
            Toast.makeText(BookEdit.this, "Book updated", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(BookEdit.this, BookDetail.class);
            BookEdit.this.startActivity(intent);
         }
      }
   }

}
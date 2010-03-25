package com.totsp.bookworm;

import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
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
   private Button replaceCoverButton;

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
            BookEdit.this.startActivity(new Intent(BookEdit.this, Main.class));
         }
      });

      this.resetCoverButton = (Button) this.findViewById(R.id.bookeditresetcoverbutton);
      this.resetCoverButton.setOnClickListener(new OnClickListener() {
         public void onClick(final View v) {
            new ResetCoverImageTask().execute(BookEdit.this.application.getSelectedBook());
         }
      });

      // TODO browse cover image replace capability
      this.replaceCoverButton = (Button) this.findViewById(R.id.bookeditreplacecoverbutton);
      this.replaceCoverButton.setOnClickListener(new OnClickListener() {
         public void onClick(final View v) {
            Toast.makeText(BookEdit.this, "TODO - browse for cover image replacement", Toast.LENGTH_SHORT).show();
         }
      });

      this.setViewData();
   }

   @Override
   public void onPause() {
      this.bookTitleFormTab = null;
      super.onPause();
   }

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
         Bitmap coverImage = this.application.getDataImageHelper().retrieveBitmap(book.getTitle(), false);
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
         this.application.establishSelectedBook(savedInstanceState.getString(Constants.ISBN));
         this.setViewData();
      }
   }

   @Override
   protected void onSaveInstanceState(final Bundle saveState) {
      // TODO add fallback to book isbn13 support
      saveState.putString(Constants.ISBN, this.application.getSelectedBook().getIsbn10());
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
            ///BookEdit.this.application.getDataImageHelper().resetCoverImage(BookEdit.this.application.getDataHelper(),
            ///         "2", BookEdit.this.application.getSelectedBook());

            Bitmap bitmap = Bitmap.createBitmap(120, 183, Bitmap.Config.ARGB_4444);
            Canvas canvas = new Canvas(bitmap);
            Bitmap bkgrnd = BitmapFactory.decodeResource(getResources(), R.drawable.book_bgrnd_small);
            canvas.drawBitmap(bkgrnd, new Matrix(), null);
            Paint paint = new Paint();
            paint.setTextSize(13);
            paint.setAlpha(255);
            paint.setAntiAlias(true);
            // TODO break up AT words
            String titleToDraw = book.getTitle();
            String firstLine = null;
            String secondLine = null;
            if (titleToDraw.length() > 30) {
               firstLine = titleToDraw.substring(0, 25);
               secondLine = titleToDraw.substring(25, titleToDraw.length());
               if (secondLine.length() > 25) {
                  secondLine = secondLine.substring(0, 22);
                  secondLine += "...";
               }
            } else {
               firstLine = titleToDraw;
            }
            canvas.drawText(firstLine, 5, 70, new Paint());
            if (secondLine != null) {
               canvas.drawText(secondLine, 5, 85, new Paint());
            }
            canvas.save();

            application.getDataImageHelper().storeBitmap(bitmap, book.getTitle());

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
            BookEdit.this.startActivity(new Intent(BookEdit.this, Main.class));
         }
      }
   }
}
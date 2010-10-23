package com.totsp.bookworm;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TabHost.OnTabChangeListener;

import com.totsp.bookworm.data.ImageManager;
import com.totsp.bookworm.model.Book;
import com.totsp.bookworm.util.StringUtil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

public class BookForm extends TabActivity {

   private static final int SELECT_IMAGE = 0;

   private BookWormApplication application;

   private TabHost tabHost;

   private TextView bookEnterEditLabel;
   private ImageView bookCover;
   private TextView bookTitleCoverTab;
   private EditText bookTitleFormTab;
   private EditText bookSubTitle;
   private EditText bookIsbn10;
   private EditText bookIsbn13;
   private EditText bookAuthors;
   private EditText bookSubject;
   private DatePicker bookDatePub;
   private EditText bookPublisher;

   private Button saveButton;
   private Button selectCoverButton;
   private Button retrieveCoverButton;
   private Button generateCoverButton;

   private ProgressDialog progressDialog;

   @Override
   public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.bookform);
      application = (BookWormApplication) getApplication();

      progressDialog = new ProgressDialog(this);
      progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
      progressDialog.setCancelable(false);
      //progressDialog.setMax(1);

      tabHost = getTabHost();
      tabHost.addTab(tabHost.newTabSpec("tab1").setIndicator(getString(R.string.menuEditBookDetails),
               getResources().getDrawable(android.R.drawable.ic_menu_edit)).setContent(R.id.bookformtab1));
      tabHost.addTab(tabHost.newTabSpec("tab2").setIndicator(getString(R.string.menuManageCoverImage),
               getResources().getDrawable(android.R.drawable.ic_menu_crop)).setContent(R.id.bookformtab2));
      tabHost.setCurrentTab(0);

      // make sure if user is ADDing a new book, and title/authors not set, they cannot go to Manage Cover Image tab
      // (we have to have a book first, before we can manage cover image)
      tabHost.setOnTabChangedListener(new OnTabChangeListener() {
         public void onTabChanged(final String tabName) {
            if (tabName.equals("tab2") && (application.selectedBook == null)) {
               Toast.makeText(BookForm.this, getString(R.string.msgMinimumSave), Toast.LENGTH_LONG).show();
               tabHost.setCurrentTab(0);
            }
         }
      });

      bookEnterEditLabel = (TextView) findViewById(R.id.bookentereditlabel);
      bookCover = (ImageView) findViewById(R.id.bookcover);
      bookTitleFormTab = (EditText) findViewById(R.id.booktitleform);
      bookTitleCoverTab = (TextView) findViewById(R.id.booktitlecover);
      bookSubTitle = (EditText) findViewById(R.id.booksubtitle);
      bookIsbn10 = (EditText) findViewById(R.id.bookisbn10);
      bookIsbn13 = (EditText) findViewById(R.id.bookisbn13);
      bookAuthors = (EditText) findViewById(R.id.bookauthors);
      bookSubject = (EditText) findViewById(R.id.booksubject);
      bookDatePub = (DatePicker) findViewById(R.id.bookdatepub);
      bookPublisher = (EditText) findViewById(R.id.bookpublisher);

      saveButton = (Button) findViewById(R.id.bookformsavebutton);
      saveButton.setOnClickListener(new OnClickListener() {
         public void onClick(final View v) {
            if (((bookTitleFormTab != null) && (bookTitleFormTab.getText() != null) && (!bookTitleFormTab.getText()
                     .toString().equals("")))
                     && ((bookAuthors != null) && (bookAuthors.getText() != null) && (!bookAuthors.getText().toString()
                              .equals("")))) {
               saveEdits();
            } else {
               Toast.makeText(BookForm.this, getString(R.string.msgMinimumSave), Toast.LENGTH_LONG).show();
            }
         }
      });

      selectCoverButton = (Button) findViewById(R.id.bookformselectcoverbutton);
      selectCoverButton.setOnClickListener(new OnClickListener() {
         public void onClick(final View v) {
            try {
               tabHost.setCurrentTab(0);
               startActivityForResult(new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI), BookForm.SELECT_IMAGE);
            } catch (ActivityNotFoundException e) {
               Toast.makeText(BookForm.this, getString(R.string.msgNoActivityGalleryError), Toast.LENGTH_LONG).show();
            }
         }
      });

      retrieveCoverButton = (Button) findViewById(R.id.bookformretrievecoverbutton);
      retrieveCoverButton.setOnClickListener(new OnClickListener() {
         public void onClick(final View v) {
            new RetrieveCoverImageTask().execute(application.selectedBook);
         }
      });

      generateCoverButton = (Button) findViewById(R.id.bookformgeneratecoverbutton);
      generateCoverButton.setOnClickListener(new OnClickListener() {
         public void onClick(final View v) {
            new GenerateCoverImageTask().execute(application.selectedBook);
         }
      });

      // if Application selectedBook is present, EDIT mode, else ADD mode
      if (application.selectedBook != null) {
         bookEnterEditLabel.setText(getString(R.string.menuEditBook));
         setExistingViewData();
      } else {
         bookEnterEditLabel.setText(getString(R.string.menuAddBook));
      }

      // do not enable the soft keyboard unless user explicitly selects textedit
      // Android seems to have an IMM bug concerning this on devices with only soft keyboard
      // http://code.google.com/p/android/issues/detail?id=7115
      getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
   }

   @Override
   public void onStart() {
      super.onStart();
   }

   @Override
   public void onPause() {
      bookTitleFormTab = null;
      if (progressDialog.isShowing()) {
         progressDialog.dismiss();
      }
      super.onPause();
   }

   @Override
   public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
      super.onActivityResult(requestCode, resultCode, data);
      if (requestCode == BookForm.SELECT_IMAGE) {
         if (resultCode == Activity.RESULT_OK) {
            // intentionally do NOT use an AsyncTask (confusing for UI, returns onActResult too fast)
            Uri selectedImageUri = data.getData();
            InputStream is = null;
            try {
               is = getContentResolver().openInputStream(selectedImageUri);
               Bitmap bitmap = BitmapFactory.decodeStream(is, null, ImageManager.options);
               Book book = application.selectedBook;
               if ((bitmap != null) && (book != null)) {
                  application.imageManager.storeBitmap(bitmap, book.title, book.id);
               }
            } catch (FileNotFoundException e) {
               Log.e(Constants.LOG_TAG, e.getMessage(), e);
            } finally {
               if (is != null) {
                  try {
                     is.close();
                  } catch (IOException e) {
                     // swallow
                  }
               }
            }

            Toast.makeText(BookForm.this, getString(R.string.msgBookUpdated), Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(BookForm.this, BookDetail.class);
            startActivity(intent);
         }
      }
   }

   @Override
   protected void onRestoreInstanceState(final Bundle savedInstanceState) {
      super.onRestoreInstanceState(savedInstanceState);
      if (application.selectedBook == null) {
         long bookId = savedInstanceState.getLong(Constants.BOOK_ID, 0L);
         if (bookId > 0) {
            application.establishSelectedBook(bookId);
            setExistingViewData();
         }
      }
   }

   @Override
   protected void onSaveInstanceState(final Bundle saveState) {
      if (application.selectedBook != null) {
         saveState.putLong(Constants.BOOK_ID, application.selectedBook.id);
      }
      super.onSaveInstanceState(saveState);
   }

   private void setExistingViewData() {
      Book book = application.selectedBook;
      if (book != null) {
         Bitmap coverImage = application.imageManager.retrieveBitmap(book.title, book.id, false);
         if (coverImage != null) {
            bookCover.setImageBitmap(coverImage);
         } else {
            bookCover.setImageResource(R.drawable.book_cover_missing);
         }

         bookTitleFormTab.setText(book.title);
         bookTitleCoverTab.setText(book.title);
         bookSubTitle.setText(book.subTitle);
         bookIsbn10.setText(book.isbn10);
         bookIsbn13.setText(book.isbn13);
         bookAuthors.setText(StringUtil.contractAuthors(book.authors));
         bookSubject.setText(book.subject);
         bookPublisher.setText(book.publisher);

         if (book.datePubStamp > 0) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(book.datePubStamp);
            try {
               bookDatePub.updateDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
            } catch (IllegalArgumentException e) {
               Log.w(Constants.LOG_TAG, "Invalid book date, can't set it, try to update/edit with a valid date.", e);
            }
         }
      }
   }

   private void saveEdits() {
      // establish newBook
      Book newBook = new Book();
      newBook.title = (bookTitleFormTab.getText().toString());
      newBook.subTitle = (bookSubTitle.getText().toString());
      newBook.isbn10 = (bookIsbn10.getText().toString());
      newBook.isbn13 = (bookIsbn13.getText().toString());
      newBook.authors = (StringUtil.expandAuthors(bookAuthors.getText().toString()));
      newBook.subject = (bookSubject.getText().toString());
      newBook.publisher = (bookPublisher.getText().toString());

      Calendar cal = Calendar.getInstance();
      cal.set(Calendar.YEAR, bookDatePub.getYear());
      cal.set(Calendar.MONTH, bookDatePub.getMonth());
      cal.set(Calendar.DAY_OF_MONTH, bookDatePub.getDayOfMonth());
      newBook.datePubStamp = cal.getTimeInMillis();

      // save settings from existing book, if present (if we are editing)
      Book book = application.selectedBook;
      if (book != null) {
         newBook.id = (book.id);

         // NOTE - properties not yet editable (will be in future)        
         newBook.description = (book.description);
         newBook.format = (book.format);

         // properties editable on display page and not on edit page
         newBook.bookUserData.rating = (book.bookUserData.rating);
         newBook.bookUserData.read = (book.bookUserData.read);

         // rename the cover images too, if title changes
         if (!book.title.equals(newBook.title)) {
            application.imageManager.renameBitmapSourceFile(book.title, newBook.title, book.id);
         }
      }

      new SaveBookTask().execute(newBook);
   }

   //
   // AsyncTasks
   //
   private class SaveBookTask extends AsyncTask<Book, String, Boolean> {
      private boolean newBook;

      @Override
      protected void onPreExecute() {
         if (progressDialog.isShowing()) {
            progressDialog.dismiss();
         }
      }

      @Override
      protected Boolean doInBackground(final Book... args) {
         Book book = args[0];
         publishProgress(new String[] { book.title });
         if (book.id > 0) {
            application.dataManager.updateBook(book);
            application.establishSelectedBook(book.id);
            return true;
         } else {
            newBook = true;
            long bookId = application.dataManager.insertBook(book);
            if (bookId > 0) {
               application.establishSelectedBook(bookId);
               // also auto store generated cover with new form based book insert
               Bitmap generatedCover = application.imageManager.createCoverImage(book.title);
               application.imageManager.storeBitmap(generatedCover, book.title, bookId);
               return true;
            }
         }
         return false;
      }

      @Override
      protected void onProgressUpdate(String... progress) {
         progressDialog.setMessage(getString(R.string.msgSavingBookInfo) + " " + progress[0]);
         if (!progressDialog.isShowing()) {
            progressDialog.show();
         }
      }

      @Override
      protected void onPostExecute(final Boolean b) {
         if (progressDialog.isShowing()) {
            progressDialog.dismiss();
         }
         if (!b) {
            Toast.makeText(BookForm.this, getString(R.string.msgBookSaveError), Toast.LENGTH_LONG).show();
         } else {
            Toast.makeText(BookForm.this, getString(R.string.msgBookSaved), Toast.LENGTH_SHORT).show();
            if (!newBook) {
               Intent intent = new Intent(BookForm.this, BookDetail.class);
               intent.putExtra("RELOAD_AFTER_EDIT", true);
               startActivity(intent);
            } else {
               setExistingViewData();
            }
         }
      }
   }

   private class RetrieveCoverImageTask extends AsyncTask<Book, String, Boolean> {
      @Override
      protected void onPreExecute() {
         if (progressDialog.isShowing()) {
            progressDialog.dismiss();
         }
      }

      @Override
      protected Boolean doInBackground(final Book... args) {
         Book book = args[0];
         publishProgress(new String[] { book.title });
         if (book.id > 0) {
            application.imageManager.resetCoverImage(book);
            return true;
         }
         return false;
      }

      @Override
      protected void onProgressUpdate(String... progress) {
         progressDialog.setMessage("Retrieving cover image" + " " + progress[0]);
         if (!progressDialog.isShowing()) {
            progressDialog.show();
         }
      }

      @Override
      protected void onPostExecute(final Boolean b) {
         if (progressDialog.isShowing()) {
            progressDialog.dismiss();
         }
         if (!b) {
            Toast.makeText(BookForm.this, getString(R.string.msgRetrieveCoverImageError), Toast.LENGTH_LONG).show();
         } else {
            Toast.makeText(BookForm.this, getString(R.string.msgBookUpdated), Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(BookForm.this, BookDetail.class);
            startActivity(intent);
         }
      }
   }

   private class GenerateCoverImageTask extends AsyncTask<Book, String, Boolean> {
      @Override
      protected void onPreExecute() {
         if (progressDialog.isShowing()) {
            progressDialog.dismiss();
         }
      }

      @Override
      protected Boolean doInBackground(final Book... args) {
         Book book = args[0];
         publishProgress(new String[] { book.title });
         if (book.id > 0) {
            Bitmap generatedCover = application.imageManager.createCoverImage(book.title);
            application.imageManager.storeBitmap(generatedCover, book.title, book.id);
            return true;
         }
         return false;
      }

      @Override
      protected void onProgressUpdate(String... progress) {
         progressDialog.setMessage("Generating cover image" + " " + progress[0]);
         if (!progressDialog.isShowing()) {
            progressDialog.show();
         }
      }

      @Override
      protected void onPostExecute(final Boolean b) {
         if (!progressDialog.isShowing()) {
            progressDialog.show();
         }
         if (!b) {
            Toast.makeText(BookForm.this, getString(R.string.msgGenerateCoverImageError), Toast.LENGTH_LONG).show();
         } else {
            Toast.makeText(BookForm.this, getString(R.string.msgBookUpdated), Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(BookForm.this, BookDetail.class);
            startActivity(intent);
         }
      }
   }
}
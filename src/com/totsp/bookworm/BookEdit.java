package com.totsp.bookworm;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;

import com.totsp.bookworm.model.Book;
import com.totsp.bookworm.util.AuthorsStringUtil;
import com.totsp.bookworm.util.DateUtil;

import java.util.Date;

public class BookEdit extends Activity {

   private BookWormApplication application;

   private ImageView bookCover;
   private EditText bookTitle;
   private EditText bookSubTitle;
   private EditText bookAuthors;
   private EditText bookSubject;
   private EditText bookDatePub;
   private EditText bookPublisher;

   private Button saveButton;

   private RatingBar ratingBar;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      this.application = (BookWormApplication) this.getApplication();

      setContentView(R.layout.bookedit);

      this.bookCover = (ImageView) this.findViewById(R.id.bookcover);
      this.bookTitle = (EditText) this.findViewById(R.id.booktitle);
      this.bookSubTitle = (EditText) this.findViewById(R.id.booksubtitle);
      this.bookAuthors = (EditText) this.findViewById(R.id.bookauthors);
      this.bookSubject = (EditText) this.findViewById(R.id.booksubject);
      this.bookDatePub = (EditText) this.findViewById(R.id.bookdatepub);
      this.bookPublisher = (EditText) this.findViewById(R.id.bookpublisher);

      this.ratingBar = (RatingBar) this.findViewById(R.id.bookrating);

      this.saveButton = (Button) this.findViewById(R.id.bookeditsavebutton);
      this.saveButton.setOnClickListener(new OnClickListener() {
         public void onClick(final View v) {
            BookEdit.this.saveEdits();
            BookEdit.this.startActivity(new Intent(BookEdit.this, Main.class));
         }
      });

      this.setViewData();
   }

   @Override
   public void onStart() {
      super.onStart();
   }

   @Override
   public void onPause() {
      this.bookTitle = null;
      super.onPause();
   }

   @Override
   protected void onStop() {
      super.onStop();
   }

   private void saveEdits() {
      // TODO AsyncTask      
      Book book = this.application.getSelectedBook();
      if (book != null) {

         Book newBook = new Book();

         newBook.setTitle(this.bookTitle.getText().toString());
         newBook.setSubTitle(this.bookSubTitle.getText().toString());         
         newBook.setAuthors(AuthorsStringUtil.expandAuthors(this.bookAuthors.getText().toString()));
         newBook.setSubject(this.bookSubject.getText().toString());
         newBook.setDatePubStamp(DateUtil.parse(this.bookDatePub.getText().toString()).getTime());
         newBook.setPublisher(this.bookPublisher.getText().toString());
         newBook.setRating(Math.round(this.ratingBar.getRating()));

         // TODO properties not yet editable, but should be
         newBook.setCoverImageId(book.getCoverImageId());
         newBook.setCoverImageTinyId(book.getCoverImageTinyId());
         newBook.setBlurb(book.getBlurb());
         newBook.setDescription(book.getDescription());
         newBook.setFormat(book.getFormat());
         newBook.setIsbn13(book.getIsbn13());
         newBook.setIsbn10(book.getIsbn10());
         newBook.setRead(book.isRead());

         newBook.setId(book.getId());

         this.application.getDataHelper().updateBook(newBook);
      }
   }

   private void setViewData() {
      Book book = this.application.getSelectedBook();
      if (book != null) {
         if (book.getCoverImageId() > 0) {
            Bitmap coverImage = application.getDataImageHelper().getBitmap((int) book.getCoverImageId());
            bookCover.setImageBitmap(coverImage);
         } else {
            bookCover.setImageResource(R.drawable.book_cover_missing);
         }

         this.bookTitle.setText(book.getTitle());
         this.bookSubTitle.setText(book.getSubTitle());

         this.ratingBar.setRating(new Float(book.getRating()));

         this.bookAuthors.setText(AuthorsStringUtil.contractAuthors(book.getAuthors()));
         this.bookSubject.setText(book.getSubject());
         this.bookDatePub.setText(DateUtil.format(new Date(book.getDatePubStamp())));
         this.bookPublisher.setText(book.getPublisher());
      }
   }

   @Override
   protected void onRestoreInstanceState(Bundle savedInstanceState) {
      super.onRestoreInstanceState(savedInstanceState);
      if (this.application.getSelectedBook() == null) {
         this.application.establishSelectedBook(savedInstanceState.getString(Constants.ISBN));
         this.setViewData();
      }
   }

   @Override
   protected void onSaveInstanceState(Bundle saveState) {
      // TODO add fallback to book isbn13 support
      saveState.putString(Constants.ISBN, this.application.getSelectedBook().getIsbn10());
      super.onSaveInstanceState(saveState);
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu) {

      return super.onCreateOptionsMenu(menu);
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {      
      switch (item.getItemId()) {

      default:
         return super.onOptionsItemSelected(item);
      }
   }
}
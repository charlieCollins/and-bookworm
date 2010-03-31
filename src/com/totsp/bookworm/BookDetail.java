package com.totsp.bookworm;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RatingBar.OnRatingBarChangeListener;

import com.totsp.bookworm.model.Author;
import com.totsp.bookworm.model.Book;
import com.totsp.bookworm.util.DateUtil;

import java.util.Date;

public class BookDetail extends Activity {

   private static final int MENU_EDIT = 0;
   private static final int MENU_WEB_GOOGLE = 1;
   private static final int MENU_WEB_AMAZON = 2;

   private BookWormApplication application;

   private ImageView bookCover;
   private TextView bookTitle;
   private TextView bookSubTitle;
   private TextView bookAuthors;
   private TextView bookSubject;
   private TextView bookDatePub;
   private TextView bookPublisher;

   private CheckBox readStatus;
   private RatingBar ratingBar;

   @Override
   public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      this.setContentView(R.layout.bookdetail);
      this.application = (BookWormApplication) this.getApplication();

      this.bookCover = (ImageView) this.findViewById(R.id.bookcover);
      this.bookTitle = (TextView) this.findViewById(R.id.booktitle);
      this.bookSubTitle = (TextView) this.findViewById(R.id.booksubtitle);
      this.bookAuthors = (TextView) this.findViewById(R.id.bookauthors);
      this.bookSubject = (TextView) this.findViewById(R.id.booksubject);
      this.bookDatePub = (TextView) this.findViewById(R.id.bookdatepub);
      this.bookPublisher = (TextView) this.findViewById(R.id.bookpublisher);

      this.readStatus = (CheckBox) this.findViewById(R.id.bookreadstatus);
      this.ratingBar = (RatingBar) this.findViewById(R.id.bookrating);

      this.readStatus.setOnCheckedChangeListener(new OnCheckedChangeListener() {
         public void onCheckedChanged(final CompoundButton button, final boolean isChecked) {
            // NOTE not sure why change listener fires when onCreate is init, but does
            BookDetail.this.saveReadStatusEdit();
         }
      });

      this.ratingBar.setOnRatingBarChangeListener(new OnRatingBarChangeListener() {
         public void onRatingChanged(final RatingBar rb, final float val, final boolean b) {
            BookDetail.this.saveRatingEdit();
         }
      });

      this.setViewData();
   }

   @Override
   public void onPause() {
      this.bookTitle = null;
      super.onPause();
   }

   private void saveRatingEdit() {
      Book book = this.application.getSelectedBook();
      if (book != null) {
         book.rating = (Math.round(this.ratingBar.getRating()));
         BookDetail.this.application.getDataHelper().updateBook(book);
      }
   }

   private void saveReadStatusEdit() {
      Book book = this.application.getSelectedBook();
      if (book != null) {
         book.read = (this.readStatus.isChecked());
         BookDetail.this.application.getDataHelper().updateBook(book);
      }
   }

   private void setViewData() {
      Book book = this.application.getSelectedBook();
      if (book != null) {
         Bitmap coverImage = this.application.getDataImageHelper().retrieveBitmap(book.title, book.id, false);
         if (coverImage != null) {
            this.bookCover.setImageBitmap(coverImage);
         } else {
            this.bookCover.setImageResource(R.drawable.book_cover_missing);
         }

         this.bookTitle.setText(book.title);
         this.bookSubTitle.setText(book.subTitle);

         String authors = null;
         for (Author a : book.authors) {
            if (authors == null) {
               authors = a.name;
            } else {
               authors += ", " + a.name;
            }
         }

         this.ratingBar.setRating(book.rating);
         this.readStatus.setChecked(book.read);
         this.bookDatePub.setText(DateUtil.format(new Date(book.datePubStamp)));
         this.bookAuthors.setText(authors);

         // we leave publisher and subject out of landscape layout         
         if (this.bookSubject != null) {
            this.bookSubject.setText(book.subject);
         }

         if (this.bookPublisher != null) {
            this.bookPublisher.setText(book.publisher);
         }
      }
   }

   @Override
   protected void onRestoreInstanceState(final Bundle savedInstanceState) {
      super.onRestoreInstanceState(savedInstanceState);
      if (this.application.getSelectedBook() == null) {
         Long id = savedInstanceState.getLong(Constants.BOOK_ID);
         if (id != null) {
            this.application.establishSelectedBook(id);
            if (this.application.getSelectedBook() != null) {
               this.setViewData();
            } else {
               this.startActivity(new Intent(this, Main.class));
            }
         } else {
            this.startActivity(new Intent(this, Main.class));
         }
      }
   }

   @Override
   protected void onSaveInstanceState(final Bundle saveState) {
      if (this.application.getSelectedBook() != null) {
         saveState.putLong(Constants.BOOK_ID, this.application.getSelectedBook().id);
      }
      super.onSaveInstanceState(saveState);
   }

   @Override
   public boolean onCreateOptionsMenu(final Menu menu) {
      menu.add(0, BookDetail.MENU_EDIT, 0, "Edit").setIcon(android.R.drawable.ic_menu_edit);
      menu.add(0, BookDetail.MENU_WEB_GOOGLE, 1, null).setIcon(R.drawable.google);
      menu.add(0, BookDetail.MENU_WEB_AMAZON, 2, null).setIcon(R.drawable.amazon);
      return super.onCreateOptionsMenu(menu);
   }

   @Override
   public boolean onOptionsItemSelected(final MenuItem item) {
      Uri uri = null;
      switch (item.getItemId()) {
      case MENU_EDIT:
         this.startActivity(new Intent(this, BookEdit.class));
         return true;
      case MENU_WEB_GOOGLE:
         // TODO add fallback book isbn13 support
         uri = Uri.parse("http://books.google.com/books?isbn=" + this.application.getSelectedBook().isbn10);
         this.startActivity(new Intent(Intent.ACTION_VIEW, uri));
         return true;
      case MENU_WEB_AMAZON:
         // TODO add fallback book isbn13 support
         uri =
                  Uri.parse("http://www.amazon.com/gp/search?keywords=" + this.application.getSelectedBook().isbn10
                           + "&index=books");
         this.startActivity(new Intent(Intent.ACTION_VIEW, uri));
         return true;
      default:
         return super.onOptionsItemSelected(item);
      }
   }
}
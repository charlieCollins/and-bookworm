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

import com.totsp.bookworm.data.DataImageHelper;
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

      this.application = (BookWormApplication) this.getApplication();

      this.setContentView(R.layout.bookdetail);

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
            // TODO not sure why change listener fires when onCreate is init, but does
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
         book.setRating(Math.round(this.ratingBar.getRating()));
         BookDetail.this.application.getDataHelper().updateBook(book);
      }
   }
   
   private void saveReadStatusEdit() {
      Book book = this.application.getSelectedBook();
      if (book != null) {         
         book.setRead(this.readStatus.isChecked());
         BookDetail.this.application.getDataHelper().updateBook(book);
      }
   }

   private void setViewData() {
      Book book = this.application.getSelectedBook();
      if (book != null) {
         ///if (book.getCoverImageId() > 0) {
            ///Bitmap coverImage = this.application.getDataImageHelper().getBitmap((int) book.getCoverImageId());
            Bitmap coverImage = DataImageHelper.retrieveBitmap(BookDetail.this, book.getTitle(), false); 
            this.bookCover.setImageBitmap(coverImage);
         ///} else {
         ///   this.bookCover.setImageResource(R.drawable.book_cover_missing);
         ///}

         this.bookTitle.setText(book.getTitle());
         this.bookSubTitle.setText(book.getSubTitle());

         String authors = null;
         for (Author a : book.getAuthors()) {
            if (authors == null) {
               authors = a.getName();
            } else {
               authors += ", " + a.getName();
            }
         }

         this.ratingBar.setRating(new Float(book.getRating()));
         this.readStatus.setChecked(book.isRead());
         this.bookDatePub.setText(DateUtil.format(new Date(book.getDatePubStamp())));
         this.bookAuthors.setText(authors);
         
         // we leave publisher and subject out of landscape layout         
         if (this.bookSubject != null) {
            this.bookSubject.setText(book.getSubject());
         }         
         
         if (this.bookPublisher != null) {            
            this.bookPublisher.setText(book.getPublisher());
         }
      }
   }

   @Override
   protected void onRestoreInstanceState(final Bundle savedInstanceState) {
      super.onRestoreInstanceState(savedInstanceState);
      if (this.application.getSelectedBook() == null) {
         String title = savedInstanceState.getString(Constants.TITLE);
         if (title != null) {
            this.application.establishSelectedBook(title);
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
         saveState.putString(Constants.TITLE, this.application.getSelectedBook().getTitle());
      }
      super.onSaveInstanceState(saveState);
   }

   @Override
   public boolean onCreateOptionsMenu(final Menu menu) {
      menu.add(0, BookDetail.MENU_EDIT, 0, "Edit").setIcon(android.R.drawable.ic_menu_edit);
      menu.add(0, BookDetail.MENU_WEB_GOOGLE, 1, "Google Books page").setIcon(android.R.drawable.ic_menu_view);
      menu.add(0, BookDetail.MENU_WEB_AMAZON, 2, "Amazon page").setIcon(android.R.drawable.ic_menu_view);
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
         uri = Uri.parse("http://books.google.com/books?isbn=" + this.application.getSelectedBook().getIsbn10());
         this.startActivity(new Intent(Intent.ACTION_VIEW, uri));
         return true;
      case MENU_WEB_AMAZON:
         // TODO add fallback book isbn13 support
         uri =
                  Uri.parse("http://www.amazon.com/gp/search?keywords="
                           + this.application.getSelectedBook().getIsbn10() + "&index=books");
         this.startActivity(new Intent(Intent.ACTION_VIEW, uri));
         return true;
      default:
         return super.onOptionsItemSelected(item);
      }
   }
}
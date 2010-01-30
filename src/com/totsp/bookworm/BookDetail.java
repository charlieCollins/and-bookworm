package com.totsp.bookworm;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.totsp.bookworm.model.Author;
import com.totsp.bookworm.model.Book;
import com.totsp.bookworm.util.DateUtil;

import java.util.Date;

public class BookDetail extends Activity {

   private static final int MENU_WEB_OPENLIB = 0;
   private static final int MENU_WEB_GOOGLE = 1;
   private static final int MENU_WEB_AMAZON = 2;
   private static final int MENU_EDIT = 3;

   private BookWormApplication application;

   private ImageView bookCover;
   private TextView bookTitle;
   private TextView bookAuthors;
   private TextView bookSubject;
   private TextView bookDatePub;
   private TextView bookPublisher;

   private RatingBar ratingBar;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      this.application = (BookWormApplication) this.getApplication();

      setContentView(R.layout.bookdetail);

      // TODO check if selectedBook present?
      this.bookCover = (ImageView) this.findViewById(R.id.bookcover);
      this.bookTitle = (TextView) this.findViewById(R.id.booktitle);
      this.bookAuthors = (TextView) this.findViewById(R.id.bookauthors);
      this.bookSubject = (TextView) this.findViewById(R.id.booksubject);
      this.bookDatePub = (TextView) this.findViewById(R.id.bookdatepub);
      this.bookPublisher = (TextView) this.findViewById(R.id.bookpublisher);

      ///this.ratingBar = (RatingBar) this.findViewById(R.id.bookrating);
      ///TODO this.ratingBar.setRating(3.0f);      

      Book book = this.application.getSelectedBook();
      if (book != null) {
         if (book.getCoverImageId() > 0) {
            Bitmap coverImage = application.getDataImageHelper().getImage((int) book.getCoverImageId());
            if (coverImage != null && coverImage.getWidth() > 10) {
               bookCover.setImageBitmap(coverImage);
            } else {
               bookCover.setImageDrawable(getResources().getDrawable(R.drawable.book_cover_missing));
            }
         }

         this.bookTitle.setText(book.getTitle());

         String authors = null;
         for (Author a : book.getAuthors()) {
            if (authors == null) {
               authors = a.getName();
            } else {
               authors += ", " + a.getName();
            }
         }

         this.bookAuthors.setText(authors);
         this.bookSubject.setText(book.getSubject());
         this.bookDatePub.setText(DateUtil.format(new Date(book.getDatePubStamp())));
         this.bookPublisher.setText(book.getPublisher());
      } else {
         Toast.makeText(this, "Sorry, there is a problem re-loading a selected book, try selecting it again.",
                  Toast.LENGTH_SHORT).show();
         this.startActivity(new Intent(this, Main.class));
      }
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

   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      menu.add(0, MENU_WEB_OPENLIB, 0, "OpenLibrary page").setIcon(android.R.drawable.ic_menu_view);
      menu.add(0, MENU_WEB_GOOGLE, 1, "Google Books page").setIcon(android.R.drawable.ic_menu_view);
      menu.add(0, MENU_WEB_AMAZON, 2, "Amazon page").setIcon(android.R.drawable.ic_menu_view);
      menu.add(0, MENU_EDIT, 3, "Edit").setIcon(android.R.drawable.ic_menu_edit);
      return super.onCreateOptionsMenu(menu);
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      Uri uri = null;
      switch (item.getItemId()) {
      case MENU_WEB_OPENLIB:
         Toast.makeText(this, "TODO OpenLib Page", Toast.LENGTH_SHORT).show();
         return true;
      case MENU_WEB_GOOGLE:
         uri = Uri.parse("http://books.google.com/books?isbn=" + this.application.getSelectedBook().getIsbn());
         this.startActivity(new Intent(Intent.ACTION_VIEW, uri));
         return true;
      case MENU_WEB_AMAZON:
         uri = Uri.parse("http://amzn.com/" + this.application.getSelectedBook().getIsbn());
         this.startActivity(new Intent(Intent.ACTION_VIEW, uri));
         return true;
      case MENU_EDIT:
         Toast.makeText(this, "TODO Edit", Toast.LENGTH_SHORT).show();
         return true;
      default:
         return super.onOptionsItemSelected(item);
      }
   }
}
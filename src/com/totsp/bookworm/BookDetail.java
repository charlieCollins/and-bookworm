package com.totsp.bookworm;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.totsp.bookworm.model.Author;
import com.totsp.bookworm.model.Book;

public class BookDetail extends Activity {

   private BookWormApplication application;
   
   private ImageView bookCover;
   private TextView bookTitle;   
   private TextView bookAuthors;
   
   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      
      this.application = (BookWormApplication) this.getApplication();
      
      setContentView(R.layout.bookdetail); 
      
      // TODO check if selectedBook present?
      this.bookCover = (ImageView) this.findViewById(R.id.bookcover);
      this.bookTitle = (TextView) this.findViewById(R.id.booktitle);
      this.bookAuthors = (TextView) this.findViewById(R.id.bookauthors);
      
      Book book = this.application.getSelectedBook();
      
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
}
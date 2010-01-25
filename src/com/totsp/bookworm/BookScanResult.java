package com.totsp.bookworm;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.totsp.bookworm.data.DataHelper;
import com.totsp.bookworm.data.GoogleBookDataSource;
import com.totsp.bookworm.data.IBookDataSource;
import com.totsp.bookworm.model.Author;
import com.totsp.bookworm.model.Book;
import com.totsp.bookworm.util.BookImageUtil;
import com.totsp.bookworm.util.DateUtil;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

public class BookScanResult extends Activity {

   private IBookDataSource bookDataSource;
   private DataHelper dataHelper;

   private Button scanBookAddButton;
   private TextView scanBookTitle;
   private ImageView scanBookCover;
   private TextView scanBookAuthor;
   private TextView scanBookDate;
   
   private Book book;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      this.bookDataSource = new GoogleBookDataSource();
      this.dataHelper = new DataHelper(this);

      setContentView(R.layout.bookscanresult);

      this.scanBookTitle = (TextView) this.findViewById(R.id.scanBookTitle);
      this.scanBookCover = (ImageView) this.findViewById(R.id.scanBookCover);
      this.scanBookAuthor = (TextView) this.findViewById(R.id.scanBookAuthor);
      this.scanBookDate = (TextView) this.findViewById(R.id.scanBookDate);

      this.scanBookAddButton = (Button) this.findViewById(R.id.scanbookaddbutton);
      this.scanBookAddButton.setOnClickListener(new OnClickListener() {
         public void onClick(View v) {
            // TODO create method for this
            if (book != null && book.getIsbn() != null) {
               Log.d(Splash.APP_NAME, "Book object created, and ADD pressed");               
               // TODO don't even let users get here if book exists, remove add button prev              
               Book retrieve = dataHelper.selectBook(book.getIsbn());
               if (retrieve == null) {
                  Log.d(Splash.APP_NAME, "Book does not already exist in DB, attempt to insert");               
                  dataHelper.insertBook(book);
               } else {
                  Log.d(Splash.APP_NAME, "Book already exists in DB, ignore");               
               }
            }
            startActivity(new Intent(BookScanResult.this, Main.class));
         }
      });

      String isbn = this.getIntent().getStringExtra("SCAN_RESULT_CONTENTS");
      Log.d(Splash.APP_NAME, "ISBN after scan - " + isbn);
      if (isbn == null || isbn.length() < 10 || isbn.length() > 13) {
         this.setViewsForInvalidScan();
      } else {
         // launch Task
         new GetBookDataTask().execute(isbn);
      }
   }
   
   private class GetBookDataTask extends AsyncTask<String, Void, Void> {
      private ProgressDialog dialog = new ProgressDialog(BookScanResult.this);

      private Book bookTask;
      private Bitmap bookCoverBitmap;

      // can use UI thread here
      protected void onPreExecute() {
         dialog.setMessage("Retrieving book data..");
         dialog.show();
      }

      // automatically done on worker thread (separate from UI thread)
      protected Void doInBackground(String... isbns) {
         bookTask = bookDataSource.getBook(isbns[0]);
         // TODO better book cover get stuff (HttpHelper binary)
         String imageUrl = BookImageUtil.getCoverUrlMedium(isbns[0]);
         Log.d(Splash.APP_NAME, "book cover imageUrl - " + imageUrl);
         if ((imageUrl != null) && !imageUrl.equals("")) {
            try {
               URL url = new URL(imageUrl);
               URLConnection conn = url.openConnection();
               conn.connect();
               BufferedInputStream bis = new BufferedInputStream(conn.getInputStream());
               bookCoverBitmap = BitmapFactory.decodeStream(bis);
            } catch (IOException e) {
               Log.e(Splash.APP_NAME, " ", e);
            }
         }
         return null;
      }

      // can use UI thread here
      protected void onPostExecute(Void unused) {
         dialog.dismiss();

         if (bookTask != null) {
            scanBookTitle.setText(bookTask.getTitle());
            String authors = null;
            for (Author a : bookTask.getAuthors()) {
               if (authors == null) {
                  authors = a.getName();
               } else {
                  authors += ", " + a.getName();
               }               
            }
            scanBookAuthor.setText(authors);
            scanBookDate.setText(DateUtil.format(bookTask.getDatePub()));

            if (bookCoverBitmap != null) {
               scanBookCover.setImageBitmap(bookCoverBitmap);
            } else {
               scanBookCover.setImageResource(R.drawable.book_cover_missing);
            }
            
            book = bookTask;
            scanBookAddButton.setVisibility(View.VISIBLE);
         } else {
          setViewsForInvalidScan();
         }
      }
   }
   
   private void setViewsForInvalidScan() {
      this.scanBookCover.setImageResource(R.drawable.book_invalid_isbn);
      this.scanBookTitle.setText("Whoops, that scan worked but the number doesn't seem to be an ISBN,"
               + " make sure the item is a book and try again.");
   }

   @Override
   public void onStart() {
      super.onStart();
   }

   @Override
   public void onPause() {
      super.onPause();
   }

   @Override
   protected void onStop() {
      super.onStop();
   }
}
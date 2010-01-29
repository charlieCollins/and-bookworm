package com.totsp.bookworm;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class BookDetail extends Activity {

   private BookWormApplication application;
   
   private TextView output;
   private String bookTitle;
   
   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      
      this.application = (BookWormApplication) this.getApplication();
      
      setContentView(R.layout.bookdetail); 
      
      // TODO check if selectedBook present?
      this.bookTitle = this.application.getSelectedBook().getTitle();
      
      this.output = (TextView) this.findViewById(R.id.detailOutput);
      this.output.setText("selected book - " + this.bookTitle);
      
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
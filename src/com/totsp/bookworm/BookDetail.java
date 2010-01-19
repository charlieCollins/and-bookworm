package com.totsp.bookworm;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.totsp.bookworm.data.DbConstants;

public class BookDetail extends Activity {

   private TextView output;
   private String bookTitle;
   
   // TODO got here
   
   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.bookdetail); 
      
      this.bookTitle = this.getIntent().getStringExtra(DbConstants.TITLE);
      
      this.output = (TextView) this.findViewById(R.id.output);
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
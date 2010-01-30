package com.totsp.bookworm;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class BookEntrySearch extends Activity {

   TextView output;
   
   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.bookentrysearch);
      
      this.output = (TextView) this.findViewById(R.id.bookentrysearchoutput);
      this.output.setText("TODO - create search for entry");
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
   
   @Override
   protected void onRestoreInstanceState(Bundle savedInstanceState) {
       super.onRestoreInstanceState(savedInstanceState);
   }

   @Override
   protected void onSaveInstanceState(Bundle saveState) {
       super.onSaveInstanceState(saveState);       
   }
   
   @Override
   public boolean onCreateOptionsMenu(Menu menu) {      
      ///menu.add(0, MENU_HELP, 0, "Help").setIcon(android.R.drawable.ic_menu_help);
      return super.onCreateOptionsMenu(menu);
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      /*
      switch (item.getItemId()) {
      case MENU_HELP:
         this.startActivity(new Intent(Main.this, Help.class));
         return true;
      }
      */
      return super.onOptionsItemSelected(item);
   }
}
package com.totsp.bookworm;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class Help extends Activity {

   Button bToMain;
   
   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.help);  
      
      this.bToMain = (Button) findViewById(R.id.button_tomain);
      this.bToMain.setOnClickListener(new OnClickListener() {
         public void onClick(View v) {
            Help.this.startActivity(new Intent(Help.this, Main.class));
         }
      });
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
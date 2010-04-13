package com.totsp.bookworm;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class About extends Activity {

   private static final int MENU_DETAIL = 0;

   TextView about;

   @Override
   public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      this.setContentView(R.layout.about);

      this.about = (TextView) this.findViewById(R.id.aboutcontent);
      this.about.setText(Html.fromHtml(this.getResources().getString(R.string.aboutcontent)),
               TextView.BufferType.SPANNABLE);
      this.about.setMovementMethod(LinkMovementMethod.getInstance());
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
   protected void onRestoreInstanceState(final Bundle savedInstanceState) {
      super.onRestoreInstanceState(savedInstanceState);
   }

   @Override
   protected void onSaveInstanceState(final Bundle saveState) {
      super.onSaveInstanceState(saveState);
   }

   @Override
   public boolean onCreateOptionsMenu(final Menu menu) {
      menu.add(0, About.MENU_DETAIL, 0, "About Details").setIcon(android.R.drawable.ic_menu_info_details);
      return super.onCreateOptionsMenu(menu);
   }

   @Override
   public boolean onOptionsItemSelected(final MenuItem item) {
      switch (item.getItemId()) {
      case MENU_DETAIL:
         new AlertDialog.Builder(About.this).setTitle("About BookWorm").setMessage(
                  this.getResources().getString(R.string.aboutdetail)).setNeutralButton("Dismiss",
                  new DialogInterface.OnClickListener() {
                     public void onClick(final DialogInterface d, final int i) {

                     }
                  }).show();
         return true;
      }
      return super.onOptionsItemSelected(item);
   }
}
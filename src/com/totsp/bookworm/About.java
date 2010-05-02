package com.totsp.bookworm;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class About extends Activity {

   private TextView about;
   private Button aboutDetails;

   @Override
   public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      this.setContentView(R.layout.about);

      this.about = (TextView) this.findViewById(R.id.aboutcontent);
      this.about.setText(Html.fromHtml(this.getResources().getString(R.string.aboutcontent)),
               TextView.BufferType.SPANNABLE);
      this.about.setMovementMethod(LinkMovementMethod.getInstance());

      this.aboutDetails = (Button) this.findViewById(R.id.aboutdetails);
      this.aboutDetails.setOnClickListener(new OnClickListener() {
         public void onClick(View v) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("file:///android_asset/release_notes.html"),
                     About.this, HTMLActivity.class));
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

   @Override
   protected void onRestoreInstanceState(final Bundle savedInstanceState) {
      super.onRestoreInstanceState(savedInstanceState);
   }

   @Override
   protected void onSaveInstanceState(final Bundle saveState) {
      super.onSaveInstanceState(saveState);
   }
}
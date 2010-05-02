package com.totsp.bookworm;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.Button;

public class HTMLScreen extends Activity {

   @Override
   public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      this.setContentView(R.layout.html_view);
      WebView webview = (WebView) this.findViewById(R.id.webkit);
      Uri u = this.getIntent().getData();
      webview.loadUrl(u.toString());
      Button done = (Button) this.findViewById(R.id.closeButton);
      done.setOnClickListener(new OnClickListener() {

         public void onClick(final View v) {
            HTMLScreen.this.finish();
         }
      });
   }
}
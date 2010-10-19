package com.totsp.bookworm;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.Button;

public class HtmlScreen extends Activity {

   @Override
   public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      this.setContentView(R.layout.html_view);
      WebView webview = (WebView) findViewById(R.id.webkit);
      Uri u = getIntent().getData();
      webview.loadUrl(u.toString());
      Button done = (Button) findViewById(R.id.closeButton);
      done.setOnClickListener(new OnClickListener() {
         public void onClick(final View v) {
            finish();
         }
      });
   }
}
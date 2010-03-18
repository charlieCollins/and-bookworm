package com.totsp.bookworm;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.totsp.bookworm.data.HttpHelper;

import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;

import java.net.URLEncoder;

public class ProviderLogin extends Activity {

   // TODO need to make all this "provider" stuff generalized, support multiple providers
   
   // TODO working on Google OAuth via Android and Signpost - not there yet

   String consumerKey = "totsp-BookWorm-1.0.0";
   String consumerSecret = "";

   private BookWormApplication application;

   private TextView info;
   private EditText emailaddress;
   private Button button;

   @Override
   public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      this.application = (BookWormApplication) this.getApplication();

      this.setContentView(R.layout.providerlogin);

      this.button = (Button) this.findViewById(R.id.provider_button);
      this.button.setOnClickListener(new OnClickListener() {
         public void onClick(final View v) {

         }
      });

      this.info = (TextView) this.findViewById(R.id.login_info);

      this.emailaddress = (EditText) this.findViewById(R.id.emailaddress);

      String stringFormat = this.getResources().getString(R.string.login_info_format);
      Log.d(Constants.LOG_TAG, "stringFormat - " + stringFormat);
      Log.d(Constants.LOG_TAG, "providerName - " + this.application.getBookDataSource().getProviderName());
      String infoString = String.format(stringFormat, this.application.getBookDataSource().getProviderName());
      Log.d(Constants.LOG_TAG, "infoString - " + infoString);
      this.info.setText(infoString);
      
      new GoogleOAuthTask().execute();

   }

   private class GoogleOAuthTask extends AsyncTask<String, Void, String> {

      private final HttpHelper httpHelper = new HttpHelper();

      private final ProgressDialog dialog = new ProgressDialog(ProviderLogin.this);

      // can use UI thread here
      protected void onPreExecute() {
         this.dialog.setMessage("Logging in to provider...");
         this.dialog.show();
      }

      // automatically done on worker thread (separate from UI thread)
      protected String doInBackground(final String... args) {

         try {

            OAuthConsumer consumer = new CommonsHttpOAuthConsumer("totsp-BookWorm-1.0.0", "etpfOSfQ4e9xnfgOJETy4D56");

            //String scope = URLEncoder.encode("http://www.google.com/books/feeds/", "utf-8");
            
            String scope = URLEncoder.encode("http://www.blogger.com/feeds", "utf-8");

            OAuthProvider provider =
                     new DefaultOAuthProvider("https://www.google.com/accounts/OAuthGetRequestToken?scope=" + scope,
                              "https://www.google.com/accounts/OAuthGetAccessToken",
                              "https://www.google.com/accounts/OAuthAuthorizeToken?hd=default");

            System.out.println("Fetching request token...");
            
            String authUrl = provider.retrieveRequestToken(consumer, OAuth.OUT_OF_BAND);
            
            System.out.println("Request token: " + consumer.getToken());
            System.out.println("Token secret: " + consumer.getTokenSecret());
            
            System.out.println("Now visit:\n" + authUrl + "\n... and grant this app authorization");

         } catch (Exception e) {
            e.printStackTrace();
         }

         return null;
      }

      // can use UI thread here
      protected void onPostExecute(final String result) {
         if (this.dialog.isShowing()) {
            this.dialog.dismiss();
         }

         String authToken = result;
         if ((authToken != null) && authToken.contains("Auth=")) {
            authToken = authToken.substring(authToken.indexOf("Auth=") + 5, authToken.length()).trim();
            ProviderLogin.this.application.setProviderToken(authToken);
         } else {
            Toast.makeText(ProviderLogin.this, "Unable to obtain ClientLogin token, invalid response.",
                     Toast.LENGTH_LONG);
         }

         Log.d(Constants.LOG_TAG, "authToken - " + authToken);
      }
   }

}
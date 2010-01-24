package com.totsp.bookworm.data;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

// TODO need to think this through some more, not sure it's necessary, and it's limited to Strings

/**
 * Wrapper to help make HTTP requests on Android easier - to encapsulate some of the lower
 * level details and bring HTTP to a higher level of abstraction on the platform.  
 * 
 * The concept here is to perform HTTP request using Apache HttpClient via an HttpHelper class
 * that deals with all responses as Strings. Responses are then passed into Android Handler
 * as Messages. Each request is made in a new Thread (so as not to block the Android UI Thread). 
 * 
 * Users construct this class with a Handler, and then invoke the "performX" methods. Upon response
 * the Handler will be populated and the response sent as a String Message (keyed either as
 * HTTP_RESPONSE or HTTP_RESPONSE_ERROR if an error is encountered).
 * 
 * Basic Usage:
 * 
 * Step 1. Create an <code>android.os.Handler</code> to be able to get an asynchronous, separate Thread based, response.
 *  <pre>
 *  {@code
 *   TODO
 *  }
 *  </pre>
 *
 * TODO better mechanism to report success/error, rather than Strings
 * 
 * @author charliecollins
 * 
 */
public class HttpHelperAndroid {

   private static final String CLASSTAG = HttpHelperAndroid.class.getSimpleName();

   private HttpHelper httpHelper;
   private Handler handler;

   /**
    * Constructor that requires an Android <code>Handler</code> as a parameter
    * and will ultimately return any HTTP request method response as a <code>String</code>
    * in the <code>Message</code> <code>Bundle</a> (with the key being the <code>String</code>
    * "HTTP_RESPONSE" or "HTTP_RESPONSE_ERROR").
    * 
    *  Public Constants for HTTP_RESPONSE and HTTP_RESPONSE_ERROR are available in <code>HttpHelper</code>.
    * 
    * @param handler
    */
   public HttpHelperAndroid(final Handler handler) {
      this.httpHelper = new HttpHelper();
      this.handler = handler;
   }

   /**
    * Perform a simple HTTP GET operation.
    * 
    */
   public void performGet(final String url) {
      new Thread() {
         public void run() {
            String response = httpHelper.performGet(url);
            if (response != null && response.contains(HttpHelper.HTTP_RESPONSE_ERROR)) {
               sendHandlerMessage(handler, HttpHelper.HTTP_RESPONSE_ERROR, response);
            } else {
               sendHandlerMessage(handler, HttpHelper.HTTP_RESPONSE, response);
            }
         }
      }.start();
   }

   /**
    * Perform an HTTP GET operation with user/pass and headers.
    * 
    */
   public void performGet(final String url, final String user, final String pass,
            final Map<String, String> additionalHeaders) {
      new Thread() {
         public void run() {
            String response = httpHelper.performGet(url, user, pass, additionalHeaders);
            if (response != null && response.contains(HttpHelper.HTTP_RESPONSE_ERROR)) {
               sendHandlerMessage(handler, HttpHelper.HTTP_RESPONSE_ERROR, response);
            } else {
               sendHandlerMessage(handler, HttpHelper.HTTP_RESPONSE, response);
            }
         }
      }.start();
   }

   /**
    * Perform a simplified HTTP POST operation.
    * 
    */
   public void performPost(final String url, final Map<String, String> params) {
      new Thread() {
         public void run() {
            String response = httpHelper.performPost(url, params);
            if (response != null && response.contains(HttpHelper.HTTP_RESPONSE_ERROR)) {
               sendHandlerMessage(handler, HttpHelper.HTTP_RESPONSE_ERROR, response);
            } else {
               sendHandlerMessage(handler, HttpHelper.HTTP_RESPONSE, response);
            }
         }
      }.start();
   }

   /**
    * Perform an HTTP POST operation with user/pass, headers, request parameters, 
    * and a default content-type of "application/x-www-form-urlencoded."
    * 
    */
   public void performPost(final String url, final String user, final String pass,
            final Map<String, String> additionalHeaders, final Map<String, String> params) {
      new Thread() {
         public void run() {
            String response = httpHelper.performPost(url, user, pass, additionalHeaders, params);
            if (response != null && response.contains(HttpHelper.HTTP_RESPONSE_ERROR)) {
               sendHandlerMessage(handler, HttpHelper.HTTP_RESPONSE_ERROR, response);
            } else {
               sendHandlerMessage(handler, HttpHelper.HTTP_RESPONSE, response);
            }
         }
      }.start();
   }

   /**
    * Perform an HTTP POST operation with all parameters optional
    * (the complicated/flexible version of the method).
    * 
    */
   public void performPost(final String contentType, final String url, final String user, final String pass,
            final Map<String, String> additionalHeaders, final Map<String, String> params) {
      new Thread() {
         public void run() {
            String response = httpHelper.performPost(contentType, url, user, pass, additionalHeaders, params);
            if (response != null && response.contains(HttpHelper.HTTP_RESPONSE_ERROR)) {
               sendHandlerMessage(handler, HttpHelper.HTTP_RESPONSE_ERROR, response);
            } else {
               sendHandlerMessage(handler, HttpHelper.HTTP_RESPONSE, response);
            }
         }
      }.start();
   }

   //
   // private
   //

   private void sendHandlerMessage(Handler handler, String key, String value) {
      Message message = handler.obtainMessage();
      Bundle bundle = new Bundle();
      bundle.putString(key, value);
      message.setData(bundle);
      handler.sendMessage(message);
      Log.d(CLASSTAG, " handler instance in use - " + handler);
      Log.d(CLASSTAG, " handler.sendMessage (with response) invoked");
   }

   public static String inputStreamToString(final InputStream stream) throws IOException {
      BufferedReader br = new BufferedReader(new InputStreamReader(stream), 8192);
      StringBuilder sb = new StringBuilder();
      String line = null;
      while ((line = br.readLine()) != null) {
         sb.append(line + "\n");
      }
      br.close();
      return sb.toString();
   }
}

package com.totsp.bookworm.data;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wrapper to help make HTTP requests easier - after all, we want to make it nice for the people.
 * This class makes each "perform" method (request) in a new <code>Thread</code> (will not block the Android UI)
 * and returns results in an Android <code>Handler</code> via a <code>Message</code>.
 * 
 * Basic Usage (if getting the response as a String is acceptable):
 * 
 * Step 1. Create an <code>android.os.Handler</code> to be able to get an asynchronous, separate thread based, response.
 *  <pre>
 *  {@code
 *  private Handler httpHandler = new Handler() {
 *      public void handleMessage(final Message msg) {
 *        String responseError = msg.getData().getString(HTTPRequestHelper.HTTP_RESPONSE_ERROR);
 *        if (responseError != null) {
 *           // handle error here
 *        }
 *        String response = msg.getData().getString(HTTPRequestHelper.HTTP_RESPONSE);
 *        if (response != null) {
 *           // handle success here - "response" is String result
 *        }
 *     }
 *  };
 *  }
 *  </pre>
 *  
 * Step 2. Create an instance of HTTPRequestHelper, and use it:
 * <pre>
 * {@code
 * HTTPRequestHelper httpHelper = new HTTPRequestHelper(httpHandler);
 * httpHelper.performGet(url);
 * }
 * </pre>
 * 
 * TODO better mechanism to report success/error, rather than Strings
 * TODO cookies 
 * TODO multi-part binary data
 * TODO shutdown connection mgr? - client.getConnectionManager().shutdown();
 * TODO tests - have some, but can't get Handler Message in test cases (works when run, not when test)? 
 * 
 * @author charliecollins
 * 
 */
public class HTTPRequestHelper {

   private static final String CLASSTAG = HTTPRequestHelper.class.getSimpleName();

   private static final int POST_TYPE = 1;
   private static final int GET_TYPE = 2;
   private static final String CONTENT_TYPE = "Content-Type";
   
   public static final String MIME_FORM_ENCODED = "application/x-www-form-urlencoded";
   public static final String MIME_TEXT_PLAIN = "text/plain";   
   public static final String HTTP_RESPONSE = "HTTP_RESPONSE"; 
   public static final String HTTP_RESPONSE_ERROR = "HTTP_RESPONSE_ERROR"; 
      

   // Establish client once, as static field with static setup block.
   // (This is a best practice in HttpClient docs - but will leave reference until *process* stopped on Android.)
   private static final DefaultHttpClient client;
   static {      
      HttpParams params = new BasicHttpParams();      
      params.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
      params.setParameter(CoreProtocolPNames.HTTP_CONTENT_CHARSET, HTTP.UTF_8);
      params.setParameter(CoreProtocolPNames.USER_AGENT, "Apache-HttpClient/Android");      
      params.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 15000);
      params.setParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false);      
      SchemeRegistry schemeRegistry = new SchemeRegistry();
      schemeRegistry.register(
               new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
      schemeRegistry.register(
               new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
      ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager(params, schemeRegistry);      
      client = new DefaultHttpClient(cm, params);      
   }

   private final ResponseHandler<?> responseHandler;

  /**
   * Constructor that accepts any <code>ResponseHandler</code> as a parameter.
   * 
   * You can create your own <code>ResponseHandler</code> any do anything needed with
   * the HTTP response using this advanced approach. Most users will not need to do this, 
   * and can instead simply use the alternative constructor that takes an Android <code>Handler</code> only.
   * 
   * @param responseHandler
   */
   public HTTPRequestHelper(final ResponseHandler<?> responseHandler) {
      this.responseHandler = responseHandler;
   }

   /**
    * Constructor that accepts an Android <code>Handler</code> as a parameter
    * and will ultimately return any HTTP request method response as a <code>String</code>
    * in the <code>Message</code> <code>Bundle</a> with the key being the <code>String</code>
    * "HTTP_RESPONSE" (see public constant for this value in this class). 
    * 
    * @param handler
    */
   public HTTPRequestHelper(final Handler handler) {
      this(HTTPRequestHelper.getResponseHandlerInstance(handler));
   }

   /**
    * Perform a simple HTTP GET operation.
    * 
    */
   public void performGet(final String url) {
      performRequest(null, url, null, null, null, null, HTTPRequestHelper.GET_TYPE);
   }

   /**
    * Perform an HTTP GET operation with user/pass and headers.
    * 
    */
   public void performGet(final String url, final String user, final String pass,
            final Map<String, String> additionalHeaders) {
      performRequest(null, url, user, pass, additionalHeaders, null, HTTPRequestHelper.GET_TYPE);
   }
   
   
   /**
    * Perform a simplified HTTP POST operation.
    * 
    */
   public void performPost(final String url, final Map<String, String> params) {
      performRequest(HTTPRequestHelper.MIME_FORM_ENCODED, url, null, null, null, params,
               HTTPRequestHelper.POST_TYPE);
   }
   
   /**
    * Perform an HTTP POST operation with user/pass, headers, request parameters, 
    * and a default content-type of "application/x-www-form-urlencoded."
    * 
    */
   public void performPost(final String url, final String user, final String pass,
            final Map<String, String> additionalHeaders, final Map<String, String> params) {
      performRequest(HTTPRequestHelper.MIME_FORM_ENCODED, url, user, pass, additionalHeaders, params,
               HTTPRequestHelper.POST_TYPE);
   }

   /**
    * Perform an HTTP POST operation with flexible parameters (the complicated/flexible version of the method).
    * 
    */
   public void performPost(final String contentType, final String url, final String user, final String pass,
            final Map<String, String> additionalHeaders, final Map<String, String> params) {
      performRequest(contentType, url, user, pass, additionalHeaders, params, HTTPRequestHelper.POST_TYPE);
   }  

   //
   // private methods
   //
   
   private void performRequest(final String contentType, final String url, final String user, final String pass,
            final Map<String, String> headers, final Map<String, String> params, final int requestType) {

      Log.d(CLASSTAG, " making HTTP request to url - " + url);

      // add user and pass to client credentials if present
      if ((user != null) && (pass != null)) {
         Log.d(CLASSTAG, " user and pass present, adding credentials to request");
         client.getCredentialsProvider().setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(user, pass));
      }

      // process headers using request interceptor
      final Map<String, String> sendHeaders = new HashMap<String, String>();
      if ((headers != null) && (headers.size() > 0)) {
         sendHeaders.putAll(headers);
      }
      if (requestType == HTTPRequestHelper.POST_TYPE) {
         sendHeaders.put(HTTPRequestHelper.CONTENT_TYPE, contentType);
      }
      if (sendHeaders.size() > 0) {
         client.addRequestInterceptor(new HttpRequestInterceptor() {
            public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {
               for (String key : sendHeaders.keySet()) {
                  if (!request.containsHeader(key)) {
                     Log.d(CLASSTAG, " adding header: " + key + " | "
                              + sendHeaders.get(key));
                     request.addHeader(key, sendHeaders.get(key));
                  }
               }
            }
         });
      }

      // handle POST or GET request respectively
      HttpRequestBase method = null;
      if (requestType == HTTPRequestHelper.POST_TYPE) {
         Log.d(CLASSTAG, " performRequest POST");
         method = new HttpPost(url);
         // data - name/value params
         List<NameValuePair> nvps = null;
         if ((params != null) && (params.size() > 0)) {
            nvps = new ArrayList<NameValuePair>();
            for (String key : params.keySet()) {
               Log.d(CLASSTAG, " adding param: " + key + " | " + params.get(key));
               nvps.add(new BasicNameValuePair(key, params.get(key)));
            }
         }
         if (nvps != null) {
            try {
               HttpPost methodPost = (HttpPost) method;
               methodPost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            } catch (UnsupportedEncodingException e) {
               Log.e(CLASSTAG, " " + HTTPRequestHelper.CLASSTAG, e);
            }
         }        
      } else if (requestType == HTTPRequestHelper.GET_TYPE) {
         Log.d(CLASSTAG, " performRequest GET");
         method = new HttpGet(url);        
      }
      
      // execute request in separate Thread (don't block Android main/UI thread - use ResponseHandler for result)
      final HttpRequestBase executeMethod = method;
      new Thread() {
         @Override
         public void run() {
            execute(executeMethod);
         }
      }.start();      
   }

   private synchronized void execute(HttpRequestBase method) {
      Log.d(CLASSTAG, " execute invoked");

      // create a response specifically for errors (in case)
      BasicHttpResponse errorResponse = new BasicHttpResponse(new ProtocolVersion("HTTP_ERROR", 1, 1), 500, "ERROR");

      try {
         client.execute(method, this.responseHandler);
         Log.d(CLASSTAG, " request completed");
      } catch (Exception e) {
         Log.e(CLASSTAG, " ", e);
         errorResponse.setReasonPhrase(e.getMessage());
         try {
            this.responseHandler.handleResponse(errorResponse);
         } catch (Exception ex) {
            Log.e(CLASSTAG, " ", ex);
         }
      }
   }
   
   private static ResponseHandler<String> getResponseHandlerInstance(final Handler handler) {
      final ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
         public String handleResponse(final HttpResponse response) {
            String responseString = null;;
            StatusLine status = response.getStatusLine();
            Log.d(CLASSTAG, " statusCode - " + status.getStatusCode());
            Log.d(CLASSTAG, " statusReasonPhrase - " + status.getReasonPhrase());
            HttpEntity entity = response.getEntity();           
            if (entity != null) {
               try {
                  responseString = HTTPRequestHelper.inputStreamToString(entity.getContent());
                  Log.d(CLASSTAG, " http response - \n" + responseString);  
                  sendHandlerMessage(handler, HTTP_RESPONSE, responseString);                            
               } catch (IOException e) {
                  Log.e(CLASSTAG, " ", e);                  
                  responseString = HTTP_RESPONSE_ERROR + " - " + e.getMessage();
                  sendHandlerMessage(handler, HTTP_RESPONSE_ERROR, responseString);
               }
            } else {
               Log.w(CLASSTAG, " empty response, HTTP error occurred");
               responseString = HTTP_RESPONSE_ERROR + " - empty response";
               sendHandlerMessage(handler, HTTP_RESPONSE_ERROR, "Error - empty response");               
            }
            return responseString;
         }
      };
      return responseHandler;
   }
   
   private static void sendHandlerMessage(Handler handler, String key, String value) {
      Message message = handler.obtainMessage();
      Bundle bundle = new Bundle();
      bundle.putString(key, value);
      message.setData(bundle);      
      handler.sendMessage(message);
      Log.d(CLASSTAG, " handler instance in use - " + handler);                  
      Log.d(CLASSTAG, " handler.sendMessage (with response) invoked");       
   }

   private static String inputStreamToString(final InputStream stream) throws IOException {
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

package com.totsp.bookworm.data;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Apache HttpClient helper class for performing HTTP requests.
 * 
 * This class is intentionally *not* bound to any Android classes so that it is easier 
 * to develop and test. Use calls to this class inside Android AsyncTask implementations
 * to make HTTP requests asynchronous and not block the UI Thread.
 * 
 * TODO cookies 
 * TODO multi-part binary data
 * TODO shutdown connection mgr? - client.getConnectionManager().shutdown();
 * 
 * @author ccollins
 *
 */
public class HttpHelper {

   private static final String CLASSTAG = HttpHelper.class.getSimpleName();   
   
   private static final String CONTENT_TYPE = "Content-Type";
   private static final int POST_TYPE = 1;
   private static final int GET_TYPE = 2;

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
      schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
      schemeRegistry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
      ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager(params, schemeRegistry);
      client = new DefaultHttpClient(cm, params);
   }

   private ResponseHandler<String> responseHandler;

   /**
    * Constructor.
    * 
    */
   public HttpHelper() {
      this.responseHandler = new BasicResponseHandler();
   }

   /**
    * Perform a simple HTTP GET operation.
    * 
    */
   public String performGet(final String url) {
      return performRequest(null, url, null, null, null, null, HttpHelper.GET_TYPE);
   }

   /**
    * Perform an HTTP GET operation with user/pass and headers.
    * 
    */
   public String performGet(final String url, final String user, final String pass,
            final Map<String, String> additionalHeaders) {
      return performRequest(null, url, user, pass, additionalHeaders, null, HttpHelper.GET_TYPE);
   }

   /**
    * Perform a simplified HTTP POST operation.
    * 
    */
   public String performPost(final String url, final Map<String, String> params) {
      return performRequest(HttpHelper.MIME_FORM_ENCODED, url, null, null, null, params, HttpHelper.POST_TYPE);
   }

   /**
    * Perform an HTTP POST operation with user/pass, headers, request parameters, 
    * and a default content-type of "application/x-www-form-urlencoded."
    * 
    */
   public String performPost(final String url, final String user, final String pass,
            final Map<String, String> additionalHeaders, final Map<String, String> params) {
      return performRequest(HttpHelper.MIME_FORM_ENCODED, url, user, pass, additionalHeaders, params,
               HttpHelper.POST_TYPE);
   }

   /**
    * Perform an HTTP POST operation with flexible parameters (the complicated/flexible version of the method).
    * 
    */
   public String performPost(final String contentType, final String url, final String user, final String pass,
            final Map<String, String> additionalHeaders, final Map<String, String> params) {
      return performRequest(contentType, url, user, pass, additionalHeaders, params, HttpHelper.POST_TYPE);
   }

   //
   // private methods
   //

   private String performRequest(final String contentType, final String url, final String user, final String pass,
            final Map<String, String> headers, final Map<String, String> params, final int requestType) {
      System.out.println(CLASSTAG + " making HTTP request to url - " + url);

      // add user and pass to client credentials if present
      if ((user != null) && (pass != null)) {
         System.out.println(CLASSTAG + " user and pass present, adding credentials to request");
         client.getCredentialsProvider().setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(user, pass));
      }

      // process headers using request interceptor
      final Map<String, String> sendHeaders = new HashMap<String, String>();
      if ((headers != null) && (headers.size() > 0)) {
         sendHeaders.putAll(headers);
      }
      if (requestType == HttpHelper.POST_TYPE) {
         sendHeaders.put(HttpHelper.CONTENT_TYPE, contentType);
      }
      if (sendHeaders.size() > 0) {
         client.addRequestInterceptor(new HttpRequestInterceptor() {
            public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {
               for (String key : sendHeaders.keySet()) {
                  if (!request.containsHeader(key)) {
                     System.out.println(CLASSTAG + " adding header: " + key + " | " + sendHeaders.get(key));
                     request.addHeader(key, sendHeaders.get(key));
                  }
               }
            }
         });
      }

      // handle POST or GET request respectively
      HttpRequestBase method = null;
      if (requestType == HttpHelper.POST_TYPE) {
         System.out.println(CLASSTAG + " performRequest POST");
         method = new HttpPost(url);
         // data - name/value params
         List<NameValuePair> nvps = null;
         if ((params != null) && (params.size() > 0)) {
            nvps = new ArrayList<NameValuePair>();
            for (String key : params.keySet()) {
               System.out.println(CLASSTAG + " adding param: " + key + " | " + params.get(key));
               nvps.add(new BasicNameValuePair(key, params.get(key)));
            }
         }
         if (nvps != null) {
            try {
               HttpPost methodPost = (HttpPost) method;
               methodPost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            } catch (UnsupportedEncodingException e) {
               e.printStackTrace();
               ///System.err.println(CLASSTAG + " " + e.getMessage());
            }
         }
      } else if (requestType == HttpHelper.GET_TYPE) {
         System.out.println(CLASSTAG + " performRequest GET");
         method = new HttpGet(url);
      }

      // execute request
      return execute(method);
   } 

   private synchronized String execute(HttpRequestBase method) {
      System.out.println(CLASSTAG + " execute invoked");
      String response = null;
      // execute method returns?!? (rather than async) - do it here sync, and wrap async elsewhere
      try {
         response = client.execute(method, this.responseHandler);
      } catch (ClientProtocolException e) {
         response = HTTP_RESPONSE_ERROR + " - " +  e.getClass().getSimpleName() + " " + e.getMessage();
         //e.printStackTrace();
      } catch (IOException e) {
         response = HTTP_RESPONSE_ERROR + " - " +  e.getClass().getSimpleName() + " " + e.getMessage();
         //e.printStackTrace();
      }
      System.out.println(CLASSTAG + " request completed");
      return response;
   }
}

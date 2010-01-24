package com.totsp.bookworm.test;

import com.totsp.bookworm.data.HttpHelper;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;
import junit.framework.TestCase;

public class HttpHelperTest extends TestCase {

   private HttpHelper httpHelper;
  
   public HttpHelperTest() {
      super();
   }

   public void setUp() throws Exception {
      super.setUp();
      
      // helper
      this.httpHelper = new HttpHelper();
   }

   public void testGet() throws Exception {
      System.out.println("BookWorm TEST HTTP GET");

      String url = "http://www.google.com";
      String response = this.httpHelper.performGet(url); 
      System.out.println("Response - " + response);
      Assert.assertNotNull(response);     
   }
   
   public void testGetSecure() throws Exception {
      System.out.println("BookWorm TEST HTTPS GET");

      String url = "https://mail.google.com";
      String response = this.httpHelper.performGet(url); 
      System.out.println("Response - " + response);
      Assert.assertNotNull(response);     
   }
   
   public void testGetError() throws Exception {
      System.out.println("BookWorm TEST HTTP GET ERROR (invalid host)");

      String url = "http://www.googleggg.com";
      String response = this.httpHelper.performGet(url); 
      System.out.println("Response - " + response);
      Assert.assertNotNull(response);     
   }  
   
   public void testPost() throws Exception {
      System.out.println("BookWorm TEST HTTP POST");
     
      String url = "http://www.snee.com/xml/crud/posttest.html";
      Map<String, String> params = new HashMap<String, String>();
      params.put("fname", "first");
      params.put("lname", "lirst");
      String response = this.httpHelper.performGet(url); 
      System.out.println("Response - " + response);
      Assert.assertNotNull(response);
   }
}

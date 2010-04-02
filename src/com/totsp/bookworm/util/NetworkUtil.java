package com.totsp.bookworm.util;

import android.util.Log;

import com.totsp.bookworm.Constants;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class NetworkUtil {

   public static String getIpAddress() {
      try {
         for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
            NetworkInterface intf = en.nextElement();
            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
               InetAddress inetAddress = enumIpAddr.nextElement();
               if (!inetAddress.isLoopbackAddress()) {
                  return inetAddress.getHostAddress().toString();
               }
            }
         }
      } catch (SocketException e) {
         Log.e(Constants.LOG_TAG, e.getMessage(), e);
      }
      return null;
   }
}

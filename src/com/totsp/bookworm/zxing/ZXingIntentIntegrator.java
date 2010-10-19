package com.totsp.bookworm.zxing;

/*
 * Copyright 2009 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.totsp.bookworm.Constants;
import com.totsp.bookworm.R;

/**
 * <p>A utility class which helps ease integration with Barcode Scanner via {@link Intent}s. This is a simple
 * way to invoke barcode scanning and receive the result, without any need to integrate, modify, or learn the
 * project's source code.</p>
 *
 * <h2>Initiating a barcode can</h2>
 *
 * <p>Integration is essentially as easy as calling {@link #initiateScan(Activity)} and waiting
 * for the result in your app.</p>
 *
 * <p>It does require that the Barcode Scanner application is installed. The
 * {@link #initiateScan(Activity)} method will prompt the user to download the application, if needed.</p>
 *
 * <p>There are a few steps to using this integration. First, your {@link Activity} must implement
 * the method {@link Activity#onActivityResult(int, int, Intent)} and include a line of code like this:</p>
 *
 * <p>{@code
 * public void onActivityResult(int requestCode, int resultCode, Intent intent) {
 *   IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
 *   if (scanResult != null) {
 *     // handle scan result
 *   }
 *   // else continue with any other code you need in the method
 *   ...
 * }
 * }</p>
 *
 * <p>This is where you will handle a scan result.
 * Second, just call this in response to a user action somewhere to begin the scan process:</p>
 *
 * <p>{@code integrator.initiateScan();}</p>
 *
 * <p>You can use {@link #initiateScan(Activity, String, String, String, String)} or
 * {@link #initiateScan(Activity, int, int, int, int)} to customize the download prompt with
 * different text labels.</p>
 *
 * <h2>Sharing text via barcode</h2>
 *
 * <p>To share text, encoded as a QR Code on-screen, similarly, see {@link #shareText(Activity, String)}.</p>
 *
 * <p>Some code, particularly download integration, was contributed from the Anobiit application.</p>
 *
 * @author Sean Owen
 * @author Fred Lin
 * @author Isaac Potoczny-Jones
 */
public final class ZXingIntentIntegrator {

   public static final int REQUEST_CODE = 0x0ba7c0de; // get it?

   private ZXingIntentIntegrator() {
   }

   /**
    * See {@link #initiateScan(Activity, String, String, String, String)} --
    * same, but uses default English labels.
    */
   public static void initiateScan(final Activity activity) {
      ZXingIntentIntegrator.initiateScan(activity, activity.getString(R.string.labelInstallScanner),
               activity.getString(R.string.msgScannerNotPresent), activity.getString(R.string.btnYes),
               activity.getString(R.string.btnNo));
   }

   /**
    * See {@link #initiateScan(Activity, String, String, String, String)} --
    * same, but takes string IDs which refer
    * to the {@link Activity}'s resource bundle entries.
    */
   public static void initiateScan(final Activity activity, final int stringTitle, final int stringMessage,
            final int stringButtonYes, final int stringButtonNo) {
      ZXingIntentIntegrator.initiateScan(activity, activity.getString(stringTitle), activity.getString(stringMessage),
               activity.getString(stringButtonYes), activity.getString(stringButtonNo));
   }

   /**
    * Invokes scanning.
    *
    * @param stringTitle title of dialog prompting user to download Barcode Scanner
    * @param stringMessage text of dialog prompting user to download Barcode Scanner
    * @param stringButtonYes text of button user clicks when agreeing to download
    *  Barcode Scanner (e.g. "Yes")
    * @param stringButtonNo text of button user clicks when declining to download
    *  Barcode Scanner (e.g. "No")
    * @return the contents of the barcode that was scanned, or null if none was found
    * @throws InterruptedException if timeout expires before a scan completes
    */
   public static void initiateScan(final Activity activity, final String stringTitle, final String stringMessage,
            final String stringButtonYes, final String stringButtonNo) {
      Intent intentScan = new Intent("com.google.zxing.client.android.SCAN");
      intentScan.putExtra("MODE", "PRODUCT_MODE");
      intentScan.addCategory(Intent.CATEGORY_DEFAULT);
      try {
         activity.startActivityForResult(intentScan, ZXingIntentIntegrator.REQUEST_CODE);
      } catch (ActivityNotFoundException e) {
         Log.w(Constants.LOG_TAG, "Barcode Scanner (com.google.zxing.client.android.SCAN) Intent not present.");
         ZXingIntentIntegrator
                  .showDownloadDialog(activity, stringTitle, stringMessage, stringButtonYes, stringButtonNo);
      }
   }

   private static void showDownloadDialog(final Activity activity, final String stringTitle,
            final String stringMessage, final String stringButtonYes, final String stringButtonNo) {
      AlertDialog.Builder downloadDialog = new AlertDialog.Builder(activity);
      downloadDialog.setTitle(stringTitle);
      downloadDialog.setMessage(stringMessage);
      downloadDialog.setPositiveButton(stringButtonYes, new DialogInterface.OnClickListener() {
         public void onClick(final DialogInterface dialogInterface, final int i) {
            Uri uri = Uri.parse("market://search?q=pname:com.google.zxing.client.android");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            activity.startActivity(intent);
         }
      });
      downloadDialog.setNegativeButton(stringButtonNo, new DialogInterface.OnClickListener() {
         public void onClick(final DialogInterface dialogInterface, final int i) {
         }
      });
      downloadDialog.show();
   }

   /**
    * <p>Call this from your {@link Activity}'s
    * {@link Activity#onActivityResult(int, int, Intent)} method.</p>
    *
    * @return null if the event handled here was not related to {@link IntentIntegrator}, or
    *  else an {@link IntentResult} containing the result of the scan. If the user cancelled scanning,
    *  the fields will be null.
    */
   public static ZXingIntentResult parseActivityResult(final int requestCode, final int resultCode, final Intent intent) {
      if (requestCode == ZXingIntentIntegrator.REQUEST_CODE) {
         if (resultCode == Activity.RESULT_OK) {
            String contents = intent.getStringExtra("SCAN_RESULT");
            String formatName = intent.getStringExtra("SCAN_RESULT_FORMAT");
            return new ZXingIntentResult(contents, formatName);
         } else {
            Log.e(Constants.LOG_TAG, "Barcode Scanner returned an invalid result.");
            return new ZXingIntentResult(null, null);
         }
      }
      return null;
   }

   /**
    * See {@link #shareText(Activity, String, String, String, String, String)} --
    * same, but uses default English labels.
    */
   public static void shareText(final Activity activity, final String text) {
      ZXingIntentIntegrator.shareText(activity, text, activity.getString(R.string.labelInstallScanner),
               activity.getString(R.string.msgScannerNotPresent), activity.getString(R.string.btnYes),
               activity.getString(R.string.btnNo));
   }

   /**
    * See {@link #shareText(Activity, String, String, String, String, String)} --
    * same, but takes string IDs which refer to the {@link Activity}'s resource bundle entries.
    */
   public static void shareText(final Activity activity, final String text, final int stringTitle,
            final int stringMessage, final int stringButtonYes, final int stringButtonNo) {
      ZXingIntentIntegrator.shareText(activity, text, activity.getString(stringTitle), activity
               .getString(stringMessage), activity.getString(stringButtonYes), activity.getString(stringButtonNo));
   }

   /**
    * Shares the given text by encoding it as a barcode, such that another user can
    * scan the text off the screen of the device.
    *
    * @param text the text string to encode as a barcode
    * @param stringTitle title of dialog prompting user to download Barcode Scanner
    * @param stringMessage text of dialog prompting user to download Barcode Scanner
    * @param stringButtonYes text of button user clicks when agreeing to download
    *  Barcode Scanner (e.g. "Yes")
    * @param stringButtonNo text of button user clicks when declining to download
    *  Barcode Scanner (e.g. "No")
    */
   public static void shareText(final Activity activity, final String text, final String stringTitle,
            final String stringMessage, final String stringButtonYes, final String stringButtonNo) {

      Intent intent = new Intent();
      intent.setAction("com.google.zxing.client.android.ENCODE");
      intent.putExtra("ENCODE_TYPE", "TEXT_TYPE");
      intent.putExtra("ENCODE_DATA", text);
      try {
         activity.startActivity(intent);
      } catch (ActivityNotFoundException e) {
         ZXingIntentIntegrator
                  .showDownloadDialog(activity, stringTitle, stringMessage, stringButtonYes, stringButtonNo);
      }
   }

}
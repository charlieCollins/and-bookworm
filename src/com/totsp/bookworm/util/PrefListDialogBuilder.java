package com.totsp.bookworm.util;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import java.util.ArrayList;

/**
 * Custom builder for preference list selection dialogs that allows lists to be built up one item at a time and
 * associates labels with preference values to allow the majority of the onClick behaviour to be handled by this
 * class. The onClick call is chained so that additional behaviour can be defined by calling setOnClickListener. Note
 * that this does not override the default behaviour. 
 */
public class PrefListDialogBuilder extends Builder {

   private SharedPreferences prefs;
   private String prefKey;
   private ArrayList<String> labels;
   private ArrayList<String> prefValues;
   private DialogInterface.OnClickListener clientListener;

   public PrefListDialogBuilder(Context context, SharedPreferences prefs, final String prefKey) {
      super(context);
      clientListener = null;
      labels = new ArrayList<String>();
      prefValues = new ArrayList<String>();
      this.prefs = prefs;
      this.prefKey = prefKey;
   }

   @Override
   public PrefListDialogBuilder setTitle(CharSequence title) {
      super.setTitle(title);
      return this;
   }

   public PrefListDialogBuilder addEntry(final String label, final String value) {
      labels.add(label);
      prefValues.add(value);
      return this;
   }

   public PrefListDialogBuilder setOnClickListener(DialogInterface.OnClickListener listener) {
      clientListener = listener;
      return this;
   }

   @Override
   public AlertDialog create() {
      CharSequence[] charLabels = new CharSequence[labels.size()];
      for (int i = 0; i < labels.size(); i++) {
         charLabels[i] = labels.get(i);
      }
      this.setItems(charLabels, new DialogInterface.OnClickListener() {
         public void onClick(DialogInterface d, int selected) {
            Editor editor = prefs.edit();
            editor.putString(prefKey, prefValues.get(selected));
            editor.commit();

            if (clientListener != null) {
               clientListener.onClick(d, selected);
            }
         }
      });
      return super.create();
   }

}

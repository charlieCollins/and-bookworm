package com.totsp.bookworm;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.AdapterView.OnItemClickListener;

import com.totsp.bookworm.data.DataHelper;
import com.totsp.bookworm.data.DbConstants;

import java.util.ArrayList;

public class Main extends TabActivity {

   private static final int MENU_HELP = 0;

   private TabHost tabHost;
   private ListView bookList;

   private DataHelper dh;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      this.dh = new DataHelper(this);

      setContentView(R.layout.main); 
      
      // TODO a lot of cleanup here

      this.bookList = (ListView) this.findViewById(R.id.tab1);
      final ArrayList<String> bookNames = new ArrayList<String>();
      bookNames.addAll(this.dh.selectAllBookNames());
      Log.d(Splash.APP_NAME, "bookNames size - " + bookNames.size());      
      ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, bookNames);      
      this.bookList.setAdapter(adapter); 
      
      this.bookList.setOnItemClickListener(new OnItemClickListener() {
         public void onItemClick(AdapterView parent, View v, int index, long id) {
            Log.d(Splash.APP_NAME, "onItemClick - " + bookNames.get(index));
            Intent intent = new Intent(Main.this, BookDetail.class);
            intent.putExtra(DbConstants.TITLE, bookNames.get(index));
            Main.this.startActivity(intent);
         }
      });
      
      this.tabHost = this.getTabHost();      
      tabHost.addTab(tabHost.newTabSpec("tab1").setIndicator("Book List").setContent(R.id.tab1));
      tabHost.addTab(tabHost.newTabSpec("tab2").setIndicator("Book Gallery").setContent(R.id.tab2));
      tabHost.addTab(tabHost.newTabSpec("tab3").setIndicator("Add a Book").setContent(R.id.tab3));      
      this.tabHost.setCurrentTab(0);
      
       
   }

   @Override
   public void onPause() {
      this.dh.cleanup();
      super.onPause();
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      menu.add(0, MENU_HELP, 0, "Help").setIcon(android.R.drawable.ic_menu_help);
      return super.onCreateOptionsMenu(menu);
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      switch (item.getItemId()) {
      case MENU_HELP:
         this.startActivity(new Intent(Main.this, Help.class));
         return true;
      }
      return super.onOptionsItemSelected(item);
   }

}
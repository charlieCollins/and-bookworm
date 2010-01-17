package com.totsp.bookworm;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import com.totsp.bookworm.data.DataHelper;

public class Main extends Activity {
    
   private static final int MENU_HELP = 0;
   
   // TODO allow user to create lists, dynamically add buttons here
   // (default lists to "Books I've Read" and "Books I Want to Read" (but allow user to delete/create)
   private Button bReadList;
   private Button bToReadList; 
   
   private DataHelper dataHelper;
   
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        this.dataHelper = new DataHelper(this);
        
        setContentView(R.layout.main);
        
        this.bReadList = (Button) findViewById(R.id.button_read_list);
        this.bToReadList = (Button) findViewById(R.id.button_toread_list);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_HELP, 0, "Help")
                .setIcon(android.R.drawable.ic_menu_help);
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
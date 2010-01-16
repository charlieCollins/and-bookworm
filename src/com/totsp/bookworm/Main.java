package com.totsp.bookworm;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

public class Main extends Activity {
    
   private static final int MENU_HELP = 0;
   private Button bReadList;
   private Button bToReadList;   
   
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
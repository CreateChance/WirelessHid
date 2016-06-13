package com.baniel.wirelesshid;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.hardware.input.InputManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends Activity {

    private final String TAG = "MainActivity";

    private FragmentManager fragmentManager = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.d(TAG, "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fragmentManager = getFragmentManager();

        Intent intent = new Intent(MainActivity.this, WirelessHidService.class);
        startService(intent);
    }

    @Override
    protected void onDestroy() {

        Log.d(TAG, "onDestroy");

        super.onDestroy();

        Intent intent = new Intent(MainActivity.this, WirelessHidService.class);
        stopService(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        menu.add(Menu.NONE, Menu.NONE, 1, "Keyboard");

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Log.d(TAG, "item id: " + item.getItemId());
        switch (item.getItemId()) {
            case 0:
                Intent intent = new Intent(MainActivity.this, KeyboardFragment.class);
                startActivity(intent);
                break;
        }

        return super.onOptionsItemSelected(item);
    }
}

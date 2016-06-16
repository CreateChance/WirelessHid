package com.baniel.wirelesshid;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class MainActivity extends Activity implements WirelessHidService.DataHandlerListener {

    private final String TAG = "MainActivity";

    private long mPressTime = 0;

    private WirelessHidService mService = null;
    private Handler mDataSendHandler = null;

    private boolean mIsBound = false;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((WirelessHidService.MyBinder)service).getService();
            mDataSendHandler = mService.getDataSendHandler();
            mService.setListener(MainActivity.this);

            Toast.makeText(MainActivity.this, "Service connected!",
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mDataSendHandler = null;

            Toast.makeText(MainActivity.this, "Service disconnected!",
                    Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.d(TAG, "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = new Intent(MainActivity.this, WirelessHidService.class);
        startService(intent);
        doBindService();
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

    @Override
    public void onBackPressed() {

        long time = System.currentTimeMillis();

        if (time - mPressTime < 1500) {
            doUnbindService();
            Intent intent = new Intent(MainActivity.this, WirelessHidService.class);
            stopService(intent);

            finish();
        } else {
            Toast.makeText(this, "Press again to exit.", Toast.LENGTH_SHORT).show();
        }

        mPressTime = time;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "keycode: " + keyCode);

        WirelessHidProto.HidData data = null;

        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            data = WirelessHidProto.HidData.newBuilder()
                    .setType(WirelessHidProto.HidData.DataType.KEYBOARD_HIT)
                    .setKeyboardValue(39).build();
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            data = WirelessHidProto.HidData.newBuilder()
                    .setType(WirelessHidProto.HidData.DataType.KEYBOARD_HIT)
                    .setKeyboardValue(37).build();
        }

        if (mDataSendHandler != null && data != null) {
            Log.d(TAG, "send data");
            mDataSendHandler.obtainMessage(0, data).sendToTarget();
        }

        return data != null || super.onKeyDown(keyCode, event);
    }

    void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        bindService(new Intent(this,
                WirelessHidService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    void doUnbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    @Override
    public void onHandlerChanged(Handler handler) {
        this.mDataSendHandler = handler;
    }
}

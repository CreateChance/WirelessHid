package com.baniel.wirelesshid;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
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

    private ShakeDetector shakeDetector = null;

    private PowerManager.WakeLock mWakeLock = null;

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

        mWakeLock = ((PowerManager)this.getSystemService(Context.POWER_SERVICE)).
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WirelessHid");

        shakeDetector = new ShakeDetector(this);
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
            mDataSendHandler.obtainMessage(0, data).sendToTarget();
        }

        return data != null || super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (shakeDetector != null) {
            shakeDetector.stop();
        }

        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
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

    private class ShakeDetector implements SensorEventListener {
        private final int SPEED_SHRESHOLD = 5000;
        private final int UPTATE_INTERVAL_TIME = 70;
        private final int SHAKE_INTERVAL_TIME = 500;

        private boolean gotShake = false;

        private SensorManager sensorManager;

        private Sensor sensor;

        private Context context;

        private float lastX;
        private float lastY;
        private float lastZ;

        private long lastUpdateTime = 0;
        private long lastShakeTime = 0;

        public ShakeDetector(Context c) {
            context = c;
            start();
        }

        public void start() {
            sensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
            if(sensorManager != null) {
                sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            }
            if(sensorManager != null && sensor != null) {
                sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME, 0);
                mWakeLock.acquire();
            }

        }

        public void stop() {
            sensorManager.unregisterListener(this);
        }

        public void onSensorChanged(SensorEvent event) {

            if (gotShake) {
                return;
            }

            long currentUpdateTime = System.currentTimeMillis();

            long shakeInterval = currentUpdateTime - lastShakeTime;
            if (shakeInterval < SHAKE_INTERVAL_TIME) {
                return;
            }

            long updateInterval = currentUpdateTime - lastUpdateTime;

            if(updateInterval < UPTATE_INTERVAL_TIME) {
                return;
            }

            lastUpdateTime = currentUpdateTime;

            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            float deltaX = x - lastX;
            float deltaY = y - lastY;
            float deltaZ = z - lastZ;

            lastX = x;
            lastY = y;
            lastZ = z;

            double square = deltaX*deltaX + deltaY*deltaY + deltaZ*deltaZ;
            double speed = Math.sqrt(square)/updateInterval * 10000;

            if(speed >= SPEED_SHRESHOLD) {
                gotShake = true;
                lastShakeTime = System.currentTimeMillis();
                onShake();
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }

        private void onShake() {
            WirelessHidProto.HidData data = WirelessHidProto.HidData.newBuilder()
                        .setType(WirelessHidProto.HidData.DataType.KEYBOARD_HIT)
                        .setKeyboardValue(39).build();
            if (mDataSendHandler != null && data != null) {
                mDataSendHandler.obtainMessage(0, data).sendToTarget();
            }
            gotShake = false;
        }
    }
}

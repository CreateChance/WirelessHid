package com.baniel.wirelesshid;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

public class KeyboardFragment extends FragmentActivity implements WirelessHidService.DataHandlerListener{

    private final String TAG = "KeyboardFragment";

    public final static int KEYBOARD_TYPE_QWERTY = 0;
    public final static int KEYBOARD_TYPE_NAVIGATION_AND_NUMERIC = 1;

    private WirelessHidService mService = null;
    private Handler mDataSendHandler = null;
    private boolean mIsBound = false;

    private boolean mIsCapsLockActive = false;
    private boolean mIsNumLockActive = false;
    private boolean mIsScrollLockActive = false;

    private LongPressCheckTask mLongPressCheckTask = null;
    private boolean mIsLongPressed = false;

    private ViewGroup keyboard = null;

    public KeyboardFragment() {

    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((WirelessHidService.MyBinder)service).getService();
            mDataSendHandler = mService.getDataSendHandler();
            mService.setListener(KeyboardFragment.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mDataSendHandler = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.keyboard_fragment);

        View view = creatQwertyKeyboard(this);

        keyboard = (ViewGroup) this.findViewById(R.id.keyboard);
        keyboard.addView(view);

        mLongPressCheckTask = new LongPressCheckTask();

        doBindService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        doUnbindService();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        menu.add(Menu.NONE, 0, 1, "MainKeyboard");
        menu.add(Menu.NONE, 1, 2, "SideKeyboard");

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Log.d(TAG, "item id: " + item.getItemId());
        keyboard.removeAllViews();
        switch (item.getItemId()) {
            case 0:
                // main keyboard.
                keyboard.addView(creatQwertyKeyboard(this));
                break;
            case 1:
                // side keyboard.
                keyboard.addView(createNavigationAndNumericKeyboard(this));
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private Keyboard createKeyboard(Context context, int xmlResourceID) {

        final Keyboard keyboard = new Keyboard(context, xmlResourceID);
        keyboard.setKeyboardListener(new Keyboard.KeyboardListener() {
            @Override
            public void onKeyUp(int keyCode) {
                Log.d(TAG, "up keycode: " + keyCode);

                if (mDataSendHandler != null) {
                    mDataSendHandler.removeCallbacks(mLongPressCheckTask);
                }
                if (mIsLongPressed) {
                    mIsLongPressed = false;
                    WirelessHidProto.HidData data = WirelessHidProto.HidData.newBuilder()
                            .setType(WirelessHidProto.HidData.DataType.KEYBOARD_LONG_RELEASE)
                            .setKeyboardValue(keyCode).build();
                    if (mDataSendHandler != null) {
                        mDataSendHandler.obtainMessage(0, data).sendToTarget();
                    }
                } else {
                    WirelessHidProto.HidData data = WirelessHidProto.HidData.newBuilder()
                            .setType(WirelessHidProto.HidData.DataType.KEYBOARD_HIT)
                            .setKeyboardValue(keyCode).build();
                    if (mDataSendHandler != null) {
                        mDataSendHandler.obtainMessage(0, data).sendToTarget();
                    }
                }
            }

            @Override
            public void onKeyDown(int keyCode) {
                Log.d(TAG, "key down: " + keyCode);

                if (keyCode == 144) {
                    // 144 means number lock
                    mIsNumLockActive = !mIsNumLockActive;
                    KeyboardFragment.this.findViewById(R.id.led_numlock).
                            setBackgroundColor(getResources().getColor(mIsNumLockActive ? R.color.led_on : R.color.led_off));
                } else if (keyCode == 20) {
                    // 20 means caps lock.
                    mIsCapsLockActive = !mIsCapsLockActive;
                    KeyboardFragment.this.findViewById(R.id.led_capslock).
                            setBackgroundColor(getResources().getColor(mIsCapsLockActive ? R.color.led_on : R.color.led_off));
                } else if (keyCode == 145) {
                    // 145 means scroll lock
                    mIsScrollLockActive = !mIsScrollLockActive;
                    KeyboardFragment.this.findViewById(R.id.led_scrolllock).
                            setBackgroundColor(getResources().getColor(mIsScrollLockActive ? R.color.led_on : R.color.led_off));
                } else if (mDataSendHandler != null) {
                    mLongPressCheckTask.setKeyCode(keyCode);
                    mDataSendHandler.postDelayed(mLongPressCheckTask, 1000);
                }
            }
        });
        return keyboard;
    }

    private View creatQwertyKeyboard(Context context) {
        return createKeyboard(context, R.xml.qwerty_keyboard);
    }

    private View createNavigationAndNumericKeyboard(Context context) {
        ViewGroup view = (ViewGroup) View.inflate(context, R.layout.numeric_keyboard, null);
        ViewGroup child;

        child = (ViewGroup) view.findViewById(R.id.navigation_keyboard);
        child.addView(createKeyboard(context, R.xml.navigation_keyboard));

        child = (ViewGroup) view.findViewById(R.id.numeric_keyboard);
        child.addView(createKeyboard(context, R.xml.numeric_keyboard));

        return view;
    }

    @Override
    public void onHandlerChanged(Handler handler) {
        this.mDataSendHandler = handler;
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

    private class LongPressCheckTask implements Runnable {

        private int keyCode = -1;

        public LongPressCheckTask() {
            this(-1);
        }

        public LongPressCheckTask(int code) {
            this.keyCode = code;
        }

        public void setKeyCode(int code) {
            this.keyCode = code;
        }

        @Override
        public void run() {
            WirelessHidProto.HidData data = WirelessHidProto.HidData.newBuilder()
                    .setType(WirelessHidProto.HidData.DataType.KEYBOARD_LONG_PRESS)
                    .setKeyboardValue(keyCode).build();

            mDataSendHandler.obtainMessage(0, data).sendToTarget();
            mIsLongPressed = true;
        }
    }
}

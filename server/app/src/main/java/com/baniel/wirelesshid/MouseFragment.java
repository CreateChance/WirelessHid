package com.baniel.wirelesshid;

import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.Date;

public class MouseFragment extends Fragment implements WirelessHidService.DataHandlerListener {

    private final String TAG = "MouseFragment";

    private View mTouchpad;

    private View mScrollZone;

    private int mSpeed = 3;

    private int mScrollSpeed = 1;

    private long time = 0;

    //ms and position between first and second click, bigger than this is single click
    //otherwise is double click.
    private int mDoubleClickTimeThreshold = 80;
    private int mDoubleClickPosThreshold = 1;

    private WirelessHidService mService = null;
    private Handler mDataSendHandler = null;
    private boolean mIsBound = false;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((WirelessHidService.MyBinder)service).getService();
            mDataSendHandler = mService.getDataSendHandler();
            mService.setListener(MouseFragment.this);

            Toast.makeText(getActivity(), "Service connected!",
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mDataSendHandler = null;

            Toast.makeText(getActivity(), "Service disconnected!",
                    Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mouse, container, false);

        // setup touchpad
        mTouchpad = view.findViewById(R.id.touchpad);
        mTouchpad.setOnTouchListener(new OnTouchListener() {

            private int mPrevX;

            private int mPrevY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        //single and double click handle here.
                        mPrevX = (int) event.getX();
                        mPrevY = (int) event.getY();
                        time = new Date().getTime();
                        break;
                    case MotionEvent.ACTION_UP:
                        if (new Date().getTime() - time < mDoubleClickTimeThreshold) {
                            if ((int) event.getX() - mPrevX < mDoubleClickPosThreshold
                                    && (int) event.getY() - mPrevY < mDoubleClickPosThreshold) {
                                mouseClickPress(Constant.MOUSE_BUTTON_LEFT);
                                mouseClickRelease(Constant.MOUSE_BUTTON_LEFT);
                            }
                        }

                    case MotionEvent.ACTION_MOVE:
                        //mouse move handle here.
                        int x = (int) (event.getX() * mSpeed);
                        int y = (int) (event.getY() * mSpeed);

                        mouseMove(x - mPrevX, y - mPrevY);

                        mPrevX = x;
                        mPrevY = y;
                        break;
                }

                return true;
            }
        });

        // setup scroll
        mScrollZone = view.findViewById(R.id.scrollzone);
        mScrollZone.setOnTouchListener(new OnTouchListener() {

            private int mPrevY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        //click scroll handle here.
                        mPrevY = (int) (event.getY() * mScrollSpeed);
                        break;

                    case MotionEvent.ACTION_MOVE:
                        //mouse scroll handle here.
                        int amt = (int) (event.getY() * mScrollSpeed);

                        mouseScroll(mPrevY - amt);

                        mPrevY = amt;
                        break;
                }

                return true;
            }
        });

        // setup buttons
        ViewGroup bar = (ViewGroup) view.findViewById(R.id.buttons);
        int count = bar.getChildCount();

        for (int i = 0; i < count; i++) {
            View child = bar.getChildAt(i);

            try {
                Button button = (Button) child;
                button.setOnTouchListener(new OnTouchListener() {

                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        int which = Integer.valueOf((String) v.getTag());

                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                //mouse button pressed
                                //para which shows which button is pressed
                                //0 is left button
                                //1 is not used(reserved).
                                //2 is right button
                                if (which == 0) {
                                    mouseClickPress(Constant.MOUSE_BUTTON_LEFT);
                                } else if (which == 1) {
                                    //Do nothing for now.
                                } else if (which == 2) {
                                    mouseClickPress(Constant.MOUSE_BUTTON_RIGHT);
                                }
                                break;

                            case MotionEvent.ACTION_UP:
                                //mouse button released
                                if (which == 0) {
                                    mouseClickRelease(Constant.MOUSE_BUTTON_LEFT);
                                } else if (which == 1) {
                                    //Do nothing for now.
                                } else if (which == 2) {
                                    mouseClickRelease(Constant.MOUSE_BUTTON_RIGHT);
                                }
                                break;
                        }

                        return false;
                    }

                });
            } catch (ClassCastException e) {
                // not a button :)
            }
        }

        // setup speed controls
        bar = (ViewGroup) view.findViewById(R.id.speed_control);
        count = bar.getChildCount();

        for (int i = 0; i < count; i++) {
            View child = bar.getChildAt(i);

            try {
                ToggleButton button = (ToggleButton) child;
                button.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        ToggleButton button = (ToggleButton) v;

                        // do not allow to uncheck button
                        if (!button.isChecked()) {
                            button.setChecked(true);
                            return;
                        }

                        updateSpeed(Integer.parseInt((String) button.getTag()));
                    }

                });
            } catch (ClassCastException e) {
                // not a button :)
            }
        }

        // setup scroll speed controls
        bar = (ViewGroup) view.findViewById(R.id.scroll_speed_control);
        count = bar.getChildCount();

        for (int i = 0; i < count; i++) {
            View child = bar.getChildAt(i);

            try {
                ToggleButton button = (ToggleButton) child;
                button.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        ToggleButton button = (ToggleButton) v;

                        // do not allow to uncheck button
                        if (!button.isChecked()) {
                            button.setChecked(true);
                            return;
                        }

                        updateScrollSpeed(Integer.parseInt((String) button.getTag()));
                    }

                });
            } catch (ClassCastException e) {
                // not a button :)
            }
        }

        doBindService();

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        // initial value
        updateSpeed(1);
        updateScrollSpeed(1);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        doUnbindService();
    }

    void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        getActivity().bindService(new Intent(getActivity(),
                WirelessHidService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    void doUnbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            getActivity().unbindService(mConnection);
            mIsBound = false;
        }
    }

    private void updateSpeed(int newSpeed) {
        // note: we assume at least button have proper speed-tag so this will
        // check what it should

        mSpeed = newSpeed;

        ViewGroup bar = (ViewGroup) getView().findViewById(R.id.speed_control);
        int count = bar.getChildCount();

        for (int i = 0; i < count; i++) {
            View child = bar.getChildAt(i);

            try {
                ToggleButton button = (ToggleButton) child;

                int speed = Integer.parseInt((String) button.getTag());

                button.setChecked(speed == newSpeed);
            } catch (ClassCastException e) {
                // not a button :)
            }
        }
    }

    private void updateScrollSpeed(int newSpeed) {
        // note: we assume at least button have proper speed-tag so this will
        // check what it should

        mScrollSpeed = newSpeed;

        ViewGroup bar = (ViewGroup) getView().findViewById(R.id.scroll_speed_control);
        int count = bar.getChildCount();

        for (int i = 0; i < count; i++) {
            View child = bar.getChildAt(i);

            try {
                ToggleButton button = (ToggleButton) child;

                int speed = Integer.parseInt((String) button.getTag());

                button.setChecked(speed == newSpeed);
            } catch (ClassCastException e) {
                // not a button :)
            }
        }
    }

    private void mouseMove(int x, int y) {
        // handle mouse move here, x and y show how many xps the pc pointer show move
        if (mService == null) {
            Log.d(TAG, "no connection.");
            return;
        }
        if (mDataSendHandler == null) {
            mDataSendHandler = mService.getDataSendHandler();
        }

        if (mDataSendHandler != null) {
            WirelessHidProto.HidData data = WirelessHidProto.HidData.newBuilder()
                    .setType(WirelessHidProto.HidData.DataType.MOUSE_MOVE)
                    .setXShift(x).setYShift(y).build();
            try {
                mDataSendHandler.obtainMessage(0, data).sendToTarget();
            } catch (IllegalStateException e) {
                mDataSendHandler = null;
            }
        } else {
            Log.d(TAG, "mDataSendHandler is null");
        }
    }

    private void mouseClickPress(int keyValue) {
        // handle mouse key click press here.
        // keyValue shows which key is pressed.
        if (mService == null) {
            Log.d(TAG, "no connection.");
            return;
        }
        if (mDataSendHandler == null) {
            mDataSendHandler = mService.getDataSendHandler();
        }

        if (mDataSendHandler != null) {
            WirelessHidProto.HidData data = WirelessHidProto.HidData.newBuilder()
                    .setType(WirelessHidProto.HidData.DataType.MOUSE_CLICK_PRESS)
                    .setMouseKeyValue(keyValue).build();

            mDataSendHandler.obtainMessage(0, data).sendToTarget();
        } else {
            Log.d(TAG, "mDataSendHandler is null");
        }
    }

    private void mouseClickRelease(int keyValue) {
        if (mService == null) {
            Log.d(TAG, "no connection.");
            return;
        }
        if (mDataSendHandler == null) {
            mDataSendHandler = mService.getDataSendHandler();
        }

        if (mDataSendHandler != null) {
            WirelessHidProto.HidData data = WirelessHidProto.HidData.newBuilder()
                    .setType(WirelessHidProto.HidData.DataType.MOUSE_CLICK_RELEASE)
                    .setMouseKeyValue(keyValue).build();
            mDataSendHandler.obtainMessage(0, data).sendToTarget();
        } else {
            Log.d(TAG, "mDataSendHandler is null");
        }
    }

    private void mouseScroll(int wheelAmt) {
        // handle mouse scroll here
        // wheelAmt shows number of "notches" to move the mouse wheel Negative values
        // indicate movement up/away from the user,
        // positive values indicate movement down/towards the user.
        if (mService == null) {
            Log.d(TAG, "no connection.");
            return;
        }
        if (mDataSendHandler == null) {
            mDataSendHandler = mService.getDataSendHandler();
        }

        if (mDataSendHandler != null) {
            WirelessHidProto.HidData data = WirelessHidProto.HidData.newBuilder()
                    .setType(WirelessHidProto.HidData.DataType.MOUSE_SCROLL)
                    .setMouseScroll(wheelAmt).build();

            mDataSendHandler.obtainMessage(0, data).sendToTarget();
        } else {
            Log.d(TAG, "mDataSendHandler is null");
        }
    }

    @Override
    public void onHandlerChanged(Handler handler) {
        this.mDataSendHandler = handler;
    }
}
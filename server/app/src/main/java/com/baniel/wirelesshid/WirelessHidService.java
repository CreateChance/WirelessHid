package com.baniel.wirelesshid;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class WirelessHidService extends Service {

    private final String TAG = "WirelessHidService";

    private Handler mDataSendHandler = null;

    private ServerSocket mServerSocket = null;
    private Socket mSocket = null;

    private ArrayList<DataHandlerListener> mListenerList = new ArrayList<>();

    private final String ACTION_RESET_CONNECTION = "com.baniel.wirelesshid.ACTION_RESET_CONNECTION";

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "action: " + action);

            if (ACTION_RESET_CONNECTION.equals(action)) {
                Log.d(TAG, "reset connection");
                Toast.makeText(context, "Connection Lost!", Toast.LENGTH_SHORT).show();
                new DataSendThread().start();
            }
        }
    };

    public WirelessHidService() {

    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_RESET_CONNECTION);
        registerReceiver(mReceiver, filter);

        new DataSendThread().start();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new MyBinder();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mDataSendHandler != null) {
            mDataSendHandler.getLooper().quitSafely();
        }

        try {
            mServerSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        unregisterReceiver(mReceiver);
    }

    public class MyBinder extends Binder {
        public WirelessHidService getService() {
            return WirelessHidService.this;
        }
    }

    public interface DataHandlerListener {
        void onHandlerChanged(Handler handler);
    }

    public void setListener(DataHandlerListener listener) {
        mListenerList.add(listener);
    }

    private class DataSendThread extends Thread {

        private OutputStream os = null;

        @Override
        public void run() {
            super.run();

            Looper.prepare();

            try {
                Log.d(TAG, "I'm waiting for connecting.");
                mServerSocket = new ServerSocket(Constant.HID_TCP_PORT);
                mServerSocket.setReuseAddress(true);
                mSocket = mServerSocket.accept();
                os = mSocket.getOutputStream();
                Toast.makeText(getApplicationContext(), "Client connected!",
                        Toast.LENGTH_SHORT).show();
                Log.d(TAG, "client connected!");
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            mDataSendHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);

                    // send data here.
                    try {
                        ((WirelessHidProto.HidData)msg.obj).writeDelimitedTo(os);
                    } catch (IOException e) {
                        Log.d(TAG, "IOException, close all resource.");
                        mDataSendHandler = null;
                        notifyListener();
                        this.getLooper().quit();
                        sendBroadcast(new Intent(ACTION_RESET_CONNECTION));
                    } finally {
                        try {
                            mServerSocket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };

            notifyListener();

            Looper.loop();
        }
    }

    public Handler getDataSendHandler() {
        return this.mDataSendHandler;
    }

    private void notifyListener() {
        for (int i = 0; i < mListenerList.size(); i++) {
            mListenerList.get(i).onHandlerChanged(mDataSendHandler);
        }
    }
}

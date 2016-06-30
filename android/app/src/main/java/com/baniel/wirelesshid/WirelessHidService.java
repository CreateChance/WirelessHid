package com.baniel.wirelesshid;

import android.app.ProgressDialog;
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
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.util.ArrayList;

public class WirelessHidService extends Service {

    private final String TAG = "WirelessHidService";

    private Handler mDataSendHandler = null;

    private PCDiscoverer mPCDiscoverer = null;

    private MulticastSocket mMulticastSocket = null;
    private InetAddress group = null;

    private InetAddress mPCIPAddress = null;

    private ArrayList<DataHandlerListener> mListenerList = new ArrayList<>();

    private final String ACTION_RESET_CONNECTION = "com.baniel.wirelesshid.ACTION_RESET_CONNECTION";

    private Handler mUIHandler = null;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "action: " + action);

            if (ACTION_RESET_CONNECTION.equals(action)) {
                Log.d(TAG, "reset connection");
                Toast.makeText(context, "Connection Lost!", Toast.LENGTH_SHORT).show();
                mPCDiscoverer.startDiscover();
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

        mPCDiscoverer = new PCDiscoverer();
        mPCDiscoverer.startDiscover();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new MyBinder();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        sendDisconnectMsg();

        if (mDataSendHandler != null) {
            mDataSendHandler.getLooper().quit();
        }

        unregisterReceiver(mReceiver);
    }

    // send disconnect message to pc.
    private void sendDisconnectMsg() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (mMulticastSocket != null) {
                    DatagramPacket packet = new DatagramPacket(Constant.HID_SERVICE_DISCONNECT.getBytes(),
                            Constant.HID_SERVICE_DISCONNECT.length(),
                            group, Constant.HID_MULTICAST_PORT);
                    try {
                        mMulticastSocket.send(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    mPCDiscoverer.stopDiscover();
                }
            }
        }).start();
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

    public void setUIHandler(Handler handler) {
        this.mUIHandler = handler;
    }

    private class PCDiscoverer {
        private final String TAG = "PCDiscoverer";

        private Thread scannerThread = null;
        private Thread listenerThread = null;

        public void startDiscover() {

            try {
                mMulticastSocket = new MulticastSocket(Constant.HID_MULTICAST_PORT);
                group = InetAddress.getByName(Constant.HID_MULTICAST_ADDRESS);
                mMulticastSocket.joinGroup(group);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            listenerThread = new Thread(new Runnable() {
                // listen response from pc.
                @Override
                public void run() {
                    try {
                        DatagramPacket packet;
                        packet = new DatagramPacket(new byte[256], 256);

                        while (true) {
                            mMulticastSocket.receive(packet);

                            String rsp = new String(packet.getData()).trim();
                            Log.d(TAG, "rsp: " + rsp);
                            if (Constant.HID_SERVICE_DISCOVERY_RSP.equals(rsp)) {
                                Log.d(TAG, "get response from pc.");
                                break;
                            } else {
                                Log.d(TAG, "It is not a valid response, just ignore it.");
                                packet.setData(new byte[256]);
                            }
                        }

                        // send message to activity to stop progress dialog.
                        if (mUIHandler != null) {
                            mUIHandler.obtainMessage(MainActivity.MSG_FOUND_SERVICE).sendToTarget();
                        }

                        mPCIPAddress = packet.getAddress();
                        Log.d(TAG, "pc ip address: " + packet.getSocketAddress().toString());

                        // interrupt scanner thread to stop scan cause we have found pc.
                        scannerThread.interrupt();

                        // start data send thread.
                        new DataSendThread().start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            scannerThread = new Thread(new Runnable() {
                // pc finder thread.
                @Override
                public void run() {
                    try {
                        DatagramPacket packet = new DatagramPacket(Constant.HID_SERVICE_DISCOVERY_REQ.getBytes(),
                                Constant.HID_SERVICE_DISCOVERY_REQ.length(),
                                group, Constant.HID_MULTICAST_PORT);
                        while (true) {
                            if (scannerThread.isInterrupted()) {
                                break;
                            }

                            Log.d(TAG, "discovery pc......");
                            mMulticastSocket.send(packet);

                            // try again after 2s.
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                break;
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

            // start discover thread.
            scannerThread.start();
            listenerThread.start();
        }

        public void stopDiscover() {
            if (mMulticastSocket != null) {
                try {
                    mMulticastSocket.leaveGroup(group);
                    mMulticastSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (listenerThread != null && mMulticastSocket != null) {
                mMulticastSocket.close();
            }

            if (scannerThread != null) {
                scannerThread.interrupt();
            }
        }
    }

    private class DataSendThread extends Thread {

        private Socket mSocket = null;
        private OutputStream os = null;

        @Override
        public void run() {
            super.run();

            Looper.prepare();

            try {
                mSocket = new Socket(mPCIPAddress, Constant.HID_TCP_PORT);
                os = mSocket.getOutputStream();
                Toast.makeText(getApplicationContext(), "PC Connected!",
                        Toast.LENGTH_SHORT).show();
                Log.d(TAG, "PC connected!");
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
                        ((WirelessHidProto.HidData) msg.obj).writeDelimitedTo(os);
                    } catch (IOException e) {
                        Log.d(TAG, "IOException, close all resource.");
                        mDataSendHandler = null;
                        notifyListener();
                        this.getLooper().quit();
                        sendBroadcast(new Intent(ACTION_RESET_CONNECTION));
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

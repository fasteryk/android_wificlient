package com.testapp.wificlient;

import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Calendar;
import java.util.Date;

public class ClientActivity extends AppCompatActivity {
    public static final String TAG = "wificlient";

    private NsdHelper mNsdHelper;
    private TcpClinet mTcpClinet = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    private void hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    protected void onStart() {
        Log.d(MainActivity.TAG, "Starting.");

        mNsdHelper = new NsdHelper(this);
        mNsdHelper.initializeNsd();
        mNsdHelper.discoverServices();
        Log.d(TAG, "start discover service");

        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mTcpClinet != null)
            mTcpClinet.tearDown();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        Log.d(MainActivity.TAG, "Being stopped.");
        if (mNsdHelper != null) {
            mNsdHelper.stopDiscovery();
        }
        super.onStop();
    }

    public void onServiceResolved(NsdServiceInfo serviceInfo) {
        Log.d(TAG, "found service " + serviceInfo.getServiceName());
        Log.d(TAG, "   service address: " + serviceInfo.getHost());
        Log.d(TAG, "   service port: " + serviceInfo.getPort());

        //if (mTcpClinet == null)
        //    mTcpClinet = new TcpClinet(serviceInfo.getHost(), serviceInfo.getPort());
    }

    private class TcpClinet {
        private static final int BUFF_LENGTH = 500;

        private InetAddress mAddress;
        private int mPort;
        private Thread mReceivingThread;
        private DatagramPacket mReceivePacket;

        public TcpClinet(InetAddress address, int port) {
            this.mAddress = address;
            this.mPort = port;

            mReceivePacket = new DatagramPacket(new byte[BUFF_LENGTH], BUFF_LENGTH);

            mReceivingThread = new Thread(new ReceivingThread());
            mReceivingThread.start();
        }

        class ReceivingThread implements Runnable {
            @Override
            public void run() {
                Socket socket = null;
                DatagramSocket udpSocket = null;

                try {
                    socket = new Socket(mAddress, mPort);
                    udpSocket = new DatagramSocket(8888);

                    while (!Thread.currentThread().isInterrupted()) {
                        udpSocket.receive(mReceivePacket);

                        byte[] recvData = mReceivePacket.getData();
                        Log.d(TAG, "receive packet, type " + (char)recvData[0] +
                                ", length " + String.format("%03d", mReceivePacket.getLength()) +
                                ",  data: " +
                                String.format("%02X ", recvData[0]) + " " +
                                String.format("%02X ", recvData[1]) + " " +
                                String.format("%02X ", recvData[2]) + " " +
                                String.format("%02X ", recvData[3]) + " " +
                                String.format("%02X ", recvData[4]));
                    }
                } catch (IOException e) {
                    Log.d(TAG, e.getMessage());
                } catch (Exception e) {
                    Log.e(TAG, "error: " + e.getMessage());
                } finally {
                    Log.d(TAG, "client thread exited");
                    try {
                        if (socket != null)
                            socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public void tearDown() {
            mReceivingThread.interrupt();
        }
    }
}

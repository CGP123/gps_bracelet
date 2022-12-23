package com.example.gpsen.bluetooth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.example.gpsen.R;

import java.io.IOException;
import java.util.UUID;

public class BluetoothConnectionThread extends Thread {
    private final Context context;
    private BluetoothReceiverThread receiverThread;
    private final BluetoothAdapter btAdapter;
    private final BluetoothDevice device;
    private BluetoothSocket mSocket;
    private static final String UUID = "00001101-0000-1000-8000-00805F9B34FB";

    @SuppressLint("MissingPermission")
    public BluetoothConnectionThread(Context context, BluetoothAdapter btAdapter, BluetoothDevice device) {
        this.context = context;
        this.btAdapter = btAdapter;
        this.device = device;
        try {
            mSocket = device.createRfcommSocketToServiceRecord(java.util.UUID.fromString(UUID));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void run() {
        btAdapter.startDiscovery();
        try {
            if (!gpsTracker.IsConnected) {
                closeConnection();
                try {
                    mSocket = device.createRfcommSocketToServiceRecord(java.util.UUID.fromString(UUID));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            mSocket.connect();
            BluetoothReceiverThread receiveThread = new BluetoothReceiverThread(mSocket);
            receiveThread.start();
            gpsTracker.IsConnected = true;
            Log.d("MyLog", "Подключено");
        } catch (IOException e) {
            e.printStackTrace();
            closeConnection();
            if (!gpsTracker.IsConnected) {
                Log.d("MyLog", "Ошибка подключения");
            }
        }
    }

    public void closeConnection() {
        try {
            mSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public BluetoothReceiverThread getReceiveThread() {
        return receiverThread;
    }
}

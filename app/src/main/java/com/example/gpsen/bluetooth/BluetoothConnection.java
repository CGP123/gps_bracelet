package com.example.gpsen.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;

public class BluetoothConnection {
    private final Context context;
    private final BluetoothAdapter btAdapter;
    private BluetoothConnectionThread bluetoothConnectionThread;
    gpsTracker gpsTracker;
    public BluetoothConnection(Context context) {
        this.context = context;
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        gpsTracker = new gpsTracker(context);
    }

    public void connect() {
        String mac = "";
        try {
            mac = gpsTracker.getMAC_KEY();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        if (mac == null){
            mac = "E8:68:E7:2B:A4:BA";
        }
        //String mac = "E8:DB:84:10:DF:02";
        if (!btAdapter.isEnabled()) return;
        BluetoothDevice device = btAdapter.getRemoteDevice(mac);
        if (device == null) return;
        bluetoothConnectionThread = new BluetoothConnectionThread(context, btAdapter, device);
        bluetoothConnectionThread.start();
    }

    public void disconnect() {
        if (bluetoothConnectionThread != null) {
            bluetoothConnectionThread.closeConnection();
        }
    }

    public void sendMessage(String message) {
        bluetoothConnectionThread.getReceiveThread().sendMessage(message.getBytes());
    }

}

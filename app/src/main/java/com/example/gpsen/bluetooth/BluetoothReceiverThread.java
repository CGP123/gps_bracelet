package com.example.gpsen.bluetooth;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BluetoothReceiverThread extends Thread {
    private InputStream inputStream;
    private OutputStream outputStream;

    public BluetoothReceiverThread(BluetoothSocket socket) {
        try {
            inputStream = socket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("MyLog", "Ошибка создания входящего потока");
        }
        try {
            outputStream = socket.getOutputStream();
        }catch (IOException e) {
            e.printStackTrace();
            Log.d("MyLog", "Ошибка создания исходящего потока");
        }
    }

    @Override
    public void run() {
        byte[] receiveBuffer = new byte[40];
        while (true) {
            try {
                int size = inputStream.read(receiveBuffer);
                if (!new String(receiveBuffer, 0, size).trim().equals("")) {
                    String passNumber = new String(receiveBuffer, 0, size);
                    gpsTracker.deviceData.add(passNumber);

                }
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    public void sendMessage(byte[] byteArray){
        try {
            outputStream.write(byteArray);
        }catch (IOException e){
            e.printStackTrace();
            Log.d("MyLog", "Ошибка отправки");
        }
    }
}

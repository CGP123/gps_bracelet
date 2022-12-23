package com.example.gpsen.bluetooth;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;

public class gpsTracker {
    public static ArrayList<String> deviceData = new ArrayList<String>();
    public static boolean IsConnected = false;
    public static boolean restartTracker = false;
    Context context;
    SharedPreferences DBSharedPreferences;
    String MAC_KEY;
    String macValue;
    public gpsTracker(Context context) {
        this.context = context;
        DBSharedPreferences = context.getSharedPreferences("lastDBUpdate", Context.MODE_PRIVATE);
        MAC_KEY = DBSharedPreferences.getString("macValue", macValue);
    }

    public String getMAC_KEY() {
        return MAC_KEY;
    }

}

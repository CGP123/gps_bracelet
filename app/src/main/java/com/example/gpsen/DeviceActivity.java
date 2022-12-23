package com.example.gpsen;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gpsen.bluetooth.BluetoothConnection;
import com.example.gpsen.bluetooth.gpsTracker;

public class DeviceActivity extends AppCompatActivity {
    ImageView IV_employeePhoto;
    RecyclerView RV_employeeInfo;
    TextView TV_trackerStatus;
    TextView TV_fullName;
    TextView TV_steps;
    TextView TV_heartRate;
    BluetoothConnection bluetoothConnection;
    private BluetoothAdapter mBluetoothAdapter;
    private final int BT_REQ_PERM = 1101;
    private static final int REQUEST_ENABLE_BT = 1;
    private boolean btPermission = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        IV_employeePhoto = findViewById(R.id.IV_employeePhoto);
        TV_fullName = findViewById(R.id.TV_fullName);
        TV_steps = findViewById(R.id.TV_steps);
        TV_heartRate = findViewById(R.id.TV_heartRate);
        TV_trackerStatus = findViewById(R.id.TV_trackerStatus);
        bluetoothConnection = new BluetoothConnection(this);
        gpsTracker.IsConnected = false;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        IntentFilter f2 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(bReceiver, f2);
        getBTPermission();
        requestEnableBT();
        checkBTConnection();
        getDataFromDevice();
    }

    protected void onResume() {
        super.onResume();
        IntentFilter f2 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(bReceiver, f2);
        gpsTracker.IsConnected = false;
    }

    protected void onPause() {
        super.onPause();
        unregisterReceiver(bReceiver);
    }

    private void checkBTConnection() {
        Runnable runnable = () -> {
            while (true) {
                if (!gpsTracker.IsConnected) {
                    Log.d("MyLog", "Идёт подключение к Bluetooth модулю");
                    bluetoothConnection = new BluetoothConnection(this);
                    bluetoothConnection.connect();
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (gpsTracker.restartTracker) {
                        gpsTracker.restartTracker = false;
                        break;
                    }
                }
            }
        };
        Thread btThread = new Thread(runnable);
        btThread.start();
    }

    private void getDataFromDevice() {
        Runnable runnable = () -> {
            while (true) {
                String data = null;
                if (!gpsTracker.deviceData.isEmpty()) {
                    if (gpsTracker.deviceData.get(0) != null){
                        data = gpsTracker.deviceData.get(0).trim();
                        Log.d("MyLog", data + "1");
                        gpsTracker.deviceData.remove(0);
                    }
                    else{
                        data = null;
                    }
                    if (data != null && data.equals("Браслет снят")) {
                        ContextCompat.getMainExecutor(this).execute(() -> {
                            TV_trackerStatus.setText("Браслет снят");
                            TV_trackerStatus.setTextColor(Color.RED);
                            TV_heartRate.setText("Пульс: нет данных");

                        });
                    }
                    else if (data != null && data.equals("Браслет надет")) {
                        ContextCompat.getMainExecutor(this).execute(() -> {
                            TV_trackerStatus.setText("Браслет надет");
                            TV_trackerStatus.setTextColor(Color.GREEN);
                        });
                    }
                    else if (data != null && data.contains("Пульс:") && TV_trackerStatus.getText().toString().equals("Браслет надет")) {
                        String finalData1 = data;
                        ContextCompat.getMainExecutor(this).execute(() -> {
                            TV_heartRate.setText(finalData1 + " ударов в минуту");
                        });
                    }
                    else if (data != null && data.contains("Количество шагов:") && TV_trackerStatus.getText().toString().equals("Браслет надет")) {
                        String finalData = data;
                        ContextCompat.getMainExecutor(this).execute(() -> {
                            TV_steps.setText(finalData);
                        });
                    }
                    else if (data != null && data.contains("Свободное падение")) {
                        ContextCompat.getMainExecutor(this).execute(() -> {
                            AlertDialog.Builder builder = new AlertDialog.Builder(this);
                            ConstraintLayout cl = (ConstraintLayout) getLayoutInflater().inflate(R.layout.confirm_window, null);
                            builder.setView(cl);
                            Button yesButton = cl.findViewById(R.id.yesButton);
                            Dialog dialog = builder.create();
                            dialog.show();

                            yesButton.setOnClickListener(view1 -> {
                                dialog.dismiss();
                            });
                        });
                    }
                }
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    @SuppressLint("MissingPermission")
    protected void requestEnableBT() {
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(this, "Это устройство не поддерживает соединение по Bluetooth", Toast.LENGTH_SHORT).show();
        }
        else if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == BT_REQ_PERM) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                btPermission = true;
            }
        }
        else super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void getBTPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, BT_REQ_PERM);
        }
        else btPermission = true;
    }

    private final BroadcastReceiver bReceiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(intent.getAction())) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    if (device.getName().equals("ESP32")) {
                        Log.d("MyLog", "Bluetooth модуль отключен");
                        bluetoothConnection.disconnect();
                        device = null;
                    }
                }
            }
        }
    };
}
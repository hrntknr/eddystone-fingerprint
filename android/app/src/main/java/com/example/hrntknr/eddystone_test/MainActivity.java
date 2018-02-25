package com.example.hrntknr.eddystone_test;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import org.apache.commons.codec.binary.Hex;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private final static int SDKVER_LOLLIPOP = 21;
    private BluetoothManager mBleManager;
    private BluetoothAdapter mBleAdapter;
    private BluetoothLeScanner mBleScanner;
    private String androidId;
    private HashMap<String,Integer> lastRssi = new HashMap<>();
    private String host = "http://192.168.0.108:3000";
    static final int REQUEST_PERMISSION = 1;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        ((EditText)findViewById(R.id.host)).setText(host);

        findViewById(R.id.register).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String x = ((EditText)findViewById(R.id.x)).getText().toString();
                String y = ((EditText)findViewById(R.id.y)).getText().toString();
                (new RegisterPos()).execute(host, x, y, androidId);
            }
        });

        findViewById(R.id.apply).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                host = ((EditText)findViewById(R.id.host)).getText().toString();
            }
        });

        if(Build.VERSION.SDK_INT > SDKVER_LOLLIPOP) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION
                }, REQUEST_PERMISSION);
            }else {
                startScanByBleScanner();
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanByBleScanner();
            }else {
            }
        }
    }
    
    @TargetApi(SDKVER_LOLLIPOP)
    private void startScanByBleScanner() {
        mBleManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBleAdapter = mBleManager.getAdapter();
        mBleScanner = mBleAdapter.getBluetoothLeScanner();
        List<ScanFilter> filters = new ArrayList<ScanFilter>();
        ScanSettings scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        mBleScanner.startScan(filters, scanSettings, new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                int rssi = result.getRssi();
                ScanRecord record = result.getScanRecord();
                byte[] data = record.getBytes();
                int offset = 0;
                boolean eddystone = false;
                while (data.length > offset) {
                    int length = data[offset];
                    if(length==0)break;;
                    byte type = data[offset+1];
                    byte[] payload = Arrays.copyOfRange(data, offset+2, offset+length+1);
                    if(type==0x03 && payload.length==2 && payload[0]!=0xaa && payload[1]!=0xfe) {
                        eddystone = true;
                    }
                    if(eddystone && type==0x16) {
                        if(payload.length==22 && payload[0]==(byte)0xaa && payload[1]==(byte)0xfe && payload[2]==(byte)0x00 && payload[20]==(byte)0x00 && payload[21]==(byte)0x00) {
                            String uid = new String(Hex.encodeHex(Arrays.copyOfRange(payload, 4, 20)));
                            //arrayAdapter.add(String.format("%s, %s", uid, rssi));
                            lastRssi.put(uid,rssi);
                            (new SendRssi()).execute(host, String.valueOf(rssi), uid, androidId);
                            Log.d("uid", uid);
                        }else {
                            //Log.e("eddystone", String.format("invalid data: %s", new String(Hex.encodeHex(payload))));
                        }
                    }
                    offset += length+1;
                }
            }
            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.e("onScanFailed", (String.valueOf(errorCode)));
            }
        });
    }
}
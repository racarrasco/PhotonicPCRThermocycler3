package com.rigo_carrasco.photonicpcrthermocycler;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Created by Rigo_Carrasco on 10/17/2016.
 */
public class DeviceListActivity extends AppCompatActivity {
    private ListView mListView;
    private DeviceListAdapter mAdapter;
    private ArrayList<BluetoothDevice> mDeviceList;




    BluetoothService btService;
    ServiceConnection serviceConnection;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_paired_devices);

        mDeviceList		= getIntent().getExtras().getParcelableArrayList("device.list");

        mListView		= (ListView) findViewById(R.id.lv_paired);

        mAdapter		= new DeviceListAdapter(this);

        mAdapter.setData(mDeviceList);


        mAdapter.setListener(new DeviceListAdapter.OnPairButtonClickListener() {
            @Override
            public void onPairButtonClick(int position) {
                BluetoothDevice device = mDeviceList.get(position);

                if (device.getBondState() == BluetoothDevice.BOND_BONDED && BluetoothService.SOCKET_CONNECTED) {
                    unpairDevice(device);
                } else {
                    pairDevice(device);
                }
            }
        });




        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                btService = ((BluetoothService.LocalBinder) iBinder).getService();


            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                btService = null;
            }
        };


        btService  = new BluetoothService();
        Intent myIntent = new Intent(getApplicationContext(),BluetoothService.class);
        bindService(myIntent,serviceConnection, Context.BIND_AUTO_CREATE);







        mListView.setAdapter(mAdapter);

        registerReceiver(mPairReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mPairReceiver);
        unbindService(serviceConnection);

    }

    @Override
    protected void onPause() {
        super.onPause();
        // unbindService(serviceConnection);
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void pairDevice(BluetoothDevice device) {

        if (device.getName().contains("HC")) {
            if(connectSerialPortProtocol(device.getAddress())) {
                btService.beginListenForData();
                finish();
            }

        }
        else {
            try {
                Method method = device.getClass().getMethod("createBond", (Class[]) null);
                method.invoke(device, (Object[]) null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private boolean connectSerialPortProtocol(String MAC) {
        if(btService.btConnect(MAC)){
            showToast("connected as serial port");
            return true;
        }
        else {
            showToast("Couldn't connect");
            return false;
        }

    }


    private void unpairDevice(BluetoothDevice device) {
        try {
            btService.btDisconnect();
            Method method = device.getClass().getMethod("removeBond", (Class[]) null);
            method.invoke(device, (Object[]) null);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final BroadcastReceiver mPairReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final int state 		= intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int prevState	= intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                    showToast("Paired");
                } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED){
                    showToast("Unpaired");
                }

                mAdapter.notifyDataSetChanged();
            }
        }
    };

}

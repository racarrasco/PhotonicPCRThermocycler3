package com.rigo_carrasco.photonicpcrthermocycler;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Set;

/**
 * Created by Rigo_Carrasco on 10/17/2016.
 */
public class BluetoothMenu extends AppCompatActivity {
    private static final int ENABLE_REQUEST = 120;

    Button scanDevicesButton, pairedDevicesButton;

    //Bluetooth service
    BluetoothService btService;



    Set<BluetoothDevice> bondedDevices;
    ArrayList<BluetoothDevice> btdevices;
    private MyHandler2 mHandler2;

    ProgressDialog scanningDlg;
    //bind to service
    ServiceConnection btServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            btService = ((BluetoothService.LocalBinder) iBinder).getService();
            btService.setHandler(mHandler2);

        }
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            btService = null;
        }
    };




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_list);


        scanDevicesButton = (Button) findViewById(R.id.buttonScanDevices);
        pairedDevicesButton = (Button) findViewById(R.id.buttonPairedDevices);





        mHandler2 = new MyHandler2(this);
        btService  = new BluetoothService();
        Intent myIntent = new Intent(getApplicationContext(),BluetoothService.class);
        bindService(myIntent,btServiceConnection, Context.BIND_AUTO_CREATE);







        scanningDlg = new ProgressDialog(this);
        scanningDlg.setMessage("Scanning...");
        scanningDlg.setCancelable(false);
        scanningDlg.setButton(DialogInterface.BUTTON_NEGATIVE,"Cancel",new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                btService.cancelBTDiscovery();
            }
        });
        turnOnBluetooth();




        scanDevicesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                btService.discoverDevices();
            }
        });


        pairedDevicesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bondedDevices =btService.getPairedDevices();
                if(bondedDevices.size()==0) {
                    showToast("No paired devices found",false);
                }
                else {
                    ArrayList<BluetoothDevice> list = new ArrayList<>();
                    list.addAll(bondedDevices);

                    Intent pairedDevicesIntent = new Intent(BluetoothMenu.this, DeviceListActivity.class);

                    pairedDevicesIntent.putParcelableArrayListExtra("device.list",list);

                    startActivity(pairedDevicesIntent);
                    finish();
                }

            }
        });


        filters();
    }
    public void filters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter);
    }

    private void turnOnBluetooth() {
        BluetoothAdapter adapter = btService.getBTAdapter();
        if(adapter==null) {
            Toast.makeText(getApplicationContext(),"This device doesn't have bluetooth",Toast.LENGTH_SHORT).show();
            finish();
        }
        else if(!adapter.isEnabled()) {
            Intent btEnable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(btEnable,ENABLE_REQUEST);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==RESULT_CANCELED) {
            Toast.makeText(getApplicationContext(),"Please enable bluetooth to continue",Toast.LENGTH_SHORT).show();
            finish();
        }
    }


    public void showToast(CharSequence msg, boolean displaylong) {
        if(displaylong) {
            Toast.makeText(getApplicationContext(),msg,Toast.LENGTH_LONG).show();
        }
        else{
            Toast.makeText(getApplicationContext(),msg,Toast.LENGTH_SHORT).show();
        }

    }
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if((BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED.equals(action))) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_ON) {
                    showToast("BluetoothEnabled", false);
                }
            }else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                btdevices = new ArrayList<>();
                scanningDlg.show();
            }else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                scanningDlg.dismiss();
                Intent newIntent = new Intent(BluetoothMenu.this,DeviceListActivity.class);
                newIntent.putParcelableArrayListExtra("device.list",btdevices);
                startActivity(newIntent);
            }else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                btdevices.add(device);
                showToast("Found " + device.getName(),false);
            }
        }
    };
    private static class MyHandler2 extends Handler {
        private final WeakReference<BluetoothMenu> mActivity;

        public MyHandler2(BluetoothMenu activity) {
            mActivity = new WeakReference<>(activity);
        }
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BluetoothService.MESSAGE_FROM_BLUETOOTH:
                    String[] data = (String[]) msg.obj;
                    Toast.makeText(mActivity.get(),(String)msg.obj,Toast.LENGTH_SHORT).show();
                    break;
                case BluetoothService.ERROR:
                    Toast.makeText(mActivity.get(), (String)msg.obj, Toast.LENGTH_LONG).show();
                    break;


            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        filters();
        startService(BluetoothService.class,btServiceConnection,null);
    }
    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!BluetoothService.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);

    }
    @Override
    protected void onPause() {
        super.onPause();
        BluetoothAdapter adapter = btService.getBTAdapter();
        if(adapter !=null && adapter.isDiscovering())
            btService.cancelBTDiscovery();
        unbindService(btServiceConnection);
    }

}

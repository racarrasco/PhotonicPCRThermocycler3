package com.rigo_carrasco.photonicpcrthermocycler;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.Set;

/**
 * Created by Rigo_Carrasco on 10/17/2016.
 */
public class Controls extends AppCompatActivity {
    Button mainButton, parButton, pIDButton,controlButton, fanOnButton,fanOffButton,lEDOnButton,lEDOffButton,
            getTempButton,conndisconnButton;
    TextView currentTemperature;


    String[] values;

    private BluetoothService btService;
    private MyHandler2 mHandler2;

    private final ServiceConnection btConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            btService = ((BluetoothService.LocalBinder) arg1).getService();
            btService.setHandler(mHandler2);

        }
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            btService = null;
        }
    };

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
    public void onResume() {
        super.onResume();
        startService(BluetoothService.class, btConnection, null); // Start btService(if it was not started before) and Bind it
    }

    @Override
    public void onPause() {
        super.onPause();
        unbindService(btConnection);
    }

    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.controls_screen);
        mHandler2 = new MyHandler2(this);

        mainButton = (Button) findViewById(R.id.buttonBackToMainActivity);
        parButton = (Button) findViewById(R.id.buttonToParameters);
        pIDButton = (Button) findViewById(R.id.buttonToPID);
        controlButton = (Button) findViewById(R.id.buttonToControl);
        conndisconnButton = (Button) findViewById(R.id.buttonBluetoothStart);


        fanOnButton = (Button) findViewById(R.id.buttonFanOn);
        fanOffButton = (Button) findViewById(R.id.buttonFanOff);
        lEDOnButton = (Button) findViewById(R.id.buttonLEDOn);
        lEDOffButton = (Button) findViewById(R.id.buttonLEDOff);
        getTempButton = (Button) findViewById(R.id.buttonCheckTemp);



        currentTemperature = (TextView) findViewById(R.id.EditTextGetTemp);


        Intent theValues = getIntent();
        values = theValues.getStringArrayExtra("values");





    }
    public String encnum(Object value) { //encode command for serial communication
        int intval = (Integer) value;
        String strval = Integer.toString(intval);
        String cmdstr;
        if (intval < 10) {
            cmdstr = "0" + "0" + strval;
        } else if (intval < 100) {
            cmdstr = "0" + strval;
        } else
            cmdstr = strval;
        return cmdstr;
    }

    public void pushcmd(String command) {
        btService.writeData(command.getBytes());
    }


    public void onClickFanOn(View view) {
        pushcmd("F\n");
    }
    public void onClickFanOff(View view) {
        pushcmd("H\n");
    }
    public void onClickCheckTemp(View view) {
        pushcmd("T\n");
    }
    public void onClickLEDOff(View view) {
        pushcmd("0\n");
    }
    public void onClickLEDOn(View view) {
        pushcmd(encnum(230)+"1\n");
    }
    public void onClickBluetoothStart(View view){
        Intent startPairDevices =  new Intent(Controls.this,BluetoothMenu.class);
        startActivity(startPairDevices);
    }


    private static class MyHandler2 extends Handler {
        private final WeakReference<Controls> mActivity;

        public MyHandler2(Controls activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BluetoothService.MESSAGE_FROM_BLUETOOTH:
                    String[] data = (String[]) msg.obj;
                    mActivity.get().currentTemperature.setText(data[0]);
                    break;
                case BluetoothService.ERROR:
                    Toast.makeText(mActivity.get(),(String) msg.obj,Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }
    public void onClickBackToMain(View view) {
        Intent backToMainIntent = new Intent(this,MainActivity.class);
        String [] theValues = values;
        backToMainIntent.putExtra("values",theValues);
        setResult(RESULT_OK,backToMainIntent);
        finish();
    }
    public void onBackPressed() {
        Intent backToMainIntent = new Intent(this,MainActivity.class);
        String [] theValues = values;
        backToMainIntent.putExtra("values",theValues);
        setResult(RESULT_OK,backToMainIntent);
        finish();
    }

}

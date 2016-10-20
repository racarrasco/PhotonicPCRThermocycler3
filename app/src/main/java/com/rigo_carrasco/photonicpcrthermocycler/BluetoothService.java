package com.rigo_carrasco.photonicpcrthermocycler;


import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Rigo_Carrasco on 10/17/2016.
 */
public class BluetoothService extends Service {
    public static final int MESSAGE_FROM_BLUETOOTH = 0;
    public static final int ERROR = 1;
    public static boolean SERVICE_CONNECTED = false;
    public static boolean SOCKET_CONNECTED = false;

    private final IBinder mBinder = new LocalBinder();
    volatile boolean stopWorker;
    Thread workerThread;





    //Bluetooth
    private BluetoothAdapter btAdapter;
    public BluetoothSocket btSocket;
    private BluetoothDevice btDevice;
    public InputStream iStream;
    public OutputStream oStream;

    public ExecutorService commService;
    private Handler mHandler;
    byte[] buffer = new byte[1024];
    int bufferSize;

    @Override
    public void onCreate() {
        super.onCreate();
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothService.SERVICE_CONNECTED = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        BluetoothService.SERVICE_CONNECTED = false;

    }

    public Set<BluetoothDevice> getPairedDevices() {
        return btAdapter.getBondedDevices();
    }

    public boolean discoverDevices() {
        return btAdapter.startDiscovery();
    }
    public BluetoothAdapter getBTAdapter(){
        return BluetoothAdapter.getDefaultAdapter();
    }
    public boolean cancelBTDiscovery() {
        return btAdapter.cancelDiscovery();
    }
    public BluetoothSocket getBtSocket() { return btSocket;}



    public boolean btConnect (String MAC) {
        // in case BT wasn't enabled when app was started
        if (btAdapter == null) {
            btAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        //already connected
        if(btSocket!=null && btSocket.isConnected()) {
            return true;
        }



        MAC = MAC.toUpperCase();

        //get device
        btDevice = btAdapter.getRemoteDevice(MAC);


        //UUID for Serial Port Protocol
        UUID mUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

        //try to connect
        try{
            btSocket = btDevice.createRfcommSocketToServiceRecord(mUUID);

        }catch (IOException e) {
            return false;
        }

        //Connect to the socket
        try{
            btSocket.connect();
            iStream = btSocket.getInputStream();
            oStream = btSocket.getOutputStream();
            BluetoothService.SOCKET_CONNECTED = true;
        }catch(IOException e) {
            return false;
        }
        commService = Executors.newSingleThreadExecutor();
        return true;
    }

    public boolean btDisconnect () {

        if(btSocket !=null && btSocket.isConnected()) {
            try{
                iStream.close();
                oStream.close();
                btSocket.close();
                BluetoothService.SOCKET_CONNECTED = false;
                if(commService != null && !commService.isShutdown()) {
                    commService.shutdown();
                }
            }catch(IOException e ) {
                mHandler.obtainMessage(ERROR,"trouble closing bluetooth connection").sendToTarget();
            }
            return true;
        } else{
            return false;
        }

    }





    private void appendBytes(byte[] buf) {
        System.arraycopy(buf,0,buffer,bufferSize,buf.length);
        bufferSize +=buf.length;
    }
    public void clearBytes(){
        buffer=new byte[1024];
        bufferSize = 0;
    }




    void beginListenForData(){
        stopWorker = false;
        workerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted() && !stopWorker){
                    try {
                        int bytesAvailable = iStream.available();
                        if(bytesAvailable>0) {
                            byte [] packetBytes = new byte [bytesAvailable];
                            iStream.read(packetBytes);
                            String data = new String(packetBytes,"UTF8");
                            if (data.contains("\n")) {
                                appendBytes(packetBytes);
                                String sdata = new String(buffer,"UTF8");
                                String [] real = sdata.split(" ");
                                if(mHandler !=null ) {
                                    mHandler.obtainMessage(MESSAGE_FROM_BLUETOOTH,real).sendToTarget();
                                    clearBytes();
                                }
                            }
                            else {
                                appendBytes(packetBytes);
                            }

                        }
                    }catch (IOException e) {
                        stopWorker = true;
                    }
                }
            }
        });
        workerThread.start();
    }

    void stopListenForData(){
        stopWorker = true;

    }

    public boolean writeData(byte[] data) {
        // Don't bother if we're not connected
        if (btSocket == null || !btSocket.isConnected()) {
            return false;
        }

        try {
            oStream.write(data);
        } catch (IOException e) {
            Log.d("Bluetooth Service","Trouble writing to device!");
        }

        return true;
    }
    public void setHandler(Handler mHandler){ this.mHandler = mHandler;}
    public interface BluetoothConnectCallback {
        void doOnConnect();
        void doOnConnectionFailed();
        void doOnDisconnect();
    }
    public interface DataReadCallback {
        void doOnDataRead(byte[] theData);
        void doOnReadFail();
    }
    private BluetoothConnectCallback emptyCallback = new BluetoothConnectCallback() {
        @Override
        public void doOnConnect() {

        }

        @Override
        public void doOnConnectionFailed() {

        }

        @Override
        public void doOnDisconnect() {
        }
    };

    private DataReadCallback anotherEmptyCallback = new DataReadCallback() {
        @Override
        public void doOnDataRead(byte[] theData) {

        }

        @Override
        public void doOnReadFail() {

        }
    };




    public class LocalBinder extends Binder {
        public BluetoothService getService() {
            // Return this instance so clients can access the d
            return BluetoothService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

}

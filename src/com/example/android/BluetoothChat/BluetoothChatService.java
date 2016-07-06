/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.BluetoothChat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class BluetoothChatService {
    // Debugging
    private static final String TAG = "BluetoothChatService";
    private static final boolean D = true;

    // Name for the SDP record when creating server socket
    private static final String NAME_SECURE = "BluetoothChatSecure";
    private static final String NAME_INSECURE = "BluetoothChatInsecure";

    // Unique UUID for this application
    private static final UUID MY_UUID_SECURE =
        //UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    		UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
                            
    private static final UUID MY_UUID_INSECURE =
        //UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
    		UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    

    // Member fields
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private AcceptThread mSecureAcceptThread;
    private AcceptThread mInsecureAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    /**
     * Constructor. Prepares a new BluetoothChat session.
     * @param context  The UI Activity Context
     * @param handler  A Handler to send messages back to the UI Activity
     */
    public BluetoothChatService(Context context, Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;
    }

    /**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
      //占쌘들러占쏙옙占쏙옙 占쏙옙占싸울옙 占쏙옙占승몌옙 占싼곤옙占쌔댐옙. UI 占쏙옙티占쏙옙티占쏙옙 占쏙옙占쏙옙占심쇽옙 占쌍듸옙占쏙옙
        mHandler.obtainMessage(BluetoothChat.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /** 占쏙옙占승곤옙 占쏙옙占쏙옙占쏙옙 占쌨쏙옙占쏙옙 
     * Return the current connection state. */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume() */
    // 
    // BuletoothChat 占쏙옙티占쏙옙티占쏙옙 onResume占쏙옙占쏙옙 호占쏙옙占�
    public synchronized void start() {
        if (D) Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        //占쏙옙占쏙옙占쏙옙 占시듸옙占싹댐옙 占쏙옙占쏙옙占썲가 占쏙옙占쏙옙占쏙옙 占쏙옙占�
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        //占쏙옙占쏙옙 占쏙옙占쏙옙占�占쏙옙占쏙옙占썲가 占쏙옙占쏙옙占쏙옙 占쏙옙占쏙옙磯占�
        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        setState(STATE_LISTEN); //占쏙옙占쏙옙占쏙옙 占쏙옙占승뤄옙 占쏙옙占쏙옙占�占쌔댐옙. 

        // Start the thread to listen on a BluetoothServerSocket
        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = new AcceptThread(true);
            mSecureAcceptThread.start();
        }
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread(false);
            mInsecureAcceptThread.start();
        }
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device  The BluetoothDevice to connect
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    //占쏙옙占쌘뤄옙 占쏙옙瀕占�占쏙옙占�占쏙옙占쏙옙占쏙옙占�占쏙옙치占쏙옙 占쏙옙占쏙옙占쏙옙 占시듸옙占싹댐옙 占쌨소듸옙
    public synchronized void connect(BluetoothDevice device, boolean secure) {
        if (D) Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        //占쏙옙占쏙옙 占쏙옙占쏙옙占싹곤옙 占쏙옙占쏙옙占쏙옙 占시듸옙 占싹곤옙 占쌍다몌옙 占쏙옙占쏙옙磯占� 
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        }
        //占쏙옙占쏙옙占�占쏙옙占승띰옙占�占쏙옙占�        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to connect with the given device
        //占쏙옙占쌘뤄옙 占쏙옙瀕占�占쏙옙치占쏙옙 占쏙옙占쏙옙占쏙옙 占시듸옙占싹깍옙 占쏙옙占쏙옙 占쏙옙占쏙옙占쏙옙 占쏙옙체占쏙옙 占쏙옙
        mConnectThread = new ConnectThread(device, secure);
        mConnectThread.start();
       //占쏙옙占쏙옙 占쏙옙占승몌옙 占쌕뀐옙占쌔댐옙. 占쏙옙占쏙옙占쏙옙 占시듸옙占싹댐옙 占쏙옙占승뤄옙
        //UI占쏙옙티占쏙옙티占쏙옙占쏙옙 占쏙옙占쏙옙寗占�占쌍듸옙占쏙옙
        setState(STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    //占쏙옙치占쏙옙 占쏙옙占쏙옙占쏙옙占쏙옙占�占쏙옙占쏙옙占� 
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device, final String socketType) {
        if (D) Log.d(TAG, "connected, Socket Type:" + socketType);

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Cancel the accept thread because we only want to connect to one device
        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        // 占쏙옙치占쏙옙 占쏙옙占쏙옙占쏙옙 占쏙옙占쏙옙占�占쏙옙占쏙옙 占쏙옙占쏙옙占쏙옙 占쏙옙 
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        //占쏙옙占쏙옙占쏙옙 占쏙옙치占쏙옙占쏙옙 UI占쏙옙티占쏙옙티占쏙옙 占쏙옙占�       
        Message msg = mHandler.obtainMessage(BluetoothChat.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothChat.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        //占쏙옙占쏙옙 표占쏙옙
        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    //占쏙옙占쏙옙占썲를 占쏙옙占쏙옙 占쏙옙키占쏙옙 占쌨소듸옙
    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }

        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }
        
        //占쏙옙占쏙옙 표占쏙옙
        setState(STATE_NONE);
    }

    public void toque(int left, int right){
    	
    	 ConnectedThread r;
         // Synchronize a copy of the ConnectedThread
         synchronized (this) {
             if (mState != STATE_CONNECTED) return;
             r = mConnectedThread;
         }
         // Perform the write unsynchronized
         r.toque(left, right);
    	
    }
    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(BluetoothChat.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothChat.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        BluetoothChatService.this.start();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(BluetoothChat.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothChat.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        BluetoothChatService.this.start();
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    
    //占쏙옙占�占쏙옙占쏙옙 占쏙옙청 占쌨댐옙 占쏙옙占쏙옙  占쏙옙占쏙옙 占쏙옙캣占쏙옙 占쏙옙占쏙옙占� 
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;
        private String mSocketType;

        public AcceptThread(boolean secure) {
        	
            BluetoothServerSocket tmp = null;
            mSocketType = secure ? "Secure":"Insecure";

            // Create a new listening server socket
            try {
                if (secure) {
                    tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE,
                        MY_UUID_SECURE);  // 占쏙옙占쏙옙占쏙옙占�占쏙옙占쏙옙占쏙옙占쏙옙占쏙옙 占쏙옙청占싹울옙 占쏙옙쨈占� 
                } else {
                    tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(
                            NAME_INSECURE, MY_UUID_INSECURE);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            if (D) Log.d(TAG, "Socket Type: " + mSocketType +
                    "BEGIN mAcceptThread" + this);
            setName("AcceptThread" + mSocketType);

            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                	
                    socket = mmServerSocket.accept(); // 클占쏙옙占싱억옙트占쏙옙 占쏙옙占쏙옙占쌕몌옙 占쏙옙占썩서 占쏙옙占쏙옙占�                    
                } catch (IOException e) {
                    Log.e(TAG, "Socket Type: " + mSocketType + "accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (BluetoothChatService.this) {
                        switch (mState) {
                        case STATE_LISTEN:
                        	 
                        case STATE_CONNECTING:
                            // Situation normal. Start the connected thread.
                        	 
                            connected(socket, socket.getRemoteDevice(),
                                    mSocketType);
                            break;
                        case STATE_NONE:
                        case STATE_CONNECTED:
                            // Either not ready or already connected. Terminate new socket.
                            try {
                                socket.close();
                            } catch (IOException e) {
                                Log.e(TAG, "Could not close unwanted socket", e);
                            }
                            break;
                        }
                    }
                }
            }
            if (D) Log.i(TAG, "END mAcceptThread, socket Type: " + mSocketType);

        }

        public void cancel() {
            if (D) Log.d(TAG, "Socket Type" + mSocketType + "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e);
            }
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    //占쏙옙치占쏙옙 占쏙옙占쏙옙천占�占싹댐옙 占쏙옙占쏙옙占쏙옙
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;  //占쏙옙占쏙옙占쏙옙占�占쏙옙占쏙옙 占쏙옙캣 
        private final BluetoothDevice mmDevice;  //占쏙옙占쏙옙占쏙옙占�占싹듸옙占쏙옙占�占쏙옙藥뱄옙占�占쏙옙占�       
        private String mSocketType;

        public ConnectThread(BluetoothDevice device, boolean secure) {
            mmDevice = device;  //占쏙옙占쏙옙占쏙옙占�占쌍쇽옙 
            BluetoothSocket tmp = null;  //占쌈쏙옙占쏙옙占쏙옙 
            mSocketType = secure ? "Secure" : "Insecure";  //占쏙옙占쏙옙 타占쏙옙 

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            // 占쌍억옙占쏙옙 占쏙옙치占쏙옙 占쏙옙占쏙옙占쌔쇽옙 占쏙옙占쏙옙占쏙옙占�占쏙옙체占쏙옙占쏙옙쨈占�            // 占쏙옙占쏙옙 클占쏙옙占싱억옙트 占쏙옙占쏙옙
            try {
                if (secure) {  //占쏙옙占쏙옙 타占쏙옙占쏙옙 占쏙옙荑�占쏙옙占�占쏙옙占쏙옙決占쏙옙占�占쏙옙占쏙옙 占쌔댐옙. 
                    tmp = device.createRfcommSocketToServiceRecord(MY_UUID_SECURE);
                } else {
                    tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID_INSECURE);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
            }
             //占쏙옙占쏙옙 占쌩삼옙
            mmSocket = tmp;
        }

        //占쏙옙占쏙옙占쏙옙 占쏙옙占쏙옙 
        public void run() {
            Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
            //占쏙옙占쏙옙占쏙옙占쏙옙 占싱몌옙 占쏙옙占쏙옙占싼댐옙.            
            setName("ConnectThread" + mSocketType);
             
            // Always cancel discovery because it will slow down a connection
            //탐占쏙옙占쏙옙 占싹곤옙 占쌍다몌옙 占쏙옙占쏙옙占쏙옙占�占쏙옙占쏙옙占실뤄옙 Discovery占쏙옙 占쏙옙占�            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
            	//占쏙옙占쏙옙占쏙옙 占쏙옙占쏙옙占싹거놂옙 占쏙옙占쌤곤옙 占쌩삼옙占쌀띰옙 占쏙옙占쏙옙 占쏙옙 占쌨소듸옙占�占쏙옙占신뤄옙홱占�
                mmSocket.connect();  //커占쌔쇽옙占쏙옙 占싼댐옙. 
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close(); // 占쏙옙占쏙옙 占쏙옙占싻쏙옙 占쏙옙캣 占쌥깍옙
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " + mSocketType +
                            " socket during connection failure", e2);
                }
                //占쏙옙占쏙옙 占쏙옙占쏙옙 占쌨쏙옙占쏙옙
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            //占쏙옙占쏙옙 占쏙옙占쏙옙占�占쏙옙占쏙옙占쏙옙 占십깍옙화
            synchronized (BluetoothChatService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            // 占쏙옙占쏙옙 占쏙옙占쏙옙占쏙옙占�占쏙옙치占쏙옙 占쏙옙占쏙옙占쏙옙 占실억옙占쏙옙占쏙옙 占쏙옙占쏙옙求占�占쏙옙占쏙옙占썲를 占쏙옙占쏙옙占싹댐옙 占쌨소듸옙 호占쏙옙
            connected(mmSocket, mmDevice, mSocketType); //占쌤븝옙 占쏙옙치占쏙옙 占쏙옙占쏙옙占싼댐옙. 
        }
       //占쏙옙占�占쌨소듸옙
        public void cancel() {
            try {
                mmSocket.close(); //占쏙옙占쏙옙 占쌥깍옙
            } catch (IOException e) {
                Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    //占쏙옙占�占쏙옙치占쏙옙 占쏙옙占쏙옙占�占쏙옙占쏙옙 占쏙옙占쏙옙풔占�占쏙옙占쏙옙占쏙옙 
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            Log.d(TAG, "create ConnectedThread: " + socketType);
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    // Send the obtained bytes to the UI Activity
                    mHandler.obtainMessage(BluetoothChat.MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        //紐⑦꽣
        public void toque(int left, int right){
        	
        	try{
        		String data = "#M";
        		
        		if(0 < left){
        			  data += 'F';       //0蹂대떎 �щ㈃ F
        		}else if(0 > left){      // 
        			  data += 'B';       //0蹂대떎 �묒쑝硫�B
        			  left = -left;      //-媛믪씠硫�紐⑦꽣媛���쭅�댁� �딆쑝誘�줈 �묒쓽 �뺤닔濡�諛붽씔��
        		}else{
        			data += 'H';    // 0 �대㈃ �뺤� 
        		}
        		
        		if(0 < right){
        			data += 'F';          //0蹂대떎 �щ㈃ F
        		}else if(0 > right){
        			data += 'B';         //0蹂대떎 �묒쑝硫�B
        			right = -right;       //-媛믪씠硫�紐⑦꽣媛���쭅�댁� �딆쑝誘�줈 �묒쓽 �뺤닔濡�諛붽씔��
        		}else{
        			data += 'H';         // 0 �대㈃ �뺤� 
        		}
 
        		//if(left !=0) data+=  (char)(left&0xff); 
        		//if(right != 0) data +=  (char)(right&0xff);  
        		mmOutStream.write(data.getBytes()); //�꾩쭊/�꾩쭊/�뺤� ���곹깭 媛믪쓣 癒쇱� 蹂대궦��
   
        		//�ш린��遺�꽣 �좏겕 媛믪쓣 蹂대궡 以�떎.
        		byte[] size = new byte[2];

        		if(left !=0){

        			size[0] = (byte) ((left >> 8) & 0x00ff);  //占쏙옙占쏙옙占쏙옙占쏙옙트
        	        size[1] = (byte)(left & 0x00ff);            //占쏙옙占쏙옙 占쏙옙占쏙옙트
                    
        	        Log.d("TANK", "left[0]:" + size[0]);
        	        Log.d("TANK", "left[1]:" + size[1]);
                    mmOutStream.write(size[0]);
                    mmOutStream.write(size[1]);
        		}
        		
        		 //占십깍옙화
        		size[0] = 0;
        		size[1] = 0;
        		
        		if(right != 0){
        			 
        			size[0] = (byte) ((right >> 8) & 0x00ff);  //占쏙옙占쏙옙占쏙옙占쏙옙트
        	        size[1] = (byte)(right & 0x00ff);            //占쏙옙占쏙옙 占쏙옙占쏙옙트
        	                
        	        Log.d("TANK", "right[0]:" + size[0]);
        	        Log.d("TANK", "right[1]:" + size[1]);
        	        
                    mmOutStream.write(size[0]);
                    mmOutStream.write(size[1]);
        		}
        			
        		
        	}catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        	
        }
        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(BluetoothChat.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}

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

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This is the main Activity that displays the current chat session.
 * 한글이 깨짐 
 */
public class BluetoothChat extends Activity {
    // Debugging
    private static final String TAG = "BluetoothChat";
    private static final boolean D = true;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1; 
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3; 

    // Layout Views
    private TextView mTitle;
    private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;

    // Tank Button
    private Button mSendUpButton;
    private Button mSendDownButton;
    private Button mSendLeftButton;
    private Button mSendRightButton;
    private Button mSendStopButton;
    
    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread
     
    private ArrayAdapter<String> mConversationArrayAdapter;
    // String buffer for outgoing messages
   
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
   
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothChatService mChatService = null;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");

        // Set up the window layout  
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.main);
        
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);

        // Set up the custom title
        mTitle = (TextView) findViewById(R.id.title_left_text);
        mTitle.setText(R.string.app_name); 
        mTitle = (TextView) findViewById(R.id.title_right_text);

        // Get local Bluetooth adapter
   
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
     
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        //������� ��ġ�� ���� �ִٸ� �ѵ����Ѵ�.
        if (!mBluetoothAdapter.isEnabled()) {
        	//������� ��ġ�� �Ѵ� ��Ƽ��Ƽ ���� 
        	// ���� ��Ƽ��Ƽ�� Pause ���°� ��.
        	//���� onActivityResult�޼ҵ尡 ȣ���
        	//����� ȣ����. 
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the chat session
            //�̶� ���� ��Ƽ��Ƽ�� onPuase���·� �� 
        } else {
        	//�Ѽ� ������ ���� ���񽺰� ���� �ȵƴٸ� ���� ���� �����Ѵ�.
            if (mChatService == null) setupChat();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
       //onPause���¿��� ����ڰ� ������� �ѱ⸦ �����ϸ� �̰��� �����
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
           //ó�� �����ߴٸ� ���°� STATE_NONE�̴�
        	if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
              // Start the Bluetooth chat services
              //������� ���� ���� ����
        		mChatService.start();
            }
        }
    }

    //���� ���� Ŭ������ �ʱ�ȭ�Ѵ�.
    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the array adapter for the conversation thread
        //��ȭ������ ǥ���ϴ� �ƴ�Ÿ Ĵü ��
        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
         //��ȭ������ ǥ���� LiveView ��ü ���
        mConversationView = (ListView) findViewById(R.id.in);
        //ListView�� ��ü �ƴ�Ÿ�� �����Ѵ�.
        mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the compose field with a listener for the return key
        //����� ���ڿ��� �Է��� EditText ��ü ���
        mOutEditText = (EditText) findViewById(R.id.edit_text_out);
        //EditText��ü�� ������ ��� 
        mOutEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
        //��� ��ư ��ü ���
            mSendButton = (Button) findViewById(R.id.button_send);
          mSendUpButton = (Button) findViewById(R.id.button_up);
        mSendDownButton = (Button) findViewById(R.id.button_down);
        mSendLeftButton = (Button) findViewById(R.id.button_left);
       mSendRightButton = (Button) findViewById(R.id.button_right);
        mSendStopButton = (Button) findViewById(R.id.button_stop);
        
        //��ư�� ������ ���
        mSendButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                //�Էµ� ���ڿ� �о�� ���
            	TextView view = (TextView) findViewById(R.id.edit_text_out);
                String message = view.getText().toString();
                sendMessage(message);
            }
            
            
        });
      
        //전진
        mSendUpButton.setOnClickListener(new OnClickListener(){
        	@Override
        	public void onClick(View v) {
        		// TODO Auto-generated method stub
         
        	 sendToque(255,255);
        	}
        });
        
        //후진
        mSendDownButton.setOnClickListener(new OnClickListener(){
        	@Override
        	public void onClick(View v) {
        		// TODO Auto-generated method stub
        	  
        	 sendToque(-255, -255);
        	}
        });
        
        //왼쪽으로 이동 
        mSendLeftButton.setOnClickListener(new OnClickListener(){
        	@Override
        	public void onClick(View v) {
        		// TODO Auto-generated method stub
         
        	 sendToque(-255,255);
        	}
        });
        
        //오른쪽으로 이동
        mSendRightButton.setOnClickListener(new OnClickListener(){
        	@Override
        	public void onClick(View v) {
        		// TODO Auto-generated method stub
        	 
        	 sendToque(255, -255);
        	}
        });
        
        //정지
        mSendStopButton.setOnClickListener(new OnClickListener(){
        	@Override
        	public void onClick(View v) {
        		// TODO Auto-generated method stub
        	 
        	 sendToque(0,0);
        	}
        });

        // Initialize the BluetoothChatService to perform bluetooth connections
        //������� ���� ��ü �� 
        mChatService = new BluetoothChatService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mChatService != null) mChatService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }

    //�ٸ� ��⿡�� ���� �˻� �Ҽ� �ֵ��� ��� �ð��� 300�� 
    private void ensureDiscoverable() {
        if(D) Log.d(TAG, "ensure discoverable");
        //�ƴ�Ÿ�� ���˸�尡 Discoverable���°� �ƴ϶�� 
        if (mBluetoothAdapter.getScanMode() !=
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            //����Ʈ�� �ۼ��Ѵ�. 
        	Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            //300�� ���� �˻����� �ϵ��� ���� 
        	discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            //��Ƽ��Ƽ ���� �� ��Ƽ��Ƽ�� onPause���°� �� 
        	// onPause�� synchronized�� ����  ����ȭ
        	startActivity(discoverableIntent);
        }
    }

    private void sendToque(int left, int right){
    	
    	if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
             
        	Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
           
        	return;
        }

    	mChatService.toque(left, right);
    	
    }
    /**
     * Sends a message.
     * @param message  A string of text to send.
     * �޽��� ����ϱ�'
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
    	//������ ���°� ���� ���°� �ƴϸ� 
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            //������� �ʾҴٹ� �޽���
        	Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
           //�޼ҵ� ��
        	return;
        }

        // Check that there's actually something to send
        //���� �޽����� �ִٸ� 
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            //�޽����� ����Ʈ ���׷� ��´�. 
        	byte[] send = message.getBytes();
            // ���� ��ü�� �̿� ���濡 �޽��� ���
        	mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            // ��ü, �Է�â �ʱ�ȭ �Ѵ�. 
        	mOutStringBuffer.setLength(0);
            mOutEditText.setText(mOutStringBuffer);
        }
    }

    //����ƮŰ���� ���� Enter �� �޾ƿ��� ��
    // The action listener for the EditText widget, to listen for the return key
    private TextView.OnEditorActionListener mWriteListener =
        new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            if(D) Log.i(TAG, "END onEditorAction");
            return true;
        }
    };

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE: //�޽��� ������ ���°� �ٲ������ 
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothChatService.STATE_CONNECTED: //�������
                    mTitle.setText(R.string.title_connected_to);
                    mTitle.append(mConnectedDeviceName);
                    mConversationArrayAdapter.clear();
                    break;
                    //����õ� ����
                case BluetoothChatService.STATE_CONNECTING:
                    mTitle.setText(R.string.title_connecting);
                    break;
                case BluetoothChatService.STATE_LISTEN:
                case BluetoothChatService.STATE_NONE://�ƹ����µ� �ƴҶ�
                    mTitle.setText(R.string.title_not_connected);
                    break;
                }
                break;
            case MESSAGE_WRITE: //�޽��� ����ϴ� ���� 
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                ///����Ʈ �迭�� ��ü ��
                String writeMessage = new String(writeBuf);
                //��ȭâ�� ���� ���� �޽��� ǥ�� 
                mConversationArrayAdapter.add("Me:  " + writeMessage);
                break;
                // ��� ����̽����� ����� �޽����� �о� ����
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                //�о�� �޽����� ȭ�鿡 ǥ�� ���� string���� ��ȯ 
                String readMessage = new String(readBuf, 0, msg.arg1);
                mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
                break;
            case MESSAGE_DEVICE_NAME: //������ ��ġ�� ǥ��
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST: //�佺Ʈ �޽���
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };

    //
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE_SECURE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                connectDevice(data, true); //�ڵ带 ������ 
            }
            break;
        case REQUEST_CONNECT_DEVICE_INSECURE:
        	
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                connectDevice(data, false);
            }
            break;
        case REQUEST_ENABLE_BT:  // ���Ⱑ ���� ó�� ����ȴ�. 
        	//����ڰ� ������� �Ѵ°� ����
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
               //���� �ϵ��� ���� ���� ��ü�� ����� �ش�. 
            	Log.d("DEBUG","-------------������� ���οϷ�");
            	setupChat();
            } else {
                // User did not enable Bluetooth or an error occured
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address   
        String address = data.getExtras()
            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BLuetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device  
        mChatService.connect(device, secure); 
    }

    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    //옵션 메뉴
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent serverIntent = null;
        switch (item.getItemId()) {
        case R.id.secure_connect_scan:
        	//디바이스 리스트 액티비티 열기
            // Launch the DeviceListActivity to see devices and do scan
            serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE); 
            return true;
        case R.id.insecure_connect_scan:
            // Launch the DeviceListActivity to see devices and do scan
            serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);  
            return true;
        case R.id.discoverable:
            // Ensure this device is discoverable by others
            ensureDiscoverable();
            return true;
        }
        return false;
    }

}

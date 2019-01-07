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

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
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

import butterknife.ButterKnife;
import butterknife.BindView;
import butterknife.OnClick;

/**
 * This is the main Activity that displays the current chat session.
 * 한글로 수정
 *
 * UI 수정및 필요 없는 부분 개선이 필요한다.
 *
 */
public class BluetoothChat extends AppCompatActivity {
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
    private static final int UP_BARREL = 101;
    private static final int DOWN_BARREL = 102;

    @BindView(R.id.button_ro_left)
    Button button_ro_left;

    @BindView(R.id.button_ro_right)
    Button button_ro_right;

    @BindView(R.id.button_ro_stop)
    Button button_ro_stop;

    //방향전환
    @BindView(R.id.button_left_up)
    Button button_left_up;

    @BindView(R.id.button_right_up)
    Button button_right_up;

    @BindView(R.id.button_down_left)
    Button button_down_left;

    @BindView(R.id.button_down_right)
    Button button_down_right;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");

        // Set up the window layout  
       // requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.main);

        ButterKnife.bind(this);


        //getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);

        // Set up the custom title
        //mTitle = (TextView) findViewById(R.id.title_left_text);
        //mTitle.setText(R.string.app_name);
        mTitle =  findViewById(R.id.title_right_text);  //연결 상태

        findViewById(R.id.button_barrel_up).setOnClickListener(v -> {
            sendBarrel(UP_BARREL);
        });

        findViewById(R.id.button_barrel_down).setOnClickListener(v -> {
            sendBarrel(DOWN_BARREL);
        });
        // Get local Bluetooth adapter
   
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }


    @OnClick(R.id.button_ro_left)
    void TurretLeft() {

         sendTurret(0);
        Log.d("DEBUG", "Left");

    }

    @OnClick(R.id.button_ro_right)
    void TurretRight() {
        sendTurret(180);
        Log.d("DEBUG", "Right");

    }

    @OnClick(R.id.button_ro_stop)
    void TurretStop() {
        sendTurret(90);
        Log.d("DEBUG", "Stop");

    }


    //완만한 좌회전
    @OnClick(R.id.button_left_up)
    void SteeringLeftUp() {
        sendToque(100,255);
    }

    //완만한 우회전
    @OnClick(R.id.button_right_up)
    void SteeringRightUp() {
        sendToque(255,100);
    }

    //후진 좌회전
    @OnClick(R.id.button_down_left)
    void SteeringDownLeft() {
        sendToque(-100,-255);
    }

    //후전 우회전
    @OnClick(R.id.button_down_right)
    void SteeringDownRight() {
        sendToque(-255,-100);
    }

    @TargetApi(23)
    public void checkBTPermissions(){

        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {

                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
            }
        }else{
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");


        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        //블루투스가 꺼져 있다면 "이 기기에서 블루투스를 사요 설정 하려 합니다" 팝업창 표시되며
        //허용 버튼을 누르면 블루투스가 켜진다
        //다시 액티비티가 활성화 되면서 onActivityResult 메소드의 setupChat() 부분이 실행된다.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the chat session
        } else {
            if (mChatService == null) setupChat();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");

       // permission();

        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already

            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                if (D) Log.d(TAG, "블루투스 서비스 시작");
                mChatService.start();
            }
        }


    }

    private void setupChat() {

        Log.d(TAG, "setupChat() UI 초기화");

        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        mConversationView = (ListView) findViewById(R.id.in);
        //ListView
        mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the compose field with a listener for the return key
        //mOutEditText = (EditText) findViewById(R.id.edit_text_out);
        //EditText????? ?????? ??? 
       // mOutEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
          //  mSendButton = (Button) findViewById(R.id.button_send);
          mSendUpButton = (Button) findViewById(R.id.button_up);
        mSendDownButton = (Button) findViewById(R.id.button_down);
        mSendLeftButton = (Button) findViewById(R.id.button_left);
       mSendRightButton = (Button) findViewById(R.id.button_right);
        mSendStopButton = (Button) findViewById(R.id.button_stop);
        
    /*    mSendButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
            	TextView view = (TextView) findViewById(R.id.edit_text_out);
                String message = view.getText().toString();
                sendMessage(message);
            }
            
            
        });*/
      
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

        //제자리 우회전
        mSendLeftButton.setOnClickListener(new OnClickListener(){
        	@Override
        	public void onClick(View v) {
        		// TODO Auto-generated method stub
         
        	 sendToque(-255,255);
        	}
        });
        
        //제자리 좌회전
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

    private void ensureDiscoverable() {
        if(D) Log.d(TAG, "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() !=
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
        	Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        	discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        	// onPause?? synchronized?? ????  ?????
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

    private void sendTurret(int status){

        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }
        mChatService.turret(status);
    }

    private void sendBarrel(int direction){

        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }
        mChatService.barrel(direction);
    }


    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
        	Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
        	return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
        	byte[] send = message.getBytes();
        	mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
        	mOutStringBuffer.setLength(0);
            mOutEditText.setText(mOutStringBuffer);
        }
    }

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
    // 서비스에서 연결 상태에 대한 정보를 표시한다.
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothChatService.STATE_CONNECTED: //블루투스 연결
                    mTitle.setText(R.string.title_connected_to);
                    mTitle.append(mConnectedDeviceName);
                    mConversationArrayAdapter.clear();
                    break;
                case BluetoothChatService.STATE_CONNECTING:  //연결중
                    mTitle.setText(R.string.title_connecting);
                    break;
                case BluetoothChatService.STATE_LISTEN:
                case BluetoothChatService.STATE_NONE:   //연결해제
                    mTitle.setText(R.string.title_not_connected);
                    break;
                }
                break;
            case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
                mConversationArrayAdapter.add("Me:  " + writeMessage);
                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };

    //블루 투스 허용하면 이쪽을 결과가 넘어 온다.
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE_SECURE:
            // When DeviceListActivity returns with a device to connect
            //인증 암호화된 연결
            if (resultCode == Activity.RESULT_OK) {
                connectDevice(data, true);
            }
            break;
        case REQUEST_CONNECT_DEVICE_INSECURE:
        	
           //Insecure(인증, 암호화되지 있지 않은) 연결을 지원하기 위한 API가 추가되었다. 기존 메서드에 'Insecure'가 덧붙여진 형태이다.
            // 페이링에 암호키 확인을 하지 않은 디바이스 등은 이 API를 이용하여 통신해야 한다
            if (resultCode == Activity.RESULT_OK) {
                connectDevice(data, false);
            }
            break;
        case REQUEST_ENABLE_BT:   //블루투스 허용일때
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
            	Log.d("DEBUG","블루투스 허용");
            	setupChat();   //UI초기화
            } else {      //허용 하지 않을때 메시지
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

    //옵션메뉴
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent serverIntent = null;
        switch (item.getItemId()) {
        case R.id.secure_connect_scan:
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

    /**
     * 퍼미션
     */

    private static final int QSC_PERMISSIONS_CAPTURE = 101;

    private void permission()
    {
        int currentAPIVersion = android.os.Build.VERSION.SDK_INT;
        int cameraPermissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int writePermissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int readPermissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        int coarsePermissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        int bluetoothPermissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH);
        int bluetoothAdimePermissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN);

        if (currentAPIVersion >= android.os.Build.VERSION_CODES.M
                && (cameraPermissionCheck == PackageManager.PERMISSION_DENIED
                || writePermissionCheck == PackageManager.PERMISSION_DENIED
                || readPermissionCheck == PackageManager.PERMISSION_DENIED
                || coarsePermissionCheck == PackageManager.PERMISSION_DENIED
                || bluetoothPermissionCheck == PackageManager.PERMISSION_DENIED
                || bluetoothAdimePermissionCheck == PackageManager.PERMISSION_DENIED)) {
            int per = QSC_PERMISSIONS_CAPTURE;
            askPermission(per);
            return;
        }

        CheckPhoneState();
    }

    private void CheckPhoneState(){

        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }

    }

    private void askPermission(int permissionCode) {
        int currentAPIVersion = android.os.Build.VERSION.SDK_INT;
        int cameraPermissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int writePermissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int readPermissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        int coarsePermissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        int bluetoothPermissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH);
        int bluetoothAdimePermissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN);

        if (currentAPIVersion >= android.os.Build.VERSION_CODES.M) {
            int denyNum = 0, index = 0;

            if (cameraPermissionCheck == PackageManager.PERMISSION_DENIED)
                denyNum++;
            if (writePermissionCheck == PackageManager.PERMISSION_DENIED)
                denyNum++;
            if (readPermissionCheck == PackageManager.PERMISSION_DENIED)
                denyNum++;
            if (coarsePermissionCheck == PackageManager.PERMISSION_DENIED)
                denyNum++;
            if (bluetoothPermissionCheck == PackageManager.PERMISSION_DENIED)
                denyNum++;
            if (bluetoothAdimePermissionCheck == PackageManager.PERMISSION_DENIED)
                denyNum++;

            String permission[] = new String[denyNum];

            if (cameraPermissionCheck == PackageManager.PERMISSION_DENIED) {
                permission[index] = Manifest.permission.CAMERA;
                index++;
            }
            if (writePermissionCheck == PackageManager.PERMISSION_DENIED) {
                permission[index] = Manifest.permission.WRITE_EXTERNAL_STORAGE;
                index++;
            }
            if (readPermissionCheck == PackageManager.PERMISSION_DENIED) {
                permission[index] = Manifest.permission.READ_EXTERNAL_STORAGE;
                index++;
            }

            if (coarsePermissionCheck == PackageManager.PERMISSION_DENIED) {
                permission[index] = Manifest.permission.ACCESS_COARSE_LOCATION;
                index++;
            }

            if (bluetoothPermissionCheck == PackageManager.PERMISSION_DENIED) {
                permission[index] = Manifest.permission.ACCESS_COARSE_LOCATION;
                index++;
            }

            if (bluetoothAdimePermissionCheck == PackageManager.PERMISSION_DENIED) {
                permission[index] = Manifest.permission.ACCESS_COARSE_LOCATION;
                index++;
            }


            if (denyNum > 0)
                ActivityCompat.requestPermissions(this, permission, permissionCode);
        }
    }


}

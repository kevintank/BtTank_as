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

import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

/**
 * This Activity appears as a dialog. It lists any paired devices and
 * devices detected in the area after discovery. When a device is chosen
 * by the user, the MAC address of the device is sent back to the parent
 * Activity in the result Intent.
 * ��ġ�� ã�� ���� ���̾˷α� â�̴� 
 */
public class DeviceListActivity extends Activity {
    // Debugging
    private static final String TAG = "DeviceListActivity";
    private static final boolean D = true;

    // Return Intent extra
    //������ ��ġ�� MAC��巹��
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    // Member fields
    // ������� �ƴ�Ÿ 
    private BluetoothAdapter mBtAdapter;
    //���� ���
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    //���� �߰��� ���
    private ArrayAdapter<String> mNewDevicesArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the window
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS); //���� �����ʿ� ���׸� �ձ� ��� ����� ����� 
        setContentView(R.layout.device_list);

        // Set result CANCELED incase the user backs out
        setResult(Activity.RESULT_CANCELED);//����ڰ� �������� -> �θ�â�� �������� ����Ʈ�� ������ �ʴ� ���� ����

        // Initialize the button to perform device discovery
        //��ġ�� �˻�
        Button scanButton = (Button) findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                doDiscovery(); //������� �߰��ϱ�
                v.setVisibility(View.GONE);  //�ϴܹ�ư�� �����.
            }
        });

        // Initialize array adapters. One for already paired devices and
        // one for newly discovered devices
        //���� ����̽��� ǥ���� ArrayAdapter ��ü����
        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        //���� �߰ߵ� ����̽��� ǥ���� ArrayAdapter ��ü 
        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);

        // Find and set up the ListView for paired devices
        // ���� ��ġ�� ǥ���� ����Ʈ��
        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        // Find and set up the ListView for newly discovered devices
        //���� ã�� ��� ����� ����Ʈ��
        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

        // Register for broadcasts when a device is discovered
        // ����̽��� �߰ߵǾ����� ��ε��ɽ�Ʈ ���ù��� ����Ѵ�.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        // ����̽�ī �߰��� �������� ������ ��ε��ɽ�Ʈ ���ù� ���
        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        // Get the local Bluetooth adapter
        //������� �ƴ�Ÿ ��ü ������
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        // Get a set of currently paired devices
        //���� ����̽� �����´�.
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
         //���� ��ġ�� ���� �Ѵٸ� ����Ʈ�� �ƴ�Ÿ�� ����Ѵ�. 
        if (pairedDevices.size() > 0) {
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
                mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
        	//���� ��ġ�� ���ٸ� ���ٰ� ����Ѵ�. 
            String noDevices = getResources().getText(R.string.none_paired).toString();
            mPairedDevicesArrayAdapter.add(noDevices);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Make sure we're not doing discovery anymore
        //�۾� ���
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }

        // Unregister broadcast listeners
        // ���ù� ���� 
        this.unregisterReceiver(mReceiver);
    }

    /**
     * Start device discover with the BluetoothAdapter
     * �� ��ġ�� �߰��ϴ� �޼ҵ��̴�. 
     */
    private void doDiscovery() {
        if (D) Log.d(TAG, "doDiscovery()");

        // Indicate scanning in the title
        //������ ������ ���ɴ� ���� ����ϱ�
        setProgressBarIndeterminateVisibility(true);
        setTitle(R.string.scanning);

        // Turn on sub-title for new devices  "other Available Devices" ǥ��
        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

        // If we're already discovering, stop it
        // �̹� �߰��ϰ� �ִٸ� ����Ѵ�.
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }

        // Request discover from BluetoothAdapter
        // ã�� ���� ~~~~~~~~~~~~~~
        mBtAdapter.startDiscovery();
    }

    // The on-click listener for all devices in the ListViews
    // ����Ʈ�� Ŭ���� 
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Cancel discovery because it's costly and we're about to connect
            
        	// �����ϱ� ���� �߰��� ���
        	mBtAdapter.cancelDiscovery();

            // Get the device MAC address, which is the last 17 chars in the View
            //��ġ�� MAC ��巹���� ���´�  ������ 17 ���ڸ� ���´�.
        	String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);
            // Create the result Intent and include the MAC address
            // ����Ʈ ��ü �����ϰ� ������ ��ġ�� MAC ��巹�� ���� �դ����ش�. 
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

            // Set result and finish this Activity
            //Request�ڵ带 ����ϰ� ����Ʈ ����
            setResult(Activity.RESULT_OK, intent);
            finish(); //��Ƽ��Ƽ ����
        }
    };

    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction(); //����Ʈ ��� ���

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {  // �߰ߵȰŽ� �ִ� 
                    mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            // When discovery is finished, change the Activity title //�߰ߵ� ���� ������
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false); 
                setTitle(R.string.select_device);
                if (mNewDevicesArrayAdapter.getCount() == 0) { //���ٴ� ǥ�� "No Devices found"
                    String noDevices = getResources().getText(R.string.none_found).toString();
                    mNewDevicesArrayAdapter.add(noDevices);
                }
            }
        }
    };

}

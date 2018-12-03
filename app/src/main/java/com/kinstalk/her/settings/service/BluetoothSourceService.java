package com.kinstalk.her.settings.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.kinstalk.her.settings.HerSettingsApplication;
import com.kinstalk.her.settings.R;
import com.kinstalk.her.settings.data.bluetooth.A2dpProfile;
import com.kinstalk.her.settings.data.bluetooth.BluetoothDeviceFilter;
import com.kinstalk.her.settings.data.bluetooth.CachedBluetoothDevice;
import com.kinstalk.her.settings.data.bluetooth.HeadsetProfile;
import com.kinstalk.her.settings.data.bluetooth.LocalBluetoothAdapter;
import com.kinstalk.her.settings.data.bluetooth.LocalBluetoothDevice;
import com.kinstalk.her.settings.data.bluetooth.LocalBluetoothProfile;
import com.kinstalk.her.settings.util.ToastHelper;

import java.util.ArrayList;
import java.util.List;

import ly.count.android.sdk.Countly;

import static com.kinstalk.her.settings.data.bluetooth.BluetoothDeviceFilter.HANDLE_DEVICE_FILTER;

/**
 * Created by Zhigang Zhang on 2018/1/2.
 */

public class BluetoothSourceService extends BluetoothBaseProfileService  {
    private static final String TAG = "BluetoothSourceService";
    private Context mContext;

    private BluetoothDeviceFilter.Filter mFilter = HANDLE_DEVICE_FILTER;

    BluetoothSourceService(BluetoothService service) {
        super(service);
        mContext = service;
    }

    @Override
    public void start() {
        if(mStarted) {
            return;
        }
        mStarted = true;
        Log.d(TAG, "Bluetooth Source Service Start");

        mService.getLocalBluetoothAdapter().setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE);
        startDiscovery();
    }

    @Override
    public void stop() {
        if(!mStarted) {
            return;
        }
        Log.d(TAG, "Bluetooth Source Service Stop");

        mStarted = false;
        stopScanning();
        mDeviceList.clear();
    }

    @Override
    boolean unmatchDevice(CachedBluetoothDevice device) {
        boolean unmatch = true;
        List<LocalBluetoothProfile> profiles = device.getProfiles();
        for(LocalBluetoothProfile profile : profiles) {
            if((profile instanceof A2dpProfile) ||
                (profile instanceof HeadsetProfile)){
                unmatch = false;
                break;
            }
        }
        return unmatch;
    }

    private boolean isMatchDevice(CachedBluetoothDevice device){
        return mFilter.matches(device.getDevice());
    }

    @Override
    public void addDevice(CachedBluetoothDevice device) {
        if (isMatchDevice(device)) {
            Log.d(TAG, "Filter match. Add A2dp/Headset device:" + device.getName());
            device.unregisterCallback(this);
            mDeviceList.add(device);
            device.registerCallback(this);
            mService.onDeviceUpdated();
        } else {
            Log.d(TAG, "Filter not match: " + device.getName());
        }
    }

    @Override
    public void removeDevice(CachedBluetoothDevice device) {
        device.unregisterCallback(this);
        mDeviceList.remove(device);
        mService.onDeviceUpdated();
    }

    @Override
    public void bondStateChanged(CachedBluetoothDevice cachedDevice, int bondState, int preBondState) {
        if (bondState == BluetoothDevice.BOND_NONE && preBondState == BluetoothDevice.BOND_BONDING) {
            if(cachedDevice.isManualConnect()) {
                cachedDevice.setManualConnect(false);
                mService.onBondFail();
            }
            if(mStarted) {
                mService.onDeviceUpdated();
            }
        }
    }

    @Override
    public boolean startDiscovery() {
        LocalBluetoothAdapter adapter = mService.getLocalBluetoothAdapter();
        if (adapter != null && adapter.isEnabled() && !adapter.isDiscovering()) {
            mService.getLocalBluetoothManager().getCachedDeviceManager().clearNonBondedDevices();
            for(CachedBluetoothDevice device : mDeviceList) {
                device.unregisterCallback(this);
            }
            mDeviceList.clear();
            processConnBondedDevices();
            adapter.startScanning(true);
            return true;
        }
        return false;
    }

    @Override
    public void cancelDiscovery() {
        LocalBluetoothAdapter adapter = mService.getLocalBluetoothAdapter();
        adapter.cancelDiscovery();
    }

    @Override
    void stopScanning() {
        LocalBluetoothAdapter adapter = mService.getLocalBluetoothAdapter();
        adapter.stopScanning();
    }

    @Override
    void onConnected() {

    }
    @Override
    void onDisconnected() {

    }

    @Override
    void connectDevice(LocalBluetoothDevice device) {
        CachedBluetoothDevice cachedDevice = findDevice(device);

        if(cachedDevice == null) {
            Log.e(TAG, "device " + device.getName() + " not exist in cached list");
            return;
        }

        Countly.sharedInstance().recordEvent("Touch_connect_bt",1);

        cachedDevice.setManualConnect(true);
        if(cachedDevice.isConnecting() || cachedDevice.isLocalConnected()) {
            Log.d(TAG, "device " + cachedDevice.getName() + " is already connected or is connecting");
        } else if (cachedDevice.isBonded()) {
            cachedDevice.connect(false);
        } else if (cachedDevice.isBonding()) {
            //bonding do nothing
        } else {
            pair(cachedDevice);
        }
    }

    @Override
    void disconnectDevice(LocalBluetoothDevice device) {
        CachedBluetoothDevice cachedDevice = findDevice(device);

        if(cachedDevice == null) {
            Log.e(TAG, "device " + device.getName() + " not exist in cached list");
            return;
        }

        Countly.sharedInstance().recordEvent("Touch_disconnect_bt", 1);
        Log.d(TAG, "Try to disconnect cacheDevice: " + cachedDevice.getName() + " " + cachedDevice.getAddress());
        if (cachedDevice.isBusy()) {
            Log.d(TAG, "cacheDevice: " + cachedDevice + " is busy, disconnect it");
            cachedDevice.getDevice().cancelBondProcess();
            cachedDevice.disconnect();
        } else if (cachedDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
            Log.d(TAG, "cacheDevice: " + cachedDevice + " is bonding, cancel it");
            cachedDevice.getDevice().cancelBondProcess();
        } else if ((cachedDevice.getBondState() == BluetoothDevice.BOND_BONDED)) {
            Log.d(TAG, "cacheDevice: " + cachedDevice + " is bonded, unbond it");
            cachedDevice.disconnect();
            cachedDevice.unpair();
        }
    }

    @Override
    List<LocalBluetoothDevice> getDeviceList() {
        ArrayList<LocalBluetoothDevice> deviceList = new ArrayList<>();
        int connect = 0;
        int connecting = 0;
        int bond = 0;
        int bonding = 0;

        int position;
        for(CachedBluetoothDevice device : mDeviceList) {
            LocalBluetoothDevice localDevice = new LocalBluetoothDevice(device);
            if(localDevice.isConnected()) {
                position = connect;
                connect++;
            } else if(localDevice.isConnecting()) {
                position = connect + connecting;
                connecting++;
            } else  if(localDevice.isBonded()) {
                position = connect + connecting + bond;
                bond++;
            } else if(localDevice.isBonding()) {
                position = connect + connecting + bond + bonding;
                bonding++;
            } else {
                position = deviceList.size();
            }
            if(position >= deviceList.size()) {
                deviceList.add(localDevice);
            } else {
                deviceList.add(position, localDevice);
            }
        }
        return deviceList;
    }

    @Override
    public String getPlayingDevice() {
        return null;
    }
    private void pair(CachedBluetoothDevice device) {
        if (!device.startPairing()) {
            Log.d(TAG, "pair failure");
            Context context = HerSettingsApplication.getApplication().getApplicationContext();
            ToastHelper.makeText(context, R.drawable.toast_fail,
                    R.string.bt_connect_failure, Toast.LENGTH_SHORT).show();
        }
    }
}

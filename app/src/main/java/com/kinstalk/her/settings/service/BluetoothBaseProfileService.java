package com.kinstalk.her.settings.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.util.Log;
import android.widget.Toast;

import com.kinstalk.her.settings.R;
import com.kinstalk.her.settings.data.bluetooth.A2dpProfile;
import com.kinstalk.her.settings.data.bluetooth.A2dpSinkProfile;
import com.kinstalk.her.settings.data.bluetooth.BluetoothCallback;
import com.kinstalk.her.settings.data.bluetooth.CachedBluetoothDevice;
import com.kinstalk.her.settings.data.bluetooth.HeadsetProfile;
import com.kinstalk.her.settings.data.bluetooth.LocalBluetoothAdapter;
import com.kinstalk.her.settings.data.bluetooth.LocalBluetoothDevice;
import com.kinstalk.her.settings.data.bluetooth.LocalBluetoothManager;
import com.kinstalk.her.settings.data.bluetooth.LocalBluetoothProfile;
import com.kinstalk.her.settings.util.ToastHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import ly.count.android.sdk.Countly;

/**
 * Created by Zhigang Zhang on 2018/1/2.
 */

public abstract class BluetoothBaseProfileService implements BluetoothCallback,CachedBluetoothDevice.Callback{
    protected static String TAG = "BaseProfileService";

    BluetoothService mService;
    boolean mStarted = false;
    ArrayList<CachedBluetoothDevice> mDeviceList = new ArrayList<>();

    BluetoothBaseProfileService(BluetoothService service) {
        mService = service;
        mService.getLocalBluetoothManager().getEventManager().registerCallback(this);
    }

    abstract void start();
    abstract void stop();
    abstract boolean unmatchDevice(CachedBluetoothDevice device);
    abstract void addDevice(CachedBluetoothDevice device);
    abstract void removeDevice(CachedBluetoothDevice device);
    abstract void bondStateChanged(CachedBluetoothDevice cachedDevice, int bondState, int preBondState);

    abstract boolean startDiscovery();
    abstract void cancelDiscovery();
    abstract void stopScanning();

    abstract void onConnected();
    abstract void onDisconnected();

    abstract void connectDevice(LocalBluetoothDevice device);
    abstract void disconnectDevice(LocalBluetoothDevice device);
    abstract List<LocalBluetoothDevice> getDeviceList();
    abstract String getPlayingDevice();

    void destroy() {
        mService.getLocalBluetoothManager().getEventManager().unregisterCallback(this);
        for(CachedBluetoothDevice device : mDeviceList) {
            device.unregisterCallback(this);
        }
        mDeviceList.clear();
    }
    @Override
    public void onBluetoothStateChanged(int bluetoothState) {
        if((bluetoothState == BluetoothAdapter.STATE_ON) && mStarted) {
            startDiscovery();
        }
    }

    @Override
    public void onScanningStateChanged(boolean started) {

    }

    @Override
    public void onDeviceAdded(CachedBluetoothDevice cachedDevice) {
        // Prevent updates while the list shows one of the state messages
        if (mService.getBluetoothState() != BluetoothAdapter.STATE_ON)
            return;

        Log.d(TAG, "onDeviceAdded: " + cachedDevice.getName());
        if(findDevice(cachedDevice) == null) {
            addDevice(cachedDevice);
        }
    }

    @Override
    public void onDeviceDeleted(CachedBluetoothDevice cachedDevice) {

        Log.d(TAG, "onDeviceDeleted: " + cachedDevice.getName());
        removeDevice(cachedDevice);
    }

    @Override
    public void onDeviceBondStateChanged(CachedBluetoothDevice cachedDevice, int bondState, int preBondState) {
        Log.d(TAG, "onDeviceBondStateChanged:: " + cachedDevice.getName() +
                "  bondState " + bondState +
                " preBondState " + preBondState);

        bondStateChanged(cachedDevice, bondState, preBondState);
    }

    @Override
    public void onDeviceAttributesChanged(CachedBluetoothDevice device) {
        if (device != null) {
            int state = getConnectionSummary(device);
            boolean found = false;
            for(CachedBluetoothDevice cachedDevice : mDeviceList) {
                if(cachedDevice.equals(device)) {
                    found = true;
                    break;
                }
            }
            if (found) {
                Log.d(TAG, "onDeviceAttributesChanged device: " + device.getName() + " state: " + state);
                boolean oldConnected = device.isLocalConnected();
                boolean oldConnecting = device.isConnecting();
                boolean isManualConnect = device.isManualConnect();
                if ((state >= 200) && (state < 300)) {// a2dp and headset both connected or only headset connected

                    boolean isConnected = device.isLocalConnected();
                    device.setConnecting(false);//must before refreshUI
                    device.setLocalConnected(true);
                    device.setManualConnect(false);

                    if(!isConnected) {
                        onConnected();

                        HashMap<String, String> segmentation = new HashMap<>();
                        segmentation.put("bt_name", device.getName());
                        Countly.sharedInstance().recordEvent("Touch_bt_connected_dev_name", segmentation, 1);

                        ToastHelper.makeText(mService.getApplicationContext(), R.drawable.toast_success,
                                R.string.bt_connect_succeed, Toast.LENGTH_SHORT).show();
                    }
                } else if(state == 303) {
                    device.setConnecting(true);
                    device.setLocalConnected(false);
                } else if(state == 302) {
                    //bonded
                    if(device.isConnecting()) {
                        device.setManualConnect(false);
                    }
                    device.setLocalConnected(false);
                    device.setConnecting(false);
                    if(device.isManualConnect()) {
                        device.connect(false);
                    }
                } else {
                    device.setLocalConnected(false);
                    device.setConnecting(false);
                    //device.setManualConnect(false);
                }
                if(!device.isLocalConnected() && oldConnecting && !device.isConnecting() && isManualConnect) {
                    ToastHelper.makeText(mService.getApplicationContext(), R.drawable.toast_fail,
                            R.string.bt_connect_failure, Toast.LENGTH_SHORT).show();
                }
                if(oldConnected && !device.isLocalConnected()) {
                    onDisconnected();

                    ToastHelper.makeText(mService.getApplicationContext(), R.drawable.toast_disconnect,
                            R.string.bt_disconnect_event, Toast.LENGTH_SHORT).show();
                    int bondState = device.getBondState();
                    if(bondState == BluetoothDevice.BOND_NONE) {
                        mDeviceList.remove(device);
                    }
                }
                mService.onDeviceUpdated();
            }
        } else {
            Log.d(TAG, "nDeviceAttributesChanged no  device");
        }
    }

    private int getConnectionSummary(CachedBluetoothDevice cachedDevice) {

        boolean profileConnected = false;       // at least one profile is connected
        boolean a2dpNotConnected = false;       // A2DP is preferred but not connected
        boolean headsetNotConnected = false;    // Headset is preferred but not connected

        boolean a2dpConnecting = false;
        boolean headsetConnecting = false;

        for (LocalBluetoothProfile profile : cachedDevice.getProfiles()) {
            int connectionStatus = cachedDevice.getProfileConnectionState(profile);
            switch (connectionStatus) {
                case BluetoothProfile.STATE_CONNECTING:
                    if (profile.isProfileReady()) {
                        if ((profile instanceof A2dpProfile) ||
                                (profile instanceof A2dpSinkProfile)) {
                            a2dpConnecting = true;
                        } else if (profile instanceof HeadsetProfile) {
                            headsetConnecting = true;
                        }
                    }
                    break;
                case BluetoothProfile.STATE_DISCONNECTING:
                    //disconnecting, do nothing
                case BluetoothProfile.STATE_CONNECTED:
                    profileConnected = true;
                    break;

                case BluetoothProfile.STATE_DISCONNECTED:
                    if (profile.isProfileReady()) {
                        if ((profile instanceof A2dpProfile) ||
                                (profile instanceof A2dpSinkProfile)) {
                            a2dpNotConnected = true;
                        } else if (profile instanceof HeadsetProfile) {
                            headsetNotConnected = true;
                        }
                    }
                    break;
            }
        }

        if (profileConnected) {
            if (a2dpNotConnected && headsetNotConnected) {
                return 305;
            } else if (a2dpNotConnected) {
                return 204;
            } else if (headsetNotConnected) {
                return 205;
            } else {
                return 200;
            }
        }

        if (a2dpConnecting || headsetConnecting) {
            return 303; // connecting
        }

        switch (cachedDevice.getBondState()) {
            case BluetoothDevice.BOND_BONDING:
                return 301;

            case BluetoothDevice.BOND_BONDED:
                return 302;
            case BluetoothDevice.BOND_NONE:
            default:
                return 300;
        }
    }

    void processConnBondedDevices() {
        LocalBluetoothAdapter adapter = mService.getLocalBluetoothAdapter();
        LocalBluetoothManager manager = mService.getLocalBluetoothManager();

        Set<BluetoothDevice> bondedDevices = adapter.getBondedDevices();
        if (bondedDevices == null) {
            return;
        }

        for (BluetoothDevice device : bondedDevices) {
            CachedBluetoothDevice cachedDevice = manager.getCachedDeviceManager().findDevice(device);

            if (cachedDevice == null) {
                cachedDevice = manager.getCachedDeviceManager().addDevice(adapter, manager.getProfileManager(), device);
                Log.d(TAG, "processConnBondedDevices " + cachedDevice.getName() + " Connected: " + device.isConnected());
            }

            if (unmatchDevice(cachedDevice)) {
                Log.d(TAG,"unmatch device, unpair it");
                cachedDevice.unpair();
            } else {
                //cachedDevice.unregisterCallback(this);
                //cachedDevice.registerCallback(this);
                addDevice(cachedDevice);
            }
        }
    }

    CachedBluetoothDevice findDevice(LocalBluetoothDevice device) {
        CachedBluetoothDevice cachedDevice = null;
        for(CachedBluetoothDevice tempDevice : mDeviceList) {
            if(tempDevice.getAddress().equals(device.getAddress())) {
                cachedDevice = tempDevice;
                break;
            }
        }

        return cachedDevice;
    }

    CachedBluetoothDevice findDevice(CachedBluetoothDevice device) {
        CachedBluetoothDevice cachedDevice = null;
        for(CachedBluetoothDevice tempDevice : mDeviceList) {
            if(tempDevice.getAddress().equals(device.getAddress())) {
                cachedDevice = tempDevice;
                break;
            }
        }

        return cachedDevice;
    }
}

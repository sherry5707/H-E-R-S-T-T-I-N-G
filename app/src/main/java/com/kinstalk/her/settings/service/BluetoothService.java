package com.kinstalk.her.settings.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.kinstalk.her.settings.HerSettingsApplication;
import com.kinstalk.her.settings.R;
import com.kinstalk.her.settings.data.bluetooth.BluetoothCallback;
import com.kinstalk.her.settings.data.bluetooth.BluetoothConstants;
import com.kinstalk.her.settings.data.bluetooth.CachedBluetoothDevice;
import com.kinstalk.her.settings.data.bluetooth.LocalBluetoothAdapter;
import com.kinstalk.her.settings.data.bluetooth.LocalBluetoothDevice;
import com.kinstalk.her.settings.data.bluetooth.LocalBluetoothManager;
import com.kinstalk.her.settings.data.bluetooth.Utils;
import com.kinstalk.her.settings.util.ToastHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Zhigang Zhang on 2018/1/2.
 */

public class BluetoothService extends Service implements BluetoothCallback {
    private static final String TAG = "BluetoothService";

    private Context mContext;
    private BluetoothServiceBinder mBinder;

    private BluetoothBaseProfileService mProfileService;
    private BluetoothSourceService mSourceService;
    private BluetoothSinkService mSinkService;

    protected LocalBluetoothAdapter mLocalAdapter;
    protected LocalBluetoothManager mLocalManager;

    private ArrayList<IBluetoothCallback> mCallbackList = new ArrayList<>();

    private boolean mScanning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        Log.d(TAG, "Creating BluetoothService");

        mLocalManager = LocalBluetoothManager.getInstance(this);
        if (mLocalManager == null) {
            Log.e(TAG, "Bluetooth is not supported on this device");
            stopSelf();
        }
        mLocalAdapter = mLocalManager.getBluetoothAdapter();

        mLocalManager.getEventManager().registerCallback(this);
        mBinder = new BluetoothServiceBinder(this);
        startBluetoothProfileService();
    }

    @Override
    public void onDestroy() {
        if(mSourceService != null) {
            mSourceService.destroy();
            mSourceService = null;
        }
        if(mSinkService != null) {
            mSinkService.destroy();
            mSinkService = null;
        }
        mProfileService = null;
        super.onDestroy();
    }

    public IBinder onBind(Intent intent) {
        Log.d(TAG,"onBind:");
        return mBinder;
    }

    public boolean onUnbind(Intent intent) {
        Log.d(TAG,"onUnbind:");
        return super.onUnbind(intent);
    }

    private void startBluetoothProfileService() {
        if(mSourceService == null) {
            mSourceService = new BluetoothSourceService(this);
        }
        if(mSinkService == null) {
            mSinkService = new BluetoothSinkService(this);
        }
        updateA2dpMode();
    }

    private void updateA2dpMode() {
        if(!isBluetoothEnabled()) {
            mSourceService.stop();
            mSinkService.stop();
            mProfileService = null;
            return;
        }

        int mode = Utils.getA2dpMode(this);
        if(mode == BluetoothConstants.A2DP_SINK_MODE) {
            mSourceService.stop();
            mSinkService.start();
            mProfileService = mSinkService;
        } else {
            mSinkService.stop();
            mSourceService.start();
            mProfileService = mSourceService;
        }
    }

    private void setForegroundActivity(boolean foreground) {
        if(foreground) {
            mLocalManager.setForegroundActivity(mContext.getApplicationContext());
        } else {
            mLocalManager.setForegroundActivity(null);
        }
    }

    private boolean isBluetoothEnabled() {
        if(mLocalAdapter == null) {
            return false;
        } else {
            return mLocalAdapter.isEnabled();
        }
    }

    private void setBluetoothEnabled(boolean enable) {
        mLocalAdapter.setBluetoothEnabled(enable);
        //updateA2dpMode();
    }
    private boolean startDiscovery() {
        if(mProfileService != null) {
            return mProfileService.startDiscovery();
        } else {
            return false;
        }
    }

    private void cancelDiscovery(){
        if(mProfileService == null) {
            return;
        }
        mProfileService.cancelDiscovery();
    }

    private void stopScanning(){
        if(mProfileService == null) {
            return;
        }
        mProfileService.stopScanning();
    }

    private List<LocalBluetoothDevice> getDeviceList() {
        if(mProfileService == null) {
            return new ArrayList<>();
        }
        return mProfileService.getDeviceList();
    }

    private void connectDevice(LocalBluetoothDevice device) {
        mProfileService.connectDevice(device);
    }

    private void disconnectDevice(LocalBluetoothDevice device) {
        mProfileService.disconnectDevice(device);
    }

    private void setA2dpMode(int mode) {
        Utils.setA2dpMode(this.getApplicationContext(), mode);
        updateA2dpMode();
    }

    private void registerCallback(IBluetoothCallback callback) {
        mCallbackList.add(callback);
        if(mProfileService != null) {
            try {
                callback.onDeviceUpdated();
                callback.onScanStarted(mScanning);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        String deviceName;
        if(mProfileService == null) {
            deviceName = null;
        } else {
            deviceName = mProfileService.getPlayingDevice();
        }
        try {
            if(!TextUtils.isEmpty(deviceName)) {
                callback.onPlayStarted(true, deviceName);
            } else {
                callback.onPlayStarted(false, null);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void unregisterCallback(IBluetoothCallback callback) {
        mCallbackList.remove(callback);
    }

    LocalBluetoothManager getLocalBluetoothManager() {
        return mLocalManager;
    }

    LocalBluetoothAdapter getLocalBluetoothAdapter() {
        return mLocalAdapter;
    }

    int getBluetoothState() {
        return mLocalAdapter.getBluetoothState();
    }

    void onDeviceUpdated() {
        for(IBluetoothCallback callback : mCallbackList) {
            try {
                callback.onDeviceUpdated();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    void onBondFail() {
        Context context = HerSettingsApplication.getApplication().getApplicationContext();
        ToastHelper.makeText(context, R.drawable.toast_fail,
                R.string.bt_connect_failure, Toast.LENGTH_SHORT).show();
    }

    void onPlaying(boolean started, String deviceName) {
        for(IBluetoothCallback callback : mCallbackList) {
            try {
                callback.onPlayStarted(started, deviceName);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
    //implement BluetoothCallback
    @Override
    public void onBluetoothStateChanged(int bluetoothState) {
        if((bluetoothState == BluetoothAdapter.STATE_ON) || (bluetoothState == BluetoothAdapter.STATE_OFF)) {
            updateA2dpMode();
        }
    }

    @Override
    public void onScanningStateChanged(boolean started) {
        Log.d(TAG, "onScanningStateChanged:" + started);

        mScanning = started;
        for(IBluetoothCallback callback : mCallbackList) {
            try {
                callback.onScanStarted(started);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDeviceAdded(CachedBluetoothDevice cachedDevice) {

    }

    @Override
    public void onDeviceDeleted(CachedBluetoothDevice cachedDevice) {

    }

    @Override
    public void onDeviceBondStateChanged(CachedBluetoothDevice cachedDevice, int bondState, int preBondState) {

    }

    //implement IBluetoothService
    private static class BluetoothServiceBinder extends IBluetoothService.Stub {
        private BluetoothService mService;

        BluetoothServiceBinder(BluetoothService service) {
            mService = service;
        }
        @Override
        public void setForground(boolean foreground) throws RemoteException {
            mService.setForegroundActivity(foreground);
        }

        @Override
        public boolean isBluetoothEnabled() {
            return mService.isBluetoothEnabled();
        }

        @Override
        public void setBluetoothEnabled(boolean enable) {
            mService.setBluetoothEnabled(enable);
        }

        @Override
        public boolean startDiscovery() {
            return mService.startDiscovery();
        }

        @Override
        public void cancelDiscovery(){
            mService.cancelDiscovery();
        }

        @Override
        public void stopScanning(){
            mService.stopScanning();
        }

        @Override
        public void connectDevice(LocalBluetoothDevice device) {
            mService.connectDevice(device);
        }

        @Override
        public void disconnectDevice(LocalBluetoothDevice device) {
            mService.disconnectDevice(device);
        }

        @Override
        public List<LocalBluetoothDevice> getDeviceList() {
            return mService.getDeviceList();
        }

        @Override
        public void setA2dpMode(int mode) {
            mService.setA2dpMode(mode);
        }

        @Override
        public void registerCallback(IBluetoothCallback callback) {
            mService.registerCallback(callback);
        }

        @Override
        public void unregisterCallback(IBluetoothCallback callback) {
            mService.unregisterCallback(callback);
        }
    }
}

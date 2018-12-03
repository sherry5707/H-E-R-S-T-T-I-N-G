package com.kinstalk.her.settings.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothA2dpSink;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.kinstalk.her.settings.R;
import com.kinstalk.her.settings.data.bluetooth.A2dpSinkProfile;
import com.kinstalk.her.settings.data.bluetooth.CachedBluetoothDevice;
import com.kinstalk.her.settings.data.bluetooth.LocalBluetoothDevice;
import com.kinstalk.her.settings.data.bluetooth.LocalBluetoothManager;
import com.kinstalk.her.settings.data.bluetooth.LocalBluetoothProfile;
import com.kinstalk.her.settings.view.activity.AudioPlayingActivity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by Zhigang Zhang on 2018/1/2.
 */

public class BluetoothSinkService extends BluetoothBaseProfileService {
    private static final String TAG = "BluetoothSinkService";

    private static final int MSG_START_PLAYING = 1;
    private static final int MSG_STOP_PLAYING = 2;

    private static final int NOTIFICATION_ID = 11;

    private Context mContext;

    private boolean mIsPlaying;
    private String mDeviceName;

    BluetoothSinkService(BluetoothService service) {
        super(service);
        mContext = service;
    }

    @Override
    public void start() {
        if(mStarted) {
            return;
        }
        mStarted = true;
        mIsPlaying = false;
        Log.d(TAG, "Bluetooth Sink Service Start");
        mService.getLocalBluetoothAdapter().setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE);
        registerReceiver();
        processConnBondedDevices();
    }

    @Override
    public void stop() {
        if(!mStarted) {
            return;
        }

        mStarted = false;
        mIsPlaying = false;
        Log.d(TAG, "Bluetooth Sink Service Stop");
        mService.getLocalBluetoothAdapter().setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE);
        unRegisterReceiver();
        cancelNotification();
        mDeviceList.clear();
    }

    @Override
    boolean unmatchDevice(CachedBluetoothDevice device) {
        return !isMatchDevice(device);
    }

    private boolean isMatchDevice(CachedBluetoothDevice device){
        boolean match = false;
        List<LocalBluetoothProfile> profiles = device.getProfiles();
        for(LocalBluetoothProfile profile : profiles) {
            if(profile instanceof A2dpSinkProfile) {
                match = true;
                break;
            }
        }
        return match;
    }

    @Override
    public void addDevice(CachedBluetoothDevice device) {
        boolean match = isMatchDevice(device);

        if(match) {
            Log.d(TAG, "Filter match. Add A2dp Src device: " + device.getName());
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
        if((bondState == BluetoothDevice.BOND_BONDED) && (preBondState != BluetoothDevice.BOND_BONDED)) {
            Log.d(TAG, "device:" + cachedDevice.getName() + " bonded, try to add");
            onDeviceAdded(cachedDevice);
        }
        //do nothing for bond state in A2DP Sink mode
    }

    @Override
    public boolean startDiscovery() {
        //do nothing in A2dp SINK mode
        return false;
    }

    @Override
    public void cancelDiscovery() {
        //do nothing in A2dp SINK mode
    }

    @Override
    void stopScanning() {
        //do nothing in A2dp SINK mode
    }

    @Override
    void onConnected() {
        if(mStarted) {
            mService.getLocalBluetoothAdapter().setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE);
        }
    }
    @Override
    void onDisconnected() {
        if(mStarted) {
            mService.getLocalBluetoothAdapter().setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE);
        }
    }

    @Override
    void connectDevice(LocalBluetoothDevice device) {
        Log.w(TAG, "connect device is not permitted in SINK mode");
    }

    @Override
    void disconnectDevice(LocalBluetoothDevice device) {
        CachedBluetoothDevice cachedDevice = findDevice(device);

        if(cachedDevice == null) {
            Log.e(TAG, "device " + device.getName() + " not exist in cached list");
            return;
        }

        Log.d(TAG, "Try to disconnect cacheDevice: " + cachedDevice.getName() + " " + cachedDevice.getAddress());
        if (cachedDevice.isLocalConnected()) {
            cachedDevice.disconnect();
        }
    }

    @Override
    List<LocalBluetoothDevice> getDeviceList() {
        ArrayList<LocalBluetoothDevice> deviceList = new ArrayList<>();
        //only return the connected device in SINK mode
        for(CachedBluetoothDevice device : mDeviceList) {
            if(device.isLocalConnected()) {
                deviceList.add(new LocalBluetoothDevice(device));
                break;
            }
        }
        return deviceList;
    }

    @Override
    String getPlayingDevice() {
        if(mIsPlaying) {
            return mDeviceName;
        }
        return null;
    }

    private void startPlayingUI() {
        Intent newIntent = new Intent(mContext, AudioPlayingActivity.class);
        newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(newIntent);
    }

    private void updateDeviceName() {
        String name = null;
        Collection<CachedBluetoothDevice> cachedDevices =
                LocalBluetoothManager.getInstance(mContext).getCachedDeviceManager().getCachedDevicesCopy();
        for(CachedBluetoothDevice device : cachedDevices) {
            if(device.isConnected()) {
                name = device.getName();
                break;
            }
        }
        mDeviceName = name;
    }

    private void postNotification() {
        Resources r = mContext.getResources();
        CharSequence title = r.getText(R.string.bt_playing_notification_title);
        CharSequence message = r.getText(R.string.bt_playing_notification_content) + mDeviceName;

        Bitmap largeIcon = BitmapFactory.decodeResource(r, R.drawable.icon_bluetooth);

        Notification.Builder notification = new Notification.Builder(mContext)
                .setContentTitle(title)
                .setContentText(message)
                .setLargeIcon(largeIcon)
                .setSmallIcon(R.drawable.icon_bluetooth)
                .setOngoing(true)
                .setAutoCancel(false);

        Intent intent = new Intent(mContext, AudioPlayingActivity.class);;
        PendingIntent pi = PendingIntent.getActivity(mContext, 0,
                intent, 0, null);

        notification.setContentIntent(pi);

        NotificationManager nm = (NotificationManager)
                mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_ID, notification.build());
    }

    private void cancelNotification() {
        NotificationManager nm = (NotificationManager)
                mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(NOTIFICATION_ID);
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothA2dpSink.ACTION_PLAYING_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        mContext.registerReceiver(mPlayingStateReceiver, filter);
    }

    private void unRegisterReceiver() {
        mContext.unregisterReceiver(mPlayingStateReceiver);
    }

    private BroadcastReceiver mPlayingStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BluetoothA2dpSink.ACTION_PLAYING_STATE_CHANGED)) {
                int preState = intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, BluetoothA2dpSink.STATE_NOT_PLAYING);
                int curState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothA2dpSink.STATE_NOT_PLAYING);

                if((curState == BluetoothA2dpSink.STATE_NOT_PLAYING) && (preState == BluetoothA2dpSink.STATE_PLAYING)) {
                    Log.d(TAG, "Bluetooth A2dp Sink stop playing");
                    mHandler.sendEmptyMessage(MSG_STOP_PLAYING);
                } else if((curState == BluetoothA2dpSink.STATE_PLAYING) && (preState == BluetoothA2dpSink.STATE_NOT_PLAYING)) {
                    Log.d(TAG, "Bluetooth A2dp Sink start playing");
                    mHandler.sendEmptyMessage(MSG_START_PLAYING);
                }
            } else if(intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if((state == BluetoothAdapter.STATE_TURNING_OFF) || (state == BluetoothAdapter.STATE_OFF)) {
                    mHandler.sendEmptyMessage(MSG_STOP_PLAYING);
                }
            }
        }
    };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_START_PLAYING:
                    if(!mIsPlaying) {
                        mIsPlaying = true;
                        updateDeviceName();
                        mService.onPlaying(mIsPlaying, mDeviceName);

                        startPlayingUI();
                        postNotification();
                    }
                    break;
                case MSG_STOP_PLAYING:
                    if(mIsPlaying) {
                        mIsPlaying = false;
                        mService.onPlaying(mIsPlaying, null);
                        cancelNotification();
                    }
                    break;
                default:
                    break;
            }
        }

    };

}

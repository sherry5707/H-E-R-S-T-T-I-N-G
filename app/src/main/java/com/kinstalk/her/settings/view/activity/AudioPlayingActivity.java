package com.kinstalk.her.settings.view.activity;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.kinstalk.her.settings.R;
import com.kinstalk.her.settings.data.bluetooth.CachedBluetoothDevice;
import com.kinstalk.her.settings.data.bluetooth.LocalBluetoothManager;
import com.kinstalk.her.settings.service.BluetoothService;
import com.kinstalk.her.settings.service.IBluetoothCallback;
import com.kinstalk.her.settings.service.IBluetoothService;

import java.lang.reflect.Method;
import java.util.Collection;

/**
 * Created by Zhigang Zhang on 2017/12/1.
 */

public class AudioPlayingActivity extends Activity{
    private static final String TAG = "AudioPlayingActivity";
    private static final int MSG_PLAYING_STATUS_START = 0;
    private static final int MSG_PLAYING_STATUS_STOP = 1;

    private Context mContext;
    private TextView mDeviceNameText;
    private ObjectAnimator mDiscPlayingAnim;
    private boolean mIsPlaying = false;

    private bluetoothCallback mCallback = new bluetoothCallback();
    private IBluetoothService mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav
                        // bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);

        setContentView(R.layout.activity_playing_state);

        ImageView discImage = (ImageView)findViewById(R.id.playing_icon);
        mDiscPlayingAnim = ObjectAnimator.ofFloat(discImage, "rotation", 0f, 360f);
        mDiscPlayingAnim.setDuration(60000);
        mDiscPlayingAnim.setInterpolator(new LinearInterpolator());
        mDiscPlayingAnim.setRepeatMode(ValueAnimator.RESTART);
        mDiscPlayingAnim.setRepeatCount(ValueAnimator.INFINITE);

        mDeviceNameText = (TextView)findViewById(R.id.device_name);
    }

    @Override
    public void onResume() {
        super.onResume();

        setAutoHome(false);

        if (mService == null) {
            Intent intent = new Intent(this, BluetoothService.class);
            bindService(intent, mConnection, 0);
            return;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        setAutoHome(true);

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mService != null) {
            unbindService(mConnection);
            mService = null;
        }
        mDiscPlayingAnim.cancel();
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_PLAYING_STATUS_START:
                    startPlaying((String)msg.obj);
                    break;
                case MSG_PLAYING_STATUS_STOP:
                    stopPlaying();
                    break;
                default:
                    break;
            }
        }

    };

    private void startPlaying(String deviceName) {
        mIsPlaying = true;
        if(TextUtils.isEmpty(deviceName)) {
            mDeviceNameText.setText(R.string.bt_src_device_name_unknown);
        } else {
            mDeviceNameText.setText(deviceName);
        }
        mDiscPlayingAnim.cancel();
        mDiscPlayingAnim.start();
    }

    private void stopPlaying() {
        mIsPlaying = false;
        mDiscPlayingAnim.cancel();
        finish();
    }

    private String getCurrentDevice() {
        String name = null;
        Collection<CachedBluetoothDevice> cachedDevices =
                LocalBluetoothManager.getInstance(mContext).getCachedDeviceManager().getCachedDevicesCopy();
        for(CachedBluetoothDevice device : cachedDevices) {
            if(device.isConnected()) {
                name = device.getName();
            }
        }
        return name;
    }

    private void setAutoHome(boolean enable) {
        WindowManager.LayoutParams attr = getWindow().getAttributes();
        try {
            Class<WindowManager.LayoutParams> attrClass = WindowManager.LayoutParams.class;
            Method method = attrClass.getMethod("setAutoActivityTimeout", new Class[]{boolean.class});
            method.setAccessible(true);
            Object object = method.invoke(attr, enable);
        } catch (Exception e1){
            e1.printStackTrace();
        }
        getWindow().setAttributes(attr);
    }

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(TAG, "onServiceConnected");
            mService = IBluetoothService.Stub.asInterface(iBinder);
            try {
                mService.registerCallback(mCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "onServiceDisconnected");
            mService = null;
        }
    };

    private class bluetoothCallback extends IBluetoothCallback.Stub {
        @Override
        public void onDeviceUpdated() throws RemoteException {
        }

        @Override
        public void onScanStarted(boolean started) throws RemoteException {
        }

        @Override
        public void onPlayStarted(boolean started, String deviceName) throws RemoteException {
            if(started) {
                mHandler.obtainMessage(MSG_PLAYING_STATUS_START, deviceName).sendToTarget();
            } else {
                mHandler.sendEmptyMessage(MSG_PLAYING_STATUS_STOP);
            }
        }
    }
}

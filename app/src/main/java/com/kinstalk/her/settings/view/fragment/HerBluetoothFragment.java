package com.kinstalk.her.settings.view.fragment;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.kinstalk.her.settings.data.bluetooth.LocalBluetoothDevice;
import com.kinstalk.her.settings.service.BluetoothService;
import com.kinstalk.her.settings.service.IBluetoothCallback;
import com.kinstalk.her.settings.service.IBluetoothService;
import com.kinstalk.her.settings.util.DebugUtils;
import com.kinstalk.her.settings.R;
import com.kinstalk.her.settings.view.adapter.HerBluetoothAdapter;

import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import ly.count.android.sdk.Countly;
/**
 * Created by ZhigangZhang on 2018/3/15.
 */

public class HerBluetoothFragment extends Fragment implements AdapterView.OnItemClickListener, View.OnClickListener {
    private static final String TAG = "HerBluetoothFragment";
    private static final int ID_STOP_DISCOVERY = 1;
    private static final int ID_INIT_UI = 2;
    private static final int ID_START_DISCOVERY = 3;
    private static final int ID_DEVICE_UPDATE = 4;
    private static final int ID_SCAN_START = 5;
    private static final int ID_SCAN_STOP = 6;
    private static final int ID_TURNON_BT = 7;
    private static final int ID_REBIND_SERVICE = 8;

    private static final int DELAYTIME_DISCOVERY = 10 * 1000;

    @BindView(R.id.listview)
    ListView listview;
    @BindView(R.id.progress_group)
    RelativeLayout progressGroup;
    @BindView(R.id.txtview_tip)
    TextView txtviewTip;
    @BindView(R.id.progress)
    ImageView progress;

    private HerBluetoothAdapter bluetoothListAdapter;

    Animation mRotateAnimation;

    private Unbinder unbinder;

    private IBluetoothService mService;
    private bluetoothCallback mCallback = new bluetoothCallback();

    public static HerBluetoothFragment getInstance() {
        HerBluetoothFragment herBluetoothFragment = new HerBluetoothFragment();
        return herBluetoothFragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_settings_bluetooth, container, false);

        unbinder = ButterKnife.bind(this, rootView);

        bluetoothListAdapter = new HerBluetoothAdapter();
        listview.setAdapter(bluetoothListAdapter);
        listview.setOnItemClickListener(this);

        txtviewTip.setText(R.string.bluetooth_searchstop);
        txtviewTip.setOnClickListener(HerBluetoothFragment.this);
        progress.setVisibility(View.INVISIBLE);

        mRotateAnimation = new RotateAnimation(0.0f, 720.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        mRotateAnimation.setFillAfter(true);
        mRotateAnimation.setInterpolator(new LinearInterpolator());
        mRotateAnimation.setDuration(2000);
        mRotateAnimation.setRepeatCount(Animation.INFINITE);
        mRotateAnimation.setRepeatMode(Animation.RESTART);
        return rootView;
    }

    @Override
    public void onPause() {
        super.onPause();
        DebugUtils.LogD("HerBluetoothFragment - onPause");
        if (mService == null) {
            return;
        }

        //removeAllDevices();
        try {
            mService.setForground(false);
            mService.unregisterCallback(mCallback);
        } catch (RemoteException e) {
            Log.e(TAG, e.toString());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mService == null) {
            bindService();
            return;
        }
        try {
            mService.registerCallback(mCallback);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        initUI();
    }

    private void bindService() {
        Intent intent = new Intent(this.getActivity().getApplicationContext(), BluetoothService.class);
        this.getActivity().getApplicationContext().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    private void initUI() {
        try {
            mService.setForground(true);
        } catch (RemoteException e) {
            Log.e(TAG, e.toString());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        DebugUtils.LogD("HerBluetoothFragment - onDestroyView");
        unbinder.unbind();

        handler.removeMessages(ID_STOP_DISCOVERY);
        handler.removeMessages(ID_REBIND_SERVICE);

        if(mService != null) {
            try {
                mService.cancelDiscovery();
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        }
        if(mService != null) {
            this.getActivity().getApplicationContext().unbindService(mConnection);
            mService = null;
        }
        bluetoothListAdapter.clearSouces();
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.txtview_tip: {
                startDiscovery();
                break;
            }
        }
    }

    private void updateDeviceList() {
        if(mService == null) {
            return;
        }
        List<LocalBluetoothDevice> deviceList = null;

        try {
            deviceList = mService.getDeviceList();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        bluetoothListAdapter.clearSouces();
        bluetoothListAdapter.addDevices(deviceList);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final LocalBluetoothDevice device = (LocalBluetoothDevice) bluetoothListAdapter.getItem(position);
        if (device == null) {
            Log.w(TAG, "null cacheDevice!");
            return;
        }

        try {
            mService.stopScanning();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        if (device.isConnected()) {
            try {
                HashMap<String, String> segmentation = new HashMap<String, String>();
                segmentation.put("name", device.getName());
                Countly.sharedInstance().endEvent("HerSettings2","timed_bluetooth_connected",segmentation,1,0);
                mService.disconnectDevice(device);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else if(device.isConnecting()) {
            //do nothing if device is connecting
        } else {
            try {
                Countly.sharedInstance().startEvent("HerSettings2","timed_bluetooth_connected");
                mService.connectDevice(device);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void cancelDiscovery() {
        DebugUtils.LogD("cancelDiscovery.");
        try {
            mService.cancelDiscovery();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void startDiscovery() {
        DebugUtils.LogD("startDiscovery.");

        if(mService == null) {
            return;
        }
        bluetoothListAdapter.clearSouces();

        try {
            mService.startDiscovery();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ID_STOP_DISCOVERY: {
                    if (mService != null) {
                        DebugUtils.LogE("停止扫描");
                        cancelDiscovery();
                    }
                    break;
                }
                case ID_START_DISCOVERY: {
                    startDiscovery();
                    break;
                }
                case ID_INIT_UI: {
                    initUI();
                    break;
                }
                case ID_DEVICE_UPDATE: {
                    updateDeviceList();
                    break;
                }
                case ID_SCAN_START: {
                    if (txtviewTip != null) {
                        txtviewTip.setText(R.string.bluetooth_searchstart);
                        txtviewTip.setOnClickListener(null);
                    }
                    handler.removeMessages(ID_STOP_DISCOVERY);
                    handler.sendEmptyMessageDelayed(ID_STOP_DISCOVERY, DELAYTIME_DISCOVERY);

                    progress.setVisibility(View.VISIBLE);
                    progress.setAnimation(mRotateAnimation);
                    break;
                }
                case ID_SCAN_STOP: {
                    if (txtviewTip != null) {
                        txtviewTip.setText(R.string.bluetooth_searchstop);
                        txtviewTip.setOnClickListener(HerBluetoothFragment.this);
                    }
                    handler.removeMessages(ID_STOP_DISCOVERY);
                    if(progress != null) {
                        progress.setVisibility(View.INVISIBLE);
                        progress.clearAnimation();
                    }
                    break;
                }
                case ID_TURNON_BT: {
                    if(mService != null) {
                        try {
                            mService.setBluetoothEnabled(true);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                    handler.sendEmptyMessage(ID_START_DISCOVERY);
                    break;
                }
                case ID_REBIND_SERVICE: {
                    if(mService == null) {
                        bindService();
                    }
                }
            }
        }
    };

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
            handler.sendEmptyMessage(ID_INIT_UI);

            boolean btEnabled = false;
            try {
                btEnabled = mService.isBluetoothEnabled();
            } catch (RemoteException e) {
                Log.e(TAG, "should not happend this exception:" + e);
                return;
            }
            if(!btEnabled) {
                handler.sendEmptyMessage(ID_TURNON_BT);
            } else {
                if (!handler.hasMessages(ID_START_DISCOVERY)) {
                    handler.sendEmptyMessage(ID_START_DISCOVERY);
                }
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
            handler.sendEmptyMessage(ID_DEVICE_UPDATE);
        }

        @Override
        public void onScanStarted(boolean started) throws RemoteException {
            if(started) {
                handler.sendEmptyMessage(ID_SCAN_START);
            } else {
                handler.sendEmptyMessage(ID_SCAN_STOP);
            }
        }

        @Override
        public void onPlayStarted(boolean started, String deviceName) throws RemoteException {

        }
    }
}

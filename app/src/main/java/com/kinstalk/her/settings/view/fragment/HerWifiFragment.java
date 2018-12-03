package com.kinstalk.her.settings.view.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.os.SystemProperties;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.kinstalk.her.httpsdk.util.DebugUtil;
import com.kinstalk.her.settings.HerSettingsApplication;
import com.kinstalk.her.settings.R;
import com.kinstalk.her.settings.data.eventbus.DataEventBus;
import com.kinstalk.her.settings.data.eventbus.entity.WifiConnectErrorEntity;
import com.kinstalk.her.settings.data.eventbus.entity.WifiConnectStatusChangeEntity;
import com.kinstalk.her.settings.data.eventbus.entity.WifiConnectSuccessEntity;
import com.kinstalk.her.settings.data.eventbus.entity.WifiScanSuccessEntity;
import com.kinstalk.her.settings.data.eventbus.entity.WifiScanTimeoutEntity;
import com.kinstalk.her.settings.data.wifi.ScanResultEntity;
import com.kinstalk.her.settings.data.wifi.WifiHelper;
import com.kinstalk.her.settings.data.wifi.WifiScanTimer;
import com.kinstalk.her.settings.util.Constants;
import com.kinstalk.her.settings.util.DebugUtils;
import com.kinstalk.her.settings.util.ToastHelper;
import com.kinstalk.her.settings.view.adapter.WifiListAdapter;
import com.kinstalk.her.settings.view.widget.WifiListHeaderView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnItemClick;
import butterknife.Unbinder;

/**
 * Created by zhigangzhang on 2017/5/15.
 */

public class HerWifiFragment extends Fragment {
    private Unbinder unbinder;
    private WifiScanTimer scanTimer;

    @BindView(R.id.wifi_listview)
    ListView mListView;

    WifiListHeaderView mHeaderView;
    ImageView mScanView;
    TextView mScanContentView;

    WifiListAdapter mAdapter;
    private boolean isAuthDlgShown = false;
    private boolean mConnecting = false;

    WifiAuthDialogFragment mWifiAuthDialogFragment;

    public static HerWifiFragment getInstance() {
        HerWifiFragment herWifiFragment = new HerWifiFragment();
        return herWifiFragment;
    }

    private void addHeaderView() {
        mHeaderView = new WifiListHeaderView(getActivity());
        mScanView = (ImageView) mHeaderView.findViewById(R.id.wifi_scan);

        mScanContentView = (TextView)mHeaderView.findViewById(R.id.scan_status_content);

        mScanContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (view.getId() == R.id.scan_status_content) {
                    startScan();
                }
            }
        });
        mScanView.setVisibility(View.INVISIBLE);
        mListView.addHeaderView(mHeaderView);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_settings_wifi, container, false);
        unbinder = ButterKnife.bind(this, rootView);
        scanTimer = new WifiScanTimer();

        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        DataEventBus.register(this);
        addHeaderView();
        mAdapter = new WifiListAdapter(mListView);
        mListView.setAdapter(mAdapter);
        boolean isWiFiConnected = WifiHelper.getInstance().isWifiConnected();
        startScan();
    }

    @Override
    public void onAttach(Context context) {
        // Log.d(TAG,"onAttach");
        super.onAttach(context);
    }

    @Override
    public void onResume() {
        super.onResume();
        Context context = HerSettingsApplication.getApplication().getApplicationContext();
        SharedPreferences sharedPref = context.getSharedPreferences(Constants.SHARED_PREF_WIFI_SETTINGS, Context.MODE_PRIVATE);
        sharedPref.edit().putBoolean(Constants.SHARED_PREF_KEY_WIFI_FOREGROUND, true).commit();
        SystemProperties.set("persist.sys.wifi.status", "1");
        context.sendBroadcast(new Intent("com.kinstalk.her.qchat.setting.status.change"));
        boolean isWiFiConnected = WifiHelper.getInstance().isWifiConnected();
    }

    @Override
    public void onPause() {
        super.onPause();
        Context context = HerSettingsApplication.getApplication().getApplicationContext();
        SharedPreferences sharedPref = context.getSharedPreferences(Constants.SHARED_PREF_WIFI_SETTINGS, Context.MODE_PRIVATE);
        sharedPref.edit().putBoolean(Constants.SHARED_PREF_KEY_WIFI_FOREGROUND, false).commit();
        SystemProperties.set("persist.sys.wifi.status", "0");
        context.sendBroadcast(new Intent("com.kinstalk.her.qchat.setting.status.change"));
    }

    @Override
    public void onDestroyView() {
        DebugUtils.LogD("onDestroyView");
        if(mWifiAuthDialogFragment != null) {
            mWifiAuthDialogFragment.dismiss();
            mWifiAuthDialogFragment = null;
        }
        scanTimer.stop();
        unbinder.unbind();
        DataEventBus.getEventBus().unregister(this);
        super.onDestroyView();
    }

    private void startScan() {
        scanTimer.stop();
        mListView.smoothScrollToPositionFromTop(0, 0);

        Animation mRotateAnimation = new RotateAnimation(0.0f, 720.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        mRotateAnimation.setFillAfter(true);
        mRotateAnimation.setInterpolator(new LinearInterpolator());
        mRotateAnimation.setDuration(2000);
        mRotateAnimation.setRepeatCount(Animation.INFINITE);
        mRotateAnimation.setRepeatMode(Animation.RESTART);

        mScanView.setVisibility(View.VISIBLE);
        mScanView.setAnimation(mRotateAnimation);

        scanTimer.start();

        mScanContentView.setEnabled(false);
        mScanContentView.setText(R.string.wifi_scanning);
    }


    @OnItemClick({R.id.wifi_listview})
    public void onItemClick(int postion) {
        isAuthDlgShown = false;
        postion = postion - mListView.getHeaderViewsCount();
        if (postion < 0 || postion >= mAdapter.getCount()) {
            return;
        }
        ScanResultEntity scanResultEntity = (ScanResultEntity) mAdapter.getItem(postion);
        ScanResult scanResult = scanResultEntity.getScanResult();
        //无密码
        if (!WifiHelper.isNeedAuth(scanResult.capabilities)) {
            WifiConfiguration configuration = WifiHelper.getInstance().createWifiConfiguration(scanResult.SSID, WifiHelper.AUTH_NONE, "", "");
            WifiHelper.getInstance().connectNetwork(configuration);
            return;
        }
        //已记录密码
        int networkId = WifiHelper.getInstance().getConfiguredNetworkID(scanResult.SSID);
        if (networkId != -1) {
            WifiHelper.getInstance().connectConfiguredNetwork(scanResult.SSID);
            return;
        }
        //有且没记录密码
        isAuthDlgShown = true;
        mWifiAuthDialogFragment = WifiAuthDialogFragment.newInstance(mAdapter.getItem(postion).getScanResult());
        mWifiAuthDialogFragment.show((getActivity()).getSupportFragmentManager(), "wifiAuthDialogFragment");
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onScanTimeout(WifiScanTimeoutEntity timeoutEntity) {
        scanTimer.stop();

        mScanView.clearAnimation();
        mScanView.setVisibility(View.INVISIBLE);

        mScanContentView.setEnabled(true);
        mScanContentView.setText(R.string.wifi_not_scan);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onScanSuccess(WifiScanSuccessEntity successEntity) {
        DebugUtil.LogD("HerWifiFragment","scanSuccess,successEntity:"+successEntity.toString());
        scanTimer.stop();

        mScanView.setVisibility(View.INVISIBLE);
        mScanView.clearAnimation();

        mScanContentView.setEnabled(true);
        mScanContentView.setText(R.string.wifi_not_scan);

        mAdapter.refreshData(WifiHelper.getInstance().getScanResultEntityList());
        mHeaderView.bindStatus(new WifiConnectStatusChangeEntity(NetworkInfo.State.CONNECTED, NetworkInfo.DetailedState.CONNECTED));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWifiNetworkStatusChange(WifiConnectStatusChangeEntity statusEntity) {
        DebugUtil.LogD("HerWifiFragment",
                "onWifiNetworkStatusChange: statusEntity.getState()"+statusEntity.getState());
        mHeaderView.bindStatus(statusEntity);
        switch (statusEntity.getState()) {
            case CONNECTED:
                DebugUtil.LogD("HerWifiFragment","connected");
                mListView.smoothScrollToPositionFromTop(0, 0);
                mAdapter.refreshData(WifiHelper.getInstance().getScanResultEntityList());
                if(mConnecting) {
                    ToastHelper.makeText(getContext(), R.drawable.toast_success,
                            R.string.connect_succeed, Toast.LENGTH_SHORT).show();
                    WifiHelper.getInstance().removeWifi(WifiHelper.getInstance().getLastNetworkId());
                }
                mConnecting = false;
                break;
            case CONNECTING:
                DebugUtil.LogD("HerWifiFragment","connecting");
                mAdapter.setLoadingStatus();
                mConnecting = true;
                break;
            case DISCONNECTED:
                if(mConnecting) {
                    DebugUtil.LogD("HerWifiFragment","disconnected and mConnecting is true");
                    if(!isAuthDlgShown) {
                        DebugUtil.LogD("HerWifiFragment","show toast");
                        ToastHelper.makeText(getContext(), R.drawable.toast_fail,
                                R.string.connect_failed, Toast.LENGTH_SHORT).show();
                        WifiHelper.getInstance().removeWifi(WifiHelper.getInstance().getLastNetworkId());
                    }
                    //重置所有loading状态
                    mAdapter.resetLoadingStatus();
                    mConnecting = false;
                }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWifiConnectSuccess(WifiConnectSuccessEntity successEntiry) {
        //do nothing
    }
}

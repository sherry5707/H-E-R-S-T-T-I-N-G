package com.kinstalk.her.settings.view.widget;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kinstalk.her.settings.R;
import com.kinstalk.her.settings.data.eventbus.DataEventBus;
import com.kinstalk.her.settings.data.eventbus.entity.WifiConnectStatusChangeEntity;
import com.kinstalk.her.settings.data.eventbus.entity.WifiConnectSuccessEntity;
import com.kinstalk.her.settings.data.wifi.WifiHelper;

/**
 * Created by pop on 17/5/18.
 */

public class WifiListHeaderView extends LinearLayout implements View.OnClickListener {
    private Context mContext;

    private TextView mLabelView;
//    private ImageView mSignalView;
    private ViewGroup mConnectLayout;
    private ImageView mPaddingView;

    public WifiListHeaderView(Context context) {
        super(context);
        initView(context);
    }

    public WifiListHeaderView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public WifiListHeaderView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private void initView(Context context) {
        this.mContext = context;
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_wifilist_header, this);

        mLabelView = (TextView) findViewById(R.id.wifi_ssid);
//        mSignalView = (ImageView) findViewById(R.id.wifi_signal_icon);
        mConnectLayout = (ViewGroup) findViewById(R.id.connected_wifi_info);
        mPaddingView = (ImageView) findViewById(R.id.head_padding);
        mConnectLayout.setOnClickListener(this);
        refreshView();
    }

    public void bindStatus(WifiConnectStatusChangeEntity statusEntity) {
        switch (statusEntity.getState()) {
            case DISCONNECTED:
                mConnectLayout.setVisibility(View.GONE);
                mPaddingView.setVisibility(View.VISIBLE);
                break;
            case CONNECTED:
                refreshView();
                break;
        }
    }

    private void refreshView() {
        WifiInfo wifiInfo = WifiHelper.getInstance().getWifiInfo();
        if ((wifiInfo != null) && (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED)) {
            ScanResult scanResult = WifiHelper.getInstance().getScanResultBySSID(wifiInfo.getSSID());
            if (scanResult != null) {
                boolean isNeedAuth = WifiHelper.isNeedAuth(scanResult.capabilities);
                int level = WifiManager.calculateSignalLevel(scanResult.level, 5);
                mLabelView.setText(scanResult.SSID);
//                if (isNeedAuth) {
//                    mSignalView.setImageResource(R.drawable.wifi_signal_lock_icon);
//                } else {
//                    mSignalView.setImageResource(R.drawable.wifi_signal_icon);
//                }
//                mSignalView.setImageLevel(level);
                mConnectLayout.setVisibility(View.VISIBLE);
                mPaddingView.setVisibility(View.GONE);
                if (WifiHelper.getInstance().getLastNetworkId() != -1) {
                    WifiHelper.getInstance().setLastConnectNetworkId(-1);
                }
                //发出当前网络已可用的事件通知
                DataEventBus.getEventBus().post(new WifiConnectSuccessEntity());
            } else {
                mConnectLayout.setVisibility(View.GONE);
                mPaddingView.setVisibility(View.VISIBLE);
            }
        } else {
            mConnectLayout.setVisibility(View.GONE);
            mPaddingView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.connected_wifi_info:
                final WifiInfo wifiInfo = WifiHelper.getInstance().getWifiInfo();
                if (wifiInfo != null) {
                    WifiHelper.getInstance().removeWifi(wifiInfo.getNetworkId());
                }
                break;
        }
    }
}

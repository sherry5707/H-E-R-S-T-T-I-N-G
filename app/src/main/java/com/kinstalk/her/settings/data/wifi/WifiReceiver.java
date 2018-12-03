package com.kinstalk.her.settings.data.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.kinstalk.her.httpsdk.util.DebugUtil;
import com.kinstalk.her.settings.data.eventbus.DataEventBus;
import com.kinstalk.her.settings.data.eventbus.entity.WifiConnectErrorEntity;
import com.kinstalk.her.settings.data.eventbus.entity.WifiConnectStatusChangeEntity;
import com.kinstalk.her.settings.data.eventbus.entity.WifiScanSuccessEntity;
import ly.count.android.sdk.Countly;
import java.util.HashMap;

/**
 * Created by pop on 17/5/17.
 */

public class WifiReceiver extends BroadcastReceiver {
    private static final String TAG = "WifiReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        //wifi接入点扫描完成，并且结果已经可以获得，可以调用getScanResults()获得结果
        if (intent.getAction().endsWith(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
            DebugUtil.LogD(TAG, "扫描到可用wifi网络");
            notifyBroadcast(new WifiScanSuccessEntity());
        }
        //wifi状态变化，有可能是：已启用，已禁用，正在启用，正在禁用
        else if (intent.getAction().endsWith(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
            int preStatus = intent.getIntExtra(WifiManager.EXTRA_PREVIOUS_WIFI_STATE, 0);
            int nowStatus = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
            DebugUtil.LogD(TAG, "wifi开关状态发生变化，之前状态：" + preStatus + " | 当前状态：" + nowStatus);
        }
        //wifi连接网络的状态广播
        else if (intent.getAction().endsWith(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
            String bssid = intent.getStringExtra(WifiManager.EXTRA_BSSID);
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if (networkInfo != null) {
                if (networkInfo.getState() == NetworkInfo.State.CONNECTED) {
                    WifiInfo wifiInfo = WifiHelper.getInstance().getWifiInfo();
                    if (wifiInfo != null && wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
                        DebugUtil.LogD(TAG, "wifi:" + wifiInfo.getSSID() + "连接成功");
                        notifyBroadcast(new WifiConnectStatusChangeEntity(networkInfo.getState(), networkInfo.getDetailedState()));
                    }
                } else if (networkInfo.getState() == NetworkInfo.State.DISCONNECTED) {
                    DebugUtil.LogD(TAG, "wifi网络连接断开");
                    Countly.sharedInstance().recordEvent("HerSettings2", "wlan_disconnected");
                    notifyBroadcast(new WifiConnectStatusChangeEntity(networkInfo.getState(), networkInfo.getDetailedState()));
                } else if (networkInfo.getState() == NetworkInfo.State.CONNECTING) {
                    String ssid = getSSID(context);
                    if (!ssid.isEmpty()) {
                        HashMap<String, String> segmentation = new HashMap<String, String>();
                        segmentation.put("ssid", ssid);
                        Countly.sharedInstance().recordEvent("HerSettings2", "wlan_connected", segmentation, 1);
                    }
                    DebugUtil.LogD(TAG, "wifi网络连接ing...");
                    notifyBroadcast(new WifiConnectStatusChangeEntity(networkInfo.getState(), networkInfo.getDetailedState()));
                }
            }
        }
    }

    private void notifyBroadcast(Object entity) {
        DataEventBus.getEventBus().post(entity);
    }

    private String getSSID(Context context) {
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifi.getConnectionInfo();
        if (info != null && info.getMacAddress() != null)
            return info.getSSID();
        return null;
    }
}

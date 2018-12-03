package com.kinstalk.her.settings.data.wifi;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.TextUtils;

import com.kinstalk.her.settings.HerSettingsApplication;
import com.kinstalk.her.settings.data.eventbus.DataEventBus;
import com.kinstalk.her.settings.data.eventbus.entity.WifiConnectStatusChangeEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by pop on 17/5/17.
 */

public class WifiHelper {
    public static final int AUTH_NONE = 1;
    public static final int AUTH_PSK = 2;
    public static final int AUTH_EAP = 3;

    private static WifiHelper sInstance = new WifiHelper();

    private WifiManager wifiManager;

    private Map<String, ScanResult> scanResultMap = new HashMap<>();
    private int lastConnectNetworkId = -1;

    public static WifiHelper getInstance() {
        return sInstance;
    }

    private WifiHelper() {
        wifiManager = (WifiManager) HerSettingsApplication.getApplication().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    public void startScan() {
        wifiManager.startScan();
    }

    public List<ScanResult> getScanResults() {
        List<ScanResult> scanResults = wifiManager.getScanResults();
        Map<String, ScanResult> dataMap = new HashMap<>();
        List<ScanResult> dataList = new ArrayList<>();
        if (scanResults != null && scanResults.size() > 0) {
            for (ScanResult scanResult : scanResults) {
                //滤空 && 滤重
                if (!TextUtils.isEmpty(scanResult.SSID) && !dataMap.containsKey(scanResult.SSID)) {
                    if (!scanResult.capabilities.contains("EAP")) {
                        //滤EAP认证模式
                        dataMap.put(scanResult.SSID, scanResult);
                    }
                }
            }
            //缓存数据
            scanResultMap.clear();
            scanResultMap.putAll(dataMap);
            //删除已连接的wifi
            WifiInfo wifiInfo = getWifiInfo();
            if (wifiInfo != null && (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) && !TextUtils.isEmpty(wifiInfo.getSSID())) {
                String SSID = wifiInfo.getSSID();
                if (SSID.startsWith("\"") && SSID.endsWith("\"")) {
                    SSID = SSID.replaceFirst("\"", "");
                    SSID = SSID.replaceFirst("\"", "");
                }
                dataMap.remove(SSID);
            }
            dataList.addAll(dataMap.values());
            //排序(信号强弱降序)
            Collections.sort(dataList, new Comparator<ScanResult>() {
                @Override
                public int compare(ScanResult s1, ScanResult s2) {
                    if (s1.level > s2.level) {
                        return -1;
                    } else if (s1.level < s2.level) {
                        return 1;
                    }
                    return 0;
                }
            });
            //如果当前wifi连接状态为未连接，尝试连接附近曾经连接过的
            if (wifiInfo == null) {
                for (ScanResult scanResult : dataList) {
                    if (connectConfiguredNetwork(scanResult.SSID)) {
                        break;
                    }
                }
            }
        }
        return dataList;
    }

    public List<ScanResultEntity> getScanResultEntityList() {
        List<ScanResultEntity> dataList = new ArrayList<>();
        for (ScanResult scanResult : getScanResults()) {
            dataList.add(new ScanResultEntity(scanResult, false));
        }
        return dataList;
    }

    public static boolean isNeedAuth(String capabilities) {
        if (TextUtils.isEmpty(capabilities)) {
            return false;
        }
        if (capabilities.contains("WEP") || capabilities.contains("PSK") || capabilities.contains("EAP")) {
            return true;
        }
        return false;
    }

    public WifiConfiguration isExsits(String SSID) {
        List<WifiConfiguration> wifiConfigurationList = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration wifiConfiguration : wifiConfigurationList) {
            if (wifiConfiguration.SSID.equals("\"" + SSID + "\"")) {
                return wifiConfiguration;
            }
        }
        return null;
    }

    //Save WIFI SSID Password zhenyubin
    public void saveWIFI(String ssid,String passwd){
        ContentResolver resolver = HerSettingsApplication.getApplication().getContentResolver();
        Uri uri = Uri.parse("content://com.kinstalk.her.settings.data.wifi.WifiIotProvider/insert");
        ContentValues values = new ContentValues();
        values.put("ssid",ssid);
        values.put("passwd",passwd);
        resolver.insert(uri,values);
    }

    public WifiConfiguration createWifiConfiguration(String SSID, int type, String userName, String password) {
        WifiConfiguration wifiConfiguration = new WifiConfiguration();

        //zhenyubin
        saveWIFI(SSID,password);

        wifiConfiguration.allowedAuthAlgorithms.clear();
        wifiConfiguration.allowedGroupCiphers.clear();
        wifiConfiguration.allowedKeyManagement.clear();
        wifiConfiguration.allowedPairwiseCiphers.clear();
        wifiConfiguration.allowedProtocols.clear();
        wifiConfiguration.SSID = "\"" + SSID + "\"";

        WifiConfiguration tempConfig = this.isExsits(SSID);
        if (tempConfig != null) {
            wifiManager.removeNetwork(tempConfig.networkId);
        }
        //无密码
        if (type == AUTH_NONE) {
            wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        }
        //WPA-PSK
        if (type == AUTH_PSK) {
            wifiConfiguration.preSharedKey = "\"" + password + "\"";
            wifiConfiguration.hiddenSSID = true;
            wifiConfiguration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            //wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            wifiConfiguration.status = WifiConfiguration.Status.ENABLED;
        }
        //WPA-EAP
        if (type == AUTH_EAP) {
            wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
            wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.IEEE8021X);
            WifiEnterpriseConfig enterpriseConfig = new WifiEnterpriseConfig();
            enterpriseConfig.setIdentity(userName);
            enterpriseConfig.setPassword(password);
            enterpriseConfig.setEapMethod(WifiEnterpriseConfig.Eap.PEAP);
            wifiConfiguration.enterpriseConfig = enterpriseConfig;
        }
        return wifiConfiguration;
    }

    public boolean connectNetwork(WifiConfiguration wifiConfiguration) {
        int networkId = wifiManager.addNetwork(wifiConfiguration);
        return enableNetwork(networkId);
    }

    public void disconnectWifi(int netId) {
        wifiManager.disableNetwork(netId);
        wifiManager.disconnect();
    }

    public void removeWifi(int netId) {
        //disconnectWifi(netId);
        wifiManager.removeNetwork(netId);
        wifiManager.saveConfiguration();
    }

    public WifiInfo getWifiInfo() {
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo != null && !TextUtils.isEmpty(wifiInfo.getSSID()) && !TextUtils.equals("<unknown ssid>", wifiInfo.getSSID())) {
            return wifiInfo;
        }
        return null;
    }

    public boolean connectConfiguredNetwork(String SSID) {
        int networkId = getConfiguredNetworkID(SSID);
        if (networkId != -1) {
            enableNetwork(networkId);
        } else {
            return false;
        }
        return true;
    }

    public int getConfiguredNetworkID(String SSID) {
        int networkId = -1;
        if (TextUtils.isEmpty(SSID)) {
            return networkId;
        }
        List<WifiConfiguration> dataList = wifiManager.getConfiguredNetworks();
        if (dataList != null) {
            for (WifiConfiguration configuration : dataList) {
                if (SSID.equals(configuration.SSID) || ("\"" + SSID + "\"").equals(configuration.SSID)) {
                    return configuration.networkId;
                }
            }
        }
        return networkId;
    }

    private boolean enableNetwork(int networkId) {
        lastConnectNetworkId = networkId;
        //DataEventBus.getEventBus().post(new WifiConnectStatusChangeEntity(NetworkInfo.State.CONNECTING));
        return wifiManager.enableNetwork(networkId, true);
    }

    public int getLastNetworkId() {
        return lastConnectNetworkId;
    }

    public void setLastConnectNetworkId(int lastConnectNetworkId) {
        this.lastConnectNetworkId = lastConnectNetworkId;
    }

    public ScanResult getScanResultBySSID(String SSID) {
        if (TextUtils.isEmpty(SSID)) {
            return null;
        }
        if (SSID.startsWith("\"") && SSID.endsWith("\"")) {
            SSID = SSID.replaceFirst("\"", "");
            SSID = SSID.replaceFirst("\"", "");
        }
        return scanResultMap.get(SSID);
    }

    public boolean isWifiConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) HerSettingsApplication.getApplication().getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return mWifi != null && mWifi.isConnected();
    }
}

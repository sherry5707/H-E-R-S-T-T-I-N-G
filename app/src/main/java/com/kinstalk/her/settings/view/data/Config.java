package com.kinstalk.her.settings.view.data;

import android.text.TextUtils;
import android.util.Log;

import com.kinstalk.her.settings.HerSettingsApplication;

public class Config {
    private static final String TAG = "Config";
    public static String sn;
    public static int qLoveProductVersionNum = -1;

    public static String getMacForSn() {
        String serialNum = getQloveSN();
        boolean isSnGot = false;
        if (!TextUtils.isEmpty(serialNum)) {
            Log.i(TAG, "getSn: serialNum = " + serialNum);
            if (serialNum.length() == 18) {
                sn = serialNum.substring(2, 18);
                isSnGot = true;
                Log.i(TAG, "getSn: sn = " + sn);
            } else {
                sn = "1234567890";
                Log.e(TAG, "getSn: wrong serial number");
            }
        } else {
            Log.e(TAG, "getSn: empty serial number ");
            String macAddr = SystemTool.getLocalMacAddress();
            if (!TextUtils.isEmpty(macAddr)) {
                Log.i(TAG, "getSn: mac = " + macAddr);
                if (macAddr.length() == 17) {
                    sn = macAddr.substring(0, 2) + macAddr.substring(3, 17);
                    isSnGot = true;
                    Log.i(TAG, "getSn: mac sn = " + sn);
                } else {
                    sn = "1234567890";
                    Log.e(TAG, "getSn: wrong mac Addr");
                }
            } else {
                sn = "1234567890";
                Log.e(TAG, "getSn: macAddr is empty");
            }
        }
        return sn;
    }

    private static String getQloveSN() {
        String qlovesn = SystemPropertiesProxy.getString(HerSettingsApplication.getApplication(),
                "ro.serialno");
        String qloveSn = TextUtils.isEmpty(qlovesn) ? "" : qlovesn;

        return qloveSn;
    }
}

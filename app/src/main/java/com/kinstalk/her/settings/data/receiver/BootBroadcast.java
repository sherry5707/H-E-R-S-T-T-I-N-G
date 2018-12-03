package com.kinstalk.her.settings.data.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemProperties;
import android.provider.Settings;

import com.kinstalk.her.settings.data.wifi.WifiHelper;
import com.kinstalk.her.settings.service.BluetoothService;
import com.kinstalk.her.settings.util.DebugUtils;
import ly.count.android.sdk.Countly;

public class BootBroadcast extends BroadcastReceiver {
    private static String TAG = "ScheduleApp";

    @Override
    public void onReceive(Context context, Intent mintent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(mintent.getAction())) {
            //开启Wifi扫描，如附近有已连接过自动连接
            WifiHelper.getInstance().startScan();

            DebugUtils.LogD("START BluetoothService");
            Countly.sharedInstance().recordEvent("HerSettings2", "power_on");
            Intent intent = new Intent(context, BluetoothService.class);
            context.startService(intent);
            SystemProperties.set("persist.sys.wifi.status", "0");
            Settings.System.putInt(context.getContentResolver(), "persist.iot.status", 0);
        }
    }

}  

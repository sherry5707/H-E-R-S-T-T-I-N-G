package com.kinstalk.her.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.multidex.MultiDexApplication;

import com.facebook.stetho.Stetho;
import com.kinstalk.her.httpsdk.HttpManager;

import kinstalk.com.countly.CountlyUtils;
import ly.count.android.sdk.Countly;

import com.kinstalk.her.settings.data.bluetooth.LocalBluetoothManager;
import com.kinstalk.her.settings.util.DebugUtils;

/**
 * Created by mamingzhang on 2017/4/21.
 */

public class HerSettingsApplication extends MultiDexApplication {

    private static HerSettingsApplication application;

    private HttpManager httpManager;

    @Override
    public void onCreate() {
        super.onCreate();

        application = this;

        Stetho.initializeWithDefaults(this);

        initStateReporter();
        CountlyUtils.initCountly(this,DebugUtils.bDebug, BuildConfig.IS_RELEASE);

        //start bluetooth manager to set device name
        LocalBluetoothManager.getInstance(this);
    }

    public static HerSettingsApplication getApplication() {
        return application;
    }

    private void initStateReporter(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        registerReceiver(mReceiver, filter);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String acyion = intent.getAction();
                //Log.d("HerSetting", acyion);
                switch (acyion) {
                    case Intent.ACTION_POWER_CONNECTED://接通电源
                        Countly.sharedInstance().recordEvent("power_connected",1);
                        Countly.sharedInstance().startEvent("Time_power_charge");
                        break;
                    case Intent.ACTION_POWER_DISCONNECTED://拔出电源
                        Countly.sharedInstance().recordEvent("power_disconnected",1);
                        Countly.sharedInstance().endEvent("Time_power_charge");
                        break;
                }
            }
        }

    };
}

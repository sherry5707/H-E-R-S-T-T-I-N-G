package com.kinstalk.her.settings.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.kinstalk.her.settings.R;

/**
 * Created by Zhigang Zhang on 2018/1/17.
 */

public class CaptivePortalLoginService extends Service {
    private static final String TAG = "CaptivePortalService";

    private static final String LOGIN_TRIGGER_INTENT = "com.kinstalk.her.settings.trigger_login";

    private static final int MSG_NETWORK_LOST = 0;
    private static final int MSG_NETWORK_VALIDATED = 1;
    private static final int MSG_TRIGGER_LOGIN = 3;

    private static final int NOTIFICATION_ID = 12;

    private Context mContext;
    private String mSsid;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        Log.d(TAG, "Creating CaptivePortalLoginService");

        isWiFiConnected();
        Log.d(TAG, "wifi ssid is:" + mSsid);

        postNotification();
        registerReceiver();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying CaptivePortalLoginService");
        cancelNotification();
        unregisterReceiver();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(ConnectivityManager.INET_CONDITION_ACTION);
        filter.addAction(LOGIN_TRIGGER_INTENT);
        mContext.registerReceiver(mWifiStateReceiver, filter);
    }

    private void unregisterReceiver() {
        mContext.unregisterReceiver(mWifiStateReceiver);
    }

    private BroadcastReceiver mWifiStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "intent action:" + intent.getAction());
            if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                if(!isWiFiConnected()) {
                    mHandler.sendEmptyMessageDelayed(MSG_NETWORK_LOST, 200);
                }
            } else if(intent.getAction().equals(ConnectivityManager.INET_CONDITION_ACTION)) {
                int connectionStatus = intent.getIntExtra(ConnectivityManager.EXTRA_INET_CONDITION, 0);
                NetworkInfo info = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
                if((info == null) || (info.getType() != ConnectivityManager.TYPE_WIFI)) {
                    return;
                }
                String ssid = removeDoubleQuotes(info.getExtraInfo());
                if(TextUtils.isEmpty(mSsid) || TextUtils.isEmpty(ssid) || !ssid.equals(mSsid)) {
                    return;
                }
                if(connectionStatus == 100) {
                    mHandler.sendEmptyMessageDelayed(MSG_NETWORK_VALIDATED, 200);
                }
            } else if(intent.getAction().equals(LOGIN_TRIGGER_INTENT)) {
                mHandler.sendEmptyMessage(MSG_TRIGGER_LOGIN);
            }
        }
    };

    private boolean isWiFiConnected() {
        WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if((wifiInfo != null) && (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED)) {
            mSsid = removeDoubleQuotes(wifiInfo.getSSID());
            return true;
        }
        mSsid = null;
        return false;
    }

    private String removeDoubleQuotes(String string) {
        if (string == null) return null;
        final int length = string.length();
        if ((length > 1) && (string.charAt(0) == '"') && (string.charAt(length - 1) == '"')) {
            return string.substring(1, length - 1);
        }
        return string;
    }

    private void postNotification() {
        Resources r = mContext.getResources();
        CharSequence title = r.getText(R.string.wifi_captive_login_notification_title) + mSsid;
        CharSequence message = r.getText(R.string.wifi_captive_login_notification_content);

        Bitmap largeIcon = BitmapFactory.decodeResource(r, R.drawable.wifi_not_validated);

        Notification.Builder notification = new Notification.Builder(mContext)
                .setContentTitle(title)
                .setContentText(message)
                .setLargeIcon(largeIcon)
                .setSmallIcon(R.drawable.wifi_not_validated)
                .setOngoing(true)
                .setAutoCancel(false);

        Intent intent = new Intent(LOGIN_TRIGGER_INTENT);
        PendingIntent pi = PendingIntent.getBroadcast(mContext, 0,
                intent, 0);

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

    private void triggerLogin() {
        ConnectivityManager mManager = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        mManager.reportBadNetwork(null);
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_NETWORK_LOST:
                    Log.d(TAG, "MSG_NETWORK_LOST");
                    stopSelf();
                    break;
                case MSG_NETWORK_VALIDATED:
                    Log.d(TAG, "MSG_NETWORK_VALIDATED");
                    stopSelf();
                    break;
                case MSG_TRIGGER_LOGIN:
                    triggerLogin();
                    break;
            }
        }
    };
}

package com.kinstalk.her.settings.view.fragment;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothA2dpSink;
import android.bluetooth.BluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.kinstalk.her.settings.R;
import com.kinstalk.her.settings.data.bluetooth.CachedBluetoothDevice;
import com.kinstalk.her.settings.data.bluetooth.LocalBluetoothManager;
import com.kinstalk.her.settings.data.bluetooth.LocalBluetoothProfileManager;
import com.kinstalk.her.settings.util.Constants;
import com.kinstalk.her.settings.util.DebugUtils;
import com.kinstalk.her.settings.util.ToastHelper;
import com.kinstalk.her.settings.view.activity.HerSettingsActivity;
import com.kinstalk.her.settings.view.activity.PersonalCenterActivity;
import com.kinstalk.her.settings.view.views.HerAlertDialog;
import com.kinstalk.her.settings.view.views.HerProgressDialog;
import com.kinstalk.her.settings.view.views.SettingPrefrence;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;

import kinstalk.com.qloveaicore.IAICoreInterface;


/**
 * Created by Zhigang Zhang on 2017/10/17.
 */

public class HerSettingTitleFragment extends PreferenceFragmentCompat implements OnPreferenceClickListener{
    private static final String TAG = "HerSettingTitleFragment";

    private static final String SETTING_BLUETOOTH_KEY = "settings_bluetooth";
    private static final String SETTING_NETWORK_KEY = "setting_network";
//    private static final String SETTING_SETTING_KEY = "setting_setting";
    private static final String SETTING_AI_KEY = "setting_ai";
    private static final String SETTING_ABOUT_KEY = "setting_about";
    private static final String SETTING_UNBIND_KEY = "setting_unbind";
    private static final String SETTING_CHANGE_WX_KEY = "setting_change_wx_account";
    private static final String SETTING_PERSONAL_CENTER_KEY = "setting_personal_center";
    private static final String SETTING_GET_QRCODE_ACTION = "com.kinstalk.her.qchat.switch.wx";

    private final static int EVENT_START_UNBIND = 3;
    private final static int EVENT_UNBIND_ACCOUNT = 4;
    private final static int EVENT_UNBIND_TIMEOUT = 5;
    private final static int EVENT_UNBIND_DONE = 6;

    private static final int UNBIND_TIME_OUT = 10 * 1000; //10 seconds

    private Preference mBluetoothPref;
    private Preference mNetworkPref;
//    private Preference mSettingPref;
//    private Preference mAiPref;
    private Preference mAboutPref;
    private Preference mUnbindPref;
    private Preference mChangeWxPref;
    private Preference mPersonalCenter;

    private IAICoreInterface mAiService;
    private HerAlertDialog mAlertDialog;
    private HerProgressDialog herProgressDialog;

    private HerSettingsActivity mActivity;
    private Context mContext;
    private static HerSettingTitleFragment mTitleFragment;

    public static HerSettingTitleFragment getInstance(HerSettingsActivity activity) {
        mTitleFragment = new HerSettingTitleFragment();
        return mTitleFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        ViewGroup view = (ViewGroup) super.onCreateView(inflater, container, savedInstanceState);
        RecyclerView recyclerView = (RecyclerView)view.findViewById(R.id.list);
        recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        recyclerView.setPadding(0, 0, 0, 0);

        setDividerHeight(0);
        return view;
    }
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.setting_title);

        mBluetoothPref = getPreferenceScreen().findPreference(SETTING_BLUETOOTH_KEY);
        mBluetoothPref.setOnPreferenceClickListener(this);
        mNetworkPref = getPreferenceScreen().findPreference(SETTING_NETWORK_KEY);
        mNetworkPref.setOnPreferenceClickListener(this);
        mAboutPref = getPreferenceScreen().findPreference(SETTING_ABOUT_KEY);
        mAboutPref.setOnPreferenceClickListener(this);
//        mAiPref = getPreferenceScreen().findPreference(SETTING_AI_KEY);
//        mAiPref.setOnPreferenceClickListener(this);
//        mSettingPref = getPreferenceScreen().findPreference(SETTING_SETTING_KEY);
//        mSettingPref.setOnPreferenceClickListener(this);
        mUnbindPref = getPreferenceScreen().findPreference(SETTING_UNBIND_KEY);
        mUnbindPref.setOnPreferenceClickListener(this);
        mChangeWxPref = getPreferenceScreen().findPreference(SETTING_CHANGE_WX_KEY);
        mChangeWxPref.setOnPreferenceClickListener(this);
        mPersonalCenter = getPreferenceScreen().findPreference(SETTING_PERSONAL_CENTER_KEY);
        mPersonalCenter.setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        boolean handled = true;

        if(preference == mBluetoothPref) {
            mActivity.gotoFragment(SETTING_BLUETOOTH_KEY);
        } else if(preference == mNetworkPref) {
            mActivity.gotoFragment(SETTING_NETWORK_KEY);
        }
//        else if(preference == mSettingPref) {
//            mActivity.gotoFragment(SETTING_SETTING_KEY);
//        }
        else if(preference == mUnbindPref) {
            doUnbind();
        }
/*        else if(preference == mAiPref) {
            mActivity.gotoFragment(SETTING_AI_KEY);
        }*/ else if(preference == mAboutPref) {
            mActivity.gotoFragment(SETTING_ABOUT_KEY);
        } else if (preference == mChangeWxPref) {
            if (isNetworkAvailable(getContext())) {
                Intent intent = new Intent();
                intent.setAction(SETTING_GET_QRCODE_ACTION);
                getActivity().sendStickyBroadcast(intent);
            } else {
                Toast.makeText(getContext(), R.string.switch_wx_account_tip, Toast.LENGTH_SHORT).show();
            }
        } else if (preference == mPersonalCenter){
            Intent intent = new Intent();
            intent.setClass(getActivity(),PersonalCenterActivity.class);
            startActivity(intent);
        } else {
            handled = false;
        }
        return handled;
    }

    @Override
    public void onResume() {
        super.onResume();
        registerBluetoothListener();
        updateInfo();
        if (!getXiaoWeiBindStatus(mContext)) {
            getPreferenceScreen().removePreference(mUnbindPref);
        } else {
            getPreferenceScreen().addPreference(mUnbindPref);
        }
        registerReceiver();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterBluetoothListener();
        unregisterReceiver();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.mActivity = (HerSettingsActivity)getActivity();
        mContext = context;
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mActivity.registerReceiver(mWifiStateReceiver, filter);

        IntentFilter btFilter = new IntentFilter();
        btFilter.addAction(BluetoothA2dpSink.ACTION_CONNECTION_STATE_CHANGED);
        btFilter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        btFilter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        mActivity.registerReceiver(mBluetoothReceiver, btFilter);
    }

    private void unregisterReceiver() {
        mActivity.unregisterReceiver(mWifiStateReceiver);
        mActivity.unregisterReceiver(mBluetoothReceiver);
    }

    private void updateInfo() {
        updateWiFiInfo();
        updateBluetoothInfo();
    }

    private void updateWiFiInfo() {
        String ssid;
        WifiManager wifiManager = (WifiManager) mActivity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if((wifiInfo != null) && (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED)) {
            DebugUtils.LogD("the ssid is " + wifiInfo.getSSID());
            ssid = removeDoubleQuotes(wifiInfo.getSSID());
        } else {
            ssid = null;
        }
        if(mNetworkPref != null) {
            ((SettingPrefrence)mNetworkPref).updateInfo(ssid);
        }
    }
    private String removeDoubleQuotes(String string) {
        if (string == null) return null;
        final int length = string.length();
        if ((length > 1) && (string.charAt(0) == '"') && (string.charAt(length - 1) == '"')) {
            return string.substring(1, length - 1);
        }
        return string;
    }

    private void registerBluetoothListener() {
        LocalBluetoothManager.getInstance(mContext).getProfileManager().addServiceListener(mLocalProfileListener);
    }

    private void unregisterBluetoothListener() {
        LocalBluetoothManager.getInstance(mContext).getProfileManager().removeServiceListener(mLocalProfileListener);
    }

    private void updateBluetoothInfo() {
        String name = null;
        Collection<CachedBluetoothDevice> cachedDevices =
                LocalBluetoothManager.getInstance(mContext).getCachedDeviceManager().getCachedDevicesCopy();
        for(CachedBluetoothDevice device : cachedDevices) {
            if(device.isConnected()) {
                name = device.getName();
            }
        }

        if(mBluetoothPref != null) {
            ((SettingPrefrence)mBluetoothPref).updateInfo(name);
        }
    }

    private BroadcastReceiver mWifiStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                /* Get current Wifi connection information. */
                updateWiFiInfo();
            }
        }
    };

    private BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateBluetoothInfo();
        }
    };

    private LocalBluetoothProfileManager.ServiceListener mLocalProfileListener = new LocalBluetoothProfileManager.ServiceListener() {

        @Override
        public void onServiceConnected() {
            DebugUtils.LogD("onServiceConnected");
            updateBluetoothInfo();
        }

        @Override
        public void onServiceDisconnected() {
            updateBluetoothInfo();
        }
    };

    //methods for unbind xiaowei account
    private void doUnbind() {
        unbind_confirm();
    }

    private void startUnbind() {
        if(!isNetworkOkay()) {
            ToastHelper.makeText(mContext, R.string.unbind_no_network, Toast.LENGTH_SHORT).show();
        } else if(mAiService == null){
            bindAiService();
        } else {
            mHandler.sendEmptyMessage(EVENT_UNBIND_ACCOUNT);
        }
    }

    private void unbind_confirm(){
        Context ct = HerSettingTitleFragment.this.getActivity();
        String confirmTitle = ct.getResources().getString(R.string.unbind_confirm_title);
        String conrirmContent = ct.getResources().getString(R.string.unbind_confirm_content);
        mAlertDialog = new HerAlertDialog(ct);
        mAlertDialog.setTitle(confirmTitle);
        mAlertDialog.setMonirContext(conrirmContent);
        mAlertDialog.setPositiveButton(ct.getResources().getString(R.string.positive_text),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mHandler.sendEmptyMessage(EVENT_START_UNBIND);
                    }
                });
        mAlertDialog.setNegativeButton(ct.getResources().getString(R.string.negative_text),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //do nothing
                    }
                });
        mAlertDialog.show();
    }

    private boolean isNetworkOkay() {
        WifiManager wifiManager = (WifiManager) mActivity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if(wifiManager == null) {
            return false;
        }
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        return ((wifiInfo != null) && (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED));
    }

    private ServiceConnection mConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DebugUtils.LogD("onServiceConnected");
            mAiService = IAICoreInterface.Stub.asInterface(service);

            mHandler.sendEmptyMessage(EVENT_UNBIND_ACCOUNT);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            DebugUtils.LogD("onServiceDisconnected");
            mAiService = null;
        }
    };

    private boolean bindAiService() {
        DebugUtils.LogD("start to bind AI service");
        Intent intent = new Intent();
        intent.setClassName(Constants.REMOTE_AI_SERVICE, Constants.REMOTE_AI_SERVICE_CLASS);
        return mContext.bindService(intent, mConn, Context.BIND_AUTO_CREATE);
    }

    private void unbindAiService() {
        mContext.unbindService(mConn);
        mAiService = null;
    }

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_START_UNBIND:
                    startUnbind();
                    break;
                case EVENT_UNBIND_ACCOUNT:
                    sendUnbindRequest();
                    break;
                case EVENT_UNBIND_TIMEOUT:
                    showUnbindFail();
                case EVENT_UNBIND_DONE:
                    unbindDone();
                    break;
            }
        }

    };

    private void sendUnbindRequest() {
        registerBindStatusReceiver();

        boolean success = true;
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(Constants.GET_DATA_CMD_STR, Constants.GET_DATA_CMD_ERASE_ALL_BINDERS);
        } catch (JSONException e) {
            e.printStackTrace();
            success = false;
        }
        if(success && (mAiService != null)) {
            try {
                mAiService.getData(jsonObject.toString(), null);
            } catch (RemoteException e) {
                e.printStackTrace();
                success = false;
            }
        } else {
            success = false;
        }

        if(success) {
            herProgressDialog = HerProgressDialog.show(mContext, mContext.getResources().getString(R.string.unbind_processing));
            mHandler.sendEmptyMessageDelayed(EVENT_UNBIND_TIMEOUT, UNBIND_TIME_OUT);
        } else {
            showUnbindFail();
            unregisterBindStatusReceiver();
        }
    }

    private void showUnbindFail() {
        ToastHelper.makeText(mContext, R.string.unbind_bad_network, Toast.LENGTH_SHORT).show();
    }

    private BroadcastReceiver mBindStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (Constants.KINSTALK_TXSDK_BIND_STATUS.equals(action)) {
                boolean bindStatus = intent.getBooleanExtra(Constants.EXTRA_BIND_STATUS, false);
                DebugUtils.LogD("mBindStatusReceiver bind status : " + bindStatus);

                if(!bindStatus) {
                    mHandler.removeMessages(EVENT_UNBIND_TIMEOUT);
                    mHandler.sendEmptyMessageDelayed(EVENT_UNBIND_DONE, 1000);
                }
            }
        }
    };

    private void registerBindStatusReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.KINSTALK_TXSDK_BIND_STATUS);
        mContext.registerReceiver(mBindStatusReceiver, intentFilter);
    }

    private void unregisterBindStatusReceiver() {
        mContext.unregisterReceiver(mBindStatusReceiver);
    }

    public static boolean getXiaoWeiBindStatus(Context context) {
        int status = Settings.Secure.getInt(context.getContentResolver(),"xiaowei_bind_status",0);
        if (status == 0) {
            return false;
        } else {
            return true;
        }
    }

    private void unbindDone() {
        if(herProgressDialog != null) {
            herProgressDialog.dismiss();
        }
        playTTSWithContent(getString(R.string.unbind_tts_content));
        mHandler.removeMessages(EVENT_UNBIND_TIMEOUT);
        unregisterBindStatusReceiver();
        unbindAiService();
        if (!getXiaoWeiBindStatus(mContext)) {
            getPreferenceScreen().removePreference(mUnbindPref);
        }
    }

    public void playTTSWithContent(String content) {
        DebugUtils.LogD("unbind xiaowei playTTSWithContent");
        final String ACTION_TXSDK_PLAY_TTS = "kingstalk.action.wateranimal.playtts";
        Intent intent = new Intent(ACTION_TXSDK_PLAY_TTS);
        Bundle bundle = new Bundle();
        bundle.putString("text", content);
        intent.putExtras(bundle);

        getContext().getApplicationContext().sendBroadcast(intent);
    }

    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            if (cm.getActiveNetworkInfo() != null) {
                return cm.getActiveNetworkInfo().isAvailable();
            }
        }
        return false;
    }
}

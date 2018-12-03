package com.kinstalk.her.settings.view.activity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.kinstalk.her.settings.R;
import com.kinstalk.her.settings.data.OwnerInfo;
import com.kinstalk.her.settings.util.Constants;
import com.kinstalk.her.settings.util.DebugUtils;
import com.kinstalk.her.settings.view.fragment.HerAIHelperFragment;
import com.kinstalk.her.settings.view.fragment.HerAboutFragment;
import com.kinstalk.her.settings.view.fragment.HerBluetoothFragment;
import com.kinstalk.her.settings.view.fragment.HerSettingTitleFragment;
import com.kinstalk.her.settings.view.fragment.HerWifiFragment;

import com.tencent.xiaowei.info.QLoveResponseInfo;
import org.json.JSONException;
import org.json.JSONObject;

import kinstalk.com.qloveaicore.IAICoreInterface;
import kinstalk.com.qloveaicore.ICmdCallback;
import ly.count.android.sdk.Countly;

public class HerSettingsActivity extends FragmentActivity implements View.OnClickListener {

    private static final int EVENT_BIND_SERVICE = 0;
    private static final int EVENT_CLEAR_OWNER_INFO = 1;
    private static final int EVENT_QUERY_OWNER_INFO = 2;
    private static final int EVENT_UPDATE_OWNER_INFO = 3;

    private static final int FRAGMENT_TITLE_ID = 0;
    private static final int FRAGMENT_BLUETOOTH_ID = 1;
    private static final int FRAGMENT_NETWORK_ID = 2;
    //    private static final int FRAGMENT_SETTING_ID = 3;
    private static final int FRAGMENT_AI_ID = 3;
    private static final int FRAGMENT_ABOUT_ID = 4;

    private static final String SETTING_BLUETOOTH_KEY = "settings_bluetooth";
    private static final String SETTING_NETWORK_KEY = "setting_network";
    //    private static final String SETTING_SETTING_KEY = "setting_setting";
    private static final String SETTING_AI_KEY = "setting_ai";
    private static final String SETTING_ABOUT_KEY = "setting_about";

    private IAICoreInterface mService;
    private Context mContext;
    private OwnerInfo mOwnerInfo;
    private final Handler mHandler = new SettingHandler();
    private int mCurrentFragment;
    private ImageView mBackView;
    private TextView mTryToSay;
    private int mFragmentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav
                        // bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);

        setContentView(R.layout.activity_her_settings);

        mTryToSay = (TextView) findViewById(R.id.try2say);

        mBackView = (ImageView) findViewById(R.id.back_button);
        mBackView.setOnClickListener(this);
        mFragmentId = getIntent().getIntExtra("from_network_fragment_id", FRAGMENT_TITLE_ID);
        gotoFragment(mFragmentId);

        bindAiService();

        registerBindStatusReceiver();
    }

    @Override
    public void onResume() {
        super.onResume();
        SystemProperties.set("sys.settings.foreground", "true");
        if (mCurrentFragment == FRAGMENT_NETWORK_ID && mFragmentId == FRAGMENT_NETWORK_ID) {
            return;
        }
        mCurrentFragment = getIntent().getIntExtra("from_network_fragment_id", FRAGMENT_TITLE_ID);
        if (mCurrentFragment == FRAGMENT_NETWORK_ID) {
            gotoFragment(mCurrentFragment);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        SystemProperties.set("sys.settings.foreground", "false");
    }

    @Override
    public void onStart() {
        super.onStart();
        Countly.sharedInstance().onStart(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        Countly.sharedInstance().onStop();
        //finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mService != null) {
            mContext.unbindService(mConn);
        }

        unregisterBindStatusReceiver();
    }

    private void registerBindStatusReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.KINSTALK_TXSDK_BIND_STATUS);
        mContext.registerReceiver(mReceiver, intentFilter);
    }

    private void unregisterBindStatusReceiver() {
        mContext.unregisterReceiver(mReceiver);
    }

    public void gotoFragment(String fragmentKey) {
        int fragmentId = -1;
        switch (fragmentKey) {
            case SETTING_BLUETOOTH_KEY:
                fragmentId = FRAGMENT_BLUETOOTH_ID;
                break;
            case SETTING_NETWORK_KEY:
                fragmentId = FRAGMENT_NETWORK_ID;
                break;
//            case SETTING_SETTING_KEY:
//                fragmentId = FRAGMENT_SETTING_ID;
//                break;
            case SETTING_AI_KEY:
                fragmentId = FRAGMENT_AI_ID;
                break;
            case SETTING_ABOUT_KEY:
                fragmentId = FRAGMENT_ABOUT_ID;
                break;
        }

        if (fragmentId >= 0) {
            gotoFragment(fragmentId);
        }
    }

    private void gotoFragment(int fragmentId) {
        Fragment fragment = null;

        switch (fragmentId) {
            case FRAGMENT_TITLE_ID:
                fragment = HerSettingTitleFragment.getInstance(this);
                break;
            case FRAGMENT_BLUETOOTH_ID:
                fragment = HerBluetoothFragment.getInstance();
                break;
            case FRAGMENT_NETWORK_ID:
                fragment = HerWifiFragment.getInstance();
                break;
//            case FRAGMENT_SETTING_ID:
//                fragment = HerSetttingFragment.getInstance();
//                break;
            case FRAGMENT_AI_ID:
                fragment = HerAIHelperFragment.getInstance();
                break;
            case FRAGMENT_ABOUT_ID:
                fragment = HerAboutFragment.getInstance();
                break;
        }
        DebugUtils.LogV("gotoFragment: "+fragment);
        if (fragment != null) {
            mCurrentFragment = fragmentId;
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            ft.setCustomAnimations(R.anim.settings_fragment_rightin,
                    R.anim.settings_fragment_leftout,
                    R.anim.settings_fragment_leftin,
                    R.anim.settings_fragment_rightout)
                    .replace(R.id.content, fragment);
            if (mCurrentFragment != FRAGMENT_TITLE_ID) {
                ft.addToBackStack(null);
            }
            ft.commitAllowingStateLoss();

        }

//        if(fragmentId == FRAGMENT_TITLE_ID) {
//            mBackView.setImageResource(R.drawable.home_btn);
//        } else {
        mBackView.setImageResource(R.drawable.setting_back);
//        }

        if (fragmentId == FRAGMENT_AI_ID) {
            mTryToSay.setVisibility(View.INVISIBLE);
        } else {
            mTryToSay.setVisibility(View.INVISIBLE);
        }
    }

    private boolean bindAiService() {
        DebugUtils.LogD("start to bind AI service");
        Intent intent = new Intent();
        intent.setClassName(Constants.REMOTE_AI_SERVICE, Constants.REMOTE_AI_SERVICE_CLASS);
        return mContext.bindService(intent, mConn, Context.BIND_AUTO_CREATE);
    }

    private ServiceConnection mConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DebugUtils.LogD("onServiceConnected");
            mService = IAICoreInterface.Stub.asInterface(service);

            mHandler.sendEmptyMessage(EVENT_QUERY_OWNER_INFO);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            DebugUtils.LogD("onServiceDisconnected");
            mService = null;
        }
    };

    @Override
    public void onClick(View view) {
/*        if(mCurrentFragment == FRAGMENT_TITLE_ID) {
            finish();
//            Intent i = new Intent(Intent.ACTION_MAIN);
//            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            i.addCategory(Intent.CATEGORY_HOME);
//            startActivity(i);
        } else {
            gotoFragment(FRAGMENT_TITLE_ID);
        }*/
        if (mFragmentId == FRAGMENT_NETWORK_ID) {
            finish();
        }
        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
            this.finish();
        } else {
            getSupportFragmentManager().popBackStack();
        }
    }

    private class AiCallback extends ICmdCallback.Stub {
        private static final String OWNER_INFO_HEAD_URL = "headUrl";
        private static final String OWNER_INFO_NAME = "remark";
        private static final String OWNER_INFO_SN = "sn";

        @Override
        public String processCmd(String s) throws RemoteException {
            DebugUtils.LogD(s);
            try {
                JSONObject json = new JSONObject(s);
                String headUrl = json.getString(OWNER_INFO_HEAD_URL);
                String name = json.getString(OWNER_INFO_NAME);
                String sn = json.getString(OWNER_INFO_SN);
                if ((name == null) || (name.isEmpty())) {
                    return null;
                } else {
                    mOwnerInfo = new OwnerInfo(headUrl, name, sn);
                    mHandler.sendEmptyMessage(EVENT_UPDATE_OWNER_INFO);
                }
            } catch (JSONException e) {
                DebugUtils.LogD("parse jason fail:" + e);
            }
            return null;
        }

        @Override
        public void handleQLoveResponseInfo(String s, QLoveResponseInfo qLoveResponseInfo, byte[] bytes) throws RemoteException {
            DebugUtils.LogD("handleQLoveResponseInfo() called with: s = [" + s + "], qLoveResponseInfo = [" + qLoveResponseInfo + "], bytes = [" + "]");
        }

        @Override
        public void handleWakeupEvent(int i, String s) throws RemoteException {
            DebugUtils.LogD("handleWakeupEvent() called with: i = [" + i + "], s = [" + s + "]");
        }
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            DebugUtils.LogD("Her Settings Activity receiver : " + action);

            if (Constants.KINSTALK_TXSDK_BIND_STATUS.equals(action)) {
                boolean bindStatus = intent.getBooleanExtra(Constants.EXTRA_BIND_STATUS, false);
                if (bindStatus) {
                    //bind, read owner info again
                    if (mService != null) {
                        mHandler.sendEmptyMessage(EVENT_QUERY_OWNER_INFO);
                    } else {
                        mHandler.sendEmptyMessage(EVENT_BIND_SERVICE);
                    }
                } else {
                    mHandler.sendEmptyMessage(EVENT_CLEAR_OWNER_INFO);
                }
            }
        }
    };

    private void clearOwnerInfo() {
        mOwnerInfo = null;
        mHandler.sendEmptyMessage(EVENT_UPDATE_OWNER_INFO);
    }

    private void queryOwnerInfo() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(Constants.GET_DATA_CMD_STR, Constants.GET_DATA_CMD_GET_OWNER);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        try {
            if (mService != null) {
                mService.getData(jsonObject.toString(), new AiCallback());
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void updateOwnerInfo() {

    }

    private class SettingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_BIND_SERVICE:
                    bindAiService();
                    break;
                case EVENT_CLEAR_OWNER_INFO:
                    clearOwnerInfo();
                    break;
                case EVENT_QUERY_OWNER_INFO:
                    queryOwnerInfo();
                    break;
                case EVENT_UPDATE_OWNER_INFO:
                    updateOwnerInfo();
                    break;
                default:
                    break;
            }
        }
    }
}

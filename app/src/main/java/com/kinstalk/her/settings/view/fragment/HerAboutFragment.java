package com.kinstalk.her.settings.view.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IPowerManager;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.service.persistentdata.PersistentDataBlockManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.kinstalk.her.settings.HerSettingsApplication;
import com.kinstalk.her.settings.R;
import com.kinstalk.her.settings.util.Constants;
import com.kinstalk.her.settings.util.DebugUtils;
import com.kinstalk.her.settings.util.ToastHelper;
import com.kinstalk.her.settings.view.views.HerAlertDialog;
import com.kinstalk.her.settings.view.views.HerProgressDialog;
import com.kinstalk.her.settings.view.views.SettingPrefrence;
import ly.count.android.sdk.Countly;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import kinstalk.com.qloveaicore.IAICoreInterface;
/**
 * Created by mamingzhang on 2017/5/15.
 */

public class HerAboutFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener {

    private static final String DEVICE_SERIAL_KEY = "device_serial";
    private static final String DEVICE_VERSION_KEY = "device_version";
    private static final String DEVICE_UPDATE_KEY = "device_update";
    private static final String DEVICE_CLEAR_KEY = "device_recovery";
    private static final String DEVICE_REPORT_PROBLEM = "report_problem";

    private final static int EVENT_START_RESTORE = 1;
    private final static int EVENT_DO_RESTORE = 2;

    private static final String ACTION_MMITEST_SECRET_CODE = "android.action.mmitest.SECRET_CODE";
    private static final String ACTION_START_AI_INFO = "kinstalk.com.intent.action.START_AICORE_INFO_WINDOW";
    private static final String LOG_TAG = "HerAboutFragment";
    private final static String OS_VERSION = "ro.product.os.version";
    protected Activity mActivity;

    private HerProgressDialog herProgressDialog;
    private HerAlertDialog mAlertDialog;
    private Context mContext;
    long[] mHits = new long[6];
    private static final String QUI_VERSION_PREFIX = "QUIf";

    private Preference mDeviceSnPref;
    private Preference mDeviceVersionPref;
    private Preference mUpdatePref;
    private Preference mClearPref;
    private Preference mReportPref;
    private AlertDialog mTestDialog;

    public static HerAboutFragment getInstance() {
        return new HerAboutFragment();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.mActivity = getActivity();
        this.mContext = context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        ViewGroup view = (ViewGroup) super.onCreateView(inflater, container, savedInstanceState);
        RecyclerView tempview = (RecyclerView)view.findViewById(R.id.list);
        tempview.setOverScrollMode(View.OVER_SCROLL_NEVER);
        tempview.setPadding(0, 0, 0, 0);

        setDividerHeight(0);
        return view;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.setting_about);

        mDeviceSnPref = getPreferenceScreen().findPreference(DEVICE_SERIAL_KEY);
        mDeviceSnPref.setOnPreferenceClickListener(this);
        mDeviceVersionPref = getPreferenceScreen().findPreference(DEVICE_VERSION_KEY);
        mDeviceVersionPref.setOnPreferenceClickListener(this);
        mUpdatePref = getPreferenceScreen().findPreference(DEVICE_UPDATE_KEY);
        mUpdatePref.setOnPreferenceClickListener(this);
        mClearPref = getPreferenceScreen().findPreference(DEVICE_CLEAR_KEY);
        mClearPref.setOnPreferenceClickListener(this);
        mReportPref = getPreferenceScreen().findPreference(DEVICE_REPORT_PROBLEM);
        mReportPref.setOnPreferenceClickListener(this);

        //Remove the links app
        getPreferenceScreen().removePreference(mReportPref);

    }

    @Override
    public void onResume() {
        super.onResume();
        initData();
        Countly.sharedInstance().recordEvent("HerSettings2", "t_setting_all_about");
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        boolean handled = true;

        if(preference == mDeviceVersionPref) {
            startFactoryApp();
        } else if(preference == mUpdatePref) {
            startOtaCheck();
        } else if(preference == mClearPref) {
            startRestoreWithPowerCheck();
        } else if(preference == mReportPref) {
            startReport();
        } else {
            handled = false;
        }
        return handled;
    }

    private void initData() {
        ((SettingPrefrence)mDeviceSnPref).updateInfo(getSerialNumber());
        //versionTv.setText(android.os.Build.DISPLAY);
        String version_num = android.os.SystemProperties.get(OS_VERSION, "2.3.0");
        String result = QUI_VERSION_PREFIX + " " + version_num;
        ((SettingPrefrence)mDeviceVersionPref).updateInfo(result);
    }

    private String getSerialNumber() {
        String serial = null;
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("get", String.class);
            serial = (String) get.invoke(c, "ro.serialno");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return serial;
    }
    private String getBuildVersion() {
        String version = null;
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("get", String.class);
            version = (String) get.invoke(c, "ro.build.description");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return version;
    }

    public void onStop() {
        super.onStop();
        if(mAlertDialog != null) {
            mAlertDialog.dismiss();
            mAlertDialog = null;
        }
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(mAlertDialog != null) {
            mAlertDialog.dismiss();
            mAlertDialog = null;
        }
    }

    private void startRestoreWithPowerCheck(){
        final BroadcastReceiver batteryLevelReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (HerAboutFragment.this.getActivity() != null) {
                    HerAboutFragment.this.getActivity().unregisterReceiver(this);
                }
                int rawlevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                //int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                if(rawlevel >= 20){
                    restore_confirm();
                }else{
                    Context ct = HerAboutFragment.this.getActivity();
                    ToastHelper.makeText(ct, R.string.factoryreset_battery_level_low, Toast.LENGTH_SHORT).show();
                }
            }
        };
        IntentFilter batteryLevelFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        this.getActivity().registerReceiver(batteryLevelReceiver, batteryLevelFilter);

    }

    private void restore_confirm(){
        Context ct = HerAboutFragment.this.getActivity();
        String confirmTitle = ct.getResources().getString(R.string.factoryreset_confirm_title);
        String conrirmContent = ct.getResources().getString(R.string.factoryreset_confimr_content);
        mAlertDialog = new HerAlertDialog(ct);
        mAlertDialog.setTitle(confirmTitle);
        mAlertDialog.setContent(conrirmContent);
        mAlertDialog.setPositiveButton(ct.getResources().getString(R.string.positive_text),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Countly.sharedInstance().recordEvent("HerSettings2", "do_factory_reset");
                        mHandler.sendEmptyMessage(EVENT_START_RESTORE);
                        mAlertDialog = null;
                    }
                });
        mAlertDialog.setNegativeButton(ct.getResources().getString(R.string.negative_text),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //do nothing
                        mAlertDialog = null;
                    }
                });
        mAlertDialog.show();
    }

    private void startRestore() {
        final PersistentDataBlockManager pdbManager = (PersistentDataBlockManager)
                getActivity().getSystemService(Context.PERSISTENT_DATA_BLOCK_SERVICE);
        Context ct = HerAboutFragment.this.getActivity();
        herProgressDialog = HerProgressDialog.show(ct, ct.getResources().getString(R.string.factoryreset_ongoing),
                ct.getResources().getString(R.string.factoryreset_warning));
        disableAutoHome(herProgressDialog.getWindow());
        SystemProperties.set("sys.reset.ongoing", "true");
        if (pdbManager != null && !pdbManager.getOemUnlockEnabled()) {
            flagMasterReset();

            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    pdbManager.wipe();
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    mHandler.sendEmptyMessageDelayed(EVENT_DO_RESTORE, 1000);
                }
            }.execute();
        } else {
            mHandler.sendEmptyMessageDelayed(EVENT_DO_RESTORE, 1000);
        }
    }

    private void flagMasterReset() {
        Context context = HerSettingsApplication.getApplication().getApplicationContext();
        SharedPreferences sharedPref = context.getSharedPreferences(Constants.SHARED_PREF_ABOUT_SETTING, Context.MODE_PRIVATE);
        sharedPref.edit().putBoolean(Constants.SHARED_PREF_KEY_MASTER_CLEAR, true).commit();
    }

    private boolean restore() {
        //Intent intent = new Intent(Intent.ACTION_MASTER_CLEAR);
        Intent intent = new Intent("android.intent.action.MASTER_CLEAR");
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        //intent.putExtra(Intent.EXTRA_REASON, "MasterClearConfirm");
        intent.putExtra("android.intent.extra.REASON", "MasterClearConfirm");
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            DebugUtils.LogD(e.toString());
        }
        getActivity().sendBroadcast(intent);
        return true;
    }

    private void startOtaCheck() {
        Countly.sharedInstance().recordEvent("HerSettings2", "ota_upgrade");
        Intent linksintent = new Intent();
        linksintent.setClassName("com.thunderst.update","com.thunderst.update.common.FotaService");
        //linksintent.setClassName("com.thunderst.update","com.thunderst.update.UI.CurrentActivity");
        //linksintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            //getActivity().startActivity(linksintent);
            getActivity().startService(linksintent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void startFactoryApp() {
        System.arraycopy(mHits, 1, mHits, 0, mHits.length - 1);
        mHits[mHits.length - 1] = SystemClock.uptimeMillis();
        if (mHits[0] >= (SystemClock.uptimeMillis() - 1000)) {
            mHits[1] = 0;
            setCustomDialog();
        }
    }

    private void setCustomDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = View.inflate(getActivity(), R.layout.test_dialog, null);
        setDialogAction(view);
        builder.setView(view);
        builder.setTitle("TestApp")
                .setNegativeButton("Cancel", null).setCancelable(false);
        mTestDialog = builder.create();
        mTestDialog.show();
    }

    private void setDialogAction(View view) {
        TextView factoryTest = (TextView) view.findViewById(R.id.factory_test);
        factoryTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent factory_intent = new Intent();
                factory_intent.setAction("com.tinnotech.FactoryTest");
                factory_intent.addCategory(Intent.CATEGORY_DEFAULT);
                try {
                    mContext.startActivity(factory_intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mTestDialog.dismiss();
            }
        });
        TextView mLinks = (TextView) view.findViewById(R.id.links);
        mLinks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //String linksintentstr = "com.kinstalk.links/.MainActivity";
                Intent linksintent = new Intent();
                linksintent.setClassName("com.kinstalk.links", "com.kinstalk.links.MainActivity");
                try {
                    mContext.startActivity(linksintent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mTestDialog.dismiss();
            }
        });
        TextView mAiInfo = (TextView) view.findViewById(R.id.ai_info);
        mAiInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent aiInfoIntent = new Intent(ACTION_START_AI_INFO);
                mContext.sendBroadcast(aiInfoIntent);
                mTestDialog.dismiss();
            }
        });
        TextView mAiConfiguration = (TextView) view.findViewById(R.id.ai_configuration);
        mAiConfiguration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent aiConfigIntent = new Intent();
                aiConfigIntent.setClassName("kinstalk.com.qloveaicore", "kinstalk.com.qloveaicore.AIConfigActivity");
                try {
                    mContext.startActivity(aiConfigIntent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mTestDialog.dismiss();
            }
        });
        TextView mDevelopmentOpt = (TextView) view.findViewById(R.id.development_opition);
        mDevelopmentOpt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClassName("com.android.settings",
                        "com.android.settings.DevelopmentSettings");
                try {
                    mContext.startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mTestDialog.dismiss();
            }
        });
        RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.choose_service_url);
        RadioButton testUrl = (RadioButton) view.findViewById(R.id.test_url);
        RadioButton normalUrl = (RadioButton) view.findViewById(R.id.normal_url);
        RadioButton otherUrl = (RadioButton) view.findViewById(R.id.other_url);
        String urlChoice = SystemProperties.get("persist.sys.url");
        if ("0".equals(urlChoice)) {
            testUrl.setChecked(true);
        } else if ("1".equals(urlChoice)) {
            normalUrl.setChecked(true);
        } else if ("2".equals(urlChoice)) {
            otherUrl.setChecked(true);
        }
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.test_url:
                        SystemProperties.set("persist.sys.url", "0");
                        break;
                    case R.id.normal_url:
                        SystemProperties.set("persist.sys.url", "1");
                        break;
                    case R.id.other_url:
                        SystemProperties.set("persist.sys.url", "2");
                        break;

                }
                mTestDialog.dismiss();
            }
        });
        TextView mVersion = (TextView) view.findViewById(R.id.version_head);
        final CharSequence[] charSequences;
        if (Build.TYPE.equals("userdebug")) {
            mVersion.setVisibility(View.VISIBLE);
            mVersion.setText(mContext.getResources().getString(R.string.version_head) + getBuildVersion());
        } else {
            mVersion.setVisibility(View.GONE);
        }
    }

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_START_RESTORE:
                    startRestore();
                    break;
                case EVENT_DO_RESTORE:
//                    if(herProgressDialog != null) {
//                        Context ct = HerAboutFragment.this.getActivity();
//                        herProgressDialog.setContentMessage(ct.getResources().getString(R.string.factoryreset_start));
//                    }
                    try {
                        restore();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }

    };

    private void disableAutoHome(Window window) {
        WindowManager.LayoutParams attr = window.getAttributes();
        try {
            Class<WindowManager.LayoutParams> attrClass = WindowManager.LayoutParams.class;
            Method method = attrClass.getMethod("setAutoActivityTimeout", new Class[]{boolean.class});
            method.setAccessible(true);
            Object object = method.invoke(attr, false);
        }catch (Exception e1){
            e1.printStackTrace();
        }
        window.setAttributes(attr);
    }

    private void startReport() {
        Intent intent = new Intent();
        intent.setClassName("com.kinstalk.links","com.kinstalk.links.LinksMainActivity");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            getActivity().startActivity(intent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}

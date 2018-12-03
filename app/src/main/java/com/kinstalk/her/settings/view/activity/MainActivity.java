package com.kinstalk.her.settings.view.activity;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.kinstalk.her.settings.R;
import com.kinstalk.her.settings.util.Constants;

import java.lang.reflect.Method;

import ly.count.android.sdk.Countly;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class MainActivity extends FragmentActivity {

    private Fragment mFragment = null;
    private FrameLayout mMediaFrameLayout = null;
    private DevicePolicyManager policyManager;
    private ComponentName componentName;
    private static final int MY_REQUEST_CODE = 1;
    //手指按下的点为(x1, y1)手指离开屏幕的点为(x2, y2)
    private float x1 = 0;
    private float x2 = 0;
    private float y1 = 0;
    private float y2 = 0;
    private static final String PRIVACY_MODE_ACTION = "kingstalk.action.privacymode";
    private boolean isPrivacyMode = false;
    private BroadcastReceiver privacyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getAction() != null) {
                String action = intent.getAction();
                if (PRIVACY_MODE_ACTION.equals(action)) {
                    isPrivacyMode = intent.getBooleanExtra("enable", false);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav
                        // bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);

        setContentView(R.layout.activity_main);

        ApplicationInfo info = null;
        try{
            info = this.getPackageManager().getApplicationInfo("com.kinstalk.qloveandlinkapp",0);
        }catch (PackageManager.NameNotFoundException e) {
            Log.d("HerSettingsActivity", "No andlink package found!");
        }

        Log.d("HerSettingsActivity","andlink package: " + info);
        if(info == null){
//        if(result == 1) {
            Button andlinkBtn = (Button) findViewById(R.id.andlink_button);
            TextView andlinkTv = (TextView) findViewById(R.id.andlink_view);
            andlinkBtn.setVisibility(View.GONE);
            andlinkTv.setVisibility(View.GONE);
        }

        FragmentManager manager = getSupportFragmentManager();
        mFragment = manager.findFragmentById(R.id.settings_fragment);
        mMediaFrameLayout = (FrameLayout)findViewById(R.id.media_frame_layout);
        registerBroadcastReceiver();
    }

    private void registerBroadcastReceiver(){
        IntentFilter intentFilter = new IntentFilter(PRIVACY_MODE_ACTION);
        this.registerReceiver(privacyReceiver,intentFilter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Countly.sharedInstance().recordEvent("HerSettings2", "t_setting_pulldown");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(privacyReceiver);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //继承了Activity的onTouchEvent方法，直接监听点击事件
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            //当手指按下的时候
            x1 = event.getX();
            y1 = event.getY();
        }
        if(event.getAction() == MotionEvent.ACTION_UP) {
            //当手指离开的时候
            x2 = event.getX();
            y2 = event.getY();
            if(y1 - y2 > 50) {
                //向上滑
                Log.d("HerSettingsActivity","a22417 up");
                finish();
            } else if(y2 - y1 > 50) {
                //向下滑
                Log.d("HerSettingsActivity","a22417 down");
            } else if(x1 - x2 > 50) {
                //向左滑
                Log.d("HerSettingsActivity","a22417 left");
            } else if(x2 - x1 > 50) {
                //向右滑
                Log.d("HerSettingsActivity","a22417 right");
            }
        }
        return super.onTouchEvent(event);
    }

    public void homeButtonClicked(View view) {
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.setFlags(FLAG_ACTIVITY_NEW_TASK);
        i.addCategory(Intent.CATEGORY_HOME);
        startActivity(i);
    }

    public void screenButtonClicked(View view) {
        Countly.sharedInstance().recordEvent("HerSettings2", "screen_off");
        screenOff();
    }

    public void iotButtonClicked(View view) {
        try {
            Intent intent = new Intent();
            String pkg = "com.kinstalk.her.iot";
            String cls = "com.kinstalk.her.iot.activity.MainActivity";
            ComponentName componentName = new ComponentName(pkg, cls);
            intent.setComponent(componentName);
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void andlinkButtonClicked(View v){
        PackageManager packageManager = getPackageManager();
        Intent intent=new Intent();
        intent =packageManager.getLaunchIntentForPackage("com.kinstalk.qloveandlinkapp");
        if(intent==null){
            System.out.println("com.kinstalk.qloveandlinkapp zyb APP not found!");
        }
        startActivity(intent);
    }

    public void privacyButtonClicked(View view) {
        switchPrivacy(!isPrivacyMode);
    }

    protected void switchPrivacy(boolean privacyMode) {
        NotificationManager mNotificationManager = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);
        try {
            Class<NotificationManager> NotificationManagerClass = NotificationManager.class;
            Method method = NotificationManagerClass.getMethod("setPrivacyMode", new Class[]{boolean.class});
            method.setAccessible(true);
            method.invoke(mNotificationManager, privacyMode);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void settingButtonClicked(View view) {
        Intent intent = new Intent();
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        intent.setClass(this, HerSettingsActivity.class);
        startActivity(intent);
        Countly.sharedInstance().recordEvent("HerSettings2", "t_setting_all");
    }

    private void updateMediaInfo(Context context, Intent intent) {
        String update = intent.getStringExtra(Constants.REMOTE_VIEW_OPERATION_KEY);
        String type = intent.getStringExtra(Constants.REMOTE_VIEW_TYPE_KEY);

        if(type != null && type.isEmpty()){
            return;
        }

        if(type.equals("media")) {
            FrameLayout layout = mMediaFrameLayout;
            if (update.equals(Constants.REMOTE_VIEW_OPERATION_ADD)) {
                RemoteViews remoteViews = intent.getParcelableExtra(Constants.REMOTE_VIEW_OBJECT_KEY);
                if (remoteViews != null) {
                    layout.removeAllViews();
                    View view = remoteViews.apply(context, layout);
                    layout.addView(view);
                }
            } else {
                layout.removeAllViews();
            }
        }
    }

    private void screenOff(){
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        pm.goToSleep(SystemClock.uptimeMillis());
     /*  policyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        componentName = new ComponentName(this, AdminReceiver.class);
        boolean admin = policyManager.isAdminActive(componentName);
        if (admin) {
            policyManager.lockNow();
          //  finish();
     } else {
            Toast.makeText(this,"没有设备管理权限",
                    Toast.LENGTH_LONG).show();
            activeManage(); //获取权限
        }*/
    }

    private void activeManage() {
        // 启动设备管理(隐式Intent) - 在AndroidManifest.xml中设定相应过滤器
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        // 权限列表
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
        // 描述(additional explanation) 在申请权限时出现的提示语句
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "激活后就能一键锁屏了");
        startActivityForResult(intent, MY_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // 获取权限成功，立即锁屏并finish自己，否则继续获取权限
        if (requestCode == MY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            policyManager.lockNow();
           // finish();
        } else {
            //activeManage();
          //  finish();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}

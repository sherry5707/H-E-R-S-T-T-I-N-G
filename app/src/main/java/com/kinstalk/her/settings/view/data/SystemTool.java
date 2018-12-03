/*
 * Copyright (c) 2018. Beijing Shuzijiayuan, All Rights Reserved.
 * Beijing Shuzijiayuan Confidential and Proprietary
 */

package com.kinstalk.her.settings.view.data;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;

import com.kinstalk.her.settings.HerSettingsApplication;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by knight.xu on 2018/4/6.
 */

public final class SystemTool {
    private static final String TAG = "SystemTool";

    public static final String AUDIO_HW_TYPE_CNXT = "cnxt"; // 科声讯
    public static final String AUDIO_HW_TYPE_SND_CNCT = "snd_cnct";// 先声
    public static final String AUDIO_HW_TYPE_AUDIENCE = "audience";// 原armstrong/columbus的audience设备
    public static final String AUDIO_HW_TYPE_UNKOWN = "unkown"; //未知

    private static String sWifiMacAddress = "";

    public static String getDataTime(String format) {
        SimpleDateFormat df = new SimpleDateFormat(format);
        return df.format(new Date());
    }

    public static String getDataTime() {
        return getDataTime("HH:mm");
    }

    @SuppressLint("MissingPermission")
    public static String getPhoneIMEI(Context cxt) {
        TelephonyManager tm = (TelephonyManager) cxt.getSystemService(Context.TELEPHONY_SERVICE);
        return tm.getDeviceId();
    }

    public static String getSystemVersion() {
        return Build.VERSION.RELEASE;
    }

    public static void sendSMS(Context cxt, String smsBody) {
        Uri smsToUri = Uri.parse("smsto:");
        Intent intent = new Intent("android.intent.action.SENDTO", smsToUri);
        intent.putExtra("sms_body", smsBody);
        cxt.startActivity(intent);
    }

    /**
     * get App versionName
     * wangzhipeng@shuzijiayuan.com
     *
     * @param context
     * @return
     */
    public static String getVersionName(Context context) {
        PackageManager packageManager = context.getPackageManager();
        PackageInfo packageInfo;
        String versionName = "";
        try {
            packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            versionName = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionName;
    }

    public static boolean checkNet(Context context) {
        if (null == context) return false;
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info == null || !info.isAvailable()) {
            return false;
        }
        return true;
    }

    public static boolean isWiFi(Context cxt) {
        if (cxt == null) return false;
        ConnectivityManager cm = (ConnectivityManager) cxt
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo.State state = cm.getNetworkInfo(1).getState();
        return (NetworkInfo.State.CONNECTED == state);
    }

    public static void hideKeyBoard(Activity aty) {
        ((InputMethodManager) aty.getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(
                        aty.getCurrentFocus().getWindowToken(), 2);
    }


    public static boolean isSleeping(Context context) {
        KeyguardManager kgMgr = (KeyguardManager) context
                .getSystemService(Context.KEYGUARD_SERVICE);
        boolean isSleeping = kgMgr.inKeyguardRestrictedInputMode();
        return isSleeping;
    }

    @SuppressLint("WrongConstant")
    public static void installApk(Context context, File file) {
        Intent intent = new Intent();
        intent.setAction("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setType("application/vnd.android.package-archive");
        intent.setData(Uri.fromFile(file));
        intent.setDataAndType(Uri.fromFile(file),
                "application/vnd.android.package-archive");
        intent.setFlags(268435456);
        context.startActivity(intent);
    }


    @SuppressLint("WrongConstant")
    public static void goHome(Context context) {
        Intent mHomeIntent = new Intent("android.intent.action.MAIN");
        mHomeIntent.addCategory("android.intent.category.HOME");
        mHomeIntent.addFlags(270532608);
        context.startActivity(mHomeIntent);
    }


    private static String hexdigest(byte[] paramArrayOfByte) {
        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'a', 'b', 'c', 'd', 'e', 'f'};
        try {
            MessageDigest localMessageDigest = MessageDigest.getInstance("MD5");
            localMessageDigest.update(paramArrayOfByte);
            byte[] arrayOfByte = localMessageDigest.digest();
            char[] arrayOfChar = new char[32];
            int i = 0;
            int j = 0;
            if (i >= 16)
                return new String(arrayOfChar);
            int k = arrayOfByte[i];
            arrayOfChar[j] = hexDigits[(0xF & k >>> 4)];
            arrayOfChar[(++j)] = hexDigits[(k & 0xF)];

            ++i;
            ++j;
        } catch (Exception localException) {
        }

        return "";
    }

    public static int getDeviceUsableMemory(Context cxt) {
        ActivityManager am = (ActivityManager) cxt.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);

        return (int) (mi.availMem / 1048576L);
    }


    /**
     * Get the device's Universally Unique Identifier (UUID).
     *
     * @return
     */
    public static String getUuid() {
        String uuid = Settings.Secure.getString(HerSettingsApplication.getApplication().getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        return uuid;
    }

    public static String getModel() {
        String model = android.os.Build.MODEL;
        return model;
    }

    public static String getProductName() {
        String productname = android.os.Build.PRODUCT;
        return productname;
    }

    public static String getDisplay() {
        String display = android.os.Build.DISPLAY;
        return display;
    }

    public static String getManufacturer() {
        String manufacturer = android.os.Build.MANUFACTURER;
        return manufacturer;
    }

    public static String getSerialNumber() {
        String serial = android.os.Build.SERIAL;
        return serial;
    }

    /**
     * Get the OS version.
     *
     * @return
     */
    public static String getOSVersion() {
        String osversion = android.os.Build.VERSION.RELEASE;
        return osversion;
    }

    public static int getSDKVersion() {
        @SuppressWarnings("deprecation")
        int sdkversion = android.os.Build.VERSION.SDK_INT;
        return sdkversion;
    }

    public static String getTimeZoneID() {
        TimeZone tz = TimeZone.getDefault();
        return (tz.getID());
    }

    public static boolean isVirtual() {
        return android.os.Build.FINGERPRINT.contains("generic") ||
                android.os.Build.PRODUCT.contains("sdk");
    }

    public static boolean isAudioDumpTest;

    public static boolean isAudioDumpTest() {
        String propKey = "persist.qlove.ai_wakeup_test";
        isAudioDumpTest = SystemPropertiesProxy.getBoolean(HerSettingsApplication.getApplication().getApplicationContext(), propKey, false);
        return  isAudioDumpTest;
    }

    public static WakeLock acquireWakeLock(Context c) {
        PowerManager pm = (PowerManager) c.getSystemService(Context.POWER_SERVICE);
        return pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "AIWindow");
    }

    public static boolean isUserType() {
        return Build.TYPE.equals("user");
    }

    /**
     * 原property 方式设置的时间可能比较晚，为保证正确读取，在mediaserver里加了一个参数来识别设备。
     *
     * @return String
     */
    public static String getAudioHwType(Context context) {

        String audioHw = SystemTool.AUDIO_HW_TYPE_UNKOWN;
        //TODO monitor bug:10167
//		try {
        AudioManager mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        String sndCardNames = mAudioManager.getParameters("SND_CARD_NAME");
        Log.d(TAG, "sndCardNames = " + sndCardNames);

        if (!TextUtils.isEmpty(sndCardNames) && sndCardNames.contains("=")) {
            String[] strs = sndCardNames.split("=");
            if ("SND_CARD_NAME".equalsIgnoreCase(strs[0])) {
                audioHw = strs[1];
            }
        }
//		} catch (Exception e){
//			Log.e(TAG, "getAudioHwType error: " + e.getMessage());
//            e.printStackTrace();
//		}
        Log.d(TAG, "audioHw = " + audioHw);

        return audioHw;
    }


    public static String getWIFILocalIpAdress(Context context) {

        //获取wifi服务
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        //判断wifi是否开启
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        String ip = formatIpAddress(ipAddress);
        return ip;
    }

    public static String formatIpAddress(int ipAdress) {

        return (ipAdress & 0xFF) + "." +
                ((ipAdress >> 8) & 0xFF) + "." +
                ((ipAdress >> 16) & 0xFF) + "." +
                (ipAdress >> 24 & 0xFF);
    }

    /**
     * 获得mac
     *
     * @return
     */
    public static String getLocalMacAddress() {

        if (!TextUtils.isEmpty(sWifiMacAddress)) {
            return sWifiMacAddress;
        }

        String Mac = null;
        try {
            String path = "sys/class/net/wlan0/address";
            if ((new File(path)).exists()) {
                FileInputStream fis = new FileInputStream(path);
                byte[] buffer = new byte[8192];
                int byteCount = fis.read(buffer);
                if (byteCount > 0) {
                    Mac = new String(buffer, 0, byteCount, "utf-8");
                }
                fis.close();
            }

            if (Mac == null || Mac.length() == 0) {
                path = "sys/class/net/eth0/address";
                FileInputStream fis = new FileInputStream(path);
                byte[] buffer_name = new byte[8192];
                int byteCount_name = fis.read(buffer_name);
                if (byteCount_name > 0) {
                    Mac = new String(buffer_name, 0, byteCount_name, "utf-8");
                }
                fis.close();
            }

            if (!TextUtils.isEmpty(Mac)) {
                Mac = Mac.substring(0, Mac.length() - 1);
            }
        } catch (Exception io) {
        }

        if (TextUtils.isEmpty(Mac)) {
            WifiManager wifiManager = (WifiManager) HerSettingsApplication.getApplication().getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo.getMacAddress() != null) {
                Mac = wifiInfo.getMacAddress();
            }
        }

        Log.d(TAG, "wifi Mac = " + Mac);
        sWifiMacAddress = Mac;

        return TextUtils.isEmpty(Mac) ? "" : Mac;
    }

    public static void getQLoveProductVersion(Context context) {
        int versionNum = SystemPropertiesProxy.getInt(context, "ro.boot.hwid", -1);
        Log.d(TAG, "getQProductVersion: ver " + versionNum);
        Config.qLoveProductVersionNum = versionNum;
        // 0 :armstrong/discovery
        // 1 :哥伦布
        // 3 :麦哲伦 M10
        // 5 :麦哲伦 M7
    }


    public static String getProcessName(Context context, int pid) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningApps = am.getRunningAppProcesses();
        if (runningApps != null && !runningApps.isEmpty()) {
            for (ActivityManager.RunningAppProcessInfo procInfo : runningApps) {
                if (procInfo.pid == pid) {
                    return procInfo.processName;
                }
            }
        }
        return null;
    }

    /**
     * 检查当前网络是否可用
     *
     * @return
     */

    public static boolean isNetworkAvailable(Context activity)
    {
        Context context = activity.getApplicationContext();
        // 获取手机所有连接管理对象（包括对wi-fi,net等连接的管理）
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null)
        {
            return false;
        }
        else
        {
            // 获取NetworkInfo对象
            NetworkInfo[] networkInfo = connectivityManager.getAllNetworkInfo();

            if (networkInfo != null && networkInfo.length > 0)
            {
                for (int i = 0; i < networkInfo.length; i++)
                {
                    System.out.println(i + "===状态===" + networkInfo[i].getState());
                    System.out.println(i + "===类型===" + networkInfo[i].getTypeName());
                    // 判断当前网络状态是否为连接状态
                    if (networkInfo[i].getState() == NetworkInfo.State.CONNECTED)
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
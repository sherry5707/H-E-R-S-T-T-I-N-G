package com.kinstalk.her.settings.util;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import java.lang.reflect.Method;

/**
 * Created by mamingzhang on 2017/4/21.
 */

public class DeviceUtils {
    public static String getHerSerialNumber(){
        String serial = null;

        try {
            Class<?> c =Class.forName("android.os.SystemProperties");
            Method get =c.getMethod("get", String.class);
            serial = (String)get.invoke(c, "persist.qlove.sn");
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (TextUtils.isEmpty(serial)) {
            serial = getSerialNumber();
        }

        return serial;
    }

    private static String getSerialNumber(){
        String serial = null;

        try {
            Class<?> c =Class.forName("android.os.SystemProperties");
            Method get =c.getMethod("get", String.class);
            serial = (String)get.invoke(c, "ro.serialno");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return serial;
    }

}

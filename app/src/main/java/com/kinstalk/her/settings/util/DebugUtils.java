package com.kinstalk.her.settings.util;

import android.os.Build;
import android.util.Log;

import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Created by mamingzhang on 2017/5/12.
 */

public class DebugUtils {
    public static final boolean bDebug = true;

    private static final String Tag = "Settings";

    public static void LogV(String msg) {
        if (bDebug) {
            Log.v(Tag, msg);
        }
    }

    public static void LogD(String msg) {
        if (bDebug) {
            Log.d(Tag, msg);
        }
    }

    public static void LogE(String msg) {
        if (bDebug) {
            Log.v(Tag, msg);
        }
    }

    public static class QAIHttpLogger implements HttpLoggingInterceptor.Logger {
        private static final String PREFIX = "QOK-";
        private String mTag;

        public QAIHttpLogger(Object object) {
            this(getPrefixFromObject(object));
        }

        public QAIHttpLogger(String tag) {
            mTag = PREFIX + tag;
        }

        @Override
        public void log(String message) {
            DebugUtils.LogD(message);
        }
    }

    private static String getPrefixFromObject(Object obj) {
        return obj == null ? "<null>" : obj.getClass().getSimpleName();
    }

    public static boolean isUserType() {
        return Build.TYPE.equals("user");
    }
}

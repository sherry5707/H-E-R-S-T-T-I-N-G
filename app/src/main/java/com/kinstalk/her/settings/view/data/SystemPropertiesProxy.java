/*
 * Copyright (c) 2018. Beijing Shuzijiayuan, All Rights Reserved.
 * Beijing Shuzijiayuan Confidential and Proprietary
 */

package com.kinstalk.her.settings.view.data;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;

/**
 * Created by Knight.Xu on 2018/4/6.
 */

public class SystemPropertiesProxy {

    private static final String TAG = "SystemPropertiesProxy";

    /**
     * 根据给定Key获取值.
     *
     * @return 如果不存在该key则返回空字符串
     * @throws IllegalArgumentException 如果key超过32个字符则抛出该异常
     */
    public static String getString(Context context, String key) {

        String ret = "";
        try {
            ClassLoader cl = context.getClassLoader();
            @SuppressWarnings("rawtypes")
            Class SystemProperties = cl.loadClass("android.os.SystemProperties");

            //参数类型
            @SuppressWarnings("rawtypes")
            Class[] paramTypes = new Class[1];
            paramTypes[0] = String.class;

            Method get = SystemProperties.getMethod("get", paramTypes);

            //参数
            Object[] params = new Object[1];
            params[0] = new String(key);

            ret = (String) get.invoke(SystemProperties, params);

            Log.d(TAG, "reflect propKey = " + key + ">>>String  propVal =  " + ret);

        } catch (Exception e) {
            Log.e(TAG, "reflect error = " + e.getMessage());
            ret = getPropByExec(key);
        }
        return ret;
    }

    /**
     * 根据Key获取值.
     *
     * @return 如果key不存在, 并且如果def不为空则返回def否则返回空字符串
     * @throws IllegalArgumentException 如果key超过32个字符则抛出该异常
     */
    public static String getString(Context context, String key, String def) {

        String ret = def;

        try {

            ClassLoader cl = context.getClassLoader();
            @SuppressWarnings("rawtypes")
            Class SystemProperties = cl.loadClass("android.os.SystemProperties");

            //参数类型
            @SuppressWarnings("rawtypes")
            Class[] paramTypes = new Class[2];
            paramTypes[0] = String.class;
            paramTypes[1] = String.class;

            Method get = SystemProperties.getMethod("get", paramTypes);

            //参数
            Object[] params = new Object[2];
            params[0] = new String(key);
            params[1] = new String(def);

            ret = (String) get.invoke(SystemProperties, params);

            Log.d(TAG, "reflect propKey = " + key + ">>>String  propVal =  " + ret);
        } catch (Exception e) {
            Log.e(TAG, "reflect error = " + e.getMessage());
            String exceVal = getPropByExec(key);
            ret = TextUtils.isEmpty(exceVal) ? def : exceVal;
        }
        return ret;

    }

    /**
     * 根据给定的key返回int类型值.
     *
     * @param key 要查询的key
     * @param def 默认返回值
     * @return 返回一个int类型的值, 如果没有发现则返回默认值
     * @throws IllegalArgumentException 如果key超过32个字符则抛出该异常
     */
    public static Integer getInt(Context context, String key, int def) {

        Integer ret = def;

        try {

            ClassLoader cl = context.getClassLoader();
            @SuppressWarnings("rawtypes")
            Class SystemProperties = cl.loadClass("android.os.SystemProperties");

            //参数类型
            @SuppressWarnings("rawtypes")
            Class[] paramTypes = new Class[2];
            paramTypes[0] = String.class;
            paramTypes[1] = int.class;

            Method getInt = SystemProperties.getMethod("getInt", paramTypes);

            //参数
            Object[] params = new Object[2];
            params[0] = new String(key);
            params[1] = new Integer(def);

            ret = (Integer) getInt.invoke(SystemProperties, params);

            Log.d(TAG, "reflect propKey = " + key + ">>>Integer  propVal =  " + ret);
        } catch (Exception e) {
            Log.e(TAG, "reflect error = " + e.getMessage());
            String exceVal = getPropByExec(key);
            if (!TextUtils.isEmpty(exceVal)) {
                ret = Integer.valueOf(exceVal);
            } else {
                ret = def;
            }
        }

        return ret;

    }

    /**
     * 根据给定的key返回long类型值.
     *
     * @param key 要查询的key
     * @param def 默认返回值
     * @return 返回一个long类型的值, 如果没有发现则返回默认值
     * @throws IllegalArgumentException 如果key超过32个字符则抛出该异常
     */
    public static Long getLong(Context context, String key, long def) {

        Long ret = def;

        try {

            ClassLoader cl = context.getClassLoader();
            @SuppressWarnings("rawtypes")
            Class SystemProperties = cl.loadClass("android.os.SystemProperties");

            //参数类型
            @SuppressWarnings("rawtypes")
            Class[] paramTypes = new Class[2];
            paramTypes[0] = String.class;
            paramTypes[1] = long.class;

            Method getLong = SystemProperties.getMethod("getLong", paramTypes);

            //参数
            Object[] params = new Object[2];
            params[0] = new String(key);
            params[1] = new Long(def);

            ret = (Long) getLong.invoke(SystemProperties, params);

            Log.d(TAG, "reflect propKey = " + key + ">>>Long  propVal =  " + ret);
        } catch (Exception e) {
            Log.e(TAG, "reflect error = " + e.getMessage());

            String exceVal = getPropByExec(key);
            if (!TextUtils.isEmpty(exceVal)) {
                ret = Long.valueOf(exceVal);
            } else {
                ret = def;
            }
        }

        return ret;

    }

    /**
     * 根据给定的key返回boolean类型值.
     * 如果值为 'n', 'no', '0', 'false' or 'off' 返回false.
     * 如果值为'y', 'yes', '1', 'true' or 'on' 返回true.
     * 如果key不存在, 或者是其它的值, 则返回默认值.
     *
     * @param key 要查询的key
     * @param def 默认返回值
     * @return 返回一个boolean类型的值, 如果没有发现则返回默认值
     * @throws IllegalArgumentException 如果key超过32个字符则抛出该异常
     */
    public static Boolean getBoolean(Context context, String key, boolean def) {

        Boolean ret = def;

        try {

            ClassLoader cl = context.getClassLoader();
            @SuppressWarnings("rawtypes")
            Class SystemProperties = cl.loadClass("android.os.SystemProperties");

            //参数类型
            @SuppressWarnings("rawtypes")
            Class[] paramTypes = new Class[2];
            paramTypes[0] = String.class;
            paramTypes[1] = boolean.class;

            Method getBoolean = SystemProperties.getMethod("getBoolean", paramTypes);

            //参数
            Object[] params = new Object[2];
            params[0] = new String(key);
            params[1] = new Boolean(def);

            ret = (Boolean) getBoolean.invoke(SystemProperties, params);

            Log.d(TAG, "reflect propKey = " + key + ">>>Boolean  propVal =  " + ret);
        } catch (Exception e) {
            Log.e(TAG, "reflect error = " + e.getMessage());
            String exceVal = getPropByExec(key);
            if (!TextUtils.isEmpty(exceVal)) {
                ret = Boolean.valueOf(exceVal);
            } else {
                ret = def;
            }
        }

        return ret;

    }

    private static String getPropByExec(String key) {
        InputStreamReader inputReader = null;
        InputStream inputStream = null;
        BufferedReader input = null;
        String ret = "";
        try {
            Process process = Runtime.getRuntime().exec("getprop " + key);
            inputStream = process.getInputStream();
            inputReader = new InputStreamReader(inputStream);
            input = new BufferedReader(inputReader);
            ret = input.readLine();
            Log.d(TAG, "exec getprop:  propKey = " + key + ">>>  propVal =  " + key);
        } catch (IOException iOException) {
            iOException.printStackTrace();
        } finally {
            if (inputReader != null) {
                try {
                    inputReader.close();
                } catch (IOException iOException) {
                    iOException.printStackTrace();
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException iOException) {
                    iOException.printStackTrace();
                }
            }
            if (input != null) {
                try {
                    input.close();
                } catch (IOException iOException) {
                    iOException.printStackTrace();
                }
            }
        }

        return TextUtils.isEmpty(ret) ? "" : ret;
    }

    public static void setProperty(Context context, String name, String val) {

        try {

            ClassLoader cl = context.getClassLoader();
            @SuppressWarnings("rawtypes")
            Class SystemProperties = cl.loadClass("android.os.SystemProperties");

            //参数类型
            @SuppressWarnings("rawtypes")
            Class[] paramTypes = new Class[2];
            paramTypes[0] = String.class;
            paramTypes[1] = String.class;

            Method set = SystemProperties.getMethod("set", paramTypes);

            //参数
            Object[] params = new Object[2];
            params[0] = new String(name);
            params[1] = new String(val);

            set.invoke(SystemProperties, params);

            Log.d(TAG, "reflect set property = " + name + " >>> String  propVal =  " + val);
        } catch (Exception e) {
            Log.e(TAG, "reflect error = " + e.getMessage());
        }

    }

}

/*
 * Copyright (c) 2016. Beijing Shuzijiayuan, All Rights Reserved.
 * Beijing Shuzijiayuan Confidential and Proprietary
 */

package com.kinstalk.her.settings.data.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.ContentResolver;
import android.provider.Settings;
import android.util.Log;

import java.util.Set;

/**
 * Created by zilong on 2016-05-09.
 */
public class QloveBluetoothGlobal {
    private final static boolean DEBUG = true;
    private final static String TAG = "QloveBluetoothGlobal";

    public static final String EXTRA_SEARCH_TYPE = "com.android.settings.qlove.bt.search_type";

    public static final int HANDLE_TYPE = 1;
    public static final int STEREO_TYPE = 2;

    //Profile is in disconnected state
    public static final int STATE_DISCONNECTED = 0;
    //Profile in connecting state
    public static final int STATE_CONNECTING = 1;
    //Profile in connected state
    public static final int STATE_CONNECTED  = 2;
    //Profile in disconnecting state
    public static final int STATE_DISCONNECTING = 3;

    public final static String BTStereoMacAddress = "A4:DE:C9";
    public final static String BTStereoName = "Q1201";

    private static boolean mSupportDock;

    private static boolean mForceWaiting = false;
    /** STEREO_TYPE or HANDLE_TYPE */
    private static int mForceWaitingDeivceType;
    private static String mMacOfDeviceToConnect;

    public static void setSupportDock(final boolean support) {
        mSupportDock = support;
    }

    public static boolean isDockSupported() {
        return mSupportDock;
    }

    public static boolean showBackgroundConnectToast;
    public static boolean showBackgroundConnectToast() {
        return showBackgroundConnectToast;
    }

    public static void setShowBackgroundConnectToast(final boolean show) {
        showBackgroundConnectToast = show;
    }

    public static  boolean getForceWaiting() {
        return mForceWaiting;
    }

    public static void clearForceWaiting() {
        mForceWaiting = false;
        mMacOfDeviceToConnect = null;
        mForceWaitingDeivceType = -1;
    }

    public static void setForceWaiting(final String mac) {
        mForceWaiting = true;
        if (mac != null) {
            mMacOfDeviceToConnect = mac;
        } else {
            mForceWaiting = false;
        }
    }

    public static void setForceWaiting(final int type, final String mac) {
        if (mac != null) {
            mForceWaitingDeivceType = type;
            mMacOfDeviceToConnect = mac;
            mForceWaiting = true;
        } else {
            mForceWaiting = false;
        }
    }

    public static int getForceWaitingType() {
        return mForceWaitingDeivceType;
    }

    public static boolean isForceWaitingStereo() {
        return mForceWaitingDeivceType == STEREO_TYPE;
    }

    public static boolean isForceWaitingHandle() {
        return mForceWaitingDeivceType == HANDLE_TYPE;
    }

    public static String getMacOfDeviceToConnect() {
        return mMacOfDeviceToConnect;
    }

    public static final boolean isStereoDevice(final BluetoothDevice device) {
        if (null == device)
            return false;

        return isStereoNameMatch(device.getName()) && isStereoAddressMatch(device.getAddress());
    }

    public static final boolean isStereoDevice(final CachedBluetoothDevice device) {
        if (null == device)
            return false;

        return isStereoNameMatch(device.getName()) && isStereoAddressMatch(device.getAddress());
    }

    public static final boolean isStereoNameMatch(final String name) {
        return name == null ? false : name.equals(BTStereoName);
    }

    public static final boolean isStereoAddressMatch(final String address) {
        return address == null ? false : address.startsWith(BTStereoMacAddress);
    }

    public static String getIdentifierCode(final CachedBluetoothDevice cachedDevice) {
        return cachedDevice == null ? "" : getIdentifierCode(cachedDevice.getAddress());
    }

    public static String getIdentifierCode(final String addr) {
        if (null == addr)
            return "";

        String hex = addr.replaceAll(":", "");
        if (hex.length() < 4)
            return "";

        hex = hex.substring(hex.length() - 4);
        int dec = Integer.parseInt(hex, 16);
        String oct = Integer.toOctalString(dec);
        Log.d(TAG, "hex = " + hex + ", dec = " + dec + ", oct = " + oct);

        while (oct.length() < 6) {
            oct = '0' + oct;
        }

        return oct;
    }

    public static boolean isHandleDevice(final CachedBluetoothDevice cachedDevice) {
        if (isStereoDevice(cachedDevice)) {
            return false;
        }

        if (cachedDevice == null)
            return false;

        BluetoothClass btClass = cachedDevice.getBtClass();
        if (btClass != null) {
            switch (btClass.getMajorDeviceClass()) {
                case BluetoothClass.Device.Major.COMPUTER:
                case BluetoothClass.Device.Major.PHONE:
                case BluetoothClass.Device.Major.PERIPHERAL:
                case BluetoothClass.Device.Major.IMAGING:
                    return false;
                default:
                    // unrecognized device class; continue
            }

            if (btClass.doesClassMatch(BluetoothClass.PROFILE_HEADSET)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isHandleDevice(final BluetoothDevice device) {
        if (isStereoDevice(device)) {
            return false;
        }

        if (device == null)
            return false;

        BluetoothClass btClass = device.getBluetoothClass();
        if (btClass != null) {
            switch (btClass.getMajorDeviceClass()) {
                case BluetoothClass.Device.Major.COMPUTER:
                case BluetoothClass.Device.Major.PHONE:
                case BluetoothClass.Device.Major.PERIPHERAL:
                case BluetoothClass.Device.Major.IMAGING:
                    return false;
                default:
                    // unrecognized device class; continue
            }

            if (btClass.doesClassMatch(BluetoothClass.PROFILE_HEADSET)) {
                return true;
            }
        }

        return false;
    }

    public static int getStereoBondState(final ContentResolver cr) {
        return null == cr ? -1 : Settings.Global.getInt(cr, "bt_stereo_paired_state", -1);
    }

    public static boolean putStereoBondState(final ContentResolver cr, final int state) {
        return null == cr ? false : Settings.Global.putInt(cr, "bt_stereo_paired_state", state);
    }

    public static int getHandleBondState(final ContentResolver cr) {
        return null == cr ? -1 : Settings.Global.getInt(cr, "bt_handle_paired_state", -1);
    }

    public static boolean putHandleBondState(final ContentResolver cr, final int state) {
        return null == cr ? false : Settings.Global.putInt(cr, "bt_handle_paired_state", state);
    }

    public static int getQloveDeviceBondState(final ContentResolver cr, final int type) {
        return STEREO_TYPE == type ? getStereoBondState(cr) : getHandleBondState(cr);
    }

    public static String getStereoProfileState(final ContentResolver cr) {
        return cr == null ? null : Settings.Global.getString(cr,
                "qlove_bt_stereo_connection_state_changed");
    }

    public static boolean putStereoProfileState(final ContentResolver cr, final String state) {
        return cr == null ? false : Settings.Global.putString(cr,
                "qlove_bt_stereo_connection_state_changed", state);
    }

    public static String getHandleProfileState(final ContentResolver cr) {
        return cr == null ? null : Settings.Global.getString(cr,
                "qlove_bt_handle_connection_state_changed");
    }

    public static boolean putHandleProfileState(final ContentResolver cr, final String state) {
        return cr == null ? false : Settings.Global.putString(cr,
                "qlove_bt_handle_connection_state_changed", state);
    }

    public static boolean setStereoConnectState(final ContentResolver cr, final int state) {
            return (null == cr) ? false : Settings.Global.putInt(cr, "bt_stereo_connect_state", state);
    }

    public static boolean setHandleConnectState(final ContentResolver cr, final int state) {
        return (null == cr) ? false : Settings.Global.putInt(cr, "bt_handle_connect_state", state);
    }

    public static boolean setQloveDeviceConnectionState(final ContentResolver cr, final int type, final int state) {
        if (STEREO_TYPE == type)
            return setStereoConnectState(cr, state);
        else if (HANDLE_TYPE == type)
            return setHandleConnectState(cr, state);

        return false;
    }

    public static boolean putPreviousCallState(final ContentResolver cr, final int state) {
        return cr == null ? false : Settings.Global.putInt(cr, "qlove_bt_previous_call_state", state);
    }

    public static int getPreviousCallState(final ContentResolver cr) {
        return cr == null ? -1 : Settings.Global.getInt(cr, "qlove_bt_previous_call_state", -1);
    }

    public static boolean setPairAfterCallEnd(final ContentResolver cr, final int pair) {
        return cr == null ? false : Settings.Global.putInt(cr, "qlove_pair_after_callend", pair);
    }

    public static int getPairAfterCallEnd(final ContentResolver cr) {
        return cr == null ? 0 : Settings.Global.getInt(cr, "qlove_pair_after_callend", 0);
    }

    public static int getHfpConnectionState(BluetoothAdapter adapter) {
        return adapter == null ? BluetoothProfile.STATE_DISCONNECTED :
                adapter.getProfileConnectionState(BluetoothProfile.HEADSET);
    }

    public static int getA2dpConnectionState(BluetoothAdapter adapter) {
        return adapter == null ? BluetoothProfile.STATE_DISCONNECTED :
                adapter.getProfileConnectionState(BluetoothProfile.A2DP);
    }

    public static boolean isDeviceConnected(int type, BluetoothAdapter adapter) {
        final int state = STEREO_TYPE == type ? QloveBluetoothGlobal.getA2dpConnectionState(adapter) :
                QloveBluetoothGlobal.getHfpConnectionState(adapter);
        return state == BluetoothProfile.STATE_CONNECTED;
    }

    public static boolean isDeviceConnected(int type) {
        final int state = STEREO_TYPE == type ?
                QloveBluetoothGlobal.getA2dpConnectionState(BluetoothAdapter.getDefaultAdapter()) :
                QloveBluetoothGlobal.getHfpConnectionState(BluetoothAdapter.getDefaultAdapter());
        return state == BluetoothProfile.STATE_CONNECTED;
    }

    public static BluetoothDevice getBondedStereoDevice(BluetoothAdapter adapter) {
        if (null == adapter)
            return null;

        Set<BluetoothDevice> devices = adapter.getBondedDevices();
        for (BluetoothDevice device : devices) {
            if (isStereoDevice(device)) {
                return device;
            }
        }

        return null;
    }

    public static BluetoothDevice getBondedHandleDevice(BluetoothAdapter adapter) {
        if (null == adapter)
            return null;

        Set<BluetoothDevice> devices = adapter.getBondedDevices();
        for (BluetoothDevice device : devices) {
            if (isHandleDevice(device)) {
                return device;
            }
        }

        return null;
    }

    public static BluetoothDevice getBondedQloveDevice(final int type) {
        Set<BluetoothDevice> devices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
        if (null == devices) return null;

        for (BluetoothDevice device : devices) {
            if (STEREO_TYPE == type) {
                if (isStereoDevice(device))
                    return device;
            } else if (HANDLE_TYPE == type) {
                if (isHandleDevice(device))
                    return device;
            }
        }

        return null;
    }
}

/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kinstalk.her.settings.data.bluetooth;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothUuid;
import android.os.ParcelUuid;
import android.util.Log;

/**
 * BluetoothDeviceFilter contains a static method that returns a
 * Filter object that returns whether or not the BluetoothDevice
 * passed to it matches the specified filter type constant from
 * {@link android.bluetooth.BluetoothDevicePicker}.
 */
public final class BluetoothDeviceFilter {
    private static final String TAG = "BluetoothDeviceFilter";

    /** The filter interface to external classes. */
    public interface Filter {
        boolean matches(BluetoothDevice device);
    }

    /** All filter singleton (referenced directly). */
    public static final Filter ALL_FILTER = new AllFilter();

    /** Bonded devices only filter (referenced directly). */
    public static final Filter BONDED_DEVICE_FILTER = new BondedDeviceFilter();

    public static final Filter HANDLE_DEVICE_FILTER = new HandleDeviceFilter();

    /** Unbonded devices only filter (referenced directly). */
    public static final Filter UNBONDED_DEVICE_FILTER = new UnbondedDeviceFilter();

    /** Table of singleton filter objects. */
    private static final Filter[] FILTERS = {
            ALL_FILTER,             // FILTER_TYPE_ALL
            new AudioFilter(),      // FILTER_TYPE_AUDIO
            new TransferFilter(),   // FILTER_TYPE_TRANSFER
            new PanuFilter(),       // FILTER_TYPE_PANU
            new NapFilter()         // FILTER_TYPE_NAP
    };

    /** Private constructor. */
    private BluetoothDeviceFilter() {
    }

    /**
     * Returns the singleton {@link Filter} object for the specified type,
     * or {@link #ALL_FILTER} if the type value is out of range.
     *
     * @param filterType a constant from BluetoothDevicePicker
     * @return a singleton object implementing the {@link Filter} interface.
     */
    static Filter getFilter(int filterType) {
        if (filterType >= 0 && filterType < FILTERS.length) {
            return FILTERS[filterType];
        } else {
            Log.w(TAG, "Invalid filter type " + filterType + " for device picker");
            return ALL_FILTER;
        }
    }

    /** Filter that matches all devices. */
    private static final class AllFilter implements Filter {
        public boolean matches(BluetoothDevice device) {
            return true;
        }
    }

    /** Filter that matches only bonded devices. */
    private static final class BondedDeviceFilter implements Filter {
        public boolean matches(BluetoothDevice device) {
            return device.getBondState() == BluetoothDevice.BOND_BONDED;
        }
    }

    /** Filter that matches only unbonded devices. */
    private static final class UnbondedDeviceFilter implements Filter {
        public boolean matches(BluetoothDevice device) {
            return device.getBondState() != BluetoothDevice.BOND_BONDED;
        }
    }

    /** Parent class of filters based on UUID and/or Bluetooth class. */
    private abstract static class ClassUuidFilter implements Filter {
        abstract boolean matches(ParcelUuid[] uuids, BluetoothClass btClass);

        public boolean matches(BluetoothDevice device) {
            return matches(device.getUuids(), device.getBluetoothClass());
        }
    }

    /** Filter that matches devices that support AUDIO profiles. */
    private static final class AudioFilter extends ClassUuidFilter {
        @Override
        boolean matches(ParcelUuid[] uuids, BluetoothClass btClass) {
            if (uuids != null) {
                if (BluetoothUuid.containsAnyUuid(uuids, A2dpProfile.SINK_UUIDS)) {
                    return true;
                }
                if (BluetoothUuid.containsAnyUuid(uuids, A2dpSinkProfile.SRC_UUIDS)) {
                    return true;
                }
                if (BluetoothUuid.containsAnyUuid(uuids, HeadsetProfile.UUIDS)) {
                    return true;
                }
            } else if (btClass != null) {
                if (btClass.doesClassMatch(BluetoothClass.PROFILE_A2DP) ||
                    btClass.doesClassMatch(BluetoothClass.PROFILE_A2DP_SINK) ||
                        btClass.doesClassMatch(BluetoothClass.PROFILE_HEADSET)) {
                    return true;
                }
            }
            return false;
        }
    }

    /** Filter that matches devices that support Object Transfer. */
    private static final class TransferFilter extends ClassUuidFilter {
        @Override
        boolean matches(ParcelUuid[] uuids, BluetoothClass btClass) {
            if (uuids != null) {
                if (BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.ObexObjectPush)) {
                    return true;
                }
            }
            return btClass != null
                    && btClass.doesClassMatch(BluetoothClass.PROFILE_OPP);
        }
    }

    /** Filter that matches devices that support PAN User (PANU) profile. */
    private static final class PanuFilter extends ClassUuidFilter {
        @Override
        boolean matches(ParcelUuid[] uuids, BluetoothClass btClass) {
            if (uuids != null) {
                if (BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.PANU)) {
                    return true;
                }
            }
            return btClass != null
                    && btClass.doesClassMatch(BluetoothClass.PROFILE_PANU);
        }
    }

    /** Filter that matches devices that support NAP profile. */
    private static final class NapFilter extends ClassUuidFilter {
        @Override
        boolean matches(ParcelUuid[] uuids, BluetoothClass btClass) {
            if (uuids != null) {
                if (BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.NAP)) {
                    return true;
                }
            }
            return btClass != null
                    && btClass.doesClassMatch(BluetoothClass.PROFILE_NAP);
        }
    }

    private static final class HandleDeviceFilter implements Filter {
        public boolean matches(BluetoothDevice device) {
            /**
             * Sometime, we get Bluetooth device with its name the same as its address,
             * so check its address further to not judge it as Handle device
             */
            if (QloveBluetoothGlobal.isStereoAddressMatch(device.getAddress()))
                return false;

            return QloveBluetoothGlobal.isHandleDevice(device);
        }
    }
}

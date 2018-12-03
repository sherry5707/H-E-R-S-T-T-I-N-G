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

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothA2dpSink;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothMap;
import android.bluetooth.BluetoothInputDevice;
import android.bluetooth.BluetoothPan;
import android.bluetooth.BluetoothDun;
import android.bluetooth.BluetoothSap;
import android.bluetooth.BluetoothPbap;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothUuid;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelUuid;
import android.provider.Settings;
import android.util.Log;
import android.os.Handler;
import android.os.Message;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;
import android.os.SystemProperties;

/**
 * LocalBluetoothProfileManager provides access to the LocalBluetoothProfile
 * objects for the available Bluetooth profiles.
 */
public final class LocalBluetoothProfileManager {
    private static final String TAG = "LBluetoothProfileMng";
    private static final boolean DEBUG = Utils.D;
    /** Singleton instance. */
    private static LocalBluetoothProfileManager sInstance;

    /**
     * An interface for notifying BluetoothHeadset IPC clients when they have
     * been connected to the BluetoothHeadset service.
     * Only used by {@link DockService}.
     */
    public interface ServiceListener {
        /**
         * Called to notify the client when this proxy object has been
         * connected to the BluetoothHeadset service. Clients must wait for
         * this callback before making IPC calls on the BluetoothHeadset
         * service.
         */
        void onServiceConnected();

        /**
         * Called to notify the client that this proxy object has been
         * disconnected from the BluetoothHeadset service. Clients must not
         * make IPC calls on the BluetoothHeadset service after this callback.
         * This callback will currently only occur if the application hosting
         * the BluetoothHeadset service, but may be called more often in future.
         */
        void onServiceDisconnected();
    }

    private final Context mContext;
    private final LocalBluetoothAdapter mLocalAdapter;
    private final CachedBluetoothDeviceManager mDeviceManager;
    private final BluetoothEventManager mEventManager;

    private A2dpProfile mA2dpProfile;
    private A2dpSinkProfile mA2dpSinkProfile;
    private HeadsetProfile mHeadsetProfile;
    private PbapServerProfile mPbapProfile;

    /**
     * Mapping from profile name, e.g. "HEADSET" to profile object.
     */
    private final Map<String, LocalBluetoothProfile>
            mProfileNameMap = new HashMap<String, LocalBluetoothProfile>();

    LocalBluetoothProfileManager(Context context,
            LocalBluetoothAdapter adapter,
            CachedBluetoothDeviceManager deviceManager,
            BluetoothEventManager eventManager) {
        mContext = context;

        mLocalAdapter = adapter;
        mDeviceManager = deviceManager;
        mEventManager = eventManager;
        // pass this reference to adapter and event manager (circular dependency)
        mLocalAdapter.setProfileManager(this);
        mEventManager.setProfileManager(this);

        ParcelUuid[] uuids = adapter.getUuids();

        // uuids may be null if Bluetooth is turned off
        if (uuids != null) {
            updateLocalProfiles(uuids);
        }

        //Create PBAP server profile
        mPbapProfile = new PbapServerProfile(context);
        addProfile(mPbapProfile, PbapServerProfile.NAME,
               BluetoothPbap.PBAP_STATE_CHANGED_ACTION);

        if (DEBUG) Log.d(TAG, "LocalBluetoothProfileManager construction complete");
    }

    /**
     * Initialize or update the local profile objects. If a UUID was previously
     * present but has been removed, we print a warning but don't remove the
     * profile object as it might be referenced elsewhere, or the UUID might
     * come back and we don't want multiple copies of the profile objects.
     * @param uuids
     */
    void updateLocalProfiles(ParcelUuid[] uuids) {
        boolean profilesChanged = false;
        // A2DP SRC
        if (BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.AudioSource)) {
            if ((mA2dpProfile == null) || !mA2dpProfile.isProfileReady()) {
                if((mA2dpProfile != null) && !mA2dpProfile.isProfileReady()) {
                    mA2dpProfile = null;
                    removeProfile(A2dpProfile.NAME, BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
                }
                if(DEBUG) Log.d(TAG, "Adding local A2DP SRC profile");
                mA2dpProfile = new A2dpProfile(mContext, mLocalAdapter, mDeviceManager, this);
                addProfile(mA2dpProfile, A2dpProfile.NAME,
                        BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
                profilesChanged = true;
            }
        } else if (mA2dpProfile != null) {
            mA2dpProfile = null;
            removeProfile(A2dpProfile.NAME, BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
            Log.w(TAG, "A2DP profile was previously added but the UUID is now missing.");
            profilesChanged = true;
        }

        if (BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.AudioSink)) {
            if ((mA2dpSinkProfile == null) || !mA2dpSinkProfile.isProfileReady()) {
                if((mA2dpSinkProfile != null) && !mA2dpSinkProfile.isProfileReady()) {
                    mA2dpSinkProfile = null;
                    removeProfile(A2dpSinkProfile.NAME, BluetoothA2dpSink.ACTION_CONNECTION_STATE_CHANGED);
                }
                if(DEBUG) Log.d(TAG, "Adding local A2DP Sink profile");
                mA2dpSinkProfile = new A2dpSinkProfile(mContext, mLocalAdapter, mDeviceManager, this);
                addProfile(mA2dpSinkProfile, A2dpSinkProfile.NAME,
                        BluetoothA2dpSink.ACTION_CONNECTION_STATE_CHANGED);
                profilesChanged = true;
            }
        } else if (mA2dpSinkProfile != null) {
            mA2dpSinkProfile = null;
            removeProfile(A2dpSinkProfile.NAME, BluetoothA2dpSink.ACTION_CONNECTION_STATE_CHANGED);
            Log.w(TAG, "A2DP Sink profile was previously added but the UUID is now missing.");
            profilesChanged = true;
        }

        // Headset / Handsfree
        if (BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.Handsfree_AG) ||
                BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.HSP_AG)) {
            if ((mHeadsetProfile == null) || !mHeadsetProfile.isProfileReady()) {
                if((mHeadsetProfile != null) && !mHeadsetProfile.isProfileReady()) {
                    mHeadsetProfile = null;
                    removeProfile(HeadsetProfile.NAME, BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
                }
                if (DEBUG) Log.d(TAG, "Adding local HEADSET profile");
                mHeadsetProfile = new HeadsetProfile(mContext, mLocalAdapter,
                        mDeviceManager, this);
                addProfile(mHeadsetProfile, HeadsetProfile.NAME,
                        BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
                profilesChanged = true;
            }
        } else if (mHeadsetProfile != null) {
            mHeadsetProfile = null;
            removeProfile(HeadsetProfile.NAME, BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
            Log.w(TAG, "Warning: HEADSET profile was previously added but the UUID is now missing.");
            profilesChanged = true;
        }

        if(profilesChanged) {
            mDeviceManager.updateDevices();
        }
        mEventManager.registerProfileIntentReceiver();

        // There is no local SDP record for HID and Settings app doesn't control PBAP
    }

    private final Collection<ServiceListener> mServiceListeners =
            new ArrayList<ServiceListener>();

    private void addProfile(LocalBluetoothProfile profile,
            String profileName, String stateChangedAction) {
        mEventManager.addProfileHandler(stateChangedAction, new StateChangedHandler(profile));
        mProfileNameMap.put(profileName, profile);
    }

    private void removeProfile(String profileName, String stateChangedAction) {
        mEventManager.removeProfileHandler(stateChangedAction);
        mProfileNameMap.remove(profileName);
    }

    LocalBluetoothProfile getProfileByName(String name) {
        return mProfileNameMap.get(name);
    }

    // Called from LocalBluetoothAdapter when state changes to ON
    void setBluetoothStateOn() {
        ParcelUuid[] uuids = mLocalAdapter.getUuids();
        if (uuids != null) {
            updateLocalProfiles(uuids);
        }

        if (mPbapProfile == null) {
            mPbapProfile = new PbapServerProfile(mContext);
            addProfile(mPbapProfile, PbapServerProfile.NAME,
            BluetoothPbap.PBAP_STATE_CHANGED_ACTION);
        }
        mEventManager.readPairedDevices();
    }

    /**
     * Generic handler for connection state change events for the specified profile.
     */
    private class StateChangedHandler implements BluetoothEventManager.Handler {
        final LocalBluetoothProfile mProfile;

        StateChangedHandler(LocalBluetoothProfile profile) {
            mProfile = profile;
        }

        public void onReceive(Context context, Intent intent, BluetoothDevice device) {
            CachedBluetoothDevice cachedDevice = mDeviceManager.findDevice(device);
            if (cachedDevice == null) {
                Log.w(TAG, "StateChangedHandler found new device: " + device);
                cachedDevice = mDeviceManager.addDevice(mLocalAdapter,
                        LocalBluetoothProfileManager.this, device);
            }
            int newState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, 0);
            int oldState = intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, 0);
            Log.w(TAG, "StateChangedHandler mProfile = " + mProfile + " new state = " + newState);
            if (newState == BluetoothProfile.STATE_DISCONNECTED &&
                    oldState == BluetoothProfile.STATE_CONNECTING) {
                Log.i(TAG, "Failed to connect " + mProfile + " device");
            }

            cachedDevice.onProfileStateChanged(mProfile, newState);
            cachedDevice.refresh();
        }
    }

    /** State change handler for NAP and PANU profiles. */
    private class PanStateChangedHandler extends StateChangedHandler {

        PanStateChangedHandler(LocalBluetoothProfile profile) {
            super(profile);
        }

        @Override
        public void onReceive(Context context, Intent intent, BluetoothDevice device) {
            PanProfile panProfile = (PanProfile) mProfile;
            int role = intent.getIntExtra(BluetoothPan.EXTRA_LOCAL_ROLE, 0);
            panProfile.setLocalRole(device, role);
            super.onReceive(context, intent, device);
        }
    }

    // called from DockService
    public void addServiceListener(ServiceListener l) {
        mServiceListeners.add(l);
    }

    // called from DockService
    public void removeServiceListener(ServiceListener l) {
        mServiceListeners.remove(l);
    }

    // not synchronized: use only from UI thread! (TODO: verify)
    void callServiceConnectedListeners() {
        for (ServiceListener l : mServiceListeners) {
            l.onServiceConnected();
        }
    }

    // not synchronized: use only from UI thread! (TODO: verify)
    void callServiceDisconnectedListeners() {
        for (ServiceListener listener : mServiceListeners) {
            listener.onServiceDisconnected();
        }
    }

    // This is called by DockService, so check Headset and A2DP.
    public synchronized boolean isManagerReady() {
        // Getting just the headset profile is fine for now. Will need to deal with A2DP
        // and others if they aren't always in a ready state.
        LocalBluetoothProfile profile = mHeadsetProfile;
        if (profile != null) {
            return profile.isProfileReady();
        }
        profile = mA2dpProfile;
        if (profile != null) {
            return profile.isProfileReady();
        }
        profile = mA2dpSinkProfile;
        if (profile != null) {
            return profile.isProfileReady();
        }
        return false;
    }

    public A2dpProfile getA2dpProfile() {
        if ((mA2dpProfile != null)&&(mA2dpProfile.isProfileReady()))
            return mA2dpProfile;
        else
            return null;
    }

    A2dpSinkProfile getA2dpSinkProfile() {
        if ((mA2dpSinkProfile != null)&&(mA2dpSinkProfile.isProfileReady()))
            return mA2dpSinkProfile;
        else
            return null;
    }

    HeadsetProfile getHeadsetProfile() {
        return mHeadsetProfile;
    }

    PbapServerProfile getPbapProfile(){
        return mPbapProfile;
    }

    /**
     * Fill in a list of LocalBluetoothProfile objects that are supported by
     * the local device and the remote device.
     *
     * @param uuids of the remote device
     * @param localUuids UUIDs of the local device
     * @param profiles The list of profiles to fill
     * @param removedProfiles list of profiles that were removed
     */
    synchronized void updateProfiles(ParcelUuid[] uuids, ParcelUuid[] localUuids,
            Collection<LocalBluetoothProfile> profiles,
            Collection<LocalBluetoothProfile> removedProfiles,
            boolean isPanNapConnected, BluetoothDevice device) {
        // Copy previous profile list into removedProfiles
        removedProfiles.clear();
        removedProfiles.addAll(profiles);
        profiles.clear();

        if (uuids == null) {
            return;
        }

        if (mHeadsetProfile != null) {
            if (BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.HSP) ||
                    BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.Handsfree)) {
                profiles.add(mHeadsetProfile);
                removedProfiles.remove(mHeadsetProfile);
            }
        }

        if (BluetoothUuid.containsAnyUuid(uuids, A2dpProfile.SINK_UUIDS) &&
            mA2dpProfile != null) {
            profiles.add(mA2dpProfile);
            removedProfiles.remove(mA2dpProfile);
        }

        if (BluetoothUuid.containsAnyUuid(uuids, A2dpSinkProfile.SRC_UUIDS) &&
                mA2dpSinkProfile != null) {
            profiles.add(mA2dpSinkProfile);
            removedProfiles.remove(mA2dpSinkProfile);
        }

        if (mPbapProfile != null) {
            profiles.add(mPbapProfile);
            removedProfiles.remove(mPbapProfile);
        }
    }

    public int getA2dpMode() {
        int isSinkEnabled = Settings.System.getInt(mContext.getContentResolver(), BluetoothConstants.A2DP_SINK_MODE_STRING, 0);
        if (isSinkEnabled == 1) {
            return BluetoothConstants.A2DP_SINK_MODE;
        } else {
            return BluetoothConstants.A2DP_SRC_MODE;
        }
    }

}

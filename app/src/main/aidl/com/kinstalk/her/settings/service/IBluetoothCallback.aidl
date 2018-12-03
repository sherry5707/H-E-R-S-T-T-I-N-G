// IBluetoothCallback.aidl
package com.kinstalk.her.settings.service;

// Declare any non-default types here with import statements

interface IBluetoothCallback {

    void onDeviceUpdated();

    void onScanStarted(boolean started);

    void onPlayStarted(boolean started, String deviceName);
}

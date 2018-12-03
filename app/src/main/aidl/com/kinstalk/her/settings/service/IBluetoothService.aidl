// IBluetoothService.aidl
package com.kinstalk.her.settings.service;

// Declare any non-default types here with import statements
import com.kinstalk.her.settings.data.bluetooth.LocalBluetoothDevice;
import com.kinstalk.her.settings.service.IBluetoothCallback;

interface IBluetoothService {

    void setForground(boolean foreground);

    boolean isBluetoothEnabled();
    void setBluetoothEnabled(boolean enable);

    boolean startDiscovery();
    void cancelDiscovery();
    void stopScanning();

    void connectDevice(in LocalBluetoothDevice device);
    void disconnectDevice(in LocalBluetoothDevice device);

    List<LocalBluetoothDevice> getDeviceList();

    void setA2dpMode(int mode);

    void registerCallback(in IBluetoothCallback callback);
    void unregisterCallback(in IBluetoothCallback callback);
}

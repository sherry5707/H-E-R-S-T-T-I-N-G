package com.kinstalk.her.settings.data.bluetooth;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Zhigang Zhang on 2018/1/3.
 */

public class LocalBluetoothDevice implements Parcelable {
    private enum BluetoothState {
        BOND_NONE,
        BONDING,
        BONDED,
        CONNECTING,
        CONNECT
    }

    private String mAddress;
    private String mName;
    private BluetoothState mState;

    public LocalBluetoothDevice(CachedBluetoothDevice device) {
        mAddress = device.getAddress();
        mName = device.getName();
        if(device.isLocalConnected()) {
            mState = BluetoothState.CONNECT;
        } else if(device.isConnecting()) {
            mState = BluetoothState.CONNECTING;
        } else if(device.isBonding()) {
            mState = BluetoothState.BONDING;
        } else if(device.isBonded()) {
            mState = BluetoothState.BONDED;
        } else {
            mState = BluetoothState.BOND_NONE;
        }
    }
    protected LocalBluetoothDevice(Parcel in) {
        mAddress = in.readString();
        mName = in.readString();
        int state = in.readInt();
        mState = BluetoothState.values()[state];
    }

    public static final Creator<LocalBluetoothDevice> CREATOR = new Creator<LocalBluetoothDevice>() {
        @Override
        public LocalBluetoothDevice createFromParcel(Parcel in) {
            return new LocalBluetoothDevice(in);
        }

        @Override
        public LocalBluetoothDevice[] newArray(int size) {
            return new LocalBluetoothDevice[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(mAddress);
        parcel.writeString(mName);
        parcel.writeInt(mState.ordinal());
    }

    public void setAddress(String address) {
        mAddress = address;
    }

    public String getAddress() {
        return mAddress;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getName() {
        return mName;
    }

    public boolean isBonded() {
        return mState == BluetoothState.BONDED;
    }

    public boolean isBonding() {
        return mState == BluetoothState.BONDING;
    }

    public boolean isConnecting() {
        return mState == BluetoothState.CONNECTING;
    }

    public boolean isConnected() {
        return mState == BluetoothState.CONNECT;
    }

    public String toString(){
        return "LocalBluetoothDevice[" + mName + " " + mAddress + " " + mState.toString();
    }
}

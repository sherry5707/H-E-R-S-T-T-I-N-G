package com.kinstalk.her.settings.view.adapter;

import android.content.Context;
import android.provider.ContactsContract;
import android.service.media.IMediaBrowserService;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.kinstalk.her.settings.R;
import com.kinstalk.her.settings.data.bluetooth.LocalBluetoothDevice;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Zhigang Zhang on 2017/5/16.
 */

public class HerBluetoothAdapter extends BaseAdapter {

    private List<LocalBluetoothDevice> mDeviceList;

    public HerBluetoothAdapter() {
        this.mDeviceList = new ArrayList<>();
    }

    public synchronized void clearSouces() {
        mDeviceList.clear();

        notifyDataSetChanged();
    }

    public synchronized void refreshUI() {
        notifyDataSetChanged();
    }

    public synchronized void addDevice(LocalBluetoothDevice device){

        mDeviceList.add(device);
        notifyDataSetChanged();
    }

    public synchronized void addDevices(List<LocalBluetoothDevice> devices) {
        if(devices == null) {
            return;
        }
        mDeviceList.addAll(devices);
        notifyDataSetChanged();
    }

    @Override
    public synchronized int getCount() {
        return mDeviceList.size();
    }

    @Override
    public Object getItem(int position) {
        return mDeviceList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.listitem_bluetooth, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.bluetoothName = (TextView) convertView.findViewById(R.id.bt_device_name);
            viewHolder.bluetoothState = (TextView) convertView.findViewById(R.id.bt_device_status);
            viewHolder.connectingView = (ImageView)convertView.findViewById(R.id.connecting_icon);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        LocalBluetoothDevice bluetoothDevice = (LocalBluetoothDevice) getItem(position);
        if(bluetoothDevice != null) {
            viewHolder.bluetoothName.setText(bluetoothDevice.getName());
            int color;
            if (bluetoothDevice.isConnecting() || bluetoothDevice.isBonding()) { //must check firstly
                viewHolder.bluetoothState.setText(R.string.bt_connecting);
                viewHolder.setConnecting(true);
                color = parent.getContext().getResources().getColor(R.color.minor_text_color);
            } else if (bluetoothDevice.isConnected()) {// || bluetoothDevice.isDeviceConnected()) {
                viewHolder.bluetoothState.setText(R.string.bt_connected);
                viewHolder.setConnecting(false);
                color = parent.getContext().getResources().getColor(R.color.major_text_color);
            } else {
                viewHolder.bluetoothState.setText(R.string.bt_not_connected);
                viewHolder.setConnecting(false);
                color = parent.getContext().getResources().getColor(R.color.minor_text_color);
            }
            viewHolder.bluetoothState.setTextColor(color);
        }

        return convertView;
    }

    public static class ViewHolder {
        TextView bluetoothName;
        TextView bluetoothState;
        ImageView connectingView;

        boolean mConnecting = false;

        void setConnecting(boolean connecting) {
            if(mConnecting == connecting) {
                return;
            }

            mConnecting = connecting;
            if(mConnecting) {
                bluetoothState.setVisibility(View.INVISIBLE);

                connectingView.setVisibility(View.VISIBLE);
                Animation rotateAnimation = new RotateAnimation(0.0f, 720.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                rotateAnimation.setFillAfter(true);
                rotateAnimation.setInterpolator(new LinearInterpolator());
                rotateAnimation.setDuration(2000);
                rotateAnimation.setRepeatCount(Animation.INFINITE);
                rotateAnimation.setRepeatMode(Animation.RESTART);
                connectingView.setAnimation(rotateAnimation);
            } else {
                bluetoothState.setVisibility(View.VISIBLE);

                connectingView.setVisibility(View.INVISIBLE);
                connectingView.clearAnimation();
            }
        }
    }
}

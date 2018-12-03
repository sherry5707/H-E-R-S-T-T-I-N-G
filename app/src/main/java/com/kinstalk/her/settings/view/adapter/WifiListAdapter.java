package com.kinstalk.her.settings.view.adapter;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.kinstalk.her.settings.HerSettingsApplication;
import com.kinstalk.her.settings.R;
import com.kinstalk.her.settings.data.wifi.ScanResultEntity;
import com.kinstalk.her.settings.data.wifi.WifiHelper;
import com.kinstalk.her.settings.util.Utility;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ZhigangZhang on 17/5/16.
 */

public class WifiListAdapter extends BaseAdapter {
    private List<ScanResultEntity> mDataList = new ArrayList<>();
    private ListView listView;

    public WifiListAdapter(ListView listView) {
        this.listView = listView;
    }

    public void refreshData(List<ScanResultEntity> dataList) {
        if (null != dataList && !dataList.isEmpty()) {
            this.mDataList = new ArrayList<ScanResultEntity>(dataList);
            this.notifyDataSetChanged();
            new Utility().setListViewHeightBasedOnChildren(listView);
        }
    }

    @Override
    public int getCount() {
        if (mDataList == null) {
            return 0;
        }
        return mDataList.size();
    }

    @Override
    public ScanResultEntity getItem(int position) {
        if (mDataList == null) {
            return null;
        }
        return mDataList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        ContentViewHolder viewHolder = null;
        if (convertView == null) {
            viewHolder = new ContentViewHolder();
            convertView = LayoutInflater.from(HerSettingsApplication.getApplication()).inflate(R.layout.item_wifilist_content, null, false);
            viewHolder.labelView = (TextView) convertView.findViewById(R.id.wifi_ssid);
//            viewHolder.signalView = (ImageView) convertView.findViewById(R.id.wifi_signal_icon);
            viewHolder.statusView = (TextView) convertView.findViewById(R.id.wifi_connect_status);
            viewHolder.connectingView = (ImageView)convertView.findViewById(R.id.connecting_icon);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ContentViewHolder) convertView.getTag();
        }
        ScanResult entity = mDataList.get(position).getScanResult();
        boolean isNeedAuth = WifiHelper.isNeedAuth(entity.capabilities);
        int level = WifiManager.calculateSignalLevel(entity.level, 5);
        viewHolder.labelView.setText(entity.SSID);
//        if (isNeedAuth) {
//            viewHolder.signalView.setImageResource(R.drawable.wifi_signal_lock_icon);
//        } else {
//            viewHolder.signalView.setImageResource(R.drawable.wifi_signal_icon);
//        }
//        viewHolder.signalView.setImageLevel(level);
        //判断正在连接
        if (getItem(position).isLoading()) {
            viewHolder.setConnecting(true);
        } else {
            viewHolder.setConnecting(false);
        }
        return convertView;
    }

    public void resetLoadingStatus() {
        if (getCount() > 0) {
            for (ScanResultEntity scanResultEntity : mDataList) {
                scanResultEntity.setLoading(false);
            }
            notifyDataSetChanged();
        }
    }

    public void setLoadingStatus() {
        for (ScanResultEntity scanResultEntity : mDataList) {
            int networkId = WifiHelper.getInstance().getConfiguredNetworkID(scanResultEntity.getScanResult().SSID);
            if (networkId != -1 && networkId == WifiHelper.getInstance().getLastNetworkId()) {
                scanResultEntity.setLoading(true);
                continue;
            } else {
                scanResultEntity.setLoading(false);
            }
        }
        notifyDataSetChanged();
    }

    public class ContentViewHolder {
        TextView labelView;
        //ImageView signalView;
        TextView statusView;
        ImageView connectingView;
        boolean mConnecting = false;

        void setConnecting(boolean connecting) {
            if(mConnecting == connecting) {
                return;
            }

            mConnecting = connecting;
            if(mConnecting) {
                statusView.setVisibility(View.INVISIBLE);

                connectingView.setVisibility(View.VISIBLE);
                Animation rotateAnimation = new RotateAnimation(0.0f, 720.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                rotateAnimation.setFillAfter(true);
                rotateAnimation.setInterpolator(new LinearInterpolator());
                rotateAnimation.setDuration(2000);
                rotateAnimation.setRepeatCount(Animation.INFINITE);
                rotateAnimation.setRepeatMode(Animation.RESTART);
                connectingView.setAnimation(rotateAnimation);
            } else {
                statusView.setVisibility(View.VISIBLE);

                connectingView.setVisibility(View.INVISIBLE);
                connectingView.clearAnimation();
            }
        }
    }
}

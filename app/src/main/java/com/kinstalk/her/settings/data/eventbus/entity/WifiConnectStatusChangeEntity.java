package com.kinstalk.her.settings.data.eventbus.entity;

/**
 * Created by pop on 17/5/18.
 */

import android.net.NetworkInfo;

/**
 * wifi连接状态变化
 */
public class WifiConnectStatusChangeEntity {
    private NetworkInfo.State state;
    private NetworkInfo.DetailedState detailedState;

    public WifiConnectStatusChangeEntity(NetworkInfo.State state, NetworkInfo.DetailedState detailedState) {
        this.state = state;
        this.detailedState = detailedState;
    }

    public NetworkInfo.State getState() {
        return state;
    }

    public NetworkInfo.DetailedState getDetailedState() {
        return this.detailedState;
    }

}

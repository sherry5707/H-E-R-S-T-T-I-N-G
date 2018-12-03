package com.kinstalk.her.settings.data.wifi;

import android.net.wifi.ScanResult;

/**
 * Created by pop on 17/5/19.
 */

public class ScanResultEntity {
    private ScanResult scanResult;
    private boolean loading;

    public ScanResultEntity(ScanResult scanResult, boolean loading) {
        this.scanResult = scanResult;
        this.loading = loading;
    }

    public ScanResult getScanResult() {
        return scanResult;
    }

    public void setScanResult(ScanResult scanResult) {
        this.scanResult = scanResult;
    }

    public boolean isLoading() {
        return loading;
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
    }
}

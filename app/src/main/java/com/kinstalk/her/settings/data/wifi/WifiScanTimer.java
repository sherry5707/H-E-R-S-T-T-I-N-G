package com.kinstalk.her.settings.data.wifi;

import com.kinstalk.her.settings.data.eventbus.DataEventBus;
import com.kinstalk.her.settings.data.eventbus.entity.WifiScanTimeoutEntity;

/**
 * Created by pop on 17/5/17.
 */

public class WifiScanTimer implements Runnable {
    public static final long MAX_SCAN_TIME = 1000 * 10;

    public boolean running = false;
    private long startTime = 0L;
    private Thread thread = null;

    @Override
    public void run() {
        while (true) {
            if (!running) {
                return;
            }
            if (System.currentTimeMillis() - startTime >= MAX_SCAN_TIME) {
                DataEventBus.getEventBus().post(new WifiScanTimeoutEntity());
            }
            try {
                Thread.sleep(100L);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void start() {
        try {
            thread = new Thread(this);
            running = true;
            startTime = System.currentTimeMillis();
            thread.start();
            WifiHelper.getInstance().startScan();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            running = false;
            thread = null;
            startTime = 0L;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

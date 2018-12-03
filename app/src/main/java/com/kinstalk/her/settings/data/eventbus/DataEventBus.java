package com.kinstalk.her.settings.data.eventbus;

import org.greenrobot.eventbus.EventBus;

public class DataEventBus {

    private static EventBus mDataEventBus;

    static {
        mDataEventBus = new CustomEventBus();
    }

    public static EventBus getEventBus() {
        return mDataEventBus;
    }

    public static void register(Object subscriber) {
        if (!getEventBus().isRegistered(subscriber)) {
            getEventBus().register(subscriber);
        }
    }

    static final class CustomEventBus extends EventBus {
        @Override
        public void register(Object subscriber) {
            if (!isRegistered(subscriber)) {
                super.register(subscriber);
            }
        }
    }
}

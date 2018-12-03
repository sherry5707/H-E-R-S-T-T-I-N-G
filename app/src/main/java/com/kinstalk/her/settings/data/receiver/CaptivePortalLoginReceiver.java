package com.kinstalk.her.settings.data.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.kinstalk.her.settings.service.CaptivePortalLoginService;
import com.kinstalk.her.settings.util.DebugUtils;

/**
 * Created by Zhigang Zhang on 2018/1/16.
 */

public class CaptivePortalLoginReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent mintent) {
        DebugUtils.LogD("START CaptivePortalLoginService");
        Intent intent = new Intent(context, CaptivePortalLoginService.class);
        context.startService(intent);
    }
}

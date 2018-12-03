package com.kinstalk.her.settings.util;

/**
 * Created by Zhang Zhigang on 2017/6/23.
 */

public class Constants {
    public static final String KINSTALK_TXSDK_BIND_STATUS = "kinstalk.com.aicore.action.txsdk.bind_status";
    public static final String EXTRA_BIND_STATUS = "bind_status";

    public static final String EXTRA_UNBIND_ACTION = "unbind_action";

    public static final String EXTRA_KEEP_TAP= "keep_tap";
    //ai service
    public static final String REMOTE_AI_SERVICE = "kinstalk.com.qloveaicore";
    public static final String REMOTE_AI_SERVICE_CLASS = "kinstalk.com.qloveaicore.QAICoreService";

    public static final String GET_DATA_CMD_STR = "cmd";
    public static final String GET_DATA_CMD_GET_OWNER = "getOwner";
    public static final String GET_DATA_CMD_ERASE_ALL_BINDERS = "eraseAllBinders";

    public static final String SHARED_PREF_WIFI_SETTINGS = "com.kinstalk.her.settings.wifi_settings";
    public static final String SHARED_PREF_KEY_WIFI_FOREGROUND = "wifi_foreground";

    public static final String SHARED_PREF_ABOUT_SETTING = "com.kinstalk.her.settings.about_settings";
    public static final String SHARED_PREF_KEY_MASTER_CLEAR = "master_clear";

    public static final String ACTION_MEDIA_INFO = "kinstalk.action.remoteview";

    // remote view 实体
    public static final String REMOTE_VIEW_OBJECT_KEY = "remote_view_object_key";

    //操作方式key
    public static final String REMOTE_VIEW_OPERATION_KEY = "remote_view_operation_key";

    //操作方式key 添加 更新重复添加即可
    public static final String REMOTE_VIEW_OPERATION_ADD = "remote_view_operation_add";

    //操作方式key 移除
    public static final String REMOTE_VIEW_OPERATION_REMOVE = "remote_view_operation_remomve";
    //  REMOTE_VIEW_TYPE
    // remind 提醒
    // calculagraph 计时
    // media 音乐
    // weather天气
    public static final String REMOTE_VIEW_TYPE_KEY = "remote_view_type_key";

}

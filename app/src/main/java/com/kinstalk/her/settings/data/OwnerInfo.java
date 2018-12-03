package com.kinstalk.her.settings.data;

/**
 * Created by Zhigang Zhang on 2017/7/5.
 */

public class OwnerInfo {
    private String mHeadUrl;
    private String mName;
    private String mSn;

    public OwnerInfo(String headUrl, String name, String sn) {
        mHeadUrl = headUrl;
        mName = name;
        mSn = sn;
    }

    public String getHeadUrl() {
        return mHeadUrl;
    }

    public String getName() {
        return mName;
    }

    public String getSn() {
        return mSn;
    }
}

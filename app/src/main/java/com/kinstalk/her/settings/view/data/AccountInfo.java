package com.kinstalk.her.settings.view.data;

public class AccountInfo {
    private String headUrl;
    private String tinyId;
    private String din;
    private String nickName;

    public AccountInfo() {
    }

    public AccountInfo(String headUrl, String tinyId, String din, String nickName) {
        this.headUrl = headUrl;
        this.tinyId = tinyId;
        this.din = din;
        this.nickName = nickName;
    }

    @Override
    public String toString() {
        return "AccountInfo{" +
                "headUrl='" + headUrl + '\'' +
                ", tinyId='" + tinyId + '\'' +
                ", din='" + din + '\'' +
                ", nickName='" + nickName + '\'' +
                '}';
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getHeadUrl() {
        return headUrl;
    }

    public void setHeadUrl(String headUrl) {
        this.headUrl = headUrl;
    }

    public String getTinyId() {
        return tinyId;
    }

    public void setTinyId(String tinyId) {
        this.tinyId = tinyId;
    }

    public String getDin() {
        return din;
    }

    public void setDin(String din) {
        this.din = din;
    }
}

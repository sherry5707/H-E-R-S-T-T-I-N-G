package com.kinstalk.her.settings.data.wifi;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SqliteWrapper;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

public class WifiIotProvider extends ContentProvider {

    private static UriMatcher matcher;
    private static final String tag = "WifiIotProvider";
    private static final String AUTHORITY = "com.kinstalk.her.settings.data.wifi.WifiIotProvider";

    private static final String WIFI_TABLE = "wifi";

    private static final int WIFI_ALL = 0;
    private static final int WIFI_ONE = 1;
    private static final int WIFI_CONFIG = 2;
    private static final int WIFI_INSERT = 3;

    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/iotwifi";
    public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/iotwifi";

    private DBHelper helper;
    private SQLiteDatabase db;

    //数据改变后立即重新查询
    private static final Uri NOTIFY_URI = Uri.parse("content://" + AUTHORITY + "/iotwifi");

    static {
        matcher = new UriMatcher(UriMatcher.NO_MATCH);
        matcher.addURI(AUTHORITY, "query", WIFI_ALL); //匹配记录集合
        matcher.addURI(AUTHORITY, "query/#", WIFI_ONE);//匹配单条记录
        matcher.addURI(AUTHORITY, "config", WIFI_CONFIG);//配置WIFI
        matcher.addURI(AUTHORITY, "insert", WIFI_INSERT);//插入WIFI
    }

    /**
     * Database Helper
     */
    private class DBHelper extends SQLiteOpenHelper {

        private static final String DATABASE_NAME = "iotwifi.db";
        private static final int DATABASE_VERSION = 1;

        public DBHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            String sql = "CREATE TABLE IF NOT EXISTS "+ WIFI_TABLE +
                    "(_id INTEGER PRIMARY KEY AUTOINCREMENT, ssid TEXT, passwd TEXT)";
            db.execSQL(sql);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS person");
            onCreate(db);
        }
    }

    @Override
    public boolean onCreate() {
        helper = new DBHelper(getContext());
        return false;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        logI("query :"+projection.toString());
        if(matcher.match(uri) == WIFI_ALL){
            SQLiteDatabase db = helper.getReadableDatabase();
            Cursor cursor = db.query(WIFI_TABLE,projection,selection,selectionArgs,null,null,sortOrder);
            return cursor;
        }
        else if(matcher.match(uri) == WIFI_ONE){
            long id = ContentUris.parseId(uri);
            SQLiteDatabase db = helper.getReadableDatabase();
            Cursor cursor = db.query(WIFI_TABLE,projection,"id=?",new String[]{id+""},null,null,sortOrder);
            return cursor;
        }
        else {
            throw new IllegalArgumentException("zyb === query error argument");
        }
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        int match = matcher.match(uri);
        switch (match) {
            case WIFI_ALL:
                return CONTENT_TYPE;
            case WIFI_ONE:
                return CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("zyb === Not yet implemented");
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        if(matcher.match(uri) == WIFI_INSERT){
            String ssid = values.getAsString("ssid");
            String passwd = values.getAsString("passwd");
            logI("insert SSID:"+ssid+" Passwd:"+passwd);
            SQLiteDatabase dbR = helper.getReadableDatabase();
            Cursor cursor = dbR.query(WIFI_TABLE,null,"ssid=?",new String[]{ssid},null,null,null);
            if(cursor.moveToNext()) {
                logW("insert have this ssid");
                SQLiteDatabase db = helper.getWritableDatabase();
                db.update(WIFI_TABLE,values,"ssid=?",new String[]{ssid});
            }
            else {
                logW("insert new ssid");
                SQLiteDatabase db = helper.getWritableDatabase();
                db.insert(WIFI_TABLE, null, values);
            }
        }
        else{
            throw new IllegalArgumentException("zyb === insert uri error !");
        }
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Nullable
    @Override
    public Bundle call(@NonNull String method, @Nullable String arg, @Nullable Bundle extras) {
        logI("call: "+method);
        if(extras != null) {
            String ssid = "",passwd = "";

            if(extras.containsKey("ssid")) ssid = extras.getString("ssid");
            if(extras.containsKey("passwd")) passwd = extras.getString("passwd");

            logD("call ssid:" + ssid + " passwd:" + passwd);
            connect(ssid, passwd);
        }
        return super.call(method, arg, extras);
    }

    private void connect(String ssid,String passwd) {
        WifiConfiguration configuration = null;
        if(TextUtils.isEmpty(passwd)) {
            configuration = WifiHelper.getInstance().createWifiConfiguration(ssid, WifiHelper.AUTH_NONE, "", "");
            WifiHelper.getInstance().connectNetwork(configuration);
            logW("connect Wifi type AUTH_NONE");
        }
        else{
            configuration = WifiHelper.getInstance().createWifiConfiguration(ssid, WifiHelper.AUTH_PSK, "", passwd);
            WifiHelper.getInstance().connectNetwork(configuration);
            logW("connect Wifi type AUTH_PSK");
        }
    }

    //Log输出
    public void logD(String str){
        Log.d(tag,"zyb ==="+str);
    }
    public void logI(String str){
        Log.i(tag,"zyb ==="+str);
    }
    public void logW(String str){
        Log.w(tag,"zyb ==="+str);
    }
    public void logE(String str){
        Log.e(tag,"zyb ==="+str);
    }
}

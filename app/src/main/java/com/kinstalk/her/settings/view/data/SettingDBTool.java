package com.kinstalk.her.settings.view.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.kinstalk.her.settings.HerSettingsApplication;

import static com.kinstalk.her.settings.view.data.SettingDBHelper.UserInfo.COLUMN_PHONE_NUMBER;
import static com.kinstalk.her.settings.view.data.SettingDBHelper.UserInfo.COLUMN_TINY_ID;

public class SettingDBTool {
    private static final String TAG = "SettingDBTool";
    public static SettingDBHelper mDBHelper;

    public static long insert(ContentValues item) {
        if (mDBHelper == null) {
            mDBHelper = new SettingDBHelper(HerSettingsApplication.getApplication());
        }
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        long id = 0;
        id = db.replace(SettingDBHelper.TABLE_USER, null, item);
        if (id < 0) {
            Log.i(TAG, "couldn't insert into qchat_provider database");
        }
        return id;
    }

    /**
     * 根据tinyId查询db
     *
     * @param tinyId
     * @return
     */
    public static Cursor query(String tinyId) {
        Log.i(TAG, "query: ");
        if (mDBHelper == null) {
            mDBHelper = new SettingDBHelper(HerSettingsApplication.getApplication());
        }
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        Cursor cursor = null;
        cursor = db.query(SettingDBHelper.TABLE_USER, null,
                COLUMN_TINY_ID + "=?", new String[]{tinyId}, null, null, null);
        if (cursor == null) {
            Log.e(TAG, "query failed in qchat_provider database,cursor is null");
        }
        return cursor;
    }

    /**
     * 根据tinyId获取电话号码
     *
     * @param tinyId
     * @return
     */
    public static String getPhoneByTinyId(String tinyId) {
        Log.i(TAG, "getPhoneByTinyId:tinyId:" + tinyId);
        Cursor cursor = query(tinyId);
        String phone;
        if (cursor != null && cursor.getCount() != 0) {
            cursor.moveToFirst();
            do {
                phone = cursor.getString(cursor.getColumnIndex(COLUMN_PHONE_NUMBER));
                Log.i(TAG, "getPhoneByTinyId: phone:" + phone);
            } while (cursor.moveToNext());
            return phone;
        } else {
            Log.e(TAG, "getPhoneByTinyId: cursor == null");
            return null;
        }
    }

    public static long savePhoneWithTinyId(String tinyId, String phone) {
        Log.i(TAG, "savePhoneWithTinyId: tinyId:" + tinyId + ",phone:" + phone);
        ContentValues item = new ContentValues();
        item.put(COLUMN_TINY_ID, tinyId);
        item.put(COLUMN_PHONE_NUMBER, phone);
        return insert(item);
    }
}

package com.kinstalk.her.settings.view.data;

import android.content.ContentUris;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

public class SettingDBHelper extends SQLiteOpenHelper {
    private static final String TAG = "SettingDBHelper";
    private static final String DATABASE_NAME = "setting.db";
    private static final int DATABASE_VERSION = 1;
    public static final String TABLE_USER = "userinfo";
    private static SettingDBHelper mSettingDatabaseHelper;

    public SettingDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static SettingDBHelper getInstance(final Context context) {
        if (mSettingDatabaseHelper == null) {
            synchronized (SettingDBHelper.class) {
                if (mSettingDatabaseHelper == null) {
                    mSettingDatabaseHelper = new SettingDBHelper(context);
                }
            }
        }

        return mSettingDatabaseHelper;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        Log.d(TAG, "onCreate");
        sqLiteDatabase.execSQL("drop table if exists userinfo");
        createTables(sqLiteDatabase);
    }

    private void createTables(SQLiteDatabase db) {
        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_USER + " ("
                    + SettingDBHelper.UserInfo.COLUMN_TINY_ID + " TEXT UNIQUE, "
                    + UserInfo.COLUMN_PHONE_NUMBER + " TEXT);");
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    public static class UserInfo implements BaseColumns {
        public static final String COLUMN_TINY_ID = "tiny_id";
        public static final String COLUMN_PHONE_NUMBER = "phone_number";
    }
}

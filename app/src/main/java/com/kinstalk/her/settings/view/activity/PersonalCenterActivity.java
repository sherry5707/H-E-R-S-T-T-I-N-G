package com.kinstalk.her.settings.view.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.kinstalk.her.settings.HerSettingsApplication;
import com.kinstalk.her.settings.R;
import com.kinstalk.her.settings.util.DebugUtils;
import com.kinstalk.her.settings.view.data.AccountInfo;
import com.kinstalk.her.settings.view.data.Api;
import com.kinstalk.her.settings.view.data.QchatThreadManager;
import com.kinstalk.her.settings.view.data.SettingDBTool;
import com.kinstalk.her.settings.view.widget.CircleImageView;
import com.kinstalk.qloveaicore.AIManager;

import org.json.JSONException;
import org.json.JSONObject;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PersonalCenterActivity extends Activity {
    private static final String TAG = PersonalCenterActivity.class.getSimpleName();
    private boolean isBindNumber;
    private String phoneNumberStr;
    private AccountInfo accountInfo = new AccountInfo();
    public static final int SWITCH_PHONE = 0;
    public static final int BIND_PHONE = 1;

    @BindView(R.id.avatar)
    CircleImageView avatar;
    @BindView(R.id.phone_number)
    TextView phoneNumber;
    @BindView(R.id.bind_or_switch_number)
    TextView bindOrSwitchNumber;
    @BindView(R.id.user_name)
    TextView userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_center);
        ButterKnife.bind(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        init();
    }

    private void init() {
        initAvatar();
        fetchPhoneNumber();
    }

    private void initAvatar() {
        String jasonStr = AIManager.getInstance(this).getAccountInfo(null);
        try {
            JSONObject jsonObject = new JSONObject(jasonStr);
            String avatarUrl = jsonObject.optString("headUrl");
            String tinyID = jsonObject.optString("tinyID");
            String din = jsonObject.optString("din");
            String remark = jsonObject.optString("remark");
            accountInfo.setHeadUrl(avatarUrl);
            accountInfo.setDin(din);
            accountInfo.setTinyId(tinyID);
            accountInfo.setNickName(remark);
            DebugUtils.LogD(accountInfo.toString());
            if (!TextUtils.isEmpty(avatarUrl)) {
                Glide.with(this)
                        .load(avatarUrl)
                        .asBitmap()
                        .error(R.mipmap.default_avatar)
                        .placeholder(R.mipmap.default_avatar)
                        .into(avatar);
            }
            if (!TextUtils.isEmpty(remark)) {
                userName.setText(remark);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void fetchPhoneNumber() {
        QchatThreadManager.getInstance().start(new Runnable() {
            @Override
            public void run() {
                String number = SettingDBTool.getPhoneByTinyId(accountInfo.getTinyId());
                if (!TextUtils.isEmpty(number)) {
                    phoneNumberStr = number;
                    mHandler.sendEmptyMessage(SWITCH_PHONE);
                } else {
                    phoneNumberStr = null;
                    mHandler.sendEmptyMessage(BIND_PHONE);
                }
            }
        });
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SWITCH_PHONE:
                    Log.i(TAG, "handleMessage: switch_phone,phone:" + phoneNumberStr);
                    isBindNumber = true;
                    phoneNumber.setVisibility(View.VISIBLE);
                    phoneNumber.setText(phoneNumberStr);
                    bindOrSwitchNumber.setText(R.string.switch_phone_number);
                    break;
                case BIND_PHONE:
                    Log.i(TAG, "handleMessage: bind_phone");
                    isBindNumber = false;
                    phoneNumber.setVisibility(View.INVISIBLE);
                    bindOrSwitchNumber.setText(R.string.bind_phone_number);
                    break;
            }
        }
    };

    public void backButtonClicked(View view) {
        finish();
    }

    public void bindOrSwitchNumber(View view) {
        Intent intent = new Intent();
        intent.setClass(this, BindPhoneNumActivity.class);
        intent.putExtra("type", isBindNumber ? SWITCH_PHONE : BIND_PHONE);
        intent.putExtra("tinyId", accountInfo.getTinyId());
        intent.putExtra("phone", phoneNumberStr);
        startActivity(intent);
    }
}

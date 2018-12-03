package com.kinstalk.her.settings.view.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.kinstalk.her.settings.R;
import com.kinstalk.her.settings.view.data.Api;
import com.kinstalk.her.settings.view.widget.CountDownTextView;

import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.kinstalk.her.settings.view.activity.PersonalCenterActivity.BIND_PHONE;
import static com.kinstalk.her.settings.view.activity.PersonalCenterActivity.SWITCH_PHONE;

public class BindPhoneNumActivity extends Activity {
    private static final String TAG = BindPhoneNumActivity.class.getSimpleName();

    @BindView(R.id.phone_number_input)
    EditText phoneNumberEditText;
    @BindView(R.id.verification_code_input)
    EditText verificationCodeEditText;
    @BindView(R.id.personal_title)
    TextView title;
    @BindView(R.id.bind_or_switch_number)
    CountDownTextView fetchVerificationCode;
    @BindView(R.id.confirm)
    TextView confirmBindTextView;

    /**
     * 用户身份唯一标识
     */
    private String tinyId;

    /**
     * 输入的手机号
     */
    private String mobilePhone;

    /**
     * 绑定的手机号
     */
    private String bindMobilePhone;

    /**
     * 标记是更换手机号还是绑定手机号
     * 0:SWITCH_PHONE
     * 1:BIND_PHONE
     */
    private int type;

    /**
     * 验证码获取状态和手机号绑定状态
     */
    private static final int GET_CODE_SUCCESS = 201;      //获取验证码成功
    private static final int GET_CODE_TOO_OFTEN = 202;    //发送验证码太频繁(60s一次)
    private static final int GET_CODE_TOO_MUCH = 203;     //发送验证码次数超过上限(10条/天)
    private static final int BIND_SUCCESS = 204;          //绑定手机号成功
    private static final int CODE_ERROR = 205;             //验证码错误
    private static final int BIND_FAIL = 206;             //绑定手机号失败

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case GET_CODE_SUCCESS:
                    Log.i(TAG, "handleMessage: GET_CODE_SUCCESS");
                    Toast.makeText(BindPhoneNumActivity.this,
                            getString(R.string.verification_code_sent), Toast.LENGTH_SHORT).show();
                    break;
                case GET_CODE_TOO_OFTEN:
                    Log.i(TAG, "handleMessage: GET_CODE_TOO_OFTEN");
                    Toast.makeText(BindPhoneNumActivity.this,
                            getString(R.string.send_sms_too_often), Toast.LENGTH_SHORT).show();
                    break;
                case GET_CODE_TOO_MUCH:
                    Log.i(TAG, "handleMessage: GET_CODE_TOO_MUCH");
                    Toast.makeText(BindPhoneNumActivity.this,
                            getString(R.string.send_sms_too_much), Toast.LENGTH_SHORT).show();
                    break;
                case BIND_SUCCESS:
                    Log.i(TAG, "BIND_SUCCESS");
                    Toast.makeText(BindPhoneNumActivity.this,
                            getString(R.string.bind_phone_success), Toast.LENGTH_SHORT).show();
                    finish();
                    break;
                case CODE_ERROR:
                    Log.i(TAG, "CODE_ERROR");
                    Toast.makeText(BindPhoneNumActivity.this,
                            getString(R.string.verification_code_error), Toast.LENGTH_SHORT).show();
                    break;
                case BIND_FAIL:
                    Log.i(TAG, "handleMessage: BIND_FAIL");
                    Toast.makeText(BindPhoneNumActivity.this,
                            getString(R.string.bind_phone_fail), Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bind_phone_num);
        ButterKnife.bind(this);

        init();
//        setAutoActivityTimeout();
    }

    @SuppressLint("NewApi")
    private void init() {
        type = getIntent().getIntExtra("type", BIND_PHONE);

        title.setText(type == BIND_PHONE ? R.string.bind_phone_number : R.string.switch_phone_number);

        if (isMatchLength(phoneNumberEditText.getText().toString(), 11)) {
            fetchVerificationCode.setUsable(true);
        } else {
            fetchVerificationCode.setUsable(false);
        }
        if (isMatchLength(verificationCodeEditText.getText().toString(), 6)) {
            confirmBindTextView.setBackgroundResource(R.drawable.personal_submit_bg);
            confirmBindTextView.setClickable(true);
        } else {
            confirmBindTextView.setBackgroundResource(R.drawable.personal_submit_bg_unusable);
            confirmBindTextView.setClickable(false);
        }
        if (type == SWITCH_PHONE) {
            bindMobilePhone = getIntent().getStringExtra("phone");
        }

        tinyId = getIntent().getStringExtra("tinyId");
        Log.i(TAG, "onCreate: tinyId:" + tinyId);
        phoneNumberEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                Log.i(TAG, "onTextChanged: " + charSequence);
                phoneNumberEditText.setSelection(charSequence.toString().length());
                if (isMatchLength(charSequence.toString(), 11) && isMainLandMobile(charSequence.toString())) {
                    fetchVerificationCode.setUsable(true);
                } else {
                    fetchVerificationCode.setUsable(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        verificationCodeEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                Log.i(TAG, "onTextChanged: " + charSequence);
                verificationCodeEditText.setSelection(charSequence.toString().length());
                if (isMatchLength(charSequence.toString(), 6)) {
                    confirmBindTextView.setBackgroundResource(R.drawable.personal_submit_bg);
                    confirmBindTextView.setClickable(true);
                } else {
                    confirmBindTextView.setBackgroundResource(R.drawable.personal_submit_bg_unusable);
                    confirmBindTextView.setClickable(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    public void backButtonClicked(View view) {
        finish();
    }

    /**
     * 获取短信验证码点击事件
     *
     * @param view
     */
    public void getVerificationCode(View view) {
        if (!isNetworkAvailable(this)) {
            Log.i(TAG, "getVerificationCode: no network");
            Toast.makeText(BindPhoneNumActivity.this,
                    getString(R.string.no_network), Toast.LENGTH_SHORT).show();
            return;
        }
        mobilePhone = phoneNumberEditText.getText().toString();
        if (type == SWITCH_PHONE) {
            if (TextUtils.equals(mobilePhone, bindMobilePhone)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                View alertView = View.inflate(this, R.layout.rebind_phone_dialog, null);
                final TextView iknow = (TextView) alertView.findViewById(R.id.i_know);
                builder.setView(alertView);

                final AlertDialog alertDialog = builder.create();
                iknow.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        alertDialog.dismiss();
                    }
                });
                alertDialog.show();
            } else {
                getVerificationCode();
            }
        } else {
            getVerificationCode();
        }

    }

    private void getVerificationCode() {
        fetchVerificationCode.start();
        Api.fetchVerification(tinyId, phoneNumberEditText.getText().toString(), new Api.VerifyCodeResultCallBack() {
            @Override
            public void verificationCodeResult(int result) {
                Log.i(TAG, "getVerificationCode,result:" + result);
                if (result == 0) {
                    mHandler.sendEmptyMessage(GET_CODE_SUCCESS);
                } else if (result == 101) {
                    mHandler.sendEmptyMessage(GET_CODE_TOO_OFTEN);
                } else if (result == 102) {
                    mHandler.sendEmptyMessage(GET_CODE_TOO_MUCH);
                }
            }
        });
    }

    public void bindPhoneNumber(View view) {
        Api.reBindPhoneNumber(tinyId, mobilePhone, verificationCodeEditText.getText().toString(), new Api.RebindPhoneResultCallBack() {
            @Override
            public void getRebindResult(int code, final String number) {
                Log.i(TAG, "bindPhoneNumber,code:" + code + ",number:" + number);
                if (code == 0) {
                    mHandler.sendEmptyMessage(BIND_SUCCESS);
                } else if (code == 103) {
                    mHandler.sendEmptyMessage(CODE_ERROR);
                } else {
                    mHandler.sendEmptyMessage(BIND_FAIL);
                }
            }
        });
    }

    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            if (cm.getActiveNetworkInfo() != null) {
                return cm.getActiveNetworkInfo().isAvailable();
            }
        }
        return false;
    }

    /**
     * 判断一个字符串的位数
     *
     * @param str
     * @param length
     * @return
     */
    public static boolean isMatchLength(String str, int length) {
        if (str.isEmpty()) {
            return false;
        } else {
            return str.length() == length ? true : false;
        }
    }

    public static boolean isMainLandMobile(String mobile) {
        if (TextUtils.isEmpty(mobile))
            return false;
        Pattern pattern = Pattern.compile("1[0-9]{10}");
        Matcher matcher = pattern.matcher(mobile);
        return matcher.matches(); // 当条件满足时，将返回true，否则返回false
    }

    protected void setAutoActivityTimeout() {
        WindowManager.LayoutParams attr = getWindow().getAttributes();
        try {
            Class<WindowManager.LayoutParams> attrClass = WindowManager.LayoutParams.class;
            Method method = attrClass.getMethod("setAutoActivityTimeout", new Class[]{boolean.class});
            method.setAccessible(true);
            Object object = method.invoke(attr, false);

        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        getWindow().setAttributes(attr);
    }
}

package com.kinstalk.her.settings.view.fragment;

import android.content.Context;
import android.graphics.Typeface;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.kinstalk.her.httpsdk.util.DebugUtil;
import com.kinstalk.her.settings.R;
import com.kinstalk.her.settings.data.eventbus.DataEventBus;
import com.kinstalk.her.settings.data.eventbus.entity.WifiConnectStatusChangeEntity;
import com.kinstalk.her.settings.data.wifi.WifiHelper;
import com.kinstalk.her.settings.util.DebugUtils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by pop on 17/5/17.
 */

public class WifiAuthDialogFragment extends DialogFragment implements View.OnClickListener {
    private static final String EXTRA_SCAN_RESULT="scan_result";
    static final int SECURITY_NONE = 0;
    static final int SECURITY_WEP = 1;
    static final int SECURITY_PSK = 2;
    static final int SECURITY_EAP = 3;

    static final int PWD_INPUTTING = 0;
    static final int PWD_RECOGNIZING = 1;
    static final int PWD_WRONG = 2;
    static final int PWD_VERIFIED = 3; //password is correct, fail to connect because of other reasons

    private static final String QINJIAN_INPUTMETHOD_ID = "com.kinstalk.her.inputmethod/.HerInputMethod";

    @BindView(R.id.back_button)
    ImageView mBackView;
    @BindView(R.id.password_done)
    TextView mDownView;
    @BindView(R.id.clear_all)
    ImageView mClearAllView;
    @BindView(R.id.wifi_auth_password)
    EditText mPasswordView;
    @BindView(R.id.divider)
    ImageView mDividerView;
    @BindView(R.id.progressbar)
    ProgressBar mProgressBar;
    @BindView(R.id.wrong_pwd_divider)
    ImageView mWrongPwdDivider;
    @BindView(R.id.status_text)
    TextView mStatusText;

    private WifiHelper wifiHelper;
    private ScanResult scanResult;
    private Unbinder unbinder;

    private String mDefaultIMM = null;
    private boolean mSwitchIMM = false;

    private int mPasswordStatus = PWD_INPUTTING;
    private boolean mConnecting = false;
    private boolean mAuthenticated = false;

    private Typeface mTypeFace;

    public static WifiAuthDialogFragment newInstance(ScanResult scanResult) {
        WifiAuthDialogFragment fragment = new WifiAuthDialogFragment();
        Bundle bundle = new Bundle(2);
        bundle.putParcelable(EXTRA_SCAN_RESULT, scanResult);
        fragment.setArguments(bundle);
        return fragment ;
    }

    public WifiAuthDialogFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        scanResult = (ScanResult)getArguments().getParcelable(EXTRA_SCAN_RESULT);
        wifiHelper = WifiHelper.getInstance();
        setDialogStyle();

        syncInputMethod();

        DataEventBus.register(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_wifi_auth, null);
        unbinder = ButterKnife.bind(this, rootView);
        initView(rootView);
        initAction();
        mPasswordView.setImeOptions(EditorInfo.IME_FLAG_NO_FULLSCREEN | EditorInfo.IME_ACTION_SEND);

        mTypeFace = mPasswordView.getTypeface();
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        switchIMM();

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager) getActivity().getApplicationContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(mPasswordView, 0);
                //imm.toggleSoftInput(0, InputMethodManager.SHOW_FORCED);
            }

        },100);
    }

    @Override
    public void onPause() {
        super.onPause();

        InputMethodManager imm = (InputMethodManager) getActivity().getApplicationContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(null, InputMethodManager.HIDE_NOT_ALWAYS);
        if(mSwitchIMM && mDefaultIMM != null) {
            imm.setInputMethod(null, mDefaultIMM);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    private void initView(View rootView) {
        String password = mPasswordView.getText().toString();
        if(TextUtils.isEmpty(password)) {
            mClearAllView.setVisibility(View.INVISIBLE);
        } else {
            mClearAllView.setVisibility(View.VISIBLE);
        }
        mDownView.setEnabled(false);
    }

    private void initAction() {
        mBackView.setOnClickListener(this);
        mDownView.setOnClickListener(this);
        mClearAllView.setOnClickListener(this);
        mPasswordView.setOnKeyListener(new View.OnKeyListener() {

            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    String password = mPasswordView.getText().toString();
                    if(TextUtils.isEmpty(password)) {
                        //do nothing for empty password
                        setPasswordStatus(PWD_INPUTTING);
                    } else if (password.length() < 8) {
                        //do nothing
                        return true;
                    } else {
                        setPasswordStatus(PWD_RECOGNIZING);
                        connect();
                    }
                }
                return false;
            }
        });
        mPasswordView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String password = mPasswordView.getText().toString();
                if(TextUtils.isEmpty(password)) {
                    mPasswordView.setTypeface(mTypeFace);
                    mClearAllView.setVisibility(View.INVISIBLE);
                } else {
                    mPasswordView.setTypeface(Typeface.MONOSPACE);
                    mClearAllView.setVisibility(View.VISIBLE);
                    if(password.length() >= 8) {
                        mDownView.setEnabled(true);
                    } else {
                        mDownView.setEnabled(false);
                    }
                }
                setPasswordStatus(PWD_INPUTTING);
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back_button:
                dismiss();
                break;
            case R.id.password_done:
                DebugUtil.LogD("WIfiAuthDialogFragment", "onClick done");
                if(TextUtils.isEmpty(mPasswordView.getText().toString())) {
                    //do nothing for empty possward
                    setPasswordStatus(PWD_INPUTTING);
                } else if(checkPasswordInvalid()) {
                    setPasswordStatus(PWD_WRONG);
                } else {
                    DebugUtil.LogD("WIfiAuthDialogFragment", "start Connect: ");
                    setPasswordStatus(PWD_RECOGNIZING);
                    connect();
                }
                break;
            case R.id.clear_all:
                DebugUtil.LogD("WIfiAuthDialogFragment", "clear all");
                mPasswordView.setText("");
                setPasswordStatus(PWD_INPUTTING);
                break;
        }
    }

    private void connect() {
        String SSID = scanResult.SSID;
        int type = WifiHelper.AUTH_PSK;
        String userName = "";
        String password = mPasswordView.getText().toString();
        WifiConfiguration configuration = wifiHelper.createWifiConfiguration(SSID, type, userName, password);
        WifiHelper.getInstance().connectNetwork(configuration);
    }

    protected void setDialogStyle() {
        setStyle(STYLE_NORMAL, R.style.Dialog_Transparent);
    }

    private boolean checkPasswordInvalid() {
        boolean passwordInvalid = false;

        int securityType = SECURITY_NONE;
        if (scanResult.capabilities.contains("WEP")) {
            securityType = SECURITY_WEP;
        } else if (scanResult.capabilities.contains("PSK")) {
            securityType = SECURITY_PSK;
        } else if (scanResult.capabilities.contains("EAP")) {
            securityType = SECURITY_EAP;
        }
        DebugUtil.LogD("WifiAuthDialogFragment", "checkPasswordInvalid: securityType:"
                +securityType+",mPassword.length:"+mPasswordView.length());
        if (mPasswordView != null &&
           ((securityType == SECURITY_WEP && mPasswordView.length() == 0) ||
            (securityType == SECURITY_PSK && mPasswordView.length() < 8))) {
            passwordInvalid = true;
            DebugUtil.LogD("WifiAuthDialogFragment", "checkPasswordInvalid: return true");
        }

        return passwordInvalid;
    }

    private void syncInputMethod() {
        mDefaultIMM = Settings.Secure.getString(getActivity().getApplicationContext().getContentResolver(),
                Settings.Secure.DEFAULT_INPUT_METHOD);
        if(QINJIAN_INPUTMETHOD_ID.equals(mDefaultIMM)) {
            mSwitchIMM = false;
        } else {
            InputMethodManager imm = (InputMethodManager) getActivity().getApplicationContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            List<InputMethodInfo> imList = imm.getEnabledInputMethodList();
            for (InputMethodInfo imInfo : imList) {
                DebugUtils.LogD("has imm:" + imInfo);
                String id = imInfo.getId();
                if (QINJIAN_INPUTMETHOD_ID.equals(id)) {
                    mSwitchIMM = true;
                }
            }
        }

        DebugUtils.LogD("default IMM is:" + mDefaultIMM + " and need to switch:" + mSwitchIMM);
    }

    private void switchIMM() {
        InputMethodManager imm = (InputMethodManager) getActivity().getApplicationContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if(mSwitchIMM) {
            imm.setInputMethod(null, QINJIAN_INPUTMETHOD_ID);
        }
    }

    private void setPasswordStatus(int status) {
        if(mPasswordStatus == status) {
            return;
        }
        mPasswordStatus = status;
        updateUI();
    }
    private void updateUI() {
        switch(mPasswordStatus) {
            case PWD_INPUTTING:
                mDividerView.setVisibility(View.VISIBLE);
                mProgressBar.setVisibility(View.INVISIBLE);
                mWrongPwdDivider.setVisibility(View.INVISIBLE);
                mStatusText.setVisibility(View.INVISIBLE);
                mPasswordView.setEnabled(true);
                mDownView.setVisibility(View.VISIBLE);
                mClearAllView.setVisibility(View.VISIBLE);
                break;
            case PWD_RECOGNIZING:
                mDividerView.setVisibility(View.INVISIBLE);
                mProgressBar.setVisibility(View.VISIBLE);
                mWrongPwdDivider.setVisibility(View.INVISIBLE);
                mStatusText.setVisibility(View.VISIBLE);
                mStatusText.setText(R.string.wifi_password_verifying);
                mStatusText.setTextColor(getResources().getColor(R.color.wifi_connecting));
                mPasswordView.setEnabled(false);
                mDownView.setVisibility(View.INVISIBLE);
                mClearAllView.setVisibility(View.INVISIBLE);
                break;
            case PWD_WRONG:
            case PWD_VERIFIED:
                mDividerView.setVisibility(View.INVISIBLE);
                mProgressBar.setVisibility(View.INVISIBLE);
                mWrongPwdDivider.setVisibility(View.VISIBLE);
                mStatusText.setVisibility(View.VISIBLE);
                if(mPasswordStatus == PWD_WRONG) {
                    mStatusText.setText(R.string.wifi_wrong_password);
                } else  {
                    mStatusText.setText(R.string.wifi_password_verified);
                }
                mStatusText.setTextColor(getResources().getColor(R.color.wifi_pwd_wrong));
                mPasswordView.setEnabled(true);
                mDownView.setVisibility(View.VISIBLE);
                mClearAllView.setVisibility(View.VISIBLE);
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWifiNetworkStatusChange(WifiConnectStatusChangeEntity statusEntity) {
        DebugUtil.LogD("WifiAuthDialogFragment",
                "onWifiNetworkStatusChange: statusEntity.getState()"+statusEntity.getState()+",mPasswordStatus:"+mPasswordStatus);
        switch (statusEntity.getState()) {
            case CONNECTED:
                if(mPasswordStatus == PWD_RECOGNIZING) {
                    DebugUtil.LogD("WifiAuthDialogFragment","connect success and dissmiss fragment");
                    dismiss();
                    mConnecting = false;
                    mAuthenticated = false;
                }
                break;
            case CONNECTING:
                if(mPasswordStatus == PWD_RECOGNIZING) {
                    DebugUtil.LogD("WifiAuthDialogFragment","connectting");
                    mConnecting = true;
                    mAuthenticated = false;
                    if((statusEntity.getDetailedState() == NetworkInfo.DetailedState.OBTAINING_IPADDR) ||
                            (statusEntity.getDetailedState() == NetworkInfo.DetailedState.VERIFYING_POOR_LINK) ||
                            (statusEntity.getDetailedState() == NetworkInfo.DetailedState.CAPTIVE_PORTAL_CHECK)) {
                        //authentication pass
                        mAuthenticated = true;
                        DebugUtil.LogD("WifiAuthDialogFragment","password is correct");
                    }
                }
                break;
            case DISCONNECTED:
                if(mConnecting && (mPasswordStatus == PWD_RECOGNIZING)) {
                    DebugUtil.LogD("WifiAuthDialogFragment","password is correctting and status is recognazing,finally disconnected");
                    mConnecting = false;
                    WifiHelper.getInstance().removeWifi(WifiHelper.getInstance().getLastNetworkId());
                    if(!mAuthenticated) {
                        //fail to connecting to an AP,authentication fail
                        setPasswordStatus(PWD_WRONG);
                        Log.e("WifiAuthDialogFragment", "onWifiNetworkStatusChange: passwordError!!!!!!!!!!!!!!");
                    } else {
                        Log.e("WifiAuthDialogFragment", "onWifiNetworkStatusChange: WLAN can not connect");
                        setPasswordStatus(PWD_VERIFIED);
                    }
                }
        }
    }
}

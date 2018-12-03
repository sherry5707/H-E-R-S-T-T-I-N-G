package com.kinstalk.her.settings.view.fragment;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IPowerManager;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.SeekBar;

import com.kinstalk.her.settings.R;
import com.kinstalk.her.settings.util.DebugUtils;

import java.lang.reflect.Method;
import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import ly.count.android.sdk.Countly;
/**
 * Created by mamingzhang on 2017/5/15.
 */

public class HerSetttingFragment extends Fragment {
    private static  final String TAG = "HerSetttingFragment";

    private static final float BRIGHTNESS_ADJ_RESOLUTION = 100;

    protected Activity mActivity;
    @BindView(R.id.music_seekbar)
    SeekBar musicSeekBar;
    @BindView(R.id.alarm_seekbar)
    SeekBar alarmSeekBar;
    @BindView(R.id.bright_seekbar)
    SeekBar brightSeekBar;

    private Unbinder unbinder;
    private AudioManager audiomanager;
    private Context mContext;

    private boolean mAutomaticBrightness;
    private int mMinimumBacklight = 0;
    private int mMaximumBacklight = 255;
    private int mTrackingValue;
    private IPowerManager mPower;
    private BrightnessObserver mBrightnessObserver;

    private SeekBar.OnSeekBarChangeListener ringListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
            audiomanager.setStreamVolume(AudioManager.STREAM_RING, progress, 0);
            Countly.sharedInstance().recordEvent("Touch_view_vol_action", 1);

            int currentVolume = audiomanager.getStreamVolume(AudioManager.STREAM_RING);
            int currentMusicVolume = audiomanager.getStreamVolume(AudioManager.STREAM_MUSIC);
            int currentAlarmVolume = audiomanager.getStreamVolume(AudioManager.STREAM_ALARM);
            HashMap<String, String> data = new HashMap<>();
            data.put("ringVol",  String.valueOf(currentVolume));
            data.put("musciVol",  String.valueOf(currentMusicVolume));
            data.put("alarmVol",  String.valueOf(currentAlarmVolume));
            Countly.sharedInstance().recordEvent("Touch_view_vol_state", data, 1);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };
    private SeekBar.OnSeekBarChangeListener musicListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
            audiomanager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);

            Countly.sharedInstance().recordEvent("Touch_view_vol_action", 1);
            int currentVolume = audiomanager.getStreamVolume(AudioManager.STREAM_RING);
            int currentMusicVolume = audiomanager.getStreamVolume(AudioManager.STREAM_MUSIC);
            int currentAlarmVolume = audiomanager.getStreamVolume(AudioManager.STREAM_ALARM);
            HashMap<String, String> data = new HashMap<>();
            data.put("ringVol",  String.valueOf(currentVolume));
            data.put("musciVol",  String.valueOf(currentMusicVolume));
            data.put("alarmVol",  String.valueOf(currentAlarmVolume));
            Countly.sharedInstance().recordEvent("Touch_view_vol_state", data, 1);

            HashMap<String, String> segmentation = new HashMap<>();
            segmentation.put("musciVol",  String.valueOf(currentMusicVolume));
            Countly.sharedInstance().recordEvent("media_volume_ctl", segmentation, 1);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            mTrackingValue = 0;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };
    private SeekBar.OnSeekBarChangeListener alarmListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
            audiomanager.setStreamVolume(AudioManager.STREAM_ALARM, progress, 0);

            Countly.sharedInstance().recordEvent("Touch_view_vol_action", 1);
            int currentVolume = audiomanager.getStreamVolume(AudioManager.STREAM_RING);
            int currentMusicVolume = audiomanager.getStreamVolume(AudioManager.STREAM_MUSIC);
            int currentAlarmVolume = audiomanager.getStreamVolume(AudioManager.STREAM_ALARM);
            HashMap<String, String> data = new HashMap<>();
            data.put("ringVol",  String.valueOf(currentVolume));
            data.put("musciVol",  String.valueOf(currentMusicVolume));
            data.put("alarmVol",  String.valueOf(currentAlarmVolume));
            Countly.sharedInstance().recordEvent("Touch_view_vol_state", data, 1);

            HashMap<String, String> segmentation = new HashMap<>();
            segmentation.put("alarmVol",  String.valueOf(currentAlarmVolume));
            Countly.sharedInstance().recordEvent("alarm_volume_ctl", segmentation, 1);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };
    private SeekBar.OnSeekBarChangeListener brightListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if(fromUser) {
                if (mAutomaticBrightness) {
                    mAutomaticBrightness = false;
                    Settings.System.putInt(mActivity.getContentResolver(),
                            Settings.System.SCREEN_BRIGHTNESS_MODE,
                            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                }
                final int val = progress + mMinimumBacklight;
                mTrackingValue = val;
                setBrightness(val);
                HashMap<String, String> data = new HashMap<>();
                data.put("brightVol", String.valueOf(val));
                Countly.sharedInstance().recordEvent("HerSettings2", "Touch_view_bright_state", data, 1);

                HashMap<String, String> segmentation = new HashMap<>();
                segmentation.put("brightVol",  String.valueOf(val));
                Countly.sharedInstance().recordEvent("brightness_ctl", segmentation, 1);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            mTrackingValue = 0;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            Settings.System.putInt(mContext.getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS, mTrackingValue);
            mTrackingValue = 0;

            Countly.sharedInstance().recordEvent("Touch_view_br_action", 1);
            HashMap<String, String> data2 = new HashMap<>();
            data2.put("bright",  String.valueOf(getSystemBrightness()));
            data2.put("auto_checked",String.valueOf(false) );
            Countly.sharedInstance().recordEvent("Touch_view_br_state", data2, 1);
        }
    };

    private void setBrightness(int brightness) {
        try {
            mPower.setTemporaryScreenBrightnessSettingOverride(brightness);
        } catch (RemoteException ex) {
            DebugUtils.LogE("setBrightness() exception:" + ex);
        }
    }

    private CompoundButton.OnCheckedChangeListener checkListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
            if (isChecked) {
                Settings.System.putInt(mActivity.getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
            } else {
                Settings.System.putInt(mActivity.getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            }
            updateMode();
            updateSlider();

            Countly.sharedInstance().recordEvent("Touch_view_br_auto_action", 1);
            HashMap<String, String> data2 = new HashMap<>();
            data2.put("bright",  String.valueOf(getSystemBrightness()));
            data2.put("auto_checked",String.valueOf(isChecked) );
            Countly.sharedInstance().recordEvent("Touch_view_br_state", data2, 1);
        }
    };

    public static HerSetttingFragment getInstance() {
        return new HerSetttingFragment();
    }

    @Override
    public void onAttach(Context context) {
       // Log.d(TAG,"onAttach");
        super.onAttach(context);
        this.mActivity = getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
       // Log.d(TAG,"onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_settings_settings, container, false);
        unbinder = ButterKnife.bind(this, rootView);

        musicSeekBar.setOnSeekBarChangeListener(musicListener);
        alarmSeekBar.setOnSeekBarChangeListener(alarmListener);
        brightSeekBar.setOnSeekBarChangeListener(brightListener);

        mContext =  getActivity().getApplicationContext();
        initData();
        //resolverObserver();
        return rootView;
    }

    private void initData() {
        audiomanager = (AudioManager) mActivity.getSystemService(Context.AUDIO_SERVICE);
        int maxMusicVolume = audiomanager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int currentMusicVolume = audiomanager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxAlarmVolume = audiomanager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        int currentAlarmVolume = audiomanager.getStreamVolume(AudioManager.STREAM_ALARM);

        musicSeekBar.setMax(maxMusicVolume);
        musicSeekBar.setProgress(currentMusicVolume);
        alarmSeekBar.setMax(maxAlarmVolume);
        alarmSeekBar.setProgress(currentAlarmVolume);

        mMinimumBacklight = getMinimumScreenBrightnessSetting();
        mMaximumBacklight = getMaximumScreenBrightnessSetting();
        mPower = IPowerManager.Stub.asInterface(ServiceManager.getService("power"));

        mBrightnessObserver = new BrightnessObserver(new Handler());

        updateMode();
        updateSlider();

        HashMap<String, String> data = new HashMap<>();
        data.put("musciVol",  String.valueOf(currentMusicVolume));
        data.put("alarmVol",  String.valueOf(currentAlarmVolume));
        Countly.sharedInstance().recordEvent("Touch_view_vol_state", data, 1);

        HashMap<String, String> data2 = new HashMap<>();
        data2.put("bright",  String.valueOf(getSystemBrightness()));
        data2.put("auto_checked",String.valueOf(mAutomaticBrightness) );
        Countly.sharedInstance().recordEvent("Touch_view_br_state", data2, 1);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //Log.d(TAG,"onDestroyView");
        unresolverObserver();
        unbinder.unbind();
    }

    @Override
    public void onResume() {
        super.onResume();
        //Log.d(TAG,"onResume");
        if (audiomanager != null && musicSeekBar != null) {
            int currentMusicVolume = audiomanager.getStreamVolume(AudioManager.STREAM_MUSIC);
            int currentAlarmVolume = audiomanager.getStreamVolume(AudioManager.STREAM_ALARM);
            musicSeekBar.setProgress(currentMusicVolume);
            alarmSeekBar.setProgress(currentAlarmVolume);

           //Log.d(TAG,"currentVolume " + currentVolume);
        }
        resolverObserver();
    }

    @Override
    public void onPause() {
        super.onPause();
        //Log.d(TAG,"onPause");
        unresolverObserver();
    }

    /**
     * 声音变更监听器，声音变更时修改画面。
     */
    private MyCommonReceiver mCommonReceiver = null;
    /**
     * 处理音量变化时的界面显示。
     */
    public class MyCommonReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // 如果音量发生变化则更改seekbar的位置
            String action = intent.getAction();
            if ("android.media.VOLUME_CHANGED_ACTION".equals(action)) {
               // Log.e("MyCommonReceiver", "android.media.VOLUME_CHANGED_ACTION Receive");
                if (audiomanager != null && musicSeekBar != null) {
                    int currentMusicVolume = audiomanager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    int currentAlarmVolume = audiomanager.getStreamVolume(AudioManager.STREAM_ALARM);
                    musicSeekBar.setProgress(currentMusicVolume);
                    alarmSeekBar.setProgress(currentAlarmVolume);

                    Countly.sharedInstance().recordEvent("Touch_view_vol_action", 1);

                    HashMap<String, String> data = new HashMap<>();
                    data.put("musciVol",  String.valueOf(currentMusicVolume));
                    data.put("alarmVol",  String.valueOf(currentAlarmVolume));
                    Countly.sharedInstance().recordEvent("Touch_view_vol_state", data, 1);                }
            }
        }
    }

    /**
     * 画面注册监听
     */
    private void resolverObserver() {
        if(mCommonReceiver == null) {
            mCommonReceiver = new MyCommonReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.media.VOLUME_CHANGED_ACTION");
            mContext.registerReceiver(mCommonReceiver, filter);
        }

        mBrightnessObserver.startObserving();
    }

    /**
     * 画面反注册监听
     */
    private void unresolverObserver() {
        // this.getActivity().getContentResolver().unregisterContentObserver(mBrightnessMode);
        if (mCommonReceiver != null) {
            mContext.unregisterReceiver(mCommonReceiver);
            mCommonReceiver = null;
        }

        mBrightnessObserver.stopObserving();
    }

    //Settings.System.SCREEN_AUTO_BRIGHTNESS_ADJ
    private final String SCREEN_AUTO_BRIGHTNESS_ADJ = "screen_auto_brightness_adj";
    /** ContentObserver to watch brightness **/
    private class BrightnessObserver extends ContentObserver {

        private final Uri BRIGHTNESS_MODE_URI =
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE);
        private final Uri BRIGHTNESS_URI =
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS);
        private final Uri BRIGHTNESS_ADJ_URI =
                Settings.System.getUriFor(SCREEN_AUTO_BRIGHTNESS_ADJ);

        BrightnessObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (selfChange) return;
            if (BRIGHTNESS_MODE_URI.equals(uri)) {
                if(updateMode()) {
                    updateSlider();
                }
            } else if (BRIGHTNESS_URI.equals(uri) && !mAutomaticBrightness) {
                updateSlider();
            } else if (BRIGHTNESS_ADJ_URI.equals(uri) && mAutomaticBrightness) {
                updateSlider();
            }
        }

        void startObserving() {
            final ContentResolver cr = mContext.getContentResolver();
            cr.unregisterContentObserver(this);
            cr.registerContentObserver(
                    BRIGHTNESS_MODE_URI,
                    false, this);
            cr.registerContentObserver(
                    BRIGHTNESS_URI,
                    false, this);
            cr.registerContentObserver(
                    BRIGHTNESS_ADJ_URI,
                    false, this);
        }

        void stopObserving() {
            final ContentResolver cr = mContext.getContentResolver();
            cr.unregisterContentObserver(this);
        }
    }

    private boolean updateMode() {
        boolean automatic = mAutomaticBrightness;
        try {
            //Log.d("bright", Settings.System.getInt(mActivity.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE) + "");
            int mode = Settings.System.getInt(mActivity.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE);
            if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                automatic = true;
            } else {
                automatic= false;
            }
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        if(mAutomaticBrightness != automatic) {
            mAutomaticBrightness = automatic;
            return true;
        }
        return false;

    }

    private int getSystemBrightness() {
        int systemBrightness = mMinimumBacklight;
        try {
            systemBrightness = Settings.System.getInt(mActivity.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        return systemBrightness;
    }

    private void updateSlider() {
        if (mAutomaticBrightness) {
            float value = Settings.System.getFloat(mContext.getContentResolver(),
                    SCREEN_AUTO_BRIGHTNESS_ADJ, 0);
            brightSeekBar.setMax((int) BRIGHTNESS_ADJ_RESOLUTION);
            brightSeekBar.setProgress((int) ((value + 1) * BRIGHTNESS_ADJ_RESOLUTION / 2f));
        } else {
            int value = getSystemBrightness();
            brightSeekBar.setMax(mMaximumBacklight - mMinimumBacklight);
            brightSeekBar.setProgress(value - mMinimumBacklight);
        }
    }

    private int getMinimumScreenBrightnessSetting() {
        PowerManager pm = (PowerManager)mContext.getSystemService(Context.POWER_SERVICE);
        try {
            Class clz = pm.getClass();
            Method get = clz.getMethod("getMinimumScreenBrightnessSetting");
            return (Integer) get.invoke(pm);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
    private int getMaximumScreenBrightnessSetting() {
        PowerManager pm = (PowerManager)mContext.getSystemService(Context.POWER_SERVICE);
        try {
            Class clz = pm.getClass();
            Method get = clz.getMethod("getMaximumScreenBrightnessSetting");
            return (Integer) get.invoke(pm);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}

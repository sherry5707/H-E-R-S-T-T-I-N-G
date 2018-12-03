package com.kinstalk.her.settings.view.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.support.v7.preference.Preference;

import com.kinstalk.her.settings.R;

/**
 * Created by Zhigang Zhang on 2017/10/17.
 */

public class SettingPrefrence extends Preference {

    private Context mContext;
    private String mTitle;
    private String mInfo;
    private String mCurrentInfo;
    private Boolean mMajor;
    private TextView mInfoView;
    private TextView mTitleView;

    public SettingPrefrence(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SettingPrefrence(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.SettingPreference);
        mTitle = ta.getString(R.styleable.SettingPreference_setting_title);
        mInfo = ta.getString(R.styleable.SettingPreference_setting_info);
        mCurrentInfo = mInfo;
        mMajor = ta.getBoolean(R.styleable.SettingPreference_setting_info_major, false);
        ta.recycle();
        mContext = context;

        setLayoutResource(R.layout.preference_setting);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        TextView titleView = (TextView)holder.findViewById(R.id.setting_title);
        titleView.setText(mTitle);
        mInfoView = (TextView)holder.findViewById(R.id.setting_info);
        mInfoView.setText(mCurrentInfo);

        float size = 0;
        if(mMajor) {
            mInfoView.setTextColor(mContext.getResources().getColor(R.color.minor_text_color));
            size = mContext.getResources().getDimensionPixelSize(R.dimen.setting_pref_major_size);
            mInfoView.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        } else {
            mInfoView.setTextColor(mContext.getResources().getColor(R.color.major_text_color));
            size = mContext.getResources().getDimensionPixelSize(R.dimen.setting_pref_minor_size);
            mInfoView.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        }
    }

    public void updateInfo(String info) {
        if(TextUtils.isEmpty(info)) {
            mCurrentInfo = mInfo;
        } else {
            mCurrentInfo = info;
        }
        if(mInfoView != null) {
            mInfoView.setText(mCurrentInfo);
        }
    }
}

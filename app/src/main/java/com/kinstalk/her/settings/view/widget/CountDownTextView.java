package com.kinstalk.her.settings.view.widget;

import android.annotation.ColorRes;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.kinstalk.her.settings.R;

@SuppressLint("AppCompatCustomView")
public class CountDownTextView extends TextView {
    /**
     * 倒计时时提示文字
     */
    private String mHintText;
    /**
     * 提示文字
     */
    private String mNormalText;

    /**
     * 倒计时时间
     */
    private long mCountDownMillis = 60000;

    /**
     * 剩余倒计时时间
     */
    private long mLastMillis;
    /**
     * 间隔时间差(两次发送handler)
     */
    private long mIntervalMillis = 1000;
    /**
     * 开始倒计时code
     */
    private final int MSG_WHAT_START = 10010;
    /**
     * 可用状态下字体颜色Id
     */
    private int usableColorId;
    /**
     * 不可用状态下字体颜色Id
     */
    private int unusableColorId;

    public CountDownTextView(Context context) {
        super(context);
    }

    @SuppressLint("ResourceAsColor")
    public CountDownTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CountDownTextView);
        if (typedArray != null) {
            mHintText = typedArray.getString(R.styleable.CountDownTextView_cdt_hint_text);
            mCountDownMillis = typedArray.getInteger(R.styleable.CountDownTextView_cdt_countdown_time, 60000);
            usableColorId = typedArray.getColor(R.styleable.CountDownTextView_cdt_usable_color,
                    getResources().getColor(R.color.blue_text_button_color));
            unusableColorId = typedArray.getColor(R.styleable.CountDownTextView_cdt_unusable_color,
                    getResources().getColor(R.color.disable_fetch_code_colr));
            mNormalText = typedArray.getString(R.styleable.CountDownTextView_cdt_normal_text);
            setText(mNormalText);
            typedArray.recycle();
        }
    }

    public CountDownTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case MSG_WHAT_START:
//                    Log.e("l", mLastMillis + "");
                    if (mLastMillis > 0) {
                        setUsable(false);
                        setText(mLastMillis / 1000 + "秒后" + mHintText);
                        mLastMillis -= mIntervalMillis;
                        mHandler.sendEmptyMessageDelayed(MSG_WHAT_START, mIntervalMillis);
                    } else {
                        setUsable(true);
                    }
                    break;
            }
        }
    };

    /**
     * 设置是否可用
     *
     * @param usable
     */
    public void setUsable(boolean usable) {
        if (usable) {
            //可用
            if (!isClickable()) {
                setClickable(usable);
                setTextColor(usableColorId);
                setText(mNormalText);
            }
        } else {
            //不可用
            if (isClickable()) {
                setClickable(usable);
                setTextColor(unusableColorId);
            }
        }
    }

    /**
     * 开始倒计时
     */
    public void start() {
        mLastMillis = mCountDownMillis;
        mHandler.sendEmptyMessage(MSG_WHAT_START);
    }

    /**
     * 重置倒计时
     */
    public void reset() {
        mLastMillis = 0;
        mHandler.sendEmptyMessage(MSG_WHAT_START);
    }
/*
    @Override
    public void setOnClickListener(@Nullable final OnClickListener onClickListener) {
        super.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mHandler.removeMessages(MSG_WHAT_START);
                start();
                onClickListener.onClick(v);
            }
        });

    }*/

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mHandler.removeMessages(MSG_WHAT_START);
    }
}

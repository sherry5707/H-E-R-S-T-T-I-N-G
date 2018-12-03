package com.kinstalk.her.settings.view.views;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kinstalk.her.settings.R;


/**
 * Created by Zhigang Zhang on 2017/10/30.
 */

public class HerAlertDialog extends AlertDialog {

    private View mView = null;
    private TextView mTitleView;
    private TextView mContentView;
    private TextView mMinorContentView;
    private Button mPositiveView;
    private Button mNegativeView;
    private Context mContext;

    private CharSequence mTitle;
    private CharSequence mContent;
    private CharSequence mMinorContent;
    private CharSequence mPositiveText;
    private View.OnClickListener mPositiveListener;
    private CharSequence mNegativeText;
    private View.OnClickListener mNegativeListener;

    public HerAlertDialog(Context context) {
        super(context, R.style.AlertDialogCustom);
        mContext = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mView = getLayoutInflater().inflate(R.layout.alert_dialog_custom, null);
        mTitleView = (TextView)mView.findViewById(R.id.title_text);
        mContentView = (TextView)mView.findViewById(R.id.content_text);
        mMinorContentView = (TextView)mView.findViewById(R.id.context_minor_text);
        mPositiveView = (Button)mView.findViewById(R.id.positive_button);
        mNegativeView = (Button)mView.findViewById(R.id.negative_button);

        setView(mView, 0, 0, 0, 0);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void show() {
        super.show();

/*        int width = mContext.getResources().getDisplayMetrics().widthPixels;
        int height = mContext.getResources().getDisplayMetrics().heightPixels;

        final Window dialogWindow = getWindow();
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialogWindow.getAttributes());
        lp.width = width;
        lp.height = height;
        dialogWindow.setAttributes(lp);
        dialogWindow.setGravity(Gravity.CENTER);*/

        if(TextUtils.isEmpty(mTitle)) {
            mTitleView.setVisibility(View.INVISIBLE);
        } else {
            mTitleView.setVisibility(View.VISIBLE);
            mTitleView.setText(mTitle);
        }

        if(TextUtils.isEmpty(mContent)) {
            mContentView.setVisibility(View.GONE);
        } else {
            mContentView.setVisibility(View.VISIBLE);
            mContentView.setText(mContent);
        }

        if(TextUtils.isEmpty(mMinorContent)) {
            mMinorContentView .setVisibility(View.GONE);
        } else {
            mMinorContentView.setVisibility(View.VISIBLE);
            mMinorContentView.setText(mMinorContent);
        }

        if(mPositiveListener == null) {
            mPositiveView.setVisibility(View.GONE);
        } else {
            mPositiveView.setVisibility(View.VISIBLE);
            mPositiveView.setText(mPositiveText);
            mPositiveView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mPositiveListener.onClick(mPositiveView);
                    dismiss();
                }
            });
        }

        if(mNegativeListener == null) {
            mNegativeView.setVisibility(View.GONE);
        } else {
            mNegativeView.setVisibility(View.VISIBLE);
            mNegativeView.setText(mNegativeText);
            mNegativeView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mNegativeListener.onClick(mNegativeView);
                    dismiss();
                }
            });
        }
    }

    public void setTitle(CharSequence title) {
        mTitle = title;
    }

    public void setContent(CharSequence content) {
        mContent = content;
    }

    public void setMonirContext(CharSequence minorContext) {
        mMinorContent = minorContext;
    }

    public void setPositiveButton(CharSequence text, View.OnClickListener listener) {
        mPositiveText = text;
        mPositiveListener = listener;
    }

    public void setNegativeButton(CharSequence text, View.OnClickListener listener) {
        mNegativeText = text;
        mNegativeListener = listener;
    }
}

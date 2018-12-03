package com.kinstalk.her.settings.view.views;

import android.app.Dialog;
import android.app.StatusBarManager;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.kinstalk.her.settings.R;

/**
 * Created by lenovo on 2017/10/31.
 */

public class HerProgressDialog extends Dialog {
    public HerProgressDialog(@NonNull Context context) {
        super(context);
    }

    public HerProgressDialog(@NonNull Context context, @StyleRes int themeResId) {
        super(context, themeResId);
    }

    protected HerProgressDialog(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    public void setContentMessage(CharSequence message) {
        if(!TextUtils.isEmpty(message)) {
            TextView content = (TextView)findViewById(R.id.content);
            content.setVisibility(View.VISIBLE);
            content.setText(message);
            content.invalidate();
        }
    }

    public void setDetailsMessage(CharSequence message) {
        TextView details = (TextView)findViewById(R.id.details);
        if(!TextUtils.isEmpty(message)) {
            details.setVisibility(View.VISIBLE);
            details.setText(message);
            details.invalidate();
        } else {
            details.setVisibility(View.INVISIBLE);
        }
    }
    public static HerProgressDialog show(Context context, CharSequence content) {
        return show(context, content, null);
    }

    public static HerProgressDialog show(Context context, CharSequence content, CharSequence details) {
        HerProgressDialog dialog = new HerProgressDialog(context, R.style.Custom_Progress);
        dialog.setContentView(R.layout.progress_dialog_custom);
        if (content == null || content.length() == 0) {
            dialog.findViewById(R.id.content).setVisibility(View.INVISIBLE);
        } else {
            TextView txt = (TextView) dialog.findViewById(R.id.content);
            txt.setVisibility(View.VISIBLE);
            txt.setText(content);
        }

        if (details == null || details.length() == 0) {
            dialog.findViewById(R.id.details).setVisibility(View.INVISIBLE);
        } else {
            TextView txt = (TextView) dialog.findViewById(R.id.details);
            txt.setVisibility(View.VISIBLE);
            txt.setText(details);
        }
        //set full screen
        int height = context.getResources().getDisplayMetrics().heightPixels;
        int width = context.getResources().getDisplayMetrics().widthPixels;

        Window dialogWindow = dialog.getWindow();
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialogWindow.getAttributes());

        lp.width = width;
        lp.height = height;
        dialogWindow.setAttributes(lp);

        dialog.show();

        StatusBarManager statusBarManager = (StatusBarManager) context.getSystemService(
                android.app.Service.STATUS_BAR_SERVICE);
        statusBarManager.disable(StatusBarManager.DISABLE_EXPAND);
        return dialog;
    }
}

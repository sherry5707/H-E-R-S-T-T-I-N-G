package com.kinstalk.her.settings.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.provider.ContactsContract;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.kinstalk.her.settings.R;

/**
 * Created by lenovo on 2017/10/20.
 */

public class ToastHelper {
    public static Toast makeText(Context context, Drawable icon, CharSequence text, int duration) {
        Toast toast = new Toast(context);

        LayoutInflater inflate = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflate.inflate(R.layout.view_toast, null);

        TextView toastText = (TextView)view.findViewById(R.id.toast_text);
        toastText.setText(text);

        ImageView toastIcon = (ImageView)view.findViewById(R.id.toast_icon);
        if(icon == null) {
            toastIcon.setVisibility(View.GONE);
        } else {
            toastIcon.setVisibility(View.VISIBLE);
            toastIcon.setImageDrawable(icon);
        }

        toast.setDuration(duration);
        toast.setGravity(Gravity.FILL, 0, 0);

        WindowManager.LayoutParams params = toast.getWindowParams();
        params.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN ;
        params.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        params.flags &= ~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        toast.setView(view);

        return toast;
    }

    public static Toast makeText(Context context, int textId, int duration)
            throws Resources.NotFoundException {
        return makeText(context, null, context.getResources().getText(textId), duration);
    }

    public static Toast makeText(Context context, int imgId, int textId, int duration) {
        return makeText(context, context.getResources().getDrawable(imgId, null),
                context.getResources().getText(textId), duration);
    }
}

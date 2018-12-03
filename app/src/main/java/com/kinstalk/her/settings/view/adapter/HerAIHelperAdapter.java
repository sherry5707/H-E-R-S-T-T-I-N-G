package com.kinstalk.her.settings.view.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.kinstalk.her.settings.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Zhigang Zhang on 2017/10/19.
 */

public class HerAIHelperAdapter extends BaseAdapter{
    private List<String> mAiTextList;

    public synchronized void clearSouces() {
        if( this.mAiTextList != null){
            this.mAiTextList.clear();
        }

        notifyDataSetChanged();
    }

    public synchronized void refreshUI() {
        // this.sources = sources;

        notifyDataSetChanged();
    }

    public synchronized void addAiText(String aiText){
        if (this.mAiTextList == null) {
            this.mAiTextList = new ArrayList<>();
        }

        this.mAiTextList.add(aiText);

        refreshUI();
    }

    public synchronized void addAiTestList(List<String> aiTexts) {
        if (this.mAiTextList == null) {
            this.mAiTextList = new ArrayList<>();
        }

        this.mAiTextList.addAll(aiTexts);

        refreshUI();
    }

    public synchronized void removeAiText(String aiText){
        if (this.mAiTextList == null) {
            return;
        }

        this.mAiTextList.remove(aiText);

        refreshUI();
    }

    @Override
    public synchronized int getCount() {
        return mAiTextList == null ? 0 : mAiTextList.size();
    }

    @Override
    public Object getItem(int position) {
        return mAiTextList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder = null;

        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.listitem_ai, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.aiText = (TextView) convertView.findViewById(R.id.ai_text);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        String helpText = (String)getItem(position);
        viewHolder.aiText.setText(helpText);
        return convertView;
    }

    public static class ViewHolder {
        public TextView aiText;
    }

}

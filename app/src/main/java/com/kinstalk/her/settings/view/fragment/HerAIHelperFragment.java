package com.kinstalk.her.settings.view.fragment;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.kinstalk.her.settings.R;
import com.kinstalk.her.settings.view.adapter.HerAIHelperAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by Zhigang Zhang on 2017/10/19.
 */

public class HerAIHelperFragment extends Fragment {
    private final static int READ_TRY_TO_SAY = 1;
    private Unbinder unbinder;

    @BindView(R.id.ai_listview)
    ListView mListView;

    HerAIHelperAdapter mAdapter;
    private Context mContext;

    public static HerAIHelperFragment getInstance() {
        HerAIHelperFragment herAiHelperFrag = new HerAIHelperFragment();
        return herAiHelperFrag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_settings_ai, container, false);
        unbinder = ButterKnife.bind(this, rootView);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAdapter = new HerAIHelperAdapter();
        mListView.setAdapter(mAdapter);
        handler.sendEmptyMessage(READ_TRY_TO_SAY);
    }

    @Override
    public void onAttach(Context context) {
        // Log.d(TAG,"onAttach");
        super.onAttach(context);
        mContext = context;
        if(mAdapter != null) {
            mAdapter.clearSouces();
            handler.sendEmptyMessage(READ_TRY_TO_SAY);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //add hardcode AI text, TODO ,read them from database

        //mAdapter.clearSouces();
        //handler.sendEmptyMessage(READ_TRY_TO_SAY);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        unbinder.unbind();
        super.onDestroyView();
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case READ_TRY_TO_SAY:
                    readTryToSay();
                    break;
            }
        }
    };

    private void readTryToSay() {
        List<String> tryToSayList = new ArrayList<>();

        try {
            InputStream is = mContext.getAssets().open("AISkills.txt");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            String text = new String(buffer, "utf-8");
            if(!TextUtils.isEmpty(text)) {
                tryToSayList.addAll(Arrays.asList(text.split(",")));
                Collections.shuffle(tryToSayList);
                mAdapter.addAiTestList(tryToSayList);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

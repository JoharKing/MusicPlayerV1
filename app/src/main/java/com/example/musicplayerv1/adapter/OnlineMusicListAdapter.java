package com.example.musicplayerv1.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.musicplayerv1.R;
import com.example.musicplayerv1.entity.Music;
import com.example.musicplayerv1.entity.OnlineMusic;
import com.example.musicplayerv1.ui.MainActivity;
import com.example.musicplayerv1.utils.Utils;

import java.io.IOException;
import java.util.List;


public class OnlineMusicListAdapter extends BaseAdapter {
    private List<OnlineMusic> mData;
    private final LayoutInflater mInflater;
    private final int mResource;
    private Context mContext;

    public OnlineMusicListAdapter(Context context, int resId, List<OnlineMusic> data)
    {
        mContext = context;
        mData = data;
        mInflater = LayoutInflater.from(context);
        mResource = resId;
    }

    @Override
    public int getCount() {
        return mData != null ? mData.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return mData != null ? mData.get(position): null ;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view;

        if (convertView == null) {
            view = mInflater.inflate(mResource, parent, false);
        }else {
            view = convertView;
        }

        OnlineMusic onlineMusic = mData.get(position);

        TextView title = (TextView) view.findViewById(R.id.lv_tv_titleol);
        title.setText(onlineMusic.getMusicName());

        TextView artist = (TextView) view.findViewById(R.id.lv_tv_artistol);
        artist.setText(onlineMusic.getMusicArtist());

        return view;
    }

}

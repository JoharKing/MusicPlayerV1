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
import com.example.musicplayerv1.ui.MainActivity;
import com.example.musicplayerv1.utils.Utils;
import com.example.musicplayerv1.entity.Music;

import java.util.List;

public class MusicListAdapter extends BaseAdapter {

    private List<Music> mData;
    private final LayoutInflater mInflater;
    private final int mResource;
    private Context mContext;

    public MusicListAdapter(Context context, int resId, List<Music> data)
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

        Music music = mData.get(position);

        TextView title = (TextView) view.findViewById(R.id.lv_tv_title);
        title.setText(music.getMusicName());

        TextView artist = (TextView) view.findViewById(R.id.lv_tv_artist);
        artist.setText(music.getMusicArtist());

        TextView duration = (TextView) view.findViewById(R.id.lv_tv_duration);

        //调用辅助函数转换时间格式
        String times = Utils.convertMSecendToTime(music.getMusicDuration());
        times = String.format("%s", times);
        duration.setText(times);

        ImageView album = (ImageView) view.findViewById(R.id.lv_iv_album);
        if(album != null) {
            if (music.getThumb() != null) {
                album.setImageBitmap(Utils.resizeImage(music.getThumb()));
            } else {
                album.setImageResource(R.drawable.logo);
            }
        }

        if (MainActivity.number == position) {// 如果当前的行就是ListView中选中的一行，就更改显示样式
//            convertView.setBackgroundColor(Color.rgb(38,188,213));// 更改整行的背景色
            title.setTextColor(Color.rgb(139,83,224));// 更改字体颜色
            artist.setTextColor(Color.rgb(139,83,224));
            duration.setTextColor(Color.rgb(139,83,224));
        }else {
            title.setTextColor(Color.BLACK);// 更改字体颜色
            artist.setTextColor(Color.BLACK);
            duration.setTextColor(Color.BLACK);
        }

        return view;
    }


}

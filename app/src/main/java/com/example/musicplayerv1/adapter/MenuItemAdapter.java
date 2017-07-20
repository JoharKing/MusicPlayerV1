package com.example.musicplayerv1.adapter;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;

import android.support.v4.widget.TextViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;


import com.example.musicplayerv1.R;
import com.example.musicplayerv1.widget.LvMenuItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MenuItemAdapter extends BaseAdapter {
    private final int mIconSize;
    private LayoutInflater mInflater;
    private Context mContext;

    public MenuItemAdapter(Context context) {
        mInflater = LayoutInflater.from(context);
        mContext = context;

        mIconSize = context.getResources().getDimensionPixelSize(R.dimen.drawer_icon_size);
    }

    private List<LvMenuItem> mItems = new ArrayList<LvMenuItem>(
            Arrays.asList(
                    new LvMenuItem(R.drawable.topmenu_ic_themes, "主题换肤"),
                    new LvMenuItem(R.drawable.topmenu_ic_sleepmode, "睡眠模式"),
                    new LvMenuItem(R.drawable.topmenu_ic_playmode, "播放模式"),
                    new LvMenuItem(R.drawable.topmenu_ic_about, "关于"),
                    new LvMenuItem(R.drawable.topmenu_ic_quit, "退出")

            ));


    @Override
    public int getCount() {
        return mItems.size();
    }


    @Override
    public Object getItem(int position) {
        return mItems.get(position);
    }


    @Override
    public long getItemId(int position) {
        return position;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LvMenuItem item = mItems.get(position);
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.design_drawer_item, parent,
                    false);
        }
        TextView itemView = (TextView) convertView;
        itemView.setText(item.name);
        Drawable icon = mContext.getResources().getDrawable(item.icon);
        // setIconColor(icon);
        if (icon != null) {
            icon.setBounds(0, 0, mIconSize, mIconSize);
            TextViewCompat.setCompoundDrawablesRelative(itemView, icon, null, null, null);
        }

        return convertView;
    }

}
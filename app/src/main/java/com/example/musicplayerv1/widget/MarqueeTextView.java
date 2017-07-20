package com.example.musicplayerv1.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;


public class MarqueeTextView extends TextView {
    public MarqueeTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public MarqueeTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    public MarqueeTextView(Context context) {
        super(context);
    }
    @Override
    //是否已获取焦点
    public boolean isFocused() {
        return true;
    }
    //将isFocused强制每次都返回true，也就是说组件永远都处于已获取焦点的状态下。
}

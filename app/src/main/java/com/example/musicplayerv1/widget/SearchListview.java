package com.example.musicplayerv1.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;

//解决ListView和ScrollView的冲突
public class SearchListview extends ListView {
    public SearchListview(Context context) {
        super(context);
    }

    public SearchListview(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SearchListview(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    //通过复写其onMeasure方法、达到对ScrollView适配的效果

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int expandSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2,
                MeasureSpec.AT_MOST);
        super.onMeasure(widthMeasureSpec, expandSpec);
    }

}

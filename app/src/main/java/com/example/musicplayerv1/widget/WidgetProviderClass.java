package com.example.musicplayerv1.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.example.musicplayerv1.R;
import com.example.musicplayerv1.ui.MainActivity;
import com.example.musicplayerv1.Service.MusicService;

public class WidgetProviderClass extends AppWidgetProvider {

    //广播标识
    public static final String BROADCAST_MUSICSERVICE_CONTROL = "MusicService.ACTION_CONTROL";
    public static final String BROADCAST_MUSICSERVICE_UPDATE_STATUS = "MusicService.ACTION_UPDATE";

    //请求码，发送不同广播
    public static final int REQUET_STARTACTIVITY = 0;
    public static final int REQUEST_PLAY = 1;
    public static final int REQUEST_PAUSE = 2;
    public static final int REQUEST_NEXT = 3;
    public static final int REQUEST_PREVIOUS = 4;

    //播放状态
    private int status;
    private RemoteViews remoteViews = null;  //获得不同进程的对象
    private String musicName = null;
    private String musicArtist = null;

    //第一个widget被创建时调用
    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
    }

    //接收广播的回调函数
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if(intent.getAction().equals(BROADCAST_MUSICSERVICE_UPDATE_STATUS)){
            status = intent.getIntExtra("status", -1);
            remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
            switch (status){
                case MusicService.STATUS_PLAYING:
                    //获取歌名，艺术家名
                    musicName = intent.getStringExtra("musicName");
                    musicArtist = intent.getStringExtra("musicArtist");

                    //修改标题及按钮图片
                    remoteViews.setTextViewText(R.id.tv_widget_title, musicName + " " + musicArtist);
                    remoteViews.setImageViewResource(R.id.ibt_widget_play, R.drawable.button_pause);

                    //播放状态时，点击播放/暂停按钮，发送待暂停指令的广播
                    Intent intentToPause = new Intent(BROADCAST_MUSICSERVICE_CONTROL);
                    intentToPause.putExtra("command", MusicService.COMMAND_PAUSE);
                    PendingIntent pendingIntentToPause = PendingIntent.getBroadcast(context,REQUEST_PAUSE,intentToPause,
                            PendingIntent.FLAG_UPDATE_CURRENT);
                    remoteViews.setOnClickPendingIntent(R.id.ibt_widget_play, pendingIntentToPause);
                    break;
                case MusicService.STATUS_PAUSED:
                    //修改按钮图片
                    remoteViews.setImageViewResource(R.id.ibt_widget_play, R.drawable.button_play);

                    //暂停状态时，点击播放/暂停按钮，发送待播放指令的广播
                    Intent intentToPlay = new Intent(BROADCAST_MUSICSERVICE_CONTROL);
                    intentToPlay.putExtra("command", MusicService.COMMAND_RESUME);
                    PendingIntent pendingIntentToPlay = PendingIntent.getBroadcast(context,REQUEST_PLAY,intentToPlay,
                            PendingIntent.FLAG_UPDATE_CURRENT);
                    remoteViews.setOnClickPendingIntent(R.id.ibt_widget_play, pendingIntentToPlay);
                    break;
                case MusicService.STATUS_STOPPED:
                    //修改标题及按钮图片
                    remoteViews.setImageViewResource(R.id.ibt_widget_play, R.drawable.button_play);
                    remoteViews.setTextViewText(R.id.tv_widget_title, "RnDPlayer");
                    break;
                default:
                    break;
            }

            //将界面显示到插件中，更新状态
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName componentName = new ComponentName(context, WidgetProviderClass.class);
            appWidgetManager.updateAppWidget(componentName, remoteViews);
        }
    }

    //更新widget时被调用
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

        //发送广播，检查状态
        Intent intent = new Intent(BROADCAST_MUSICSERVICE_CONTROL);
        intent.putExtra("command",MusicService.COMMAND_CHECK_IS_PLAYING);
        context.sendBroadcast(intent);

        //点击标题
        Intent intentOnTitle = new Intent();
        intent.setClass(context, MainActivity.class);
        PendingIntent pendingIntentOnTitle = PendingIntent.getActivity(context,REQUET_STARTACTIVITY,
                intentOnTitle, PendingIntent.FLAG_UPDATE_CURRENT);

        //点击下一首,发送下一首指令
        Intent intentOnNext = new Intent(BROADCAST_MUSICSERVICE_CONTROL);
        intentOnNext.putExtra("command",MusicService.COMMAND_NEXT);
        PendingIntent pendingIntentOnNext = PendingIntent.getBroadcast(context,REQUEST_NEXT,
                intentOnNext, PendingIntent.FLAG_UPDATE_CURRENT);

        //点击上一首，发送上一首指令
        Intent intentOnPre = new Intent(BROADCAST_MUSICSERVICE_CONTROL);
        intentOnNext.putExtra("command",MusicService.COMMAND_PREVIOUS);
        PendingIntent pendingIntentOnPre = PendingIntent.getBroadcast(context,REQUEST_PREVIOUS,
                intentOnPre, PendingIntent.FLAG_UPDATE_CURRENT);

        //加入按钮的事件响应
        remoteViews.setOnClickPendingIntent(R.id.tv_widget_title,pendingIntentOnTitle);
        remoteViews.setOnClickPendingIntent(R.id.ibt_widget_presong,pendingIntentOnPre);
        remoteViews.setOnClickPendingIntent(R.id.ibt_widget_next,pendingIntentOnNext);
        appWidgetManager.updateAppWidget(appWidgetIds, remoteViews);
    }

    //在widget被删除时调用
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
    }

    //最后一个widget被删除时调用

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
    }
}

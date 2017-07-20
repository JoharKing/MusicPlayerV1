package com.example.musicplayerv1.Service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.example.musicplayerv1.R;
import com.example.musicplayerv1.ui.MainActivity;
import com.example.musicplayerv1.entity.MusicList;
import com.example.musicplayerv1.ui.SearchOnlineActivity;

public class MusicService extends Service {

    //播放控制命令，标识操作
    public static final int COMMAND_UNKNOW = -1;
    public static final int COMMAND_PLAY = 0;
    public static final int COMMAND_PAUSE = 1;
    public static final int COMMAND_STOP = 2;
    public static final int COMMAND_RESUME = 3;
    public static final int COMMAND_PREVIOUS = 4;
    public static final int COMMAND_NEXT = 5;
    public static final int COMMAND_CHECK_IS_PLAYING = 6;
    public static final int COMMAND_SEEK_TO = 7;
    public static final int COMMAND_PLAY_ONLINE = 0;
    //播放器状态
    public static final int STATUS_PLAYING = 0;
    public static final int STATUS_PAUSED = 1;
    public static final int STATUS_STOPPED = 2;
    public static final int STATUS_COMPLETED = 3;
    public static final int STATUS_PLAYING_ONLINE = 4;
    public static final int STATUS_PAUSED_ONLINE = 5;
    public static final int STATUS_STOPPED_ONLINE = 6;
    public static final int STATUS_COMPLETED_ONLINE = 7;
    //广播标识
    public static final String BROADCAST_MUSICSERVICE_CONTROL = "MusicService.ACTION_CONTROL";
    public static final String BROADCAST_MUSICSERVICE_UPDATE_STATUS = "MusicService.ACTION_UPDATE";
    public static final String BROADCAST_MUSICSERVICE_CONTROL_ONLINE = "MusicService.ACTION_CONTROL_ONLINE";

    //歌曲序号，从0开始
    private int number = 0;
    private int numberOnline = 0;
    public static int status;
    //媒体播放类
    private MediaPlayer player = new MediaPlayer();

    private NotificationManager manager;
    //广播接收器
    private CommandReceiver receiver;

    private boolean phone = false;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //绑定广播接收器，可以接收广播
        bindCommandReceiver();
        status = MusicService.STATUS_STOPPED;
        TelephonyManager telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(new MyPhoneListener(), PhoneStateListener.LISTEN_CALL_STATE);
    }

    @Override
    public void onDestroy() {
        sendBroadcastOnStatusChanged(MusicService.STATUS_STOPPED);
        if (manager != null){
            manager.cancel(1);
        }
        if(receiver != null){
            unregisterReceiver(receiver);
            receiver = null;
        }
        //释放播放器资源
        if(player != null){
            player.release();
        }
        super.onDestroy();
    }

    //绑定广播接收器
    private void bindCommandReceiver(){
        receiver = new CommandReceiver();
        IntentFilter filter = new IntentFilter();
        //用Intent的putExtra不能进行区分
        filter.addAction(BROADCAST_MUSICSERVICE_CONTROL);
        filter.addAction(BROADCAST_MUSICSERVICE_CONTROL_ONLINE);
        filter.addAction("RESUME");
        filter.addAction("PAUSE");
        filter.addAction("PREVIOUS");
        filter.addAction("NEXT");
        filter.addAction("CLOSE");
        filter.addAction("SLEEPTIME");
        registerReceiver(receiver, filter);
    }
    //发送广播，提醒状态改变了
    private void sendBroadcastOnStatusChanged(int status){
        Intent intent = new Intent(BROADCAST_MUSICSERVICE_UPDATE_STATUS);
        intent.putExtra("status", status);
        if(status != STATUS_STOPPED && status != STATUS_STOPPED_ONLINE){
            intent.putExtra("time", player.getCurrentPosition());
            intent.putExtra("duration", player.getDuration());

            if(MusicList.getMusicList().size() != 0){
                if(number < 0){
                    number = 0;
                }
                intent.putExtra("number",number);
                intent.putExtra("musicName",MusicList.getMusicList().get(number).getMusicName());
                intent.putExtra("musicArtist",MusicList.getMusicList().get(number).getMusicArtist());
            }

            if(MusicList.getOnlineMusicsList().size() != 0){
                intent.putExtra("numberOnline",numberOnline);
                intent.putExtra("musicNameOnline",MusicList.getOnlineMusicsList().get(numberOnline).getMusicName());
                intent.putExtra("musicArtistOnline",MusicList.getOnlineMusicsList().get(numberOnline).getMusicArtist());
            }

        }
        sendBroadcast(intent);
    }

    //内部类，接收广播命令，并执行操作
    class CommandReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String ctrlCode = intent.getAction();

            if(BROADCAST_MUSICSERVICE_CONTROL.equals(ctrlCode)){
                //获取命令
                int command = intent.getIntExtra("command", COMMAND_UNKNOW);
                //执行命令
                switch (command){
                    case COMMAND_SEEK_TO:
                        if(status == STATUS_PLAYING || status == STATUS_PAUSED
                                || status == STATUS_STOPPED || status == STATUS_COMPLETED){
                            seekTo(intent.getIntExtra("time", 0));
                        }else {
                            seekToOnline(intent.getIntExtra("time", 0));
                        }
                        break;
                    case COMMAND_PLAY:
                        if(status == STATUS_PLAYING || status == STATUS_PAUSED
                                || status == STATUS_STOPPED || status == STATUS_COMPLETED){
                            number = intent.getIntExtra("number", 0);
                            play(number);
                        }else {
                            numberOnline = intent.getIntExtra("number", 0);
                            playOnline(numberOnline);
                        }
//                        number = intent.getIntExtra("number", 0);
//                        play(number);

                        Log.d("MusicService", "splay");
                        break;
                    case COMMAND_PREVIOUS:
                        if(status == STATUS_PLAYING || status == STATUS_PAUSED
                                || status == STATUS_STOPPED || status == STATUS_COMPLETED){
                            moveNumberToPrevious();
                        }else {
                            moveNumberToPreviousOnline();
                        }
                        Log.d("MusicService", "sprevious");
                        break;
                    case COMMAND_NEXT:
                        if(status == STATUS_PLAYING || status == STATUS_PAUSED
                                || status == STATUS_STOPPED || status == STATUS_COMPLETED){
                            moveNumberToNext();
                        }else {
                            moveNumberToNextOnline();
                        }
                        Log.d("MusicService", "snext");

                        break;
                    case COMMAND_PAUSE:
                        if(status == STATUS_PLAYING || status == STATUS_PAUSED
                                || status == STATUS_STOPPED || status == STATUS_COMPLETED){
                            pause();
                        }else {
                            pauseOnline();
                        }
                        Log.d("MusicService", "spause");

                        break;
                    case COMMAND_STOP:
                        if(status == STATUS_PLAYING || status == STATUS_PAUSED
                                || status == STATUS_STOPPED || status == STATUS_COMPLETED){
                            stop();
                        }else {
                            stopOnline();
                        }
                        Log.d("MusicService", "sstop");

                        break;
                    case COMMAND_RESUME:
                        if(status == STATUS_PLAYING || status == STATUS_PAUSED
                                || status == STATUS_STOPPED || status == STATUS_COMPLETED){
                            resume();
                        }else {
                            resumeOnline();
                        }
                        Log.d("MusicService", "sresume");

                        break;
                    case COMMAND_CHECK_IS_PLAYING:
                        if(status == STATUS_PLAYING || status == STATUS_PAUSED
                                || status == STATUS_STOPPED || status == STATUS_COMPLETED){
                            if(player != null && player.isPlaying()){
                                sendBroadcastOnStatusChanged(MusicService.STATUS_PLAYING);
                            }
                        }else {
                            if(player != null && player.isPlaying()){
                                sendBroadcastOnStatusChanged(MusicService.STATUS_PLAYING_ONLINE);
                            }
                        }
                        break;
                    case COMMAND_UNKNOW:
                    default:
                        break;
                }
            }else if(BROADCAST_MUSICSERVICE_CONTROL_ONLINE.equals(ctrlCode)){
                //获取命令
                int command = intent.getIntExtra("command", COMMAND_UNKNOW);
                //执行命令
                switch (command){
                    case COMMAND_PLAY_ONLINE:
                        number = 1;
                        MainActivity.number = -1;
                        numberOnline = intent.getIntExtra("numberOnline", 0);
                        playOnline(numberOnline);
                        break;
                    default:
                        break;
                }
            }
            else if("RESUME".equals(ctrlCode)){
                resume();
            }else if("PAUSE".equals(ctrlCode)){
                pause();
            }else if("PREVIOUS".equals(ctrlCode)){
                moveNumberToPrevious();
            }else if("NEXT".equals(ctrlCode)){
                moveNumberToNext();
            }else if("CLOSE".equals(ctrlCode)){
                if (manager != null){
                    manager.cancel(1);
                }
                System.exit(0);
            }
        }
    }

    //读取音乐
    private void load(int number){
        try{
            Log.d("MusicService", "loadf");
            player.reset();
            player.setDataSource(MusicList.getMusicList().get(number).getMusicPath());
            player.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //注册监听器
        player.setOnCompletionListener(completionListener);
    }
    private void loadOnline(int numberOnline){
        try{
            player.reset();
            player.setDataSource(MusicList.getOnlineMusicsList().get(numberOnline).getMusicPath());
            player.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //注册监听器
        player.setOnCompletionListener(completionListener);
    }

    //播放结束监听器
    MediaPlayer.OnCompletionListener completionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            if(player.isLooping()){
                replay();
            }else {
                sendBroadcastOnStatusChanged(MusicService.STATUS_COMPLETED);
            }
        }
    };

    //选择下一首
    private void moveNumberToNext(){
        if(MainActivity.playmode == MainActivity.MODE_RANDOM_PLAY){
            MainActivity.number = (int) (Math.random() * MusicList.getMusicList().size());
            number = (int) (Math.random() * MusicList.getMusicList().size());
        }else{
            //判断是否到达底端
            if((number) == MusicList.getMusicList().size()-1){
                Toast.makeText(MusicService.this, MusicService.this.getString(R.string.tip_reach_bottom),
                        Toast.LENGTH_SHORT).show();
                number = 0;
            }else {
                ++number;
            }
        }
        play(number);
    }
    //选择下一首
    private void moveNumberToNextOnline(){
        if(MainActivity.playmode == MainActivity.MODE_RANDOM_PLAY){
            SearchOnlineActivity.numberOnline = (int) (Math.random() * MusicList.getOnlineMusicsList().size());
            numberOnline = (int) (Math.random() * MusicList.getOnlineMusicsList().size());
        }else{
            //判断是否到达底端
            if((numberOnline) == MusicList.getOnlineMusicsList().size()-1){
                Toast.makeText(MusicService.this, MusicService.this.getString(R.string.tip_reach_bottom),
                        Toast.LENGTH_SHORT).show();
                numberOnline = 0;
            }else {
                ++numberOnline;
            }
        }
        playOnline(numberOnline);
    }
    //上一曲
    private void moveNumberToPrevious(){
        if(MainActivity.playmode == MainActivity.MODE_RANDOM_PLAY){
            MainActivity.number = (int) (Math.random() * MusicList.getMusicList().size());
            number = (int) (Math.random() * MusicList.getMusicList().size());
        }else{
            //判断是否到达列表顶部
            if( number == 0 ){
                Toast.makeText(MusicService.this, MusicService.this.getString(R.string.tip_reach_top),
                        Toast.LENGTH_SHORT).show();
                number = MusicList.getMusicList().size()-1;
            }else {
                --number;
            }
        }
        play(number);
    }
    private void moveNumberToPreviousOnline(){
        if(MainActivity.playmode == MainActivity.MODE_RANDOM_PLAY){
            SearchOnlineActivity.numberOnline = (int) (Math.random() * MusicList.getOnlineMusicsList().size());
            numberOnline = (int) (Math.random() * MusicList.getOnlineMusicsList().size());
        }else{
            //判断是否到达列表顶部
            if( numberOnline == 0 ){
                Toast.makeText(MusicService.this, MusicService.this.getString(R.string.tip_reach_top),
                        Toast.LENGTH_SHORT).show();
                numberOnline = MusicList.getOnlineMusicsList().size()-1;
            }else {
                --numberOnline;
            }
        }
        playOnline(numberOnline);
    }
    //播放音乐
    private void play(int number){
        //停止当前播放
        if(player!=null && player.isPlaying()){
            player.stop();
        }
        if (MainActivity.number == -1){
            number = 0;
        }
        load(number);
        player.start();
        status = MusicService.STATUS_PLAYING;
        sendBroadcastOnStatusChanged(MusicService.STATUS_PLAYING);
        updateNotification();
    }
    private void playOnline(int numberOnline){
        //停止当前播放
        if(player!=null && player.isPlaying()){
            player.stop();
        }
        loadOnline(numberOnline);
        player.start();
        status = MusicService.STATUS_PLAYING_ONLINE;
        sendBroadcastOnStatusChanged(MusicService.STATUS_PLAYING_ONLINE);
        updateNotification();
    }
    //跳转至播放位置
    private void seekTo(int time){
        player.seekTo(time);
        if(status == MusicService.STATUS_PAUSED){
        }else {
            status = MusicService.STATUS_PLAYING;
            sendBroadcastOnStatusChanged(MusicService.STATUS_PLAYING);
        }
        updateNotification();
    }
    private void seekToOnline(int time){
        player.seekTo(time);
        if(status == MusicService.STATUS_PAUSED_ONLINE){
        }else {
            status = MusicService.STATUS_PLAYING_ONLINE;
            sendBroadcastOnStatusChanged(MusicService.STATUS_PLAYING_ONLINE);
        }
        updateNotification();
    }
    //暂停音乐
    private void pause(){
        if(player.isPlaying()){
            player.pause();
            status = MusicService.STATUS_PAUSED;
            sendBroadcastOnStatusChanged(MusicService.STATUS_PAUSED);
            updateNotification();
        }
    }
    private void pauseOnline(){
        if(player.isPlaying()){
            player.pause();
            status = MusicService.STATUS_PAUSED_ONLINE;
            sendBroadcastOnStatusChanged(MusicService.STATUS_PAUSED_ONLINE);
            updateNotification();
        }
    }
    //停止播放
    private void stop(){
        if(status != MusicService.STATUS_STOPPED){
            player.stop();
            status = MusicService.STATUS_STOPPED;
            sendBroadcastOnStatusChanged(MusicService.STATUS_STOPPED);
            updateNotification();
        }
    }
    private void stopOnline(){
        if(status != MusicService.STATUS_STOPPED_ONLINE){
            player.stop();
            status = MusicService.STATUS_STOPPED_ONLINE;
            sendBroadcastOnStatusChanged(MusicService.STATUS_STOPPED_ONLINE);
            updateNotification();
        }
    }
    //恢复播放(暂停后)
    private void resume(){
        player.start();
        status = MusicService.STATUS_PLAYING;
        sendBroadcastOnStatusChanged(MusicService.STATUS_PLAYING);
        updateNotification();
    }
    private void resumeOnline(){
        player.start();
        status = MusicService.STATUS_PLAYING_ONLINE;
        sendBroadcastOnStatusChanged(MusicService.STATUS_PLAYING_ONLINE);
        updateNotification();
    }
    //重新播放（播放完成后）
    private void replay(){
        player.start();
        status = MusicService.STATUS_PLAYING;
        sendBroadcastOnStatusChanged(MusicService.STATUS_PLAYING);
        updateNotification();
    }
    private void replayOnline(){
        player.start();
        status = MusicService.STATUS_PLAYING_ONLINE;
        sendBroadcastOnStatusChanged(MusicService.STATUS_PLAYING_ONLINE);
        updateNotification();
    }

    private class MyPhoneListener extends PhoneStateListener {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            switch (state){
                case TelephonyManager.CALL_STATE_RINGING:
                    if(status == MusicService.STATUS_PLAYING){
                        pause();
                        phone = true;
                    }
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    if(phone == true){
                        resume();
                        phone = false;
                    }
                    break;
            }
        }
    }

    public void updateNotification(){
        manager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        android.support.v4.app.NotificationCompat.Builder builder = new android.support.v4.app.NotificationCompat.Builder(this);
        //通过RemoteViews获取自定义布局
        RemoteViews views = new RemoteViews(getPackageName(),R.layout.statusbar);
        //设置图片，文本
        views.setImageViewResource(R.id.iv_notice_icon,R.drawable.logo);
        views.setTextViewText(R.id.tv_notice_musicname, MusicList.getMusicList().get(number).getMusicName());
        views.setTextViewText(R.id.tv_notice_musicartist, MusicList.getMusicList().get(number).getMusicArtist());
        //根据播放状态显示按钮
        if(status == MusicService.STATUS_PLAYING){
            views.setViewVisibility(R.id.ibt_notice_play, View.GONE);
            views.setViewVisibility(R.id.ibt_notice_pause, View.VISIBLE);
        }else {
            views.setViewVisibility(R.id.ibt_notice_play, View.VISIBLE);
            views.setViewVisibility(R.id.ibt_notice_pause, View.GONE);
        }
        //监听按钮
        views.setOnClickPendingIntent(R.id.ibt_notice_previous,PendingIntentToPre());
        views.setOnClickPendingIntent(R.id.ibt_notice_play,PendingIntentToPlay());
        views.setOnClickPendingIntent(R.id.ibt_notice_pause,PendingIntentToPause());
        views.setOnClickPendingIntent(R.id.ibt_notice_next,PendingIntentToNext());
        views.setOnClickPendingIntent(R.id.ibt_notice_close,PendingIntentToClose());

        Intent intent = new Intent(this,MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this,0,intent,0);
        builder.setContent(views)
                .setContentIntent(pi)
                .setPriority(Notification.PRIORITY_MAX)
                .setOngoing(true)
                .setSmallIcon(R.drawable.logo);

        Notification notification = builder.build();
        notification.flags = Notification.FLAG_ONGOING_EVENT;
        manager.notify(1, notification);
    }

    //设置具体监听事件
    private PendingIntent PendingIntentToClose(){
        Intent intent = new Intent("CLOSE");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this,0,intent,0);
        return pendingIntent;
    }
    private PendingIntent PendingIntentToNext(){
        Intent intent = new Intent("NEXT");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this,0,intent,0);
        return pendingIntent;
    }
    private PendingIntent PendingIntentToPause(){
        Intent intent = new Intent("PAUSE");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this,0,intent,0);
        return pendingIntent;
    }
    private PendingIntent PendingIntentToPlay(){
        Intent intent = new Intent("RESUME");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this,0,intent,0);
        return pendingIntent;
    }
    private PendingIntent PendingIntentToPre(){
        Intent intent = new Intent("PREVIOUS");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this,0,intent,0);
        return pendingIntent;
    }
}

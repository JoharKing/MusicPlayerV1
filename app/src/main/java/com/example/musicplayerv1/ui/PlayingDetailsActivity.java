package com.example.musicplayerv1.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.musicplayerv1.R;
import com.example.musicplayerv1.Service.MusicService;
import com.example.musicplayerv1.entity.MusicList;
import com.example.musicplayerv1.https.APIsData;
import com.example.musicplayerv1.https.LrcParser;
import com.example.musicplayerv1.utils.Utils;
import com.example.musicplayerv1.widget.LyricView;

import static com.example.musicplayerv1.https.LrcParser.lyricInfo;
import static com.example.musicplayerv1.ui.MainActivity.MODE_LIST_CYCLE;
import static com.example.musicplayerv1.ui.MainActivity.MODE_LIST_SEQUENCE;
import static com.example.musicplayerv1.ui.MainActivity.MODE_RANDOM_PLAY;
import static com.example.musicplayerv1.ui.MainActivity.MODE_SINGLE_CYCLE;
import static com.example.musicplayerv1.ui.MainActivity.number;
import static com.example.musicplayerv1.ui.MainActivity.playmode;

public class PlayingDetailsActivity extends AppCompatActivity {

    private ImageButton mIbtPrevious;
    private ImageButton mIbtPlay;
    private ImageButton mIbtStop;
    private ImageButton mIbtNext;
    private ImageButton mIbtPlayMode;

    private TextView mTitle;
    private TextView mArtist;

    private TextView mTextCurrent;
    private TextView mTextDuration;
    private SeekBar mSeekBar;
    private LyricView mLrcTextView;
    //播放状态
    private int status;
    //广播接收器
    private StatusChangedReceiver receiver;
    //当前歌曲的进度和总时长
    private int duration;
    private int time;

    //更新进度条的Handler
    private Handler seekBarHandler;
    private Handler lrcViewHandler;
    //进度条控制常量
    private static final int PROGRESS_INCREASE = 3;
    private static final int PROGRESS_PAUSE = 4;
    private static final int PROGRESS_RESET = 5;
    private static final int PROGRESS_RUN = 6;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.playing_details_layout);

        Intent intent = getIntent();
        time = intent.getIntExtra("currentTime",0);

        //替换成ToolBar
        Toolbar toolbar = (Toolbar) findViewById(R.id.play_activity_toolbar);
        setSupportActionBar(toolbar);

        findViews();
        registerListeners();
        bindStatusChangedReceiver();
        initSeekBarHandler();
        sendBroadcastOnCommand(MusicService.COMMAND_CHECK_IS_PLAYING);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sendBroadcastOnCommand(MusicService.COMMAND_CHECK_IS_PLAYING);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (receiver != null) {
            unregisterReceiver(receiver);
            receiver = null;
        }

    }

    private void showLrc(int songID){
        LrcParser.askLrc(songID);
//        Toast.makeText(MainActivity.this,lyricInfo.song_lines.size(),Toast.LENGTH_SHORT).show();
        StringBuilder stringBuilder = new StringBuilder();
        if(lyricInfo != null && lyricInfo.song_lines != null) {
            int size = lyricInfo.song_lines.size();
            for (int i = 0; i < size; i ++) {
                stringBuilder.append(lyricInfo.song_lines.get(i).content + "\n");
            }
//            mLrcText.setText(stringBuilder.toString());
            mLrcTextView.setText(stringBuilder.toString());
        }
    }

    //绑定广播接收器
    private void bindStatusChangedReceiver() {
        receiver = new StatusChangedReceiver();
        IntentFilter filter = new IntentFilter(MusicService.BROADCAST_MUSICSERVICE_UPDATE_STATUS);
        registerReceiver(receiver, filter);
    }


    //内部类，用于播放器状态更新的接收广播
    class StatusChangedReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            //获取播放器状态
            status = intent.getIntExtra("status", -1);
            String musicName = intent.getStringExtra("musicName");
            String musicArtist = intent.getStringExtra("musicArtist");
            String musicNameOnline = intent.getStringExtra("musicNameOnline");
            String musicArtistOnline = intent.getStringExtra("musicArtistOnline");
            switch (status) {
                case MusicService.STATUS_PLAYING:
                    seekBarHandler.removeMessages(PROGRESS_INCREASE);//防止队列中increase消息重复
                    seekBarHandler.removeMessages(PROGRESS_RUN);//防止队列中increase消息重复
                    time = intent.getIntExtra("time", 0);
                    duration = intent.getIntExtra("duration", 0);
                    number = intent.getIntExtra("number", number);
                    mSeekBar.setProgress(time);
                    mSeekBar.setMax(duration);
                    seekBarHandler.sendEmptyMessageDelayed(PROGRESS_INCREASE, 1000);
                    String URL = APIsData.buildURLByKeyword(musicName,"1");
                    APIsData.getSongs(URL);
                    showLrc(APIsData.searchSongID);
                    seekBarHandler.sendEmptyMessageDelayed(PROGRESS_RUN, 1000);
                    mTextDuration.setText(Utils.convertMSecendToTime(duration));

                    mIbtPlay.setBackgroundResource(R.drawable.big_pause);
                    //顶部文本提示
                    mTitle.setText(musicName);
                    mArtist.setText(musicArtist);

                    break;
                case MusicService.STATUS_PAUSED:
                    seekBarHandler.sendEmptyMessage(PROGRESS_PAUSE);

                    mTitle.setText(musicName);
                    mArtist.setText(musicArtist);

                    mIbtPlay.setBackgroundResource(R.drawable.big_play);
                    break;
                case MusicService.STATUS_STOPPED:
                    time = 0;
                    duration = 0;
                    number = -1;
                    mTextCurrent.setText(Utils.convertMSecendToTime(time));
                    mTextDuration.setText(Utils.convertMSecendToTime(duration));
                    seekBarHandler.sendEmptyMessage(PROGRESS_RESET);

                    mTitle.setText("");
                    mArtist.setText("");
                    mIbtPlay.setBackgroundResource(R.drawable.big_play);
//                    mBottomAlbum.setImageResource(R.drawable.bg_bottom_album);
                    break;
                case MusicService.STATUS_COMPLETED:
                    number = intent.getIntExtra("number", 0);
                    if (playmode == MODE_LIST_SEQUENCE) {         //顺序播放
                        if (number == MusicList.getMusicList().size() - 1)
                            sendBroadcastOnCommand(MusicService.COMMAND_STOP);
                        else
                            sendBroadcastOnCommand(MusicService.COMMAND_NEXT);
                    } else if (playmode == MainActivity.MODE_SINGLE_CYCLE) {     //单曲循环
                        sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
                    } else if (playmode == MainActivity.MODE_LIST_CYCLE) {       //列表循环
                        if (number == MusicList.getMusicList().size() - 1) {
                            number = 0;
                            sendBroadcastOnCommand(MusicService.COMMAND_STOP);
                        } else
                            sendBroadcastOnCommand(MusicService.COMMAND_NEXT);
                    } else if (playmode == MainActivity.MODE_RANDOM_PLAY) {      //随机播放
                        number = (int) (Math.random() * MusicList.getMusicList().size());
                        sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
                    }
                    seekBarHandler.sendEmptyMessage(PROGRESS_RESET);

                    mTitle.setText("");
                    mArtist.setText("");
                    mIbtPlay.setBackgroundResource(R.drawable.big_play);
                    break;
                case MusicService.STATUS_PLAYING_ONLINE:
                    seekBarHandler.removeMessages(PROGRESS_INCREASE);//防止队列中increase消息重复
                    seekBarHandler.removeMessages(PROGRESS_RUN);//防止队列中increase消息重复
                    time = intent.getIntExtra("time", 0);
                    duration = intent.getIntExtra("duration", 0);
                    SearchOnlineActivity.numberOnline = intent.getIntExtra("numberOnline", SearchOnlineActivity.numberOnline);
                    mSeekBar.setProgress(time);
                    mSeekBar.setMax(duration);
                    seekBarHandler.sendEmptyMessageDelayed(PROGRESS_INCREASE, 1000);

                    showLrc(MusicList.getOnlineMusicsList().get(SearchOnlineActivity.numberOnline).getSongID());
                    seekBarHandler.sendEmptyMessageDelayed(PROGRESS_RUN, 1000);
                    mTextDuration.setText(Utils.convertMSecendToTime(duration));
//                    bottomAlbum.setImageBitmap(Utils.resizeImage(MusicList.getMusicList().get(SearchOnlineActivity.numberOnline).getThumb()));

                    mIbtPlay.setBackgroundResource(R.drawable.big_pause);

                    mTitle.setText(musicNameOnline);
                    mArtist.setText(musicArtistOnline);

                    break;
                case MusicService.STATUS_PAUSED_ONLINE:
                    seekBarHandler.sendEmptyMessage(PROGRESS_PAUSE);

                    mTitle.setText(musicNameOnline);
                    mArtist.setText(musicArtistOnline);

                    mIbtPlay.setBackgroundResource(R.drawable.big_play);
                    break;
                case MusicService.STATUS_STOPPED_ONLINE:
                    time = 0;
                    duration = 0;
                    SearchOnlineActivity.numberOnline = -1;
                    mTextCurrent.setText(Utils.convertMSecendToTime(time));
                    mTextDuration.setText(Utils.convertMSecendToTime(duration));
                    seekBarHandler.sendEmptyMessage(PROGRESS_RESET);

                    mTitle.setText("");
                    mArtist.setText("");

                    mIbtPlay.setBackgroundResource(R.drawable.big_play);

                    break;
                case MusicService.STATUS_COMPLETED_ONLINE:
                    SearchOnlineActivity.numberOnline = intent.getIntExtra("number", 0);
                    if (playmode == MODE_LIST_SEQUENCE) {         //顺序播放
                        if (SearchOnlineActivity.numberOnline == MusicList.getOnlineMusicsList().size() - 1)
                            sendBroadcastOnCommand(MusicService.COMMAND_STOP);
                        else
                            sendBroadcastOnCommand(MusicService.COMMAND_NEXT);
                    } else if (playmode == MainActivity.MODE_SINGLE_CYCLE) {     //单曲循环
                        sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
                    } else if (playmode == MainActivity.MODE_LIST_CYCLE) {       //列表循环
                        if (number == MusicList.getMusicList().size() - 1) {
                            number = 0;
                            sendBroadcastOnCommand(MusicService.COMMAND_STOP);
                        } else
                            sendBroadcastOnCommand(MusicService.COMMAND_NEXT);
                    } else if (playmode == MainActivity.MODE_RANDOM_PLAY) {      //随机播放
                        number = (int) (Math.random() * MusicList.getMusicList().size());
                        sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
                    }
                    seekBarHandler.sendEmptyMessage(PROGRESS_RESET);

                    mIbtPlay.setBackgroundResource(R.drawable.play);
                    break;
                default:
                    break;
            }
        }
    }

    //发送命令，控制音乐播放，参数定义在MusicService中
    public void sendBroadcastOnCommand(int command) {
        Intent intent = new Intent(MusicService.BROADCAST_MUSICSERVICE_CONTROL);
        intent.putExtra("command", command);
        //根据不同命令，封装不同数据
        switch (command) {
            case MusicService.COMMAND_PLAY:
                intent.putExtra("number", number);
                intent.putExtra("numberOnline", number);
                break;
            case MusicService.COMMAND_SEEK_TO:
                intent.putExtra("time", time);
            case MusicService.COMMAND_PREVIOUS:
            case MusicService.COMMAND_NEXT:
            case MusicService.COMMAND_PAUSE:
            case MusicService.COMMAND_STOP:
            case MusicService.COMMAND_RESUME:
            default:
                break;
        }
        sendBroadcast(intent);
    }

    private void findViews() {
        mIbtPrevious = (ImageButton) findViewById(R.id.ibt_preview);
        mIbtPlay = (ImageButton) findViewById(R.id.ibt_play);
        mIbtStop = (ImageButton) findViewById(R.id.ibt_stop);
        mIbtNext = (ImageButton) findViewById(R.id.ibt_next);
        mIbtPlayMode = (ImageButton) findViewById(R.id.ibt_playmode);

        mLrcTextView = (LyricView) findViewById(R.id.lyricview);

        mSeekBar = (SeekBar) findViewById(R.id.progressbar);
        mTextCurrent = (TextView) findViewById(R.id.tv_pass_time);
        mTextDuration = (TextView) findViewById(R.id.tv_main_time);

        mTitle = (TextView)findViewById(R.id.tv_main_title);
        mArtist = (TextView)findViewById(R.id.tv_main_artist);
    }

    private void registerListeners() {

        mIbtPlayMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (MainActivity.playmode == MODE_LIST_SEQUENCE) {
                    mIbtPlayMode.setImageResource(R.drawable.ic_single_circle);
                    playmode = MainActivity.MODE_SINGLE_CYCLE;
                    Toast.makeText(getApplicationContext(), R.string.singlecycle, Toast.LENGTH_SHORT).show();
                } else if (MainActivity.playmode == MODE_SINGLE_CYCLE) {
                    mIbtPlayMode.setImageResource(R.drawable.ic_list_circle);
                    playmode = MainActivity.MODE_LIST_CYCLE;
                    Toast.makeText(getApplicationContext(), R.string.listcycle, Toast.LENGTH_SHORT).show();
                } else if (MainActivity.playmode == MODE_LIST_CYCLE) {
                    mIbtPlayMode.setImageResource(R.drawable.ic_randomplay);
                    playmode = MainActivity.MODE_RANDOM_PLAY;
                    Toast.makeText(getApplicationContext(), R.string.listcycle, Toast.LENGTH_SHORT).show();
                } else if (MainActivity.playmode == MODE_RANDOM_PLAY) {
                    mIbtPlayMode.setImageResource(R.drawable.ic_list_sequence);
                    playmode = MainActivity.MODE_LIST_SEQUENCE;
                    Toast.makeText(getApplicationContext(), R.string.sequence, Toast.LENGTH_SHORT).show();
                }
            }
        });

        mIbtPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendBroadcastOnCommand(MusicService.COMMAND_PREVIOUS);
            }
        });
        mIbtPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (status) {
                    case MusicService.STATUS_PLAYING_ONLINE:
                    case MusicService.STATUS_PLAYING:
                        sendBroadcastOnCommand(MusicService.COMMAND_PAUSE);
                        break;
                    case MusicService.STATUS_PAUSED_ONLINE:
                    case MusicService.STATUS_PAUSED:
                        sendBroadcastOnCommand(MusicService.COMMAND_RESUME);
                        break;
                    case MusicService.STATUS_STOPPED_ONLINE:
                    case MusicService.STATUS_STOPPED:
                        sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
                        break;
                    default:
                        break;
                }
            }
        });
        mIbtStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendBroadcastOnCommand(MusicService.COMMAND_STOP);
            }
        });
        mIbtNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendBroadcastOnCommand(MusicService.COMMAND_NEXT);
            }
        });

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (status != MusicService.STATUS_STOPPED && status != MusicService.STATUS_STOPPED_ONLINE) {
                    time = seekBar.getProgress();
                    //更新文本
                    mTextCurrent.setText(Utils.convertMSecendToTime(time));
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //进度条暂停移动
                seekBarHandler.sendEmptyMessage(PROGRESS_PAUSE);
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (status != MusicService.STATUS_STOPPED || status != MusicService.STATUS_STOPPED_ONLINE) {
                    sendBroadcastOnCommand(MusicService.COMMAND_SEEK_TO);
                }
                if (status == MusicService.STATUS_PLAYING || status == MusicService.STATUS_PLAYING_ONLINE) {
                    //进度条恢复移动
                    seekBarHandler.sendEmptyMessageDelayed(PROGRESS_INCREASE, 1000);
                }
            }
        });
    }


    private void initSeekBarHandler() {
        seekBarHandler = new Handler() {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case PROGRESS_INCREASE:
                        if (mSeekBar.getProgress() < duration) {
                            //进度图前进一秒
                            mSeekBar.setProgress(time);
                            seekBarHandler.sendEmptyMessageDelayed(PROGRESS_INCREASE, 1000);
                            //修改当前进度文本
                            mTextCurrent.setText(Utils.convertMSecendToTime(time));
                            time += 1000;
                        }
                        break;
                    case PROGRESS_PAUSE:
                        seekBarHandler.removeMessages(PROGRESS_INCREASE);
                        seekBarHandler.removeMessages(PROGRESS_RUN);
                        break;
                    case PROGRESS_RESET:
                        //重置进度条界面
                        seekBarHandler.removeMessages(PROGRESS_INCREASE);
                        mSeekBar.setProgress(0);
                        mTextCurrent.setText("00:00");
                        mLrcTextView.setText(null);
                        break;
                    case PROGRESS_RUN:
                        if (LrcParser.lyricInfo != null )
                        for(int i=0;i < LrcParser.lyricInfo.song_lines.size();i++){
                            if(LrcParser.lyricInfo.song_lines.get(i).start > time){
                                mLrcTextView.setCurrentPosition(i - 1);
                                break;
                            }
                        }
                        seekBarHandler.sendEmptyMessageDelayed(PROGRESS_RUN, 1000);
                        break;
                }
            };
        };
    }

}

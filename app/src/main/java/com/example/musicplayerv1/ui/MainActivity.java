package com.example.musicplayerv1.ui;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.musicplayerv1.adapter.MenuItemAdapter;
import com.example.musicplayerv1.adapter.MusicListPageAdapter;
import com.example.musicplayerv1.R;
import com.example.musicplayerv1.Service.MusicService;
import com.example.musicplayerv1.utils.Utils;
import com.example.musicplayerv1.entity.MusicList;
import com.example.musicplayerv1.db.PropertyBean;
import com.example.musicplayerv1.view.LocalFragment;
import com.example.musicplayerv1.view.OnlineFragment;
import com.example.musicplayerv1.widget.MarqueeTextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    //显示组件
    private ImageButton mIbtPrevious;
    private ImageButton mIbtPlayOrPause;
    private ImageButton mIbtStop;
    private ImageButton mIbtNext;
    private ImageButton mIbtBottomPlay;
    private ImageButton mIbtBottomNext;

    private TextView textCurrent;
    private TextView textDuration;
    private MarqueeTextView textPlayMessage;
    private SeekBar seekBar;
    private ImageButton mBottomAlbum;

    private DrawerLayout mDrawerLayout;
    private ListView mLvLeftMenu;
    private LinearLayout rootLayout;

    //toolbar有关组件
    private ImageView mNetBar, mMusicBar, mGroupsBar;
    private ArrayList<ImageView> tabs = new ArrayList<>();


    //更新进度条的Handler
    private Handler seekBarHandler;
    //当前歌曲的进度和总时长
    private int duration;
    private int time;

    //进度条控制常量
    private static final int PROGRESS_INCREASE = 0;
    private static final int PROGRESS_PAUSE = 1;
    private static final int PROGRESS_RESET = 2;

    //播放模式常量
    public static final int MODE_LIST_SEQUENCE = 0;
    public static final int MODE_SINGLE_CYCLE = 1;
    public static final int MODE_LIST_CYCLE = 2;
    public static final int MODE_RANDOM_PLAY = 3;
    public static int playmode;


    //音量控制
    private TextView textVolume;
    private SeekBar seekbarVolume;

    //当前歌曲的序号，下标从0开始
    public static int number = 0;
    //播放状态
    private int status;
    //广播接收器
    private StatusChangedReceiver receiver;

    //睡眠模式相关组件，，标识常量
    private ImageView ivSleepMode;
    private Timer timerSleepMode;
    private static final boolean NOTSLEEP = false;
    private static final boolean ISSLEEP = true;
    //默认睡眠时间
    private int sleepMinutes = 20;
    //标记是否打开睡眠模式
    private static boolean sleepMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //替换成ToolBar
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
        }

        findViews();
        registerListeners();
        setViewPager();
        setUpDrawer();


        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.
                WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } else {

            checkMusicFile();
        }
        duration = 0;
        time = 0;
        //绑定广播接收器，可以接收广播
        bindStatusChangedReceiver();
        initSeekBarHandler();
        Intent startIntent = new Intent(this, MusicService.class);
        startService(startIntent);
        //默认停止
        status = MusicService.STATUS_STOPPED;
        //默认顺序播放
        playmode = MainActivity.MODE_LIST_SEQUENCE;
        //睡眠模式默认关闭
        sleepMode = MainActivity.NOTSLEEP;
    }

    @Override
    protected void onResume() {
        super.onResume();
        sendBroadcastOnCommand(MusicService.COMMAND_CHECK_IS_PLAYING);
        PropertyBean property = new PropertyBean(MainActivity.this);
        String theme = property.getTheme();
        setTheme(theme);
        conctrolAudio();

        //睡眠模式打开则显示图标，反之不显示
        if (sleepMode == MainActivity.ISSLEEP)
            ivSleepMode.setVisibility(View.VISIBLE);
        else
            ivSleepMode.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (receiver != null) {
            unregisterReceiver(receiver);
            receiver = null;
        }
        //防止不断生成MediaPlayer对象占用内存，程序结束时释放该对象
        if (status == MusicService.STATUS_STOPPED) {
            stopService(new Intent(this, MusicService.class));
        }
        Intent stopIntent = new Intent(this, MusicService.class);
        stopService(stopIntent);


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    checkMusicFile();
                } else {
                    Toast.makeText(this, "拒绝就用不了美美的播放器啦", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
        }
    }

    private void setViewPager() {
        tabs.add(mMusicBar);
        tabs.add(mNetBar);

        //构造适配器
        List<Fragment> fragments=new ArrayList<Fragment>();
        fragments.add(new LocalFragment());
        fragments.add(new OnlineFragment());
        MusicListPageAdapter adapter = new MusicListPageAdapter(getSupportFragmentManager(), fragments);
        //设定适配器
        final ViewPager vp = (ViewPager)findViewById(R.id.vp_musiclist);
        vp.setAdapter(adapter);
        vp.setCurrentItem(1);

        mMusicBar.setSelected(true);
        vp.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }
            @Override
            public void onPageSelected(int position) {
                switchTabs(position);
            }
            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        mMusicBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vp.setCurrentItem(0);
            }
        });
        mNetBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vp.setCurrentItem(1);
            }
        });

        mGroupsBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
    }

    private void setUpDrawer() {
        LayoutInflater inflater = LayoutInflater.from(this);
        mLvLeftMenu.addHeaderView(inflater.inflate(R.layout.nav_header_main, mLvLeftMenu, false));
        mLvLeftMenu.setAdapter(new MenuItemAdapter(this));
        mLvLeftMenu.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 1:
                        //显示列表对话框
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("Themes Choosing...")
                                .setItems(R.array.theme, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int which) {
                                        //获取在Array.xml中定义的主题
//                                String[] themes = MainActivity.this.getResources().getStringArray(R.array.theme);
                                        String theme = PropertyBean.THEMES[which];
                                        //设置主题
//                                setTheme(themes[which]);
                                        setTheme(theme);
                                        //保存选择的主题
                                        PropertyBean property = new PropertyBean(MainActivity.this);
                                        property.setAndSaveTheme(theme);
                                    }
                                }).show();
                        break;
                    case 2:
                        showSleepDialog();
                        break;
                    case 3:
                        String[] mode = new String[]{"顺序循环", "单曲循环", "列表循环", "随机播放"};
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setTitle("播放模式");
                        builder.setSingleChoiceItems(mode, playmode, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int arg1) {
                                playmode = arg1;
                            }
                        });
                        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                switch (playmode) {
                                    case 0:
                                        playmode = MainActivity.MODE_LIST_SEQUENCE;
                                        Toast.makeText(getApplicationContext(), R.string.sequence, Toast.LENGTH_SHORT).show();
                                        break;
                                    case 1:
                                        playmode = MainActivity.MODE_SINGLE_CYCLE;
                                        Toast.makeText(getApplicationContext(), R.string.singlecycle, Toast.LENGTH_SHORT).show();
                                        break;
                                    case 2:
                                        playmode = MainActivity.MODE_LIST_CYCLE;
                                        Toast.makeText(getApplicationContext(), R.string.listcycle, Toast.LENGTH_SHORT).show();
                                        break;
                                    case 3:
                                        playmode = MainActivity.MODE_RANDOM_PLAY;
                                        Toast.makeText(getApplicationContext(), R.string.randomplay, Toast.LENGTH_SHORT).show();
                                        break;
                                    default:
                                        break;
                                }
                            }
                        });
                        builder.create().show();
                        break;
                    case 4:
                        //显示about文本
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("RndPlayer")
                                .setMessage(R.string.about2).show();
                        break;
                    case 5:
                        //退出程序
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("(；´д｀)ゞ")
                                .setMessage(R.string.quit_message)
                                .setPositiveButton("丑拒", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        Intent intent = new Intent(MainActivity.this, MusicService.class);
                                        stopService(intent);
                                        finish();
                                    }
                                }).setNegativeButton("么么哒", new DialogInterface.OnClickListener() {
                            @Override

                            public void onClick(DialogInterface dialogInterface, int i) {
                            }
                        }).setCancelable(false).show();
                        break;
                }
            }
        });
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
                    time = intent.getIntExtra("time", 0);
                    duration = intent.getIntExtra("duration", 0);
                    number = intent.getIntExtra("number", number);
                    seekBar.setProgress(time);
                    seekBar.setMax(duration);
                    seekBarHandler.sendEmptyMessageDelayed(PROGRESS_INCREASE, 1000);
                    textDuration.setText(Utils.convertMSecendToTime(duration));
                    mBottomAlbum.setImageBitmap(Utils.resizeImage(MusicList.getMusicList().get(number).getThumb()));

                    mIbtPlayOrPause.setBackgroundResource(R.drawable.pause);
                    mIbtBottomPlay.setBackgroundResource(R.drawable.pause);
                    //底部文本提示
                    textPlayMessage.setText(musicArtist + " - " + musicName);
                    break;
                case MusicService.STATUS_PAUSED:
                    seekBarHandler.sendEmptyMessage(PROGRESS_PAUSE);

                    textPlayMessage.setText(musicArtist + " - " + musicName);

                    mIbtPlayOrPause.setBackgroundResource(R.drawable.play);
                    mIbtBottomPlay.setBackgroundResource(R.drawable.play);
                    break;
                case MusicService.STATUS_STOPPED:
                    time = 0;
                    duration = 0;
                    number = -1;
                    textCurrent.setText(Utils.convertMSecendToTime(time));
                    textDuration.setText(Utils.convertMSecendToTime(duration));
                    seekBarHandler.sendEmptyMessage(PROGRESS_RESET);

                    textPlayMessage.setText("");
                    mIbtPlayOrPause.setBackgroundResource(R.drawable.play);
                    mIbtBottomPlay.setBackgroundResource(R.drawable.play);
                    mBottomAlbum.setImageResource(R.drawable.bg_bottom_album);
                    break;
                case MusicService.STATUS_COMPLETED:
                    number = intent.getIntExtra("number", 0);
                    if (playmode == MainActivity.MODE_LIST_SEQUENCE) {         //顺序播放
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

                    textPlayMessage.setText("");
                    mIbtPlayOrPause.setBackgroundResource(R.drawable.play);
                    mIbtBottomPlay.setBackgroundResource(R.drawable.play);
                    break;
                case MusicService.STATUS_PLAYING_ONLINE:
                    seekBarHandler.removeMessages(PROGRESS_INCREASE);//防止队列中increase消息重复
                    time = intent.getIntExtra("time", 0);
                    duration = intent.getIntExtra("duration", 0);
                    SearchOnlineActivity.numberOnline = intent.getIntExtra("numberOnline", SearchOnlineActivity.numberOnline);
                    seekBar.setProgress(time);
                    seekBar.setMax(duration);
                    seekBarHandler.sendEmptyMessageDelayed(PROGRESS_INCREASE, 1000);
                    textDuration.setText(Utils.convertMSecendToTime(duration));
//                    bottomAlbum.setImageBitmap(Utils.resizeImage(MusicList.getMusicList().get(SearchOnlineActivity.numberOnline).getThumb()));

                    mIbtPlayOrPause.setBackgroundResource(R.drawable.pause);
                    mIbtBottomPlay.setBackgroundResource(R.drawable.pause);
                    //底部文本提示
                    textPlayMessage.setText(musicArtistOnline + " - " + musicNameOnline);
                    break;
                case MusicService.STATUS_PAUSED_ONLINE:
                    seekBarHandler.sendEmptyMessage(PROGRESS_PAUSE);

                    textPlayMessage.setText(musicArtistOnline + " - " + musicNameOnline);

                    mIbtPlayOrPause.setBackgroundResource(R.drawable.play);
                    mIbtBottomPlay.setBackgroundResource(R.drawable.play);
                    break;
                case MusicService.STATUS_STOPPED_ONLINE:
                    time = 0;
                    duration = 0;
                    SearchOnlineActivity.numberOnline = -1;
                    textCurrent.setText(Utils.convertMSecendToTime(time));
                    textDuration.setText(Utils.convertMSecendToTime(duration));
                    seekBarHandler.sendEmptyMessage(PROGRESS_RESET);

                    textPlayMessage.setText("");
                    mIbtPlayOrPause.setBackgroundResource(R.drawable.play);
                    mIbtBottomPlay.setBackgroundResource(R.drawable.play);
                    mBottomAlbum.setImageResource(R.drawable.bg_bottom_album);
                    break;
                case MusicService.STATUS_COMPLETED_ONLINE:
                    SearchOnlineActivity.numberOnline = intent.getIntExtra("number", 0);
                    if (playmode == MainActivity.MODE_LIST_SEQUENCE) {         //顺序播放
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

                    textPlayMessage.setText("");
                    mIbtPlayOrPause.setBackgroundResource(R.drawable.play);
                    mIbtBottomPlay.setBackgroundResource(R.drawable.play);
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

    //如果列表没有歌曲，则播放按钮不能用，并提示用户
    private void checkMusicFile() {
//        if (mMusicList.isEmpty()) {
//            ibtNext.setEnabled(false);
//            ibtPlayOrPause.setEnabled(false);
//            ibtPrevious.setEnabled(false);
//            ibtStop.setEnabled(false);
//            Toast.makeText(getApplicationContext(), "WOC,没音乐啦", Toast.LENGTH_SHORT).show();
//        } else {
//            ibtNext.setEnabled(true);
//            ibtPlayOrPause.setEnabled(true);
//            ibtPrevious.setEnabled(true);
//            ibtStop.setEnabled(true);
//        }
    }

    //获取实例
    private void findViews() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.root_drawerlayout);
        rootLayout = (LinearLayout) findViewById(R.id.activity_main);
        mLvLeftMenu = (ListView)findViewById(R.id.lv_left_menu);

        mMusicBar = (ImageView)findViewById(R.id.iv_bar_music);
        mNetBar = (ImageView)findViewById(R.id.iv_bar_net);
        mGroupsBar = (ImageView)findViewById(R.id.iv_bar_groups);

        seekbarVolume = (SeekBar) findViewById(R.id.sb_volumebar);

        textVolume = (TextView) findViewById(R.id.tv_volume);
        ivSleepMode = (ImageView) findViewById(R.id.iv_sleep);
        mBottomAlbum = (ImageButton) findViewById(R.id.ibt_bottom_album);
        seekBar = (SeekBar) findViewById(R.id.sb_bottom_duration);
        textCurrent = (TextView) findViewById(R.id.tv_begin);
        textDuration = (TextView) findViewById(R.id.tv_end);
        textPlayMessage = (MarqueeTextView) findViewById(R.id.tv_play_message);

        mIbtPrevious = (ImageButton) findViewById(R.id.imgBtn_previous);
        mIbtPlayOrPause = (ImageButton) findViewById(R.id.imgBtn_play);
        mIbtStop = (ImageButton) findViewById(R.id.imgBtn_stop);
        mIbtNext = (ImageButton) findViewById(R.id.imgBtn_next);

        mIbtBottomPlay = (ImageButton)findViewById(R.id.ibt_bottom_play);
        mIbtBottomNext = (ImageButton)findViewById(R.id.ibt_bottom_next);
    }

    //设置点击事件
    private void registerListeners() {
        mIbtPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("MusicAvtivity", "btprevious");
                sendBroadcastOnCommand(MusicService.COMMAND_PREVIOUS);
            }
        });
        mIbtPlayOrPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (status) {
                    case MusicService.STATUS_PLAYING_ONLINE:
                    case MusicService.STATUS_PLAYING:
                        Log.d("MusicAvtivity", "pause");
                        sendBroadcastOnCommand(MusicService.COMMAND_PAUSE);
                        break;
                    case MusicService.STATUS_PAUSED_ONLINE:
                    case MusicService.STATUS_PAUSED:
                        Log.d("MusicAvtivity", "resume");
                        sendBroadcastOnCommand(MusicService.COMMAND_RESUME);
                        break;
                    case MusicService.STATUS_STOPPED_ONLINE:
                    case MusicService.STATUS_STOPPED:
                        Log.d("MusicAvtivity", "play");
                        sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
                        break;
                    default:
                        break;
                }
            }
        });
        mIbtBottomPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (status) {
                    case MusicService.STATUS_PLAYING_ONLINE:
                    case MusicService.STATUS_PLAYING:
                        Log.d("MusicAvtivity", "pause");
                        sendBroadcastOnCommand(MusicService.COMMAND_PAUSE);
                        break;
                    case MusicService.STATUS_PAUSED_ONLINE:
                    case MusicService.STATUS_PAUSED:
                        Log.d("MusicAvtivity", "resume");
                        sendBroadcastOnCommand(MusicService.COMMAND_RESUME);
                        break;
                    case MusicService.STATUS_STOPPED_ONLINE:
                    case MusicService.STATUS_STOPPED:
                        Log.d("MusicAvtivity", "play");
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
                Log.d("MusicAvtivity", "btstop");
                sendBroadcastOnCommand(MusicService.COMMAND_STOP);
            }
        });
        mIbtNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("MusicAvtivity", "btnext");
                sendBroadcastOnCommand(MusicService.COMMAND_NEXT);
            }
        });
        mIbtBottomNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("MusicAvtivity", "btnext");
                sendBroadcastOnCommand(MusicService.COMMAND_NEXT);
            }
        });

        mBottomAlbum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, PlayingDetailsActivity.class);
                intent.putExtra("currentTime",time);
                startActivity(intent);
            }
        });
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (status != MusicService.STATUS_STOPPED && status != MusicService.STATUS_STOPPED_ONLINE) {
                    time = seekBar.getProgress();
                    //更新文本
                    textCurrent.setText(Utils.convertMSecendToTime(time));
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
                        if (seekBar.getProgress() < duration) {
                            //进度图前进一秒
                            seekBar.setProgress(time);
                            seekBarHandler.sendEmptyMessageDelayed(PROGRESS_INCREASE, 1000);
                            //修改当前进度文本
                            textCurrent.setText(Utils.convertMSecendToTime(time));
                            time += 1000;
                        }
                        break;
                    case PROGRESS_PAUSE:
                        seekBarHandler.removeMessages(PROGRESS_INCREASE);
                        break;
                    case PROGRESS_RESET:
                        //重置进度条界面
                        seekBarHandler.removeMessages(PROGRESS_INCREASE);
                        seekBar.setProgress(0);
                        textCurrent.setText("00:00");
                        break;
                }
            };
        };
    }

    //设置主题，修改背景
    private void setTheme(String theme) {
        if ("Blue".equals(theme)) {
            rootLayout.setBackgroundResource(R.drawable.bg_blue);
        } else if ("Green".equals(theme)) {
            rootLayout.setBackgroundResource(R.drawable.bg_green);
        } else if ("Red".equals(theme)) {
            rootLayout.setBackgroundResource(R.drawable.bg_red);
        } else if ("Pink".equals(theme)) {
            rootLayout.setBackgroundResource(R.drawable.bg_pink);
        }
    }

    //创建菜单

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_toolbar, menu);
        return super.onCreateOptionsMenu(menu);
    }
    //处理菜单点击事件

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                break;
            case R.id.it_toolbar_find:
                Intent intent = new Intent(MainActivity.this, SearchOnlineActivity.class);
                startActivity(intent);
                break;
        }
        return true;
    }

    //睡眠模式弹窗
    private void showSleepDialog() {
        //先用getLayoutInflater().inflate方法获取布局，用来初始化一个View对象
        final View userView = this.getLayoutInflater().inflate(R.layout.dialog, null);

        //不能用Avtivity类对象，要通过View类的findViewById方法获取布局，来初始化一个View类对象
        final TextView tvSleepMinute = (TextView) userView.findViewById(R.id.tv_dialog);
        final Switch swIsSleep = (Switch) userView.findViewById(R.id.sw_dialog);
        final SeekBar sbAdjustSleep = (SeekBar) userView.findViewById(R.id.sb_dialog);

        tvSleepMinute.setText("睡眠于:" + sleepMinutes + "分钟");
        //根据睡眠状态改变Switch状态
        if (sleepMode == MainActivity.ISSLEEP)
            swIsSleep.setChecked(true);
        sbAdjustSleep.setMax(60);
        sbAdjustSleep.setProgress(sleepMinutes);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                sleepMinutes = i;
                tvSleepMinute.setText("睡眠于:" + sleepMinutes + "分钟");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        swIsSleep.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                sleepMode = b;
            }
        });

        //定时关闭
        final TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                //System.exit(0);
                Intent intent = new Intent(MainActivity.this, MusicService.class);
                stopService(intent);
                finish();
            }
        };
        //定义对话框以及初始化
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("选择睡眠时间（0-60min）");
        //设置布局
        dialog.setView(userView);
        //取消按钮
        dialog.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        //重置按钮
        dialog.setNeutralButton("重置", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (sleepMode == MainActivity.ISSLEEP) {
                    timerTask.cancel();
                    timerSleepMode.cancel();
                }
                sleepMode = MainActivity.NOTSLEEP;
                sleepMinutes = 20;
                ivSleepMode.setVisibility(View.INVISIBLE);
            }
        });
        //确定按钮
        dialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (sleepMode == MainActivity.ISSLEEP) {
                    timerSleepMode = new Timer();
                    int time = sbAdjustSleep.getProgress();
                    //启动任务
                    timerSleepMode.schedule(timerTask, time * 60 * 1000);
                    ivSleepMode.setVisibility(View.VISIBLE);
                } else {
                    //取消任务
                    timerTask.cancel();
                    if (timerSleepMode != null)
                        timerSleepMode.cancel();
                    dialogInterface.dismiss();
                    ivSleepMode.setVisibility(View.INVISIBLE);
                }
            }
        });
        dialog.show();
    }

    private void switchTabs(int position) {
        for (int i = 0; i < tabs.size(); i++) {
            if (position == i) {
                tabs.get(i).setSelected(true);
            } else {
                tabs.get(i).setSelected(false);
            }
        }
    }

    //控制手机虚拟键
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        int progress;
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                moveTaskToBack(false);
                break;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                progress = seekbarVolume.getProgress();
                if (progress != 0)
                    seekbarVolume.setProgress(progress - 1);
                return true;
            case KeyEvent.KEYCODE_VOLUME_UP:
                progress = seekbarVolume.getProgress();
                if (progress != seekbarVolume.getMax())
                    seekbarVolume.setProgress(progress + 1);
                return true;
            default:
                break;
        }
        return false;
    }

    //控制音量
    private void conctrolAudio() {
        //获取音量管理器
        final AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        //设置当前调整音量大小只针对媒体音乐
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        //设置滑条最大值
        final int maxProgress = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        seekbarVolume.setMax(maxProgress);
        //获取当前音量
        int currentProgress = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        seekbarVolume.setProgress(currentProgress);
        textVolume.setText("Volume:" + (currentProgress * 100 / maxProgress) + "%");
        seekbarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                textVolume.setText("Volume:" + (i * 100 / maxProgress) + "%");
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, i, AudioManager.FLAG_PLAY_SOUND);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }
}

package com.example.musicplayerv1.ui;


import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.example.musicplayerv1.R;
import com.example.musicplayerv1.Service.MusicService;
import com.example.musicplayerv1.adapter.OnlineMusicListAdapter;
import com.example.musicplayerv1.entity.Music;
import com.example.musicplayerv1.entity.MusicList;
import com.example.musicplayerv1.entity.OnlineMusic;
import com.example.musicplayerv1.https.APIsData;


import java.util.ArrayList;

import static com.example.musicplayerv1.ui.MainActivity.number;

public class SearchOnlineActivity extends AppCompatActivity {

    private EditText editTextToSearch;
    private Button buttonSearch;


    private ListView mOnlineMusicList;

    public static int numberOnline;
    private int sorts;

//    private ArrayList<OnlineMusic> onlineMusics = APIsData.mOnlineMusic;
    private ArrayList<OnlineMusic> onlineMusics = MusicList.getOnlineMusicsList();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_online_layout);

        Toolbar toolbar = (Toolbar)findViewById(R.id.search_activity_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        setTitle("Searching Online");

//        findViews();
    }

    private void findViews() {
//        editTextToSearch = (EditText)findViewById(R.id.edt_search_online);
        mOnlineMusicList = (ListView)findViewById(R.id.lv_music_online);
//        buttonSearch = (Button)findViewById(R.id.bt_search_online);
        buttonSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onlineMusics.clear();
                String URL = APIsData.buildURLByKeyword(editTextToSearch.getText().toString(),"1");
                APIsData.getSongs(URL);

                OnlineMusicListAdapter adapter = new OnlineMusicListAdapter(SearchOnlineActivity.this, R.layout.online_music_item, onlineMusics);
                mOnlineMusicList.setAdapter(adapter);
                mOnlineMusicList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                        numberOnline = position;
                        sendBroadcastOnCommand(MusicService.COMMAND_PLAY_ONLINE);
                        startActivity(new Intent(SearchOnlineActivity.this,MainActivity.class));
                    }
                });
            }
        });
    }



    //发送命令，控制音乐播放，参数定义在MusicService中
    public void sendBroadcastOnCommand(int command) {
        Intent intent = new Intent(MusicService.BROADCAST_MUSICSERVICE_CONTROL_ONLINE);
        intent.putExtra("command", command);
        //根据不同命令，封装不同数据
        switch (command) {
            case MusicService.COMMAND_PLAY_ONLINE:
                number = -1;
                intent.putExtra("numberOnline", numberOnline);
                break;
//            case MusicService.COMMAND_SEEK_TO:
//                intent.putExtra("time", time);
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}

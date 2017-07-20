package com.example.musicplayerv1.view;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.musicplayerv1.adapter.MusicListAdapter;
import com.example.musicplayerv1.R;
import com.example.musicplayerv1.Service.MusicService;
import com.example.musicplayerv1.ui.MainActivity;
import com.example.musicplayerv1.utils.Utils;
import com.example.musicplayerv1.entity.Music;
import com.example.musicplayerv1.entity.MusicList;

import java.util.ArrayList;


public class LocalFragment extends Fragment {

    //广播接收器
    private CommandReceiver receiver;

    //歌曲列表对象
    private ArrayList<Music> mLocalMusic = MusicList.getMusicList();

    private MusicUpdateTask mLocalListUpdateTask;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        final View view=inflater.inflate(R.layout.local_list_layout, container, false);
        final ListView list = (ListView) view.findViewById(R.id.lv_musiclist);

        mLocalMusic.clear();

        if(list != null){
            mLocalListUpdateTask = new MusicUpdateTask(list);
            mLocalListUpdateTask.execute();
        }



        return view;
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (mLocalListUpdateTask != null && mLocalListUpdateTask.getStatus() == AsyncTask.Status.RUNNING) {
            mLocalListUpdateTask.cancel(true);
        }
        mLocalListUpdateTask = null;

        if (receiver != null) {
            getActivity().unregisterReceiver(receiver);
            receiver = null;
        }

    }

    //绑定广播接收器
    private void bindCommandReceiver(ListView listView, MusicListAdapter adapter){
        receiver = new CommandReceiver(listView,adapter);
        IntentFilter filter = new IntentFilter();
        filter.addAction(MusicService.BROADCAST_MUSICSERVICE_CONTROL);
        ((MainActivity)getActivity()).registerReceiver(receiver, filter);
    }

    //内部类，接收广播命令，并执行操作
    class CommandReceiver extends BroadcastReceiver {

        private ListView mListView;
        private MusicListAdapter mAdapter;
        public CommandReceiver(ListView listView, MusicListAdapter adapter){
            mListView = listView;
            mAdapter = adapter;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String ctrlCode = intent.getAction();

            if(MusicService.BROADCAST_MUSICSERVICE_CONTROL.equals(ctrlCode)){
                //获取命令
                int command = intent.getIntExtra("command", MusicService.COMMAND_UNKNOW);
                //执行命令
                switch (command){
                    case MusicService.COMMAND_PLAY:
                    case MusicService.COMMAND_STOP:
                    case MusicService.COMMAND_PREVIOUS:
                    case MusicService.COMMAND_NEXT:
                        mAdapter.notifyDataSetInvalidated();
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private class MusicUpdateTask extends AsyncTask<Object, Music, Void> {

        private ListView mListView;

        public MusicUpdateTask(ListView MusicList){
            mListView = MusicList;
        }

        @Override
        protected Void doInBackground(Object... params) {
            //这里是工作线程，处理耗时的查询音乐的操作
            Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            String[] searchKey = new String[]{
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Albums.ALBUM_ID,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.DURATION
            };

            String where = MediaStore.Audio.Media.DATA + " like \"%" + "/music" + "%\"";
            String[] keywords = null;
            String sortOrder = MediaStore.Audio.Media.DEFAULT_SORT_ORDER;

            ContentResolver resolver = ((MainActivity)getActivity()).getContentResolver();
            Cursor cursor = resolver.query(uri, searchKey, where, keywords, sortOrder);
//            Cursor cursor = resolver.query(_uri, prjs, selections, selectArgs, order);

            if (cursor != null) {
                while (cursor.moveToNext() && !isCancelled()) {
                    String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                    String id = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
                    Uri musicUri = Uri.withAppendedPath(uri, id);

                    String name = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                    String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                    long duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));


                    int albumId = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM_ID));
                    Uri albumUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId);
                    Music music = new Music(musicUri, path, albumUri, name, artist, duration);
                    if (uri != null) {
                        ContentResolver res = ((MainActivity)getActivity()).getContentResolver();
                        music.setThumb(Utils.createThumbFromUir(res, albumUri));
                    }

                    Log.d("MusicUpdateTask.class", "real music found: " + path);

                    publishProgress(music);

                }
                cursor.close();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Music... values) {

            Music music = values[0];

            //这是主线程，在这里把要显示的音乐添加到音乐的展示列表当中。
            mLocalMusic.add(music);
//            MusicListAdapter adapter = (MusicListAdapter) mListView .getAdapter();
//            adapter.notifyDataSetChanged();
            final MusicListAdapter adapter = new MusicListAdapter((MainActivity)getActivity(), R.layout.local_music_item, mLocalMusic);
            mListView.setAdapter(adapter);

            mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);// 一定要设置这个属性，否则ListView不会刷新
            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                    MainActivity.number = position;

//                  MusicListAdapter adapter = (MusicListAdapter) list .getAdapter();
//                  adapter.notifyDataSetInvalidated();
                    MusicService.status = MusicService.STATUS_STOPPED;
                    ((MainActivity)getActivity()).sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
                }
            });

//          notifyDataSetChanged ():
//          该方法内部实现了在每个观察者上面调用onChanged事件。每当发现数据集有改变的情况，或者读取到数据的新状态时，就会调用此方法。
//          notifyDataSetInvalidated ():
//          该方法内部实现了在每个观察者上面调用onInvalidated事件。每当发现数据集监控有改变的情况，比如该数据集不再有效，就会调用此方法。
            bindCommandReceiver(mListView,adapter);
        }
    }
}

package com.example.musicplayerv1.https;

//    搜歌API：
//    http://route.showapi.com/213-1?showapi_appid=31475&showapi_sign=86625d192d6745879b781b4282826522&keyword=%E5%BC%A0%E6%9D%B0&page=1
//
//    keyword=歌名或歌手 ;page=当前页数
//
//    热门歌曲分类API：
//    http://route.showapi.com/213-4?showapi_appid=31475&showapi_sign=86625d192d6745879b781b4282826522&topid=5
//    topid=歌曲type，如下：
//    //歌曲类型的type
//    public static final String MUSIC_WEST = "3";//欧美
//    public static final String MUSIC_INLAND = "5";//内地
//    public static final String MUSIC_HONGKANG = "6";//港台
//    public static final String MUSIC_KOREA = "16";//韩国
//    public static final String MUSIC_JAPAN = "17";//日本
//    public static final String MUSIC_VOLKSLIED = "18";//民谣
//    public static final String MUSIC_ROCK = "19";//摇滚
//    public static final String MUSIC_SALES = "23";//销量
//    public static final String MUSIC_HOT = "26";//热歌
//    public static final String MUSIC_LOCAL = "27";//本地音乐
//    public static final String MUSIC_SEARCH =  "28";//搜索到的音乐
//    public static final String MUSIC_Like = "29";//收藏的音乐
//
//    歌词API：
//    http://route.showapi.com/213-2?showapi_appid=31475&showapi_sign=86625d192d6745879b781b4282826522&musicid=200576210
//    musicid=歌曲id（从搜歌API获得的单首歌曲的songid）

import android.util.Log;

import com.example.musicplayerv1.entity.MusicList;
import com.example.musicplayerv1.entity.OnlineMusic;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class APIsData {

    public static final String SEARCH_BY_SONGORSINGER_P = "http://route.showapi.com/213-1?" +
            "showapi_appid=31475&showapi_sign=86625d192d6745879b781b4282826522&keyword=";
    public static final String SEARCH_BY_SONGORSINGER_L = "&page=";
    public static final String SEARCH_BY_SORTS = "http://route.showapi.com/213-4?" +
            "showapi_appid=31475&showapi_sign=86625d192d6745879b781b4282826522&topid=";

    public static final String MUSIC_WEST = "3";//欧美
    public static final String MUSIC_INLAND = "5";//内地
    public static final String MUSIC_HONGKANG = "6";//港台
    public static final String MUSIC_KOREA = "16";//韩国
    public static final String MUSIC_JAPAN = "17";//日本
    public static final String MUSIC_VOLKSLIED = "18";//民谣
    public static final String MUSIC_ROCK = "19";//摇滚
    public static final String MUSIC_SALES = "23";//销量
    public static final String MUSIC_HOT = "26";//热歌
    public static final String MUSIC_LOCAL = "27";//本地音乐
    public static final String MUSIC_SEARCH =  "28";//搜索到的音乐
    public static final String MUSIC_Like = "29";//收藏的音乐

    private int mKeyword = 1;
    private int mTopic = 1;
    public static int searchSongID;
    private static String mURLs;
    public static ArrayList<OnlineMusic> mOnlineMusic = MusicList.getOnlineMusicsList();



    public static void getSongs(String URL){
        mURLs = URL;
        ArrayList<OnlineMusic> onlineMusicList = new ArrayList<OnlineMusic>();

        //开启线程来发起网络请求
        new Thread(new Runnable() {
            @Override
            public void run() {

                HttpURLConnection connection = null;
                BufferedReader reader = null;
                try{
                    URL url = new URL(mURLs);
                    connection = (HttpURLConnection)url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(8000);
                    connection.setReadTimeout(8000);
                    InputStream in = connection.getInputStream();

                    //读取输入流
                    reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null){
                        response.append(line);
                    }
                    parseSongs(response.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void parseSongs(String jsonData){
        try{

            JSONObject jsonAll = new JSONObject(jsonData);
            JSONObject body = jsonAll.getJSONObject("showapi_res_body");
            JSONObject pageBean = body.getJSONObject("pagebean");
            JSONArray contentList = pageBean.getJSONArray("contentlist");
            mOnlineMusic.clear();
            for(int i = 0; i < contentList.length(); i++){
                JSONObject jsonItem = contentList.getJSONObject(i);
                int songID = jsonItem.getInt("songid");
                if (i == 0){
                    searchSongID = songID;
                }

                String m4a = jsonItem.getString("m4a");
                String songName = jsonItem.getString("songname");
                String singerName = jsonItem.getString("singername");
//                String smallAlbum = jsonItem.getString("albumpic_small");
//                String bigAlbum = jsonItem.getString("albumpic_big");
//                String albumName = jsonItem.getString("albumname");
                String downUrl = jsonItem.getString("downUrl");
                Log.d("MainActivity", "m4a is " + m4a);
                Log.d("MainActivity", "songname is " + songName);
                Log.d("MainActivity", "singername is " + singerName);
                Log.d("MainActivity", "downUrl is " + downUrl);
                OnlineMusic onlineMusic = new OnlineMusic(songID,m4a,songName,singerName,null,
                        null,null,downUrl,0);
                mOnlineMusic.add(onlineMusic);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static String buildURLByKeyword(String keywords, String page) {

        StringBuilder URLs = new StringBuilder(SEARCH_BY_SONGORSINGER_P + keywords
                + SEARCH_BY_SONGORSINGER_L + page);
        return URLs.toString();
    }
}

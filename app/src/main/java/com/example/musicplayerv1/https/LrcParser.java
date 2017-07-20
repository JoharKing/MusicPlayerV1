package com.example.musicplayerv1.https;

import android.util.Log;

import com.example.musicplayerv1.entity.LineInfo;
import com.example.musicplayerv1.entity.LyricInfo;
import com.example.musicplayerv1.widget.LyricView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;


public class LrcParser {

    private LyricView mLrcTextView;
    public static LyricInfo lyricInfo ;
    private static String mSongID;
    public static final String SEARCH_LRC = "http://route.showapi.com/213-2?" +
            "showapi_appid=31475&showapi_sign=86625d192d6745879b781b4282826522&musicid=";

    public static void askLrc(final int songID) {
        mSongID = String.valueOf(songID);
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                BufferedReader reader = null;
                try{
                    URL url = new URL("http://route.showapi.com/213-2?showapi_appid=31475&showapi_sign=86625d192d6745879b781b4282826522&musicid=" + mSongID);
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
//                    showResponse(response.toString());
                    parseLrc(response.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private static void parseLrc(String jsonData){
        try{
            JSONObject jsonAll = new JSONObject(jsonData);
            JSONObject body = jsonAll.getJSONObject("showapi_res_body");
            String lyric = body.getString("lyric");
            String lyricText = lyric.replaceAll("&#58;",":")
                    .replaceAll("&#46;",".")
                    .replaceAll("&#10;","\n")
                    .replaceAll("&#13;"," ")
                    .replaceAll("&#32;"," ")
                    .replaceAll("&#39;"," ")
                    .replaceAll("&#40;"," ")
                    .replaceAll("&#41;"," ")
                    .replaceAll("&#63;"," ")
                    .replaceAll("&#45;","-");

            String[] split = lyricText.split("\n");
            lyricInfo = new LyricInfo();;
            lyricInfo.song_lines = new ArrayList<LineInfo>();
            for(int i = 0; i < split.length; i++){
//                Log.d("xxx",split[i]);
                analyzeLyric(split[i]);
            }
        } catch (JSONException e1) {
            e1.printStackTrace();
        }
    }

    private static void analyzeLyric(String line) {
        int index = line.lastIndexOf("]");
        if(line != null && line.startsWith("[offset:")) {
            // 时间偏移量
            String string = line.substring(8, index).trim();
            lyricInfo.song_offset = Long.parseLong(string);
            Log.d("analayze",string);
            return;
        }
        if(line != null && line.startsWith("[ti:")) {
            // title 标题
            String string = line.substring(4, index).trim();
            Log.d("analayze",string);
            lyricInfo.song_title = string;
            return;
        }
        if(line != null && line.startsWith("[ar:")) {
            // artist 作者
            String string = line.substring(4, index).trim();
            Log.d("analayze",string);
            lyricInfo.song_artist = string;
            return;
        }
        if(line != null && line.startsWith("[al:")) {
            // album 所属专辑
            String string = line.substring(4, index).trim();
            Log.d("analayze",string);
            lyricInfo.song_album = string;
            return;
        }
        if(line != null && line.startsWith("[by:")) {
            return;
        }
        if(line != null && index == 9 && line.trim().length() > 10) {
            // 歌词内容
            Log.d("analayze",line);
            LineInfo lineInfo = new LineInfo();
            lineInfo.content = line.substring(10, line.length());
            lineInfo.start = measureStartTimeMillis(line.substring(0, 10));
            lyricInfo.song_lines.add(lineInfo);
        }
    }


    /**
     * 从字符串中获得时间值
     * */
    private static long measureStartTimeMillis(String str) {
        long minute = Long.parseLong(str.substring(1, 3));
        long second = Long.parseLong(str.substring(4, 6));
        long millisecond = Long.parseLong(str.substring(7, 9));
        return millisecond + second * 1000 + minute * 60 * 1000;
    }
}

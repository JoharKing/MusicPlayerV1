package com.example.musicplayerv1.entity;

import java.util.List;

/**
 * Created by qq113 on 2017/5/5.
 */

public class LyricInfo {
    public static List<LineInfo> song_lines;

    public String song_artist;  // 歌手
    public String song_title;  // 标题
    public String song_album;  // 专辑

    public long song_offset;  // 偏移量
}

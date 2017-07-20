package com.example.musicplayerv1.entity;

import android.graphics.Bitmap;
import android.net.Uri;

/**
 * 歌曲实体-歌曲名，艺术家，路径，时长等属性，以及相关的获取方法
 */

public class Music {

    private String musicName; //--存储音乐的名字
    private String musicArtist; //--存储音乐的艺术家
    private Uri songUri; //存储音乐的Uri地址
    private String musicPath;
    private Uri albumUri;//--存储音乐封面的Uri地址
    private Bitmap thumb;//--存储封面图片
    private long musicDuration;//--存储音乐的播放时长，单位是毫秒

    public Music(Uri songUri, String musicPath, Uri albumUri, String musicName, String musicArtist, long musicDuration) {
        this.songUri = songUri;
        this.musicPath = musicPath;
        this.musicName = musicName;
        this.albumUri = albumUri;
        this.musicName = musicName;
        this.musicArtist = musicArtist;
        this.musicDuration = musicDuration;
    }

    public Uri getSongUri() {
        return songUri;
    }

    public String getMusicPath() {
        return musicPath;
    }

    public Uri getAlbumUri() {
        return albumUri;
    }

    public String getMusicName() {
        return musicName;
    }

    public String getMusicArtist() {
        return musicArtist;
    }

    public long getMusicDuration() {
        return musicDuration;
    }

    public Bitmap getThumb() {
        return thumb;
    }

    public void setThumb(Bitmap thumb) {
        this.thumb = thumb;
    }
}

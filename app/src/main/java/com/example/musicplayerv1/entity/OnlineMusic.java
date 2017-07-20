package com.example.musicplayerv1.entity;

public class OnlineMusic {
    private int songID ; //--用于搜索歌词
    private String musicName; //--存储音乐的名字
    private String musicArtist; //--存储音乐的艺术家
    private String musicPath; //存储音乐的Url地址
    private String smallAlbumUrl;//--存储音乐专辑封面的Url地址
    private String bigAlbumUrl;//--存储音乐专辑封面的Url地址
    private String albumName; //--专辑名称
    private String downUrl; //--下载地址
    private long musicDuration;//--存储音乐的播放时长，单位是毫秒

    public OnlineMusic(int songID, String musicPath, String musicName, String musicArtist, String
            smallAlbumUrl, String bigAlbumUrl, String albumName, String downUrl, long musicDuration) {
        this.songID = songID;
        this.musicPath = musicPath;
        this.musicName = musicName;
        this.musicArtist = musicArtist;
        this.smallAlbumUrl = smallAlbumUrl;
        this.bigAlbumUrl = bigAlbumUrl;
        this.albumName = albumName;
        this.downUrl = downUrl;
        this.musicDuration = musicDuration;
    }

    public int getSongID(){
        return songID;
    }

    public String getMusicPath() {
        return musicPath;
    }

    public String getMusicName() {
        return musicName;
    }

    public String getMusicArtist() {
        return musicArtist;
    }

    public String getSmallAlbumUrl() {
        return smallAlbumUrl;
    }

    public String getBigAlbumUrl() {
        return bigAlbumUrl;
    }

    public String getAlbumName() {
        return albumName;
    }

    public String getDownUrl() {
        return downUrl;
    }

    public long getMusicDuration() {
        return musicDuration;
    }

}

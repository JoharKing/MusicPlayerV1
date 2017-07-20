package com.example.musicplayerv1.entity;

import java.util.ArrayList;

/**
 * 采用单一实例
 * 只能通过getMusicList方法获取
 * 共享，唯一的ArrayList<Music>和<OnlineMusic>对象
 */

public class MusicList {

    private static ArrayList<Music> localMusics = new ArrayList<Music>();
    private static ArrayList<OnlineMusic> onlineMusics = new ArrayList<OnlineMusic>();

    private MusicList(){

    }

    public static ArrayList<Music> getMusicList(){
        return localMusics;
    }

    public static ArrayList<OnlineMusic> getOnlineMusicsList(){
        return onlineMusics;
    }
}

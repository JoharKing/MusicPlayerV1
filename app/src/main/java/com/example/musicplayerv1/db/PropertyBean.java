package com.example.musicplayerv1.db;

import android.content.Context;

import com.example.musicplayerv1.R;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class PropertyBean {
    public static String[] THEMES;
    private static String DEFAULT_THEME;
    //应用上下文
    private Context context;

    private String theme;
    public PropertyBean(Context context){
        this.context = context;
        //获取array.xml中的主题名称
        THEMES = context.getResources().getStringArray(R.array.theme);
        DEFAULT_THEME = THEMES[0];
        loadTheme();
    }

    //读取主题，保存在文件"configuration.cfg"中
    private void loadTheme() {
        Properties properties = new Properties();
        try {
            FileInputStream in = context.openFileInput("configuration.cfg");
            properties.load(in);
            theme = properties.getProperty("theme").toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //保存主题，保存在文件"configuration.cfg"中
    private boolean saveTheme(String theme){
        Properties propertise = new Properties();
        propertise.put("theme", theme);
        try {
            FileOutputStream out = context.openFileOutput("configuration.cfg",
                    Context.MODE_PRIVATE);
            propertise.store(out, "");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getTheme(){
        return theme;
    }

    public void setAndSaveTheme(String theme){
        this.theme = theme;
        saveTheme(theme);
    }
}

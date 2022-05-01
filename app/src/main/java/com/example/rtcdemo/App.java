package com.example.rtcdemo;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

import com.example.rtcdemo.bean.RoomInfoMessage;

import io.rong.imlib.RongIMClient;

public class App extends Application {
    public static final String APP_KEY = "lmxuhwagl6ddd";
//    public static final String APP_KEY = "cpj2xarlc2i7n";

    public static final String APP_SECRET = "X73cq2WP9keSot";
//    public static final String APP_SECRET = "1tgWoBhgc9Sxh";

    private static Context context;

    public static Context getAppContext(){
        return context;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        // 初始化 SDK，在整个应用程序全局只需要调用一次, 建议在 Application 继承类中调用。
        RongIMClient.init(this, APP_KEY, false);
        //注册自定义消息 （主播通过房间属性发送消息）
        RongIMClient.registerMessageType(RoomInfoMessage.class);
    }

}

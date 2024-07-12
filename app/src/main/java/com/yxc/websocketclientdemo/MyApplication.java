package com.yxc.websocketclientdemo;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import androidx.lifecycle.ViewModelStore;
import androidx.multidex.MultiDex;

public class MyApplication extends Application {

    private ViewModelStore mAppViewModelStore;
    private static MyApplication instance;
    private static Context mAppContext;

    public static final String STATUS_DONE = "status_done";
    public static String FIRST_TIME_MIGRATION = "first_time_migration";

    @Override
    public void onCreate() {
        super.onCreate();
        mAppViewModelStore = new ViewModelStore();
        instance = this;
        mAppContext = getApplicationContext();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    // 静态方法获取ApplicationContext
    public static Context getAppContext() {
        return mAppContext;
    }

    // 单例获取MyApplication实例
    public static MyApplication getInstance() {
        return instance;
    }
}
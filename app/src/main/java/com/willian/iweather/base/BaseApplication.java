package com.willian.iweather.base;

import android.app.Application;

import com.orhanobut.logger.Logger;

/**
 * Created by willian on 2017/1/18.
 */

public class BaseApplication extends Application {

    private static final String TAG = "WeatherLogger";

    @Override
    public void onCreate() {
        super.onCreate();
        // 自定义TAG
        Logger.init(TAG);
    }
}

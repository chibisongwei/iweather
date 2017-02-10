package com.willian.iweather.base;

import com.orhanobut.logger.LogLevel;
import com.orhanobut.logger.Logger;

import org.litepal.LitePalApplication;

/**
 * 基类Application
 */

public class BaseApplication extends LitePalApplication {

    private static final String TAG = "WeatherLogger";

    @Override
    public void onCreate() {
        super.onCreate();
        // 自定义Logger的TAG
        Logger.init(TAG).logLevel(LogLevel.FULL);
    }
}

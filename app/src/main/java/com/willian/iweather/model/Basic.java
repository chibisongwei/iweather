package com.willian.iweather.model;

import com.google.gson.annotations.SerializedName;

/**
 * Created by willian on 2017/2/7.
 */

public class Basic {

    @SerializedName("city")
    public String cityName;

    @SerializedName("id")
    public String weatherId;

    public Update update;

    public class Update {
        @SerializedName("loc")
        public String updateTime;
    }
}

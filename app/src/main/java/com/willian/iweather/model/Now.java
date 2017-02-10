package com.willian.iweather.model;

import com.google.gson.annotations.SerializedName;

/**
 * Created by willian on 2017/2/7.
 */

public class Now {

    @SerializedName("tmp")
    public String temperature;

    @SerializedName("cond")
    public More more;

    public class More {
        @SerializedName("txt")
        public String info;
    }
}

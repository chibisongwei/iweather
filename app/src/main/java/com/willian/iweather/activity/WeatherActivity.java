package com.willian.iweather.activity;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.willian.iweather.R;
import com.willian.iweather.model.Forecast;
import com.willian.iweather.model.Weather;
import com.willian.iweather.util.HttpUtil;
import com.willian.iweather.util.JsonUtil;
import com.willian.iweather.util.ToastUtil;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * 天气信息
 */

public class WeatherActivity extends AppCompatActivity {

    private ScrollView mScrollView;

    private TextView mTitleText;

    private TextView mUpdateTimeText;

    private TextView mDegreeText;

    private TextView mWeatherInfoText;

    private LinearLayout mForecastLayout;

    private TextView mAqiText;

    private TextView mPmText;

    private TextView mComfortText;

    private TextView mCarWashText;

    private TextView mSportText;

    private ImageView mBingImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= 21) {
            View dectorView = getWindow().getDecorView();
            dectorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        setContentView(R.layout.activity_weather);

        initView();
    }

    private void initView() {
        mScrollView = (ScrollView) findViewById(R.id.sv_weather);
        mTitleText = (TextView) findViewById(R.id.tv_title);
        mUpdateTimeText = (TextView) findViewById(R.id.tv_update_time);
        mDegreeText = (TextView) findViewById(R.id.tv_degree);
        mWeatherInfoText = (TextView) findViewById(R.id.tv_weather_info);

        mForecastLayout = (LinearLayout) findViewById(R.id.layout_forecast);
        mAqiText = (TextView) findViewById(R.id.tv_aqi);
        mPmText = (TextView) findViewById(R.id.tv_pm);
        mComfortText = (TextView) findViewById(R.id.tv_comfort);
        mCarWashText = (TextView) findViewById(R.id.tv_car);
        mSportText = (TextView) findViewById(R.id.tv_sport);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather", null);
        if (!TextUtils.isEmpty(weatherString)) {
            Weather weather = JsonUtil.parseWeatherResponse(weatherString);
            showWeatherInfo(weather);
        } else {
            String weatherId = getIntent().getStringExtra("weather_id");
            mScrollView.setVisibility(View.INVISIBLE);
            requestWeather(weatherId);
        }

        mBingImage = (ImageView) findViewById(R.id.iv_bing);
        String bingUrl = prefs.getString("bing_pic", null);
        if (!TextUtils.isEmpty(bingUrl)) {
            Glide.with(WeatherActivity.this).load(bingUrl).into(mBingImage);
        } else {
            loadBingPic();
        }
    }

    private void loadBingPic() {
        String requestUrl = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendHttpRequest(requestUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().toString();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic", bingPic);
                editor.apply();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic).into(mBingImage);
                    }
                });
            }
        });
    }

    /**
     * 根据id查询天气信息
     *
     * @param weatherId
     */
    private void requestWeather(final String weatherId) {
        String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=bc0418b57b2d4918819d3974ac1285d9";
        HttpUtil.sendHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToastUtil.showToast(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().toString();
                final Weather weather = JsonUtil.parseWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather != null && "ok".equals(weather.status)) {
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather", responseText);
                            editor.apply();
                            showWeatherInfo(weather);
                        } else {
                            ToastUtil.showToast(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT);
                        }
                    }
                });
            }
        });
    }


    private void showWeatherInfo(Weather weather) {
        String titleName = weather.basic.cityName;
        String updateTIme = weather.basic.update.updateTime.split(" ")[1];
        String degree = weather.now.temperature + "℃";
        String weatherInfo = weather.now.more.info;

        mTitleText.setText(titleName);
        mUpdateTimeText.setText(updateTIme);
        mDegreeText.setText(degree);
        mWeatherInfoText.setText(weatherInfo);

        mForecastLayout.removeAllViews();

        for (Forecast forecast : weather.forecastList) {
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, mForecastLayout, false);
            TextView dateText = (TextView) view.findViewById(R.id.tv_date);
            TextView infoText = (TextView) view.findViewById(R.id.tv_info);
            TextView maxText = (TextView) view.findViewById(R.id.tv_max);
            TextView minText = (TextView) view.findViewById(R.id.tv_min);

            dateText.setText(forecast.date);
            infoText.setText(forecast.more.info);
            maxText.setText(forecast.temperature.max);
            minText.setText(forecast.temperature.min);

            mForecastLayout.addView(view);
        }

        if (weather.aqi != null) {
            mAqiText.setText(weather.aqi.aqiCity.aqi);
            mPmText.setText(weather.aqi.aqiCity.pm25);
        }

        String comfort = "舒适度：" + weather.suggestion.comfort.info;
        String carWash = "洗车指数：" + weather.suggestion.carWash.info;
        String sport = "运动建议：" + weather.suggestion.sport.info;

        mComfortText.setText(comfort);
        mCarWashText.setText(carWash);
        mSportText.setText(sport);

        mScrollView.setVisibility(View.VISIBLE);
    }
}

package com.willian.iweather.exception;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.os.Process;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.widget.Toast;

import com.orhanobut.logger.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * UncaughtExceptionHandler异常处理类
 * 需要在Application中注册，为了要在应用启动开始就监控整个应用
 */

public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private Context mContext;
    // 系统默认的UncaughtException处理类
    private Thread.UncaughtExceptionHandler mDefaultHandler;
    // 用来存储设备信息和异常信息
    private Map<String, String> mInfos = new HashMap<String, String>();

    private DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

    private static final String TAG = "CrashHandler";

    private static final String APP_CACHE_PATH = Environment.getExternalStorageDirectory().getPath() + "/iweather/crach/";

    private static CrashHandler mInstance;

    private CrashHandler() {

    }

    /**
     * 单例模式
     *
     * @return
     */
    public static CrashHandler getInstance() {
        if (mInstance == null) {
            mInstance = new CrashHandler();
        }
        return mInstance;
    }

    /**
     * 初始化
     */
    public void init() {
        // 获取系统默认的UncaughtException处理器
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        // 设置该CrashHandler为程序的默认处理器
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    /**
     * 切换发生Crash所在的Activity
     */
    public void switchCrashActivity(Context context) {
        this.mContext = context;
    }

    /**
     * 当UncaughtException发生时，会转入该方法来处理
     *
     * @param thread
     * @param ex
     */
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        if (!handleException(ex) && mDefaultHandler != null) {
            //如果用户没有处理，则让系统默认的异常处理器来处理
            mDefaultHandler.uncaughtException(thread, ex);
        } else {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // 退出程序
            Process.killProcess(Process.myPid());
            System.exit(1);
        }
    }

    /**
     * 自定义错误处理，收集错误信息
     *
     * @param ex
     * @return
     */
    private boolean handleException(Throwable ex) {
        if (ex == null) {
            return false;
        }
        // 将crach发送到服务器
        sendCrashToServer(mContext, ex);

        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                Toast.makeText(mContext, "很抱歉，程序出现异常，即将退出", Toast.LENGTH_SHORT).show();
                Looper.loop();
            }
        }).start();

        // 保存日志文件
        saveCrashInfoInFile(ex);

        return true;
    }

    /**
     * 保存错误信息到文件中
     *
     * @param ex
     * @return 返回文件名称, 便于将文件传送到服务器
     */
    private String saveCrashInfoInFile(Throwable ex) {
        StringBuffer sb = new StringBuffer();
        for (Map.Entry<String, String> entry : mInfos.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            sb.append(key + "=" + value + "\n");
        }

        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        Throwable cause = ex.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.close();
        String result = writer.toString();
        sb.append(result);
        try {
            long timestamp = System.currentTimeMillis();
            String time = formatter.format(new Date());
            String fileName = "crash-" + time + "-" + timestamp + ".jpg";

            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                File dir = new File(APP_CACHE_PATH);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                FileOutputStream fos = new FileOutputStream(APP_CACHE_PATH + fileName);
                fos.write(sb.toString().getBytes());
                fos.close();
            }

            return fileName;

        } catch (Exception e) {
            Logger.e(e, TAG, "an error occured while writing file...");
        }

        return null;
    }

    /**
     * 收集程序奔溃的信息
     */
    private void sendCrashToServer(Context ctx, Throwable ex) {
        // 取出版本号
        PackageManager pm = ctx.getPackageManager();
        PackageInfo pi = null;
        try {
            pi = pm.getPackageInfo(ctx.getPackageName(), PackageManager.GET_ACTIVITIES);
            if (pi != null) {
                String versionName = pi.versionName == null ? "null" : pi.versionName;
                String versionCode = pi.versionCode + "";
                mInfos.put("versionName", versionName);
                mInfos.put("versionCode", versionCode);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        HashMap<String, String> exceptionInfo = new HashMap<>();
        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        String pageName = ctx.getClass().getName();
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        String memoryInfo = "MemoryInfo:" + mi.availMem + ",app holds:" + mi.threshold + ",Low Memory:" + mi.lowMemory;
        try {
            ApplicationInfo appInfo = ctx.getPackageManager().getApplicationInfo(ctx.getPackageName(), PackageManager.GET_META_DATA);
            String channelId = appInfo.metaData.getString("UMENG_CHANNEL");
            String version = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionName;

            exceptionInfo.put("PageName", pageName);
            exceptionInfo.put("ExceptionName", ex.getClass().getName());
            exceptionInfo.put("ExceptionType", "1");
            exceptionInfo.put("ExceptionsStackDetail", getStackTrace(ex));
            exceptionInfo.put("AppVersion", version);
            exceptionInfo.put("OSVersion", Build.VERSION.RELEASE);
            exceptionInfo.put("DeviceModel", Build.MODEL);
            exceptionInfo.put("DeviceId", getDeviceId(ctx));
            exceptionInfo.put("NetWorkType", String.valueOf(isWifi(ctx)));
            exceptionInfo.put("ChannelId", channelId);
            exceptionInfo.put("ClientType", "100");
            exceptionInfo.put("MemoryInfo", memoryInfo);

            final String requestParam = exceptionInfo.toString();

            new Thread(new Runnable() {
                @Override
                public void run() {

                }
            }).start();

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private int isWifi(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            return 1;
        }
        return 0;
    }

    /**
     * 获取手机设备号
     *
     * @return
     */
    private String getDeviceId(Context context) {
        String deviceId;
        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            deviceId = tm.getDeviceId();
            tm = null;
            if (TextUtils.isEmpty(deviceId)) {
                deviceId = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID);
            }
        } catch (Exception e) {
            deviceId = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID);
        }
        return deviceId;
    }

    private String getStackTrace(Throwable th) {
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);

        // If the exception was thrown in a background thread inside
        // AsyncTask, then the actual exception can be found with getCause
        Throwable cause = th;
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        final String stacktraceAsString = result.toString();
        printWriter.close();

        return stacktraceAsString;
    }
}

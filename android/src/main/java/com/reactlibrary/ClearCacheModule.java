package com.reactlibrary;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ClearCacheModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;
    static public ClearCacheModule myclearCacheModule;

    public ClearCacheModule(ReactApplicationContext reactContext) {
        super(reactContext);

        this.reactContext = reactContext;
        myclearCacheModule = this;
    }

    @NonNull
    @Override
    public String getName() {
        return "ClearCache";
    }

    private List<File> getCacheDirs() {
        List<File> list = new ArrayList<File>() {{
            // add(getReactApplicationContext().getFilesDir());
            add(getReactApplicationContext().getCacheDir());
        }};
        if (isMethodsCompat(android.os.Build.VERSION_CODES.FROYO)) {
            File externalCacheDir = getExternalCacheDir(getReactApplicationContext());//"<sdcard>/Android/data/<package_name>/cache/"
            list.add(externalCacheDir);
        }
        return list;
    }

    @ReactMethod
    public void getAppCacheSize(Callback callback) {
        long fileSize = 0;
        List<File> dirs = getCacheDirs();
        for (File dir : dirs) {
            fileSize += getDirSize(dir);
        }
        if (fileSize > 0) {
            String strFileSize = formatFileSize(fileSize);
            String unit = formatFileSizeName(fileSize);
            callback.invoke(strFileSize, unit);
        } else {
            WritableMap params = Arguments.createMap();
            callback.invoke("0", "B");
        }
    }

    @ReactMethod
    public void clearAppCache(Callback callback) {
        ClearCacheAsyncTask asyncTask = new ClearCacheAsyncTask(myclearCacheModule, callback);
        asyncTask.execute(10);
    }

    private long getDirSize(File dir) {
        if (dir == null) {
            return 0;
        }
        if (!dir.isDirectory()) {
            return 0;
        }
        long dirSize = 0;
        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isFile()) {
                dirSize += file.length();
            } else if (file.isDirectory()) {
                dirSize += file.length();
                dirSize += getDirSize(file);
            }
        }
        return dirSize;
    }

    private boolean isMethodsCompat(int VersionCode) {
        int currentVersion = android.os.Build.VERSION.SDK_INT;
        return currentVersion >= VersionCode;
    }

    private File getExternalCacheDir(Context context) {
        return context.getExternalCacheDir();
    }

    private String formatFileSizeName(long fileS) {
        java.text.DecimalFormat df = new java.text.DecimalFormat("#.00");
        String fileSizeString = "";
        if (fileS < 1024) {
            fileSizeString = "B";
        } else if (fileS < 1048576) {
            fileSizeString = "KB";
        } else if (fileS < 1073741824) {
            fileSizeString = "MB";
        } else {
            fileSizeString = "G";
        }
        return fileSizeString;
    }

    private String formatFileSize(long fileS) {
        java.text.DecimalFormat df = new java.text.DecimalFormat("#.00");
        String fileSizeString = "";
        if (fileS < 1024) {
            fileSizeString = df.format((double) fileS);
        } else if (fileS < 1048576) {
            fileSizeString = df.format((double) fileS / 1024);
        } else if (fileS < 1073741824) {
            fileSizeString = df.format((double) fileS / 1048576);
        } else {
            fileSizeString = df.format((double) fileS / 1073741824);
        }
        return fileSizeString;
    }

    public void clearCache() {
        getReactApplicationContext().deleteDatabase("webview.db");
        getReactApplicationContext().deleteDatabase("webview.db-shm");
        getReactApplicationContext().deleteDatabase("webview.db-wal");
        getReactApplicationContext().deleteDatabase("webviewCache.db");
        getReactApplicationContext().deleteDatabase("webviewCache.db-shm");
        getReactApplicationContext().deleteDatabase("webviewCache.db-wal");

        List<File> dirs = getCacheDirs();
        for (File dir : dirs) {
            clearCacheFolder(dir, System.currentTimeMillis());
        }
    }

    private int clearCacheFolder(File dir, long curTime) {
        int deletedFiles = 0;
        if (dir != null && dir.isDirectory()) {
            try {
                for (File child : dir.listFiles()) {
                    if (child.isDirectory()) {
                        deletedFiles += clearCacheFolder(child, curTime);
                    }
                    if (child.lastModified() < curTime) {
                        if (child.delete()) {
                            deletedFiles++;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return deletedFiles;
    }
}

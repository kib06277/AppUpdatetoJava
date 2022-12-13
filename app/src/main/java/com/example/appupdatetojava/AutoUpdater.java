package com.example.appupdatetojava;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoUpdater extends AppCompatActivity {

    // 是否是最新的应用,默认为false
    private Boolean isNew = false;
    private Boolean intercept = false;

    // 下载安装包的网络路径
    private String apkUrl = "http://122.146.250.130/TPMC_BID/Scripts/app-debug_toJava.apk";

    Context mContext;

    // 保存APK的文件夹
    private String savePath;
    private String saveFileName;

    // 下载线程
    Thread downLoadThread = null;
    int progress = 0; //當前進度

    // 进度条与通知UI刷新的handler和msg常量
    private ProgressBar mProgress;

    public AutoUpdater(Context context) {
        mContext = context;
        savePath = mContext.getExternalCacheDir().getPath().toString();
        saveFileName = savePath + "UpdateDemoRelease.apk";
    }

    /**
     * 检查是否更新的内容
     */
    public void checkUpdateInfo() {
        //这里的isNew本来是要从服务器获取的，我在这里先假设他需要更新
        if (isNew) {

        } else {
            showUpdateDialog();
        }
    }

    /**
     * 显示更新程序对话框，供主程序调用
     */
    private void showUpdateDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setCancelable(false);
        builder.setTitle("软件版本更新");
        builder.setMessage("有最新的软件包，请下载!");
        builder.setNegativeButton("下载", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                showDownloadDialog();
            }
        });
        builder.create().show();
    }

    /**
     * 显示下载进度的对话框
     */
    private void showDownloadDialog() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setView(R.layout.progressbar);
            builder.setCancelable(false);
            builder.setTitle("软件版本更新");

            LayoutInflater inflater = LayoutInflater.from(mContext);
            View v = inflater.inflate(R.layout.progressbar, null);
            mProgress = (ProgressBar) v.findViewById(R.id.progress);
            builder.setView(v);
            builder.show();
            downloadApk();
        } catch (Exception e) {
            Log.i("EE" , "e = " + e);
        }
    }

    /**
     * 从服务器下载APK安装包
     */
    private void downloadApk() {
        downLoadThread = new Thread(DownApkRunnable);
        downLoadThread.start();
    }

    Runnable DownApkRunnable = new Runnable(){
        @Override
        public void run() {
            URL url;
            try {
                url = new URL(apkUrl);
                URLConnection conn = url.openConnection();
                conn.connect();
                int length = conn.getContentLength();
                InputStream ins = conn.getInputStream();
                File file = new File(savePath);
                if (!file.exists()) {
                    file.mkdir();
                }
                File apkFile = new File(saveFileName);
                FileOutputStream fos = new FileOutputStream(apkFile);
                int count = 0;
                byte[] buf = new byte[1024];
                while (!intercept) {
                    int numread = ins.read(buf);
                    count = count + numread;
                    progress = (int)(count / length * 100);
                    // 下载进度
                    mHandler.sendEmptyMessage(1);
                    if (numread <= 0) {
                        // 下载完成通知安装
                        mHandler.sendEmptyMessage(2);
                        break;
                    }
                    fos.write(buf, 0, numread);
                }
                fos.close();
                ins.close();
            } catch (Exception e) {
                Log.i("EE" , "e = " + e);
            }
        }
    };

    /**
     * 安装APK内容
     */
    private void installAPK() {
        try {
            File file = new File(saveFileName);
            //判断是否是AndroidN以及更高的版本
            Intent intent = new Intent(Intent.ACTION_VIEW);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Uri contentUri = FileProvider.getUriForFile(mContext, "com.example.appupdatetojava.MainActivity", file);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
            } else {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
            }
            mContext.startActivity(intent);
        } catch (Exception e) {
            Log.i("Error" , "installAPK e = " + e);
        }
    }

    private Handler mHandler = new Handler(){
        public void handleMessage(Message msg){
            switch(msg.what){
                case 1:
                    mProgress.setProgress(progress);
                    break;
                case 2:
                    installAPK();
                    break;
            }
        }
    };
}
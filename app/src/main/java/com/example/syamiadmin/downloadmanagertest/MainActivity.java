package com.example.syamiadmin.downloadmanagertest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import com.arthar.downloadheighten.DownloadApi;
import com.arthar.downloadheighten.DownloadHelper;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private Button mButton;
    private ProgressBar mProgressBar;
    private Context mContext;
    private long downloadId;
    private DownloadHelper mDownloadHelper;
    private DownloadReceive mDownloadReceive;

//  private static final String url = "http://www.vsg3.com/game_down/index/?gameid=25";

    private static final String url = "http://dldir1.qq.com/qqfile/QQIntl/QQi_wireless/Android/qqi_5.0.10.6046_android_office.apk";
    private static final String packageName = "com.tencent.mobileqqi";

//  private static final String url = "http://g33.gdl.netease.com/thdmx-1.0.5.apk";
//  private static final String packageName = "com.netease.thdmx";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;

        //初始化下载帮助类
        mDownloadHelper = DownloadHelper.getInstance(mContext);

        mButton = (Button)findViewById(R.id.download);
        mButton.setTag(R.id.tag_button_download_status,false);
        mProgressBar = (ProgressBar)findViewById(R.id.progressbar);

        mDownloadReceive = new DownloadReceive();

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if((Boolean) view.getTag(R.id.tag_button_download_status)){
                    mDownloadHelper.removeRequest(downloadId);
                }else{

                    downloadId = mDownloadHelper.Download(url,"青丘狐","一款好游戏",packageName);
                }
            }
        });

    }

    private void registerDownloadReceiver(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(DownloadApi.HS_DOWNLOAD_ACTION);
        registerReceiver(mDownloadReceive, filter);
    }

    @Override
    protected void onResume(){
        super.onResume();
        registerDownloadReceiver();
    }

    @Override
    protected void onPause(){
        super.onPause();
        unregisterReceiver(mDownloadReceive);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        print("destroy");

    }

    private class DownloadReceive extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {

            int count = intent.getIntExtra(DownloadApi.DOWNLOAD_FILE_COUNT,-1);
            int length = intent.getIntExtra(DownloadApi.DOWNLOAD_FILE_LENGTH,-1);
            print("进度count" + count);
            print("进度length" + length);

            if((count == DownloadApi.DOWNLOAD_ID_FAILED) && (length == DownloadApi.DOWNLOAD_ID_FAILED)){

                mButton.setText(R.string.download);
                mButton.setTag(R.id.tag_button_download_status,false);
                mProgressBar.setProgress(0);
                return;
            }

            if(count == length){

                mButton.setText(R.string.download);
                mButton.setTag(R.id.tag_button_download_status,false);
            }else{

                mButton.setText(R.string.cancel);
                mButton.setTag(R.id.tag_button_download_status,true);
            }
            int progress = (int) (count*100f/length);
            print("进度" + progress);

            mProgressBar.setProgress(progress);
        }
    }

    private void print(String msg){
        Log.d(TAG,"ZZY=========" + msg);
    }
}

package com.arthar.downloadheighten;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.text.format.Formatter;
import android.util.Log;

import java.util.HashMap;
import java.util.List;

/**
 * Created by syamiadmin on 2016/4/14.
 */
public class DownloadHelper {
    private static final String TAG = DownloadHelper.class.getSimpleName();
    private static volatile DownloadHelper sDownloadHelper = null;

    private Context mContext;
    private DownloadManager mDownloadManager;

    /**管理下载过程中的观察者**/
    private HashMap<String,DownloadChangeObserver> mObserverManager;

    private Handler mHandler = new Handler();

    public static DownloadHelper getInstance(Context context){
        DownloadHelper inst = sDownloadHelper;
        if(inst == null){
            synchronized (DownloadHelper.class){
                inst = sDownloadHelper;
                if(inst == null){
                    inst = new DownloadHelper(context);
                    sDownloadHelper = inst;
                }
            }
        }
        return inst;
    }

    private DownloadHelper(Context context){

        mContext = context.getApplicationContext();
        mDownloadManager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);

//        Intent intent = new Intent(mContext,DownloadService.class);
//        mContext.startService(intent);
        mObserverManager = new HashMap<>();
    }

    /**
     * 下载接口
     * @param url
     * @param gameName
     * @param desc
     * @param packageName
     */
    public long Download(String url ,String gameName,String desc, String packageName){

        /**根据包名尝试打开应用**/
        if(isInstalled(packageName)){

            openApp(packageName);
            return DownloadApi.DOWNLOAD_STATUS_INSTALLED;
        }

        /**尝试查找本地已下载资源，找到则弹出安装提示**/
        if (isDownloadLocal(url)) {

            return DownloadApi.DOWNLOAD_STATUS_LOCAL;
        }

        Uri uri = Uri.parse(url);
        DownloadManager.Request request = new DownloadManager.Request(uri);

        /**设置只在wifi环境下下载**/
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);

        /**指定存储文件的路径是应用在外部存储中的专用文件夹**/
        request.setDestinationInExternalFilesDir(mContext, Environment.DIRECTORY_DOWNLOADS, packageName + DownloadApi.SUFFIXES);

        /**只在下载过长中显示notification**/
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);

        /**设置下载通知栏提示的title*/
        request.setTitle(gameName);

        /**设置下载通知栏提示的描述**/
        request.setDescription(desc);

        /**下载完成后点击的打开方式**/
        request.setMimeType("application/vnd.android.package-archive");

        /**不允许扫描与关联**/
        request.setAllowedOverRoaming(false);

        /**不被系统下载管理器管理**/
        request.setVisibleInDownloadsUi(false);

        long downloadId = mDownloadManager.enqueue(request);

        saveDownloadFileId(url,String.valueOf(downloadId));
        /**注册观察者**/
        registerObserver(downloadId);
        /**下载开始并返回id，reference变量是系统为当前的下载请求分配的一个唯一的ID**/
        return downloadId;
    }

    /**
     * 判断是否已下载在本地
     * @param url
     * @return
     */
    public boolean isDownloadLocal(String url) {

        SharedPreferences sharedPreferences = getDownLoadingMap();
        String saveId = sharedPreferences.getString(url,DownloadApi.PLACE_HOLDER);
        if(saveId.equals(DownloadApi.PLACE_HOLDER)){

            removeDownloadFileId(url);
            print("本地不存在安装包，删除记录");
            return false;
        }else{
            print("本地存在安装包，尝试打开");
            if(installApp(Long.valueOf(saveId))){
                return true;
            }
            removeDownloadFileId(url);
            return false;
        }
    }

    /**
     * 删除正在下载的任务
     * @param id
     */
    public void removeRequest(long id){
        print("取消下载任务，删除观察者监听");
        mDownloadManager.remove(id);
        unregisterObserver(id);
        Intent intent = new Intent();
        intent.setAction(DownloadApi.HS_DOWNLOAD_ACTION);
        intent.putExtra(DownloadApi.DOWNLOAD_FILE_COUNT, DownloadApi.DOWNLOAD_ID_FAILED);
        intent.putExtra(DownloadApi.DOWNLOAD_FILE_LENGTH, DownloadApi.DOWNLOAD_ID_FAILED);
        mContext.sendBroadcast(intent);
    }

    /**
     * 注册观察者
     * @param id
     */
    public void registerObserver(long id){
        DownloadChangeObserver downloadObserver = new DownloadChangeObserver(mContext, mHandler, id);
        mObserverManager.put(Long.toString(id),downloadObserver);
        mContext.getContentResolver().registerContentObserver(DownloadApi.CONTENT_URI, true, downloadObserver);
    }

    /**
     * 取消观察
     * @param key
     */
    public void unregisterObserver(long key){

        DownloadChangeObserver downloadChangeObserver = mObserverManager.get(Long.toString(key));
        if (downloadChangeObserver != null) {

            mContext.getContentResolver().unregisterContentObserver(downloadChangeObserver);
        }

        mObserverManager.remove(Long.toString(key));
    }

    /**
     * 保存正在下载的应用
     * @param url
     * @param downloadId
     */
    public void  saveDownloadFileId(String url, String downloadId){
        print("保存下载列表key：" + url);
        SharedPreferences sPreferences = mContext.getSharedPreferences(DownloadApi.HS_GAME_DOWNLOADING, Context.MODE_APPEND);
        sPreferences.edit().putString(url, downloadId).commit();
    }

    /**
     * 删除正在下载列表的key
     * @param url
     */
    public void removeDownloadFileId(String url){
        print("删除下载列表key：" + url);
        SharedPreferences sPreferences = mContext.getSharedPreferences(DownloadApi.HS_GAME_DOWNLOADING, Context.MODE_APPEND);
        sPreferences.edit().remove(url).commit();
    }

    /**
     * 获取正在下载列表
     * @return
     */
    public SharedPreferences getDownLoadingMap(){
        print("获取正在下载列表");
        return mContext.getSharedPreferences(DownloadApi.HS_GAME_DOWNLOADING, Context.MODE_APPEND);
    }

    /**
     * 安装已下载的app
     * @param completeId
     */
    public boolean installApp(long completeId) {

        Uri downloadFileUri = mDownloadManager.getUriForDownloadedFile(completeId);

        if(downloadFileUri != null){
            Intent install = new Intent(Intent.ACTION_VIEW);
            install.setDataAndType(downloadFileUri, DownloadApi.APP_INSTALL);
            install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(install);
            return true;
        }

        return false;
    }

    /**判断app是否安装**/
    private boolean isInstalled(String packageName) {
        boolean hasInstalled = false;
        PackageManager pm = mContext.getPackageManager();
        List<PackageInfo> packageInfos = pm.getInstalledPackages(PackageManager.GET_INSTRUMENTATION);
        for (PackageInfo packageInfo : packageInfos) {
            if (packageName != null && packageName.equals(packageInfo.packageName)) {
                hasInstalled = true;
                break;
            }
        }
        return hasInstalled;
    }

    /**尝试打开app**/
    public void openApp(String packageName){
        try{
            Intent intent = mContext.getPackageManager().getLaunchIntentForPackage(packageName);
            mContext.startActivity(intent);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void queryDownloadStatus(long downloadId) {

        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);

        Cursor cursor = mDownloadManager.query(query);
        try {
            if (cursor != null && cursor.moveToFirst()) {

                int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                int reasonIdx = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
                int titleIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE);
                int fileSizeIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
                int bytesDLIdx = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);

                String title = cursor.getString(titleIdx);
                int fileSize = cursor.getInt(fileSizeIdx);
                int bytesDL = cursor.getInt(bytesDLIdx);
                // Translate the pause reason to friendly text.
                int reason = cursor.getInt(reasonIdx);

                print("下载完成度" + title + Formatter.formatFileSize(mContext,bytesDL) + "/" + Formatter.formatFileSize(mContext,fileSize));

                Intent intent = new Intent();
                intent.setAction(DownloadApi.HS_DOWNLOAD_ACTION);
                intent.putExtra(DownloadApi.DOWNLOAD_FILE_COUNT, bytesDL);
                intent.putExtra(DownloadApi.DOWNLOAD_FILE_LENGTH, fileSize);
                mContext.sendBroadcast(intent);

                switch (status) {
                    case DownloadManager.STATUS_PAUSED:

                        print("某种原因导致暂停" + reason);
                        break;
                    case DownloadManager.STATUS_PENDING:

                        print("准备下载");
                        break;
                    case DownloadManager.STATUS_RUNNING:

                        print("正在下载。。。");
                        break;
                    case DownloadManager.STATUS_SUCCESSFUL:

                        DownloadCompleted(downloadId);
                        print("下载完成");
                        break;
                    case DownloadManager.STATUS_FAILED:

                        print("清除已下载的内容，重新下载");
                        break;

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cursor.close();
        }
    }

    /**下载完成**/
    private void DownloadCompleted(long downloadId){

        installApp(downloadId);
        unregisterObserver(downloadId);
    }

    private void print(String msg){
        Log.d(TAG,"ZZY=========" + msg);
    }
}

package com.arthar.downloadheighten;

import android.net.Uri;

/**
 * Created by syamiadmin on 2016/4/14.
 */
public class DownloadApi {

    /**下载文件的后缀**/
    public static final String SUFFIXES = ".apk";

    /**正在下载列表**/
    public static final String HS_GAME_DOWNLOADING = "hs_downloading";

    /**DownloadManager下载管理器的uri**/
    public static final Uri CONTENT_URI = Uri.parse("content://downloads/my_downloads");

    /**apk安装**/
    public static final String APP_INSTALL = "application/vnd.android.package-archive";

    /**下载动态广播action**/
    public static final String HS_DOWNLOAD_ACTION = "app.vsg3.com.hsGame.downloading";

    /**已下载长度**/
    public static final String DOWNLOAD_FILE_COUNT = "count";

    /**总长度**/
    public static final String DOWNLOAD_FILE_LENGTH = "length";

    /**本地已安装**/
    public static final long DOWNLOAD_STATUS_INSTALLED = 1;

    /**本地已下载**/
    public static final long DOWNLOAD_STATUS_LOCAL = 2;

    /**正在下载中**/
    public static final long DOWNLOAD_STATUS_RUNNING = 3;

    /**下载id缺省值**/
    public static final int DOWNLOAD_ID_FAILED = -1;

    /**资源占位符**/
    public static final String PLACE_HOLDER = "";

}

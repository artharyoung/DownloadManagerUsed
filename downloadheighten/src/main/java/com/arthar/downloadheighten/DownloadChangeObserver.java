package com.arthar.downloadheighten;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;

/**
 * Created by syamiadmin on 2016/4/14.
 */
public class DownloadChangeObserver extends ContentObserver {

    private Context mContext;
    private long mDownloadId;
    public DownloadChangeObserver(Context context,Handler handler,long downloadId) {
        super(handler);
        mContext = context;
        mDownloadId = downloadId;
    }

    @Override
    public void onChange(boolean selfChange) {

        DownloadHelper.getInstance(mContext).queryDownloadStatus(mDownloadId);
    }
}
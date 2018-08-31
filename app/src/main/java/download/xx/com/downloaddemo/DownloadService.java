package download.xx.com.downloaddemo;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.io.File;

public class DownloadService extends Service {
    //通知栏没显示可能是手机设置问题

    private DownloadTask downloadTask;
    private String downloadUrl;
    private int lastProgress;

    private DownloadListener downloadListener = new DownloadListener() {
        @Override
        public void onPaused(int lastProgress) {
            downloadTask = null;
            getNotificationManager().notify(1,getNotification("已暂停",lastProgress));
            toastShort("下载暂停");
        }

        @Override
        public void onSuccess() {
            downloadTask = null;
            stopForeground(true);
            getNotificationManager().notify(1,getNotification("下载成功",-1));
            toastShort("下载成功");
        }

        @Override
        public void onFailed() {
            downloadTask = null;
            stopForeground(true);
            getNotificationManager().notify(1,getNotification("下载失败",-1));
            toastShort("下载失败");
        }

        @Override
        public void onProgress(int progress) {
            getNotificationManager().notify(1,getNotification("下载中...",progress));
        }

        @Override
        public void onCanceled() {
            downloadTask = null;
            stopForeground(true);
            toastShort("取消下载");
        }
    };

    class DownloadBinder extends Binder{
        public void startDownload(String downloadUrl){
            if(downloadTask == null){
                DownloadService.this.downloadUrl = downloadUrl;
                downloadTask = new DownloadTask(downloadListener);
                downloadTask.execute(downloadUrl);
                startForeground(1,getNotification("下载中...",0));
                toastShort("下载中");
            }
        }
        public void pauseDownload(){
            if(downloadTask != null){
                downloadTask.pauseDownload();
            }
        }
        public void canceledDownload(){
            if(downloadTask != null){
                downloadTask.cancelDownload();
            }
            if(downloadUrl != null){
                //因为可能先暂停后才点的取消，已经退出downloadTask，所以要重新删除
                String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
                String directory  = Environment.getExternalStoragePublicDirectory
                        (Environment.DIRECTORY_DOWNLOADS).getPath();
                File file = new File(directory + fileName);
                if(file.exists()){
                    file.delete();
                }
                getNotificationManager().cancel(1);
                stopForeground(true);
            }

        }
    }
    private DownloadBinder mBinder = new DownloadBinder();
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     *
     * @param title
     * @param progress
     * @return
     */
    private Notification getNotification(String title,int progress){
        Intent intent = new Intent(this,MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this,0,intent,0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);//
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher));
        builder.setContentIntent(pi);
        builder.setContentTitle(title);
        if(progress >= 0){
            builder.setContentText(progress + "%");
            builder.setProgress(100,progress,false);
        }
        return builder.build();
    }

    /**
     *
     * @return
     */
    private NotificationManager getNotificationManager(){
        return (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    }

    private void toastShort(String text){
        Toast.makeText(this,text,Toast.LENGTH_SHORT).show();
    }
}

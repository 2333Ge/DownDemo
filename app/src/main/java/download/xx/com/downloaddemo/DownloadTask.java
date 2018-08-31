package download.xx.com.downloaddemo;

import android.os.AsyncTask;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadTask extends AsyncTask<String,Integer,Integer> {

    public static final  int TYPE_PAUSED = 0;
    public static final  int TYPE_FAILED = 1;
    public static final  int TYPE_SUCCESS = 2;
    public static final  int TYPE_CANCELED = 3;


    private boolean isPaused = false;
    private boolean isCanceled = false;

    private int lastProgress = 0;//为什么要记录上一次

    private DownloadListener downloadListener;

    public DownloadTask(DownloadListener downloadListener){
        this.downloadListener = downloadListener;
    }

    @Override
    protected Integer doInBackground(String... strings) {
        String downloadUrl = strings[0];
        File file = null;
        RandomAccessFile randomAccessFile = null;
        InputStream is = null;
        long downloadLength = 0;
        //创建本地存储
        String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
        String directory  = Environment.getExternalStoragePublicDirectory
                (Environment.DIRECTORY_DOWNLOADS).getPath();
        file = new File(directory + fileName);

        if(file.exists()){
            downloadLength = file.length();
        }
        try {
            long contentLength = getContentLength(downloadUrl);
            if(contentLength == 0){
                return TYPE_FAILED;
            }else if(contentLength == downloadLength){
                return TYPE_SUCCESS;
            }
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .addHeader("RANGE","byte=" + downloadLength + "-")
                    .url(downloadUrl)
                    .build();
            Response response = client.newCall(request).execute();
            if(response != null){
                is = response.body().byteStream();
                randomAccessFile = new RandomAccessFile(file,"rw");
                randomAccessFile.seek(downloadLength);//跳过下载的地方
                byte[] b = new byte[1024];
                long total = 0;
                int len;
                while((len = is.read(b)) != -1){
                    if(isCanceled){
                        return TYPE_CANCELED;
                    }else if(isPaused){
                        return TYPE_PAUSED;
                    }else {
                        total += len;
                        randomAccessFile.write(b,0,len);
                        int progress = (int)((total + downloadLength) * 100 / contentLength);
                        publishProgress(progress);
                    }
                }
                response.body().close();
                return TYPE_SUCCESS;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try{
                if (is !=  null){
                    is.close();
                }
                if ( randomAccessFile != null){
                    randomAccessFile.close();
                }
                if (isCanceled && file != null){
                    file.delete();//为什么在这没删掉？？
//                    一般来说 java file.delete失败 有以下几个原因
//                    1.看看是否被别的进程引用，手工删除试试(删除不了就是被别的进程占用)
//                    2.file是文件夹 并且不为空，有别的文件夹或文件，
//                    3.极有可能有可能自己前面没有关闭此文件的流(我遇到的情况)
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return TYPE_FAILED;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        int progress = values[0];
        if(progress > lastProgress){
            downloadListener.onProgress(progress);//传递下载进度
            lastProgress = progress;
        }
    }

    @Override
    protected void onPostExecute(Integer status) {
        super.onPostExecute(status);
        switch (status){
            case TYPE_CANCELED:
                downloadListener.onCanceled();
                break;
            case TYPE_FAILED:
                downloadListener.onFailed();
                break;
            case TYPE_PAUSED:
                downloadListener.onPaused(lastProgress);
                break;
            case TYPE_SUCCESS:
                downloadListener.onSuccess();
                break;
            default:
                break;

        }
    }
    public void pauseDownload(){
        isPaused = true;
    }
    public void cancelDownload(){
        isCanceled = true;
    }

    private long getContentLength(String downloadUrl) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(downloadUrl)
                .build();
        Response response = client.newCall(request).execute();
        if(response != null&& response.isSuccessful()){
            long contentLength = response.body().contentLength();
            response.body().close();
            return contentLength;
        }
        return 0;
    }
}

package download.xx.com.downloaddemo;

public interface DownloadListener {
    void onPaused(int lastProgress);
    void onSuccess();
    void onFailed();
    void onProgress(int progress);
    void onCanceled();
}

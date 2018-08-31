package download.xx.com.downloaddemo;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private Button b_startOrPause;
    private Button b_cancel;
    private String downloadUrl = "https://raw.githubusercontent.com/guolindev/eclipse/master/eclipse-inst-win64.exe";
    private int REQUEST_WRITE_EXTERNAL_STORAGE = 0;
    private DownloadService.DownloadBinder downloadBinder;
    private boolean isDownload = true;
    private ServiceConnection  serviceConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            downloadBinder = (DownloadService.DownloadBinder) iBinder;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermission();
        initView();
        initService();
    }

    private void initService() {
        Intent intent = new Intent(this,DownloadService.class);
        startService(intent);
        bindService(intent,serviceConnection,BIND_AUTO_CREATE);
    }

    private void initView() {
        b_startOrPause = findViewById(R.id.b_startOrPause);
        b_cancel = findViewById(R.id.b_cancel);
        b_startOrPause.setOnClickListener(downloadBtnClick);
        b_cancel.setOnClickListener(downloadCancelBtnClick);
    }

    private View.OnClickListener downloadBtnClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (downloadBinder == null)
                return;
            if (isDownload){
                downloadBinder.startDownload(downloadUrl);
                b_startOrPause.setText("暂停下载");
                isDownload = false;
            }else{
                downloadBinder.pauseDownload();
                b_startOrPause.setText("开始下载");
                isDownload = true;
            }

        }
    };
    private View.OnClickListener downloadCancelBtnClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (downloadBinder == null)
                return;
            downloadBinder.canceledDownload();
            b_startOrPause.setText("开始下载");
            isDownload = true;
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
    }
    /**android6.0以上权限申请*/
    private void checkPermission() {
        //检查权限（NEED_PERMISSION）是否被授权 PackageManager.PERMISSION_GRANTED表示同意授权
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            //用户已经拒绝过一次，再次弹出权限申请对话框需要给用户一个解释,用户选择不再询问时，此方法返回 false。
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission
                    .WRITE_EXTERNAL_STORAGE)) {
                Toast.makeText(this, "请开通相关权限，否则无法正常使用本应用！", Toast.LENGTH_SHORT).show();
            }
            //申请权限,第三个参数是请求码便于在onRequestPermissionsResult 方法中根据requestCode进行判断.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS}, REQUEST_WRITE_EXTERNAL_STORAGE);

        } else {

            Log.d("checkPermission", "checkPermission: 已经授权！");
        }
    }
}

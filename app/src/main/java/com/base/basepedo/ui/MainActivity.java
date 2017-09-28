package com.base.basepedo.ui;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.base.basepedo.R;
import com.base.basepedo.base.StepMode;
import com.base.basepedo.config.Constant;
import com.base.basepedo.pojo.Acceleration;
import com.base.basepedo.pojo.Gravity;
import com.base.basepedo.service.StepService;

import java.util.List;

public class MainActivity extends AppCompatActivity implements Handler.Callback, View.OnClickListener {
    private final String TAG = MainActivity.class.getSimpleName();
    //循环取当前时刻的步数中间的间隔时间
    private long TIME_INTERVAL = 500;
    private TextView text_step;
    private Messenger messenger;
    private Messenger mGetReplyMessenger = new Messenger(new Handler(this));

    private TextView tv_acceleration_x;
    private TextView tv_acceleration_y;
    private TextView tv_acceleration_z;

    private TextView tv_gyroscope_x;
    private TextView tv_gyroscope_y;
    private TextView tv_gyroscope_z;
    private TextView tv_gyroscope_a;

    private TextView tv_gravity_x;
    private TextView tv_gravity_y;
    private TextView tv_gravity_z;

    private Button bt_reset;


    private Handler delayHandler;
    ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("MainActivity", "onServiceConnected");
            try {
                messenger = new Messenger(service);
                Message msg = Message.obtain(null, Constant.MSG_FROM_CLIENT);
                msg.replyTo = mGetReplyMessenger;
                messenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case Constant.MSG_FROM_SERVER:
                // 更新界面上的步数
                text_step.setText(msg.getData().getInt("step") + "");
                Bundle bundle = msg.getData();
                if (bundle != null) {
                    Acceleration acc = (Acceleration) bundle.getSerializable("acceleration");
                    float[] deltaRotationVector = (float[]) bundle.getSerializable("gyroscope");
                    Gravity gravity = (Gravity) bundle.getSerializable("gravity");
                    tv_acceleration_x.setText(String.valueOf(acc.getX()));
                    tv_acceleration_y.setText(String.valueOf(acc.getY()));
                    tv_acceleration_z.setText(String.valueOf(acc.getZ()));

                    tv_gyroscope_x.setText(String.valueOf(deltaRotationVector[0]));
                    tv_gyroscope_y.setText(String.valueOf(deltaRotationVector[1]));
                    tv_gyroscope_z.setText(String.valueOf(deltaRotationVector[2]));
                    tv_gyroscope_a.setText(String.valueOf(deltaRotationVector[3]));

                    tv_gravity_x.setText(String.valueOf(gravity.getX()));
                    tv_gravity_y.setText(String.valueOf(gravity.getY()));
                    tv_gravity_z.setText(String.valueOf(gravity.getZ()));
                }
                delayHandler.sendEmptyMessageDelayed(Constant.REQUEST_SERVER, TIME_INTERVAL);
                break;
            case Constant.REQUEST_SERVER:
                try {
                    Message msg1 = Message.obtain(null, Constant.MSG_FROM_CLIENT);
                    msg1.replyTo = mGetReplyMessenger;
                    messenger.send(msg1);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            case Constant.DISPLAY_ACCELERATION:
                // Acceleration acc = (Acceleration) msg.obj;
                // tv_acceleration_x.setText(String.valueOf(acc.getX()));
                // tv_acceleration_y.setText(String.valueOf(acc.getY()));
                // tv_acceleration_z.setText(String.valueOf(acc.getZ()));
                break;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        // Log.d("MainActivity", "...");
        startServiceForStrategy();
        // checkExternalPermission();
    }

    public static final int REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 0x01;

    private void checkExternalPermission() {
        boolean b = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        Log.d("MainActivity", "b:" + b);

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            //RUNTIME PERMISSION Android M
            Log.d("MainActivity", "MEDIA_MOUNTED");
            if (PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "myPhoto");
            } else {
                Log.d("MainActivity", "no request");
                Log.d("MainActivity", "no request");
                requestPermission(this);
            }
        }
    }

    private static void requestPermission(final Context context){
        if(ActivityCompat.shouldShowRequestPermissionRationale((Activity)context,Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            new AlertDialog.Builder(context)
                    .setMessage(context.getResources().getString(R.string.permission_storage))
                    .setPositiveButton("好", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions((Activity) context,
                                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                    REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE);
                        }
                    }).show();
        }else {
            // permission has not been granted yet. Request it directly.
            ActivityCompat.requestPermissions((Activity)context,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE: {
                if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this,
                            getResources().getString(R.string.permission_storage_success),
                            Toast.LENGTH_SHORT).show();

                } else {
                    Toast.makeText(this,
                            getResources().getString(R.string.permission_storage_failure),
                            Toast.LENGTH_SHORT).show();
                    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                }
                return;
            }
        }
    }

    private void startServiceForStrategy() {
        if (!isServiceWork(this, StepService.class.getName())) {
            setupService(true);
            Log.d("MainActivity", StepService.class.getName() + " is work");
        } else {
            setupService(false);
            Log.d("MainActivity", StepService.class.getName() + " is not work");
        }
    }


    private void init() {
        text_step = (TextView) findViewById(R.id.text_step);
        delayHandler = new Handler(this);

        tv_acceleration_x = (TextView) findViewById(R.id.tv_acceleration_x);
        tv_acceleration_y = (TextView) findViewById(R.id.tv_acceleration_y);
        tv_acceleration_z = (TextView) findViewById(R.id.tv_acceleration_z);

        tv_gyroscope_x = (TextView) findViewById(R.id.tv_gyroscope_x);
        tv_gyroscope_y = (TextView) findViewById(R.id.tv_gyroscope_y);
        tv_gyroscope_z = (TextView) findViewById(R.id.tv_gyroscope_z);
        tv_gyroscope_a = (TextView) findViewById(R.id.tv_gyroscope_a);

        tv_gravity_x = (TextView) findViewById(R.id.tv_gravity_x);
        tv_gravity_y = (TextView) findViewById(R.id.tv_gravity_y);
        tv_gravity_z = (TextView) findViewById(R.id.tv_gravity_z);

        bt_reset = (Button) findViewById(R.id.bt_reset);
        bt_reset.setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        setupService(true);
    }

    /**
     * 启动service
     *
     * @param flag true-bind和start两种方式一起执行 false-只执行bind方式
     */
    private void setupService(boolean flag) {
        Intent intent = new Intent(this, StepService.class);
        boolean b = bindService(intent, conn, Context.BIND_AUTO_CREATE);
        // Log.d("MainActivity", "b:" + b);
        if (flag) {
            startService(intent);
        }
    }


    /**
     * 判断某个服务是否正在运行的方法
     *
     * @param mContext
     * @param serviceName 是包名+服务的类名（例如：net.loonggg.testbackstage.TestService）
     * @return true代表正在运行，false代表服务没有正在运行
     */
    public boolean isServiceWork(Context mContext, String serviceName) {
        boolean isWork = false;
        ActivityManager myAM = (ActivityManager) mContext
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> myList = myAM.getRunningServices(40);
        Log.d("MainActivity", "myList.size():" + myList.size());
        if (myList.size() <= 0) {
            return false;
        }
        for (int i = 0; i < myList.size(); i++) {
            String mName = myList.get(i).service.getClassName().toString();
            if (mName.equals(serviceName)) {
                isWork = true;
                Log.d("MainActivity", "find stepService");
                break;
            }
        }
        return isWork;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        // TODO
        moveTaskToBack(true);
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(conn);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_reset:
                StepMode.CURRENT_SETP = 0;
                break;
        }
    }
}

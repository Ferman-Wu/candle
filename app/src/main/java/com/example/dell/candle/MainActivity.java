package com.example.dell.candle;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import static android.media.AudioFormat.CHANNEL_IN_MONO;
import static android.media.AudioFormat.ENCODING_PCM_16BIT;
import static android.media.MediaRecorder.AudioSource.MIC;

public class MainActivity extends AppCompatActivity {
    public static final int UPDATE_CANDLE = 1;
    public static final int LOCK_SCREEN = 2;
    public static boolean cState = true;
    private DevicePolicyManager devicePolicyManager;  //DPM
    private ComponentName componentName;
    static final int bufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_DEFAULT, ENCODING_PCM_16BIT); //录音buffer大小
    private ImageView candleImg;      //蜡烛图片
    private Button openAdmin;         //开启权限按钮
    private AudioRecord audioRecord;  //录音
    private Handler handler = new Handler(){
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_CANDLE:  //  更新candle UI图片
                    candleImg.setImageResource(R.drawable.off);
                    break;
                case LOCK_SCREEN:   // 锁屏
                    if (devicePolicyManager.isAdminActive(componentName)) {
                        devicePolicyManager.lockNow();
                        finish();
                    }
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        candleImg = findViewById(R.id.candleON);

        devicePolicyManager = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);
        componentName = new ComponentName(this,Admin.class);


        openAdmin = findViewById(R.id.openAdmin);
        openAdmin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {  //开启权限
                //设置激活超级管理员
                Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                //设置激活那个超级管理员
                //mDeviceAdminSample : 超级管理员的标示
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
                //设置描述信息
                intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,"吹蜡烛锁屏");
                startActivity(intent);
            }
        });

        //初始化录音
        audioRecord = new AudioRecord(MIC,44100,CHANNEL_IN_MONO,ENCODING_PCM_16BIT,bufferSize);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                audioRecord.startRecording();  //开始录音
                while(true) {
                    double volume = getVoiceDB();  //当前分贝
                    if (volume > 60) {   //不超过60db
                        Message message = new Message();
                        message.what = UPDATE_CANDLE; //  更新candle UI图片
                        handler.sendMessage(message); //  将Message
                        try{
                            Thread.sleep(1000); // 休眠1秒
                            message.what = LOCK_SCREEN;   //休眠一秒锁屏
                            handler.sendMessage(message); //  将Message
                        }catch (Exception e){
                            e.printStackTrace();
                        }


                        break;
                    }
                }
                audioRecord.stop();
                devicePolicyManager.lockNow();

            }
        });
        thread.start();
    }

    public double getVoiceDB(){  //计算分贝值
        short[] buffer = new short[bufferSize];
        int r = audioRecord.read(buffer, 0, bufferSize);
        long v = 0;
        // 将 buffer 内容取出，进行平方和运算
        for (int i = 0; i < buffer.length; i++) {
            v += buffer[i] * buffer[i];
        }
        double mean = v / (double) r;
        double volume = 10 * Math.log10(mean);
        Log.i("分贝值:", "" + volume);
        return volume;
    }

    public void delete(View v){   //注销
        //注销超级管理员
        //判断超级管理员是否激活
        if (devicePolicyManager.isAdminActive(componentName)) {
            //注销超级管理员
            devicePolicyManager.removeActiveAdmin(componentName);
        }
    }

}



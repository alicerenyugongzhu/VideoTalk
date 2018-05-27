package com.alice.videotalk;

import android.Manifest;
import android.app.Activity;
import android.app.MediaRouteActionProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaActionSound;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import master.flame.danmaku.controller.DrawHandler;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.IDanmakus;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.ui.widget.DanmakuView;


/**
 * Created by alice on 2018/3/24.
 */

public class VideoPlay extends Activity {

    private boolean showDanm;

    private DanmakuView danmView;

    private DanmakuContext danmContext;

    VideoView videoView;

    boolean isRecording = false;

    static final int REQUEST_CODE = 1;
    public final static int FILE_CHOICE = 2;

    MediaProjectionManager mediaProjectionManager;
    MediaProjectionCallback mMediaProjectionCallback;

    VirtualDisplay mVirtualDisplay;

    MediaRecorder mMediaRecorder;

    MediaProjection mMediaProjection;

    private BaseDanmakuParser parser = new BaseDanmakuParser(){
        @Override
        protected IDanmakus parse(){
            return new Danmakus();
        }
    };

    String path;
    String pathSaved;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout);
        videoView = (VideoView) findViewById(R.id.video_view);
        Log.d("alice_debug","External storage is " + Environment.getExternalStorageDirectory().getPath().toString());
        //videoView.setVideoPath(Environment.getExternalStorageDirectory().toString() + "/tencent/MicroMsg/cb89c00867bf7187e15f027b4bbf3956/video/12304327081724d0e4733711.mp4");
        Intent intent = getIntent();
        path = intent.getStringExtra("videoPath");
        //path = Environment.getExternalStorageDirectory().toString() + "/" + path;
        Log.d("alice_debug", "final path is " + path);
        videoView.setVideoPath(path);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            videoView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
        videoView.start();

        danmView = (DanmakuView)findViewById(R.id.danmaku_view);
        danmView.enableDanmakuDrawingCache(true);
        danmView.setCallback(new DrawHandler.Callback() {
            @Override
            public void prepared() {
                showDanm = true;
                Log.d("alice_debug", "I am here to set showDanm to true");
                danmView.start();
                generateDanm();

            }

            @Override
            public void updateTimer(DanmakuTimer timer) {

            }

            @Override
            public void danmakuShown(BaseDanmaku danmaku) {

            }

            @Override
            public void drawingFinished() {

            }
        });
        danmContext = DanmakuContext.create();
        danmView.prepare(parser,danmContext);

        final LinearLayout operationView = (LinearLayout)findViewById(R.id.operate_view);
        final Button danmuSend = (Button)findViewById(R.id.danmu_send);
        final Button videoRecord = (Button)findViewById(R.id.video_record);
        final EditText danmuContent = (EditText)findViewById(R.id.danmu_content);
        danmView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                if(operationView.getVisibility() == View.GONE){
                    operationView.setVisibility(View.VISIBLE);
                } else {
                    operationView.setVisibility(View.GONE);
                }
                if(Build.VERSION.SDK_INT >= 19) {
                    videoView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                }
            }
        });
        danmuSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String content = danmuContent.getText().toString();
                if(!TextUtils.isEmpty(content)){
                    addDanm(content, true);
                    danmuContent.setText("");
                }
            }
        });
        videoRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isRecording == false) {
                    if(mediaProjectionManager == null) {
                        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                        Intent captureIntent = mediaProjectionManager.createScreenCaptureIntent();
                        startActivityForResult(captureIntent, REQUEST_CODE);
                        Log.d("alice_debug", "mediaProjectionManager is empty");
                        mMediaRecorder = new MediaRecorder();
                        DisplayMetrics metrics = new DisplayMetrics();
                        metrics = getResources().getDisplayMetrics();
                        int mScreenWidth = metrics.widthPixels;
                        int mScreenHeight = metrics.heightPixels;
                        int mScreenDensity = metrics.densityDpi;
                        Log.d("alice_debug", "initial mMediaRecorder in videoRecord");
                        //initRecorder(mScreenWidth, mScreenHeight);
                        //Intent intent = new Intent();
                        //intent.setType("video/*");
                        //intent.setAction(Intent.ACTION_GET_CONTENT);
                        //startActivityForResult(intent, FILE_CHOICE);

                        videoRecord.setText("Stop");
                    }
                } else {
                    StopRecord();
                    videoRecord.setText("Record");
                }

            }
        });
        /*
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener(){
            @Override
            public void onSystemUiVisibilityChange(int visibility){
                if(visibility == View.SYSTEM_UI_FLAG_VISIBLE){
                    onWindowFocusChanged(true);
                }
            }
        });
        */
    }

    private void StopRecord() {
        mMediaRecorder.stop();
        isRecording = false;
        mVirtualDisplay.release();
        mMediaProjection.unregisterCallback(mMediaProjectionCallback);
        mMediaProjection.stop();
        mMediaProjection = null;
    }

    private void StartRecord() {
        mMediaRecorder.start();
        isRecording = true;
    }


    //Random generate Dan mu
    private void generateDanm() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(showDanm){
                    int time = new Random().nextInt(5000);
                    String content = "time is : " + time + time;
                    addDanm(content, new Random().nextBoolean());
                    try{
                        Thread.sleep(time);
                    } catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void addDanm(String content, boolean widthBorder) {
        BaseDanmaku danmaku = danmContext.mDanmakuFactory.createDanmaku(BaseDanmaku.TYPE_SCROLL_LR);
        danmaku.text = content;
        danmaku.padding = 5;
        danmaku.textColor = Color.BLUE;
        danmaku.textSize = sp2px(new Random().nextFloat()*50);
        danmaku.setTime(danmView.getCurrentTime());
        if(widthBorder){
            danmaku.borderColor = Color.YELLOW;
        }
        danmView.addDanmaku(danmaku);
    }

    private float sp2px(float spValue) {
        final float fontScale = getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    public void onWindowFocusChanges(boolean hasFocus){
        super.onWindowFocusChanged(hasFocus);
        //if(hasFocus && Build.VERSION.SDK_INT >= 19){
        if(Build.VERSION.SDK_INT >= 19){
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    protected void  onPause(){
        super.onPause();
        if(danmView != null && danmView.isPrepared()){
            danmView.pause();
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(danmView != null && danmView.isPrepared() && danmView.isPaused()){
            danmView.resume();
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        showDanm = false;
        if(danmView != null){
            danmView.release();
            danmView = null;
        }
    }

    @Override
    public void onActivityResult(int request_code, int result_code, Intent data){
        super.onActivityResult(request_code,result_code,data);
        Log.d("alice_debug", "I am in the on Activity Result");
        if(request_code == REQUEST_CODE) {
            if(result_code != RESULT_OK) {
                Log.d("alice_debug", "record is forbidden");
                return;
            }
            mMediaProjection = mediaProjectionManager.getMediaProjection(result_code, data);
            mMediaProjectionCallback = new MediaProjectionCallback();
            DisplayMetrics metrics = new DisplayMetrics();
            metrics = getResources().getDisplayMetrics();
            int mScreenWidth = metrics.widthPixels;
            int mScreenHeight = metrics.heightPixels;
            int mScreenDensity = metrics.densityDpi;
            Log.d("alice_debug", "initial mMediaRecorder");
            initRecorder(mScreenWidth, mScreenHeight);
            mVirtualDisplay = mMediaProjection.createVirtualDisplay("MainActivity",
                    mScreenWidth, mScreenHeight, mScreenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    mMediaRecorder.getSurface(), null, null);
            StartRecord();
//            StartRecord();


        } else if(request_code == FILE_CHOICE){
            if(result_code == RESULT_OK){

                Uri uri = data.getData();
                pathSaved = FileUtils.getFileAbsolutePath(this, uri); //Path
            }

        }
    }

    //初始化录制参数
    private void initRecorder(int DISPLAY_WIDTH, int  DISPLAY_HEIGHT) {
        try {
            if (mMediaRecorder == null) {
                Log.d("alice_debug", "initRecorder: MediaRecorder为空啊---");
                return;
            }
            //Check the path is already choosed
            if(pathSaved == null){
                pathSaved = path.replace(".mp4", "_new.mp4");
            }

            //check the record access
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                Log.d("alice_debug", "need access the record");
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)!= PackageManager.PERMISSION_GRANTED){

                    if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.RECORD_AUDIO))
                    {
                        Log.d("alice_debug", "I am here that access is refused");
                    } else {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 0);
                    }
                }
            }

            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);// 音频源
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);// 视频源
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);//视频输出格式
            Log.d("alice_debug", "pathSaved is " + pathSaved);
            mMediaRecorder.setOutputFile(pathSaved);//存储路径
            mMediaRecorder.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT);// 设置分辨率
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);// 视频录制格式
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);// 音频格式
            mMediaRecorder.setVideoFrameRate(16);//帧率
            mMediaRecorder.setVideoEncodingBitRate(5242880);//视频清晰度
            //int rotation = getWindowManager().getDefaultDisplay().getRotation();
            //int orientataion = ORIENTTIONS.get(rotation + 90);
            //mMediaRecorder.setOrientationHint(orientataion);//设置旋转方向
            mMediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            if (isRecording) {
                StopRecord();
                mMediaRecorder.stop();
                mMediaRecorder.reset();
            }
            mMediaProjection = null;
        }
    }

}

/*
 * Copyright (C) 2018 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.aip.fl;


import android.Manifest;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.baidu.aip.FaceSDKManager;
import com.baidu.aip.ImageFrame;
import com.baidu.aip.face.CameraImageSource;
import com.baidu.aip.face.DetectRegionProcessor;
import com.baidu.aip.face.FaceDetectManager;
import com.baidu.aip.face.PreviewView;
import com.baidu.aip.face.camera.ICameraControl;
import com.baidu.aip.face.camera.PermissionCallback;
import com.baidu.aip.fl.widget.BrightnessTools;
import com.baidu.idl.facesdk.FaceInfo;
import com.baidu.idl.facesdk.FaceSDK;
import com.baidu.idl.facesdk.FaceTracker;

import java.lang.ref.WeakReference;

import uni.UNI6BD21A1.R;

// import android.graphics.Rect;

/**
 * 实时检测人脸并展示人脸轮廓正方形及人脸轮廓关键点位
 * Intent intent = new Intent(MainActivity.this, TrackActivity.class);
 * startActivity(intent);
 */
public class TrackActivity extends AppCompatActivity {

    private static final int MSG_INITVIEW = 1001;
    private PreviewView previewView;
    private ImageView closeIv;
    private boolean mDetectStoped = false;
    private Handler mHandler;


    private FaceDetectManager faceDetectManager;
    private DetectRegionProcessor cropProcessor = new DetectRegionProcessor();
    private int mScreenW;
    private int mScreenH;
    private int mRound = 2;
    private boolean mIsPortrait = true;
    // textureView用于绘制人脸框等。
    private TextureView textureView;
    private Paint paint = new Paint();
    private FaceTracker mTracker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track);
        faceDetectManager = new FaceDetectManager(this);
        initScreen();
        initView();
        initPaint();
        initTracker();
        mHandler = new InnerHandler(this);
        mHandler.sendEmptyMessageDelayed(MSG_INITVIEW, 500);
    }

    /**
     * 初始化屏幕参数
     */
    private void initScreen() {
        WindowManager manager = getWindowManager();
        DisplayMetrics outMetrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(outMetrics);
        mScreenW = outMetrics.widthPixels;
        mScreenH = outMetrics.heightPixels;

        mRound = getResources().getDimensionPixelSize(R.dimen.round);
    }

    /**
     * 初始化画笔
     */
    private void initPaint() {
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
    }

    /**
     * 人脸跟踪参数设置，参数可调整按自己需求设置
     */
    private void initTracker() {
        mTracker = FaceSDKManager.getInstance().getFaceTracker(this);
        mTracker.set_isFineAlign(false);
        mTracker.set_isVerifyLive(false);
        mTracker.set_DetectMethodType(1);
        mTracker.set_isCheckQuality(false);
        mTracker.set_notFace_thr(0.6f);
        mTracker.set_min_face_size(200);
        mTracker.set_cropFaceSize(400);
        mTracker.set_illum_thr(40);
        mTracker.set_blur_thr(0.7f);
        mTracker.set_occlu_thr(0.5f);
        mTracker.set_max_reg_img_num(1);
        mTracker.set_eulur_angle_thr(10, 10, 10);

        mTracker.set_track_by_detection_interval(80);
        FaceSDK.setNumberOfThreads(4);

    }

    /**
     * 初始化view
     */
    private void initView() {

        previewView = (PreviewView) findViewById(R.id.preview_view);
        textureView = (TextureView) findViewById(R.id.texture_view);
        textureView.setOpaque(false);

        // 不需要屏幕自动变黑。
        textureView.setKeepScreenOn(true);
        initCamera();
        closeIv = (ImageView) findViewById(R.id.closeIv);
        closeIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        initBrightness();
    }

    /**
     * 初始化相机
     */
    private void initCamera() {
        // 初始化相机图片资源
        final CameraImageSource cameraImageSource = new CameraImageSource(this);
        // 设置预览界面
        cameraImageSource.setPreviewView(previewView);

        // 设置人脸检测图片资源
        faceDetectManager.setImageSource(cameraImageSource);
        // 设置人脸检测回调,其中 retCode为人脸检测回调值（0通常为检测到人脸),infos为人脸信息，frame为相机回调图片资源
        faceDetectManager.setOnFaceDetectListener(new FaceDetectManager.OnFaceDetectListener() {
            @Override
            public void onDetectFace(final int retCode, FaceInfo[] infos, ImageFrame frame) {

                if (infos != null && infos[0] != null) {
                    showFrame(infos[0]);
                } else {
                    showFrame(null);
                }
            }
        });

        cameraImageSource.getCameraControl().setPermissionCallback(new PermissionCallback() {
            @Override
            public boolean onRequestPermission() {
                ActivityCompat.requestPermissions(TrackActivity.this,
                        new String[]{Manifest.permission.CAMERA}, 100);
                return true;
            }
        });


        ICameraControl control = cameraImageSource.getCameraControl();
        control.setPreviewView(previewView);
        // 设置检测裁剪处理器
        faceDetectManager.addPreProcessor(cropProcessor);

        // 获取相机屏幕方向
        int orientation = getResources().getConfiguration().orientation;
        mIsPortrait = (orientation == Configuration.ORIENTATION_PORTRAIT);

        // 根据屏幕方向决定预览拉伸类型
        Log.d("liujinhui", "mScreenW is:" + mScreenW);
        if (mIsPortrait) {
            previewView.setScaleType(PreviewView.ScaleType.FIT_WIDTH);
        } else {
            previewView.setScaleType(PreviewView.ScaleType.FIT_HEIGHT);
        }

        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        cameraImageSource.getCameraControl().setDisplayOrientation(rotation);
        // 设置相机摄像头类型，包括前置、后置及usb等类型
        setCameraType(cameraImageSource);
    }

    /**
     * 初始化屏幕亮度，不到200自动调整到200
     */
    private void initBrightness() {
        int brightness = BrightnessTools.getScreenBrightness(TrackActivity.this);
        if (brightness < 200) {
            BrightnessTools.setBrightness(this, 200);
        }
    }

    /**
     * 摄像头类型设置，可根据自己需求设置前置、后置及usb摄像头
     *
     * @param cameraImageSource
     */
    private void setCameraType(CameraImageSource cameraImageSource) {
        // TODO 选择使用前置摄像头
        cameraImageSource.getCameraControl().setCameraFacing(ICameraControl.CAMERA_FACING_FRONT);

        // TODO 选择使用usb摄像头
        //  cameraImageSource.getCameraControl().setCameraFacing(ICameraControl.CAMERA_USB);
        // 如果不设置，人脸框会镜像，显示不准
        //  previewView.getTextureView().setScaleX(-1);

        // TODO 选择使用后置摄像头
        // cameraImageSource.getCameraControl().setCameraFacing(ICameraControl.CAMERA_FACING_BACK);
        // previewView.getTextureView().setScaleX(-1);
    }

    /**
     * 人脸检测启动
     */
    private void start() {
        RectF newDetectedRect = new RectF(0, 0, mScreenW, mScreenH);
        cropProcessor.setDetectedRect(newDetectedRect);
        faceDetectManager.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        faceDetectManager.stop();
        mDetectStoped = true;
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mDetectStoped) {
            faceDetectManager.start();
            mDetectStoped = false;
        }

    }

    private static class InnerHandler extends Handler {
        private WeakReference<TrackActivity> mWeakReference;

        public InnerHandler(TrackActivity activity) {
            super();
            this.mWeakReference = new WeakReference<TrackActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            if (mWeakReference == null || mWeakReference.get() == null) {
                return;
            }
            TrackActivity activity = mWeakReference.get();
            if (activity == null) {
                return;
            }
            if (msg == null) {
                return;

            }
            switch (msg.what) {
                case MSG_INITVIEW:
                    activity.start();
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 绘制人脸框。
     *
     * @param info 追踪到的人脸
     */
    private void showFrame(FaceInfo info) {
        Canvas canvas = textureView.lockCanvas();
        if (canvas == null) {
            return;
        }

        // 清空canvas
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        if (info != null) {
            Log.d("liujinhui", " has frame");
            RectF rectCenter = new RectF(info.mCenter_x - 2 - info.mWidth * 3 / 5,
                    info.mCenter_y - 2 - info.mWidth * 3 / 5,
                    info.mCenter_x + 2 + info.mWidth * 3 / 5,
                    info.mCenter_y + 2 + info.mWidth * 3 / 5);
            previewView.mapFromOriginalRectEx(rectCenter);
            // 绘制框
            paint.setStrokeWidth(mRound);
            paint.setAntiAlias(true);
            canvas.drawRect(rectCenter, paint);
            if (info.landmarks.length > 0) {
                int len = info.landmarks.length;
                for (int i = 0; i < len; i += 2) {
                    RectF rectPoint = new RectF(info.landmarks[i] - mRound, info.landmarks[i + 1] - mRound,
                            info.landmarks[i] + mRound, info.landmarks[i + 1] + mRound);
                    previewView.mapFromOriginalRectEx(rectPoint);

                    paint.setStrokeWidth(rectPoint.width() * 2 / 3);
                    canvas.drawCircle(rectPoint.centerX(), rectPoint.centerY(), rectPoint.width() / 2, paint);

                }
            }
        } else {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        }
        textureView.unlockCanvasAndPost(canvas);
    }
}

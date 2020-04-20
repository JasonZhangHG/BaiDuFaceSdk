/*
 * Copyright (C) 2018 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.aip.fl;


import android.Manifest;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.opengl.GLSurfaceView;
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
import android.widget.TextView;

import com.baidu.aip.ImageFrame;
import com.baidu.aip.face.CameraImageSource;
import com.baidu.aip.face.DetectRegionProcessor;
import com.baidu.aip.face.FaceDetectManager;
import com.baidu.aip.face.PreviewView;
import com.baidu.aip.face.camera.ICameraControl;
import com.baidu.aip.face.camera.PermissionCallback;
import com.baidu.aip.fl.cube.SquareRenderer;
import com.baidu.aip.fl.widget.BrightnessTools;
import com.baidu.idl.facesdk.FaceInfo;

import java.lang.ref.WeakReference;

import uni.UNI6BD21A1.R;

/**
 * 实时检测人脸轮廓并可随着人脸相对相机的角度变更展示立方体表示3d人脸角度
 * Intent intent = new Intent(MainActivity.this, AttrActivity.class);
 * startActivity(intent);
 */
public class AttrActivity extends AppCompatActivity {

    private static final int MSG_INITVIEW = 1001;
    private PreviewView previewView;
    private ImageView closeIv;
    private boolean mDetectStoped = false;
    private Handler mHandler;
    private FaceDetectManager faceDetectManager;
    private DetectRegionProcessor cropProcessor = new DetectRegionProcessor();
    private float mX;
    private float mY;
    private float mZ;
    private GLSurfaceView mGlView;
    private SquareRenderer mSquareRender;
    private int mScreenW;
    private int mScreenH;
    private TextureView textureView;
    private Paint paint = new Paint();
    private TextView mXTv;
    private TextView mYTv;
    private TextView mZTv;
    private int mRound = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attr);
        faceDetectManager = new FaceDetectManager(this);
        initScreen();
        initView();
        initCube();
        initPaint();
        mHandler = new InnerHandler(this);
        mHandler.sendEmptyMessageDelayed(MSG_INITVIEW, 500);
    }

    /**
     * 初始化画笔
     */
    private void initPaint() {
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(mRound);
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
     * 初始化view
     */
    private void initView() {

        mXTv = (TextView) findViewById(R.id.xTv);
        mYTv = (TextView) findViewById(R.id.yTv);
        mZTv = (TextView) findViewById(R.id.zTv);

        previewView = (PreviewView) findViewById(R.id.preview_view);
        textureView = (TextureView) findViewById(R.id.texture_view);
        textureView.setOpaque(false);

        final CameraImageSource cameraImageSource = new CameraImageSource(this);
        cameraImageSource.setPreviewView(previewView);

        faceDetectManager.setImageSource(cameraImageSource);
        faceDetectManager.setOnFaceDetectListener(new FaceDetectManager.OnFaceDetectListener() {
            @Override
            public void onDetectFace(final int retCode, FaceInfo[] infos, ImageFrame frame) {
                if (infos != null && infos[0] != null) {
                    mX = infos[0].headPose[0];
                    mY = infos[0].headPose[1];
                    mZ = infos[0].headPose[2];
                    Log.d("DetectLoginActivity", "mX is:" + mX + " mY is:" + mY + " mZ is:" + mZ);
                    if (mSquareRender != null) {
                        mSquareRender.setAngle(1.2f * mX, 1.2f * mY, 1.2f * mZ);
                    }
                    showXYZ(true);
                    showFrame(infos[0]);
                } else {
                    showXYZ(false);
                    showFrame(null);
                }
            }
        });

        cameraImageSource.getCameraControl().setPermissionCallback(new PermissionCallback() {
            @Override
            public boolean onRequestPermission() {
                ActivityCompat.requestPermissions(AttrActivity.this,
                        new String[]{Manifest.permission.CAMERA}, 100);
                return true;
            }
        });


        ICameraControl control = cameraImageSource.getCameraControl();
        control.setPreviewView(previewView);
        // 设置检测裁剪处理器
        faceDetectManager.addPreProcessor(cropProcessor);

        int orientation = getResources().getConfiguration().orientation;
        boolean isPortrait = (orientation == Configuration.ORIENTATION_PORTRAIT);

        if (isPortrait) {
            previewView.setScaleType(PreviewView.ScaleType.FIT_WIDTH);
        } else {
            previewView.setScaleType(PreviewView.ScaleType.FIT_HEIGHT);
        }
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        cameraImageSource.getCameraControl().setDisplayOrientation(rotation);
        setCameraType(cameraImageSource);
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
     * 显示人脸角度X,Y,Z坐标及表示人脸角度样式的立方体
     *
     * @param show
     */
    private void showXYZ(final boolean show) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (show) {
                    mSquareRender.setDrawable(true);
                    mXTv.setText("yaw:" + mX);
                    mYTv.setText("pitch:" + mY);
                    mZTv.setText("roll:" + mZ);
                } else {
                    mSquareRender.setDrawable(false);
                    mXTv.setText("");
                    mYTv.setText("");
                    mZTv.setText("");
                }

            }
        });

    }

    /**
     * 初始化立方体
     */
    private void initCube() {
        mGlView = (GLSurfaceView) findViewById(R.id.glView);
        mGlView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        mSquareRender = new SquareRenderer();
        mGlView.setRenderer(mSquareRender);
        mSquareRender.setDrawable(false);
        mGlView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        mGlView.setZOrderOnTop(true);
    }

    /**
     * 初始化屏幕亮度，不到200则设置为200
     */
    private void initBrightness() {
        int brightness = BrightnessTools.getScreenBrightness(AttrActivity.this);
        if (brightness < 200) {
            BrightnessTools.setBrightness(this, 200);
        }
    }

    /**
     * 启动检测
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
        private WeakReference<AttrActivity> mWeakReference;

        public InnerHandler(AttrActivity activity) {
            super();
            this.mWeakReference = new WeakReference<AttrActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            if (mWeakReference == null || mWeakReference.get() == null) {
                return;
            }
            AttrActivity activity = mWeakReference.get();
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
            //  Log.d("liujinhui", " has frame");
            RectF rectCenter = new RectF(info.mCenter_x - mRound - info.mWidth * 3 / 5,
                    info.mCenter_y - mRound - info.mWidth * 3 / 5,
                    info.mCenter_x + mRound + info.mWidth * 3 / 5,
                    info.mCenter_y + mRound + info.mWidth * 3 / 5);
            previewView.mapFromOriginalRect(rectCenter);
            // 绘制框
            canvas.drawRect(rectCenter, paint);
        } else {
            // Log.d("liujinhui", " no frame");
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        }
        textureView.unlockCanvasAndPost(canvas);
    }

    /**
     * 设置摄像头，可设置前置、后置及usb摄像头
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
}

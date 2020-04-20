/*
 * Copyright (C) 2017 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.aip.fl;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import uni.UNI6BD21A1.R;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button mTrackBtn;
    private Button mAttrBtn;
    private Button mDetectBtn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        addListener();
    }

    private void initView() {
        mTrackBtn = (Button) findViewById(R.id.track_btn);
        mAttrBtn = (Button) findViewById(R.id.attr_btn);
        mDetectBtn = (Button) findViewById(R.id.detect_btn);
    }

    private void addListener() {
        mTrackBtn.setOnClickListener(this);
        mAttrBtn.setOnClickListener(this);
        mDetectBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100);
            return;
        }

        switch (v.getId()) {
            case R.id.track_btn:
                Intent itTrack = new Intent(MainActivity.this, TrackActivity.class);
                startActivity(itTrack);
                break;
            case R.id.attr_btn:
                Intent itAttr = new Intent(MainActivity.this, AttrActivity.class);
                startActivity(itAttr);
                break;
            case R.id.detect_btn:
                // TODO 实时人脸检测
                Intent itDetect = new Intent(MainActivity.this, DetectActivity.class);
                startActivity(itDetect);
                break;
            default:
                break;
        }

    }
}

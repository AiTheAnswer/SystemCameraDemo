package com.allen.systemcamerademo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;

import com.allen.customcamera.CameraView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Allen on 2017/11/20.
 * 相机界面Activity
 */

public class CustomCameraActivity extends AppCompatActivity {
    private CameraView cameraView;
    private String mPath;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //去掉状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        cameraView = new CameraView(this);
        setContentView(cameraView);
        //去掉ActionBar
        getSupportActionBar().hide();
        mPath = getIntent().getStringExtra("path");
        cameraView.setCameraListener(cameraListener);
    }

    private CameraView.CameraListener cameraListener = new CameraView.CameraListener() {
        @Override
        public void onCapture(Bitmap bitmap) {
            File file = new File(mPath);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            try {
                FileOutputStream out = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                out.flush();
                out.close();
            } catch (IOException e) {
            }

            if (file.exists()) {
                Intent data = new Intent();
                data.setData(Uri.parse(mPath));
                setResult(RESULT_OK, data);
            }

            finish();
        }

        @Override
        public void onCameraClose() {
            finish();
        }

        @Override
        public void onCameraError(Throwable th) {
            onCameraClose();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        cameraView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraView.onPause();
    }
}

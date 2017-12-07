package com.allen.customcamera;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;

import static android.content.ContentValues.TAG;

/**
 * Created by Allen on 2017/11/20.
 * 自定义相机View
 */

public class CameraView extends FrameLayout implements SensorEventListener, View.OnClickListener {

    private Context mContext;
    //使用SurfaceView预览摄像头获取到的图像
    private SurfaceView mSurfaceView;
    //相机焦点View
    private FocusView mFocusView;
    //转换前后摄像头
    private ImageView mSwitchCamera;
    //点击拍照后 图片的预览
    private ImageView mPreViewPicture;
    //拍照、取消、重拍的布局
    private CaptureLayout mCaptureLayout;
    //相机管理类
    private CameraManager cameraManager;
    private Bitmap mPicture;
    private int mSensorRotation;
    private boolean isSurfaceCreated;
    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleGestureDetector;
    private SensorManager mSensorManager;


    /**
     * 回调接口
     * 相机的监听器
     */
    public interface CameraListener {

        //拍照
        void onCapture(Bitmap bitmap);

        //关闭
        void onCameraClose();

        //打开相机失败
        void onCameraError(Throwable th);

    }

    private CameraListener mCameraListener;

    /**
     * 设置相机的监听器
     *
     * @param cameraListener
     */
    public void setCameraListener(CameraListener cameraListener) {
        this.mCameraListener = cameraListener;
    }

    public CameraView(@NonNull Context context) {
        this(context, null);
    }

    public CameraView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        init();
    }

    private void init() {
        setBackgroundColor(Color.BLACK);
        LayoutInflater.from(getContext()).inflate(R.layout.camera_layout, this, true);
        mSurfaceView = findViewById(R.id.camera_surface_view);
        mFocusView = findViewById(R.id.camera_focus_view);
        mSwitchCamera = findViewById(R.id.img_switch_camera);
        mCaptureLayout = findViewById(R.id.camera_capture_layout);
        mPreViewPicture = findViewById(R.id.camera_picture_preview);
        //设置拍照、取消、重拍、确定按钮的点击监听事件
        mCaptureLayout.setOnCaptureClickListener(captureClickListener);
        mSurfaceView.setOnTouchListener(surfaceTouchListener);
        //设置SurfaceView的状态改变callback对象
        mSurfaceView.getHolder().addCallback(surfaceViewCallBack);
        cameraManager = CameraManager.getInstance(mContext);
        //设置转换摄像头的图标是否可见
        mSwitchCamera.setVisibility(cameraManager.hasMultiCamera() ? VISIBLE : GONE);
        mGestureDetector = new GestureDetector(getContext(), simpleOnGestureListener);
        mScaleGestureDetector = new ScaleGestureDetector(getContext(), onScaleGestureListener);
        mSensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
        mSwitchCamera.setOnClickListener(this);
    }

    //底部拍照、取消、重拍、确认按钮的点击事件回调接口的对象
    private CaptureLayout.OnCaptureClickListener captureClickListener = new CaptureLayout.OnCaptureClickListener() {
        @Override
        public void onCancel() {
            if (mCameraListener != null) {
                mCameraListener.onCameraClose();
            }
        }

        @Override
        public void onCapture() {
            cameraManager.takePicture(new CameraManager.Callback<Bitmap>() {
                @Override
                public void onEvent(Bitmap bitmap) {
                    if (bitmap != null) {
                        mSurfaceView.setVisibility(GONE);
                        mSwitchCamera.setVisibility(GONE);
                        mPreViewPicture.setVisibility(VISIBLE);
                        mPicture = bitmap;
                        mPreViewPicture.setImageBitmap(mPicture);
                        mCaptureLayout.setIsExpand(true);
                    } else {
                        // 不知道什么原因拍照失败，重新预览
                        onRetry();
                    }
                }
            });

        }

        @Override
        public void onRetry() {
            mPicture = null;
            mSurfaceView.setVisibility(VISIBLE);
            mSwitchCamera.setVisibility(cameraManager.hasMultiCamera() ? VISIBLE : GONE);
            mPreViewPicture.setImageBitmap(null);
            mPreViewPicture.setVisibility(GONE);
            mCaptureLayout.setIsExpand(false);
        }

        @Override
        public void onConfirm() {
            if (mPicture != null && mCameraListener != null) {
                mCameraListener.onCapture(mPicture);
            }
        }
    };

    //SurfaceView创建、改变、销毁状态回调的对象
    private SurfaceHolder.Callback surfaceViewCallBack = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {//SurfaceView被创建
            isSurfaceCreated = true;
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {//SurfaceView 的格式或者尺寸改变的回调
            //设置相机预览的SurfaceHolder
            cameraManager.setSurfaceHolder(holder, width, height);
            //如果相机是打开的先关闭相机
            if (cameraManager.isOpened()) {
                cameraManager.close();
            }
            //打开相机
            cameraManager.open(new CameraManager.Callback<Boolean>() {
                @Override
                public void onEvent(Boolean success) {
                    if (!success && mCameraListener != null) {
                        mCameraListener.onCameraError(new Exception("open camera failed"));
                    }
                }
            });
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {//SurfaceView 被销毁的回调

        }
    };

    public void onResume() {

        if (!cameraManager.isOpened() && isSurfaceCreated) {
            cameraManager.open(new CameraManager.Callback<Boolean>() {
                @Override
                public void onEvent(Boolean success) {
                    if (!success && mCameraListener != null) {
                        mCameraListener.onCameraError(new Exception("open camera failed"));
                    }
                }
            });
        }

        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
    }

    private OnTouchListener surfaceTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mScaleGestureDetector.onTouchEvent(event);
            if (mScaleGestureDetector.isInProgress()) {
                return true;
            }

            return mGestureDetector.onTouchEvent(event);
        }
    };

    @Override
    public void onClick(View v) {
        if (v == mSwitchCamera) {
            cameraManager.switchCamera(new CameraManager.Callback<Boolean>() {
                @Override
                public void onEvent(Boolean success) {
                    if (!success && mCameraListener != null) {
                        mCameraListener.onCameraError(new Exception("switch camera failed"));
                    }
                }
            });
        }
    }

    private GestureDetector.OnGestureListener simpleOnGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onDown(MotionEvent e) {
            if (!cameraManager.isOpened()) {
                return false;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                mFocusView.removeCallbacks(timeoutRunnable);
                mFocusView.postDelayed(timeoutRunnable, 1500);

                mFocusView.setVisibility(VISIBLE);
                LayoutParams focusParams = (LayoutParams) mFocusView.getLayoutParams();
                focusParams.leftMargin = (int) e.getX() - focusParams.width / 2;
                focusParams.topMargin = (int) e.getY() - focusParams.height / 2;
                mFocusView.setLayoutParams(focusParams);

                ObjectAnimator scaleX = ObjectAnimator.ofFloat(mFocusView, "scaleX", 1, 0.5f);
                scaleX.setDuration(300);
                ObjectAnimator scaleY = ObjectAnimator.ofFloat(mFocusView, "scaleY", 1, 0.5f);
                scaleY.setDuration(300);
                ObjectAnimator alpha = ObjectAnimator.ofFloat(mFocusView, "alpha", 1f, 0.3f, 1f, 0.3f, 1f, 0.3f, 1f);
                alpha.setDuration(600);
                AnimatorSet animatorSet = new AnimatorSet();
                animatorSet.play(scaleX).with(scaleY).before(alpha);
                animatorSet.start();

                CameraManager.Callback<Boolean> focusCallback = new CameraManager.Callback<Boolean>() {
                    @Override
                    public void onEvent(Boolean success) {
                        if (mFocusView.getTag() == this && mFocusView.getVisibility() == VISIBLE) {
                            mFocusView.setVisibility(INVISIBLE);
                        }
                    }
                };
                mFocusView.setTag(focusCallback);
                cameraManager.setFocus(e.getX(), e.getY(), focusCallback);
            }

            return cameraManager.hasMultiCamera();
        }
    };
    private ScaleGestureDetector.OnScaleGestureListener onScaleGestureListener = new ScaleGestureDetector.OnScaleGestureListener() {
        private float mLastSpan;

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float span = detector.getCurrentSpan() - mLastSpan;
            mLastSpan = detector.getCurrentSpan();
            if (cameraManager.isOpened()) {
                cameraManager.setZoom(span);
                return true;
            }
            return false;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            mLastSpan = detector.getCurrentSpan();
            return cameraManager.isOpened();
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            Log.d(TAG, "onScaleEnd");
        }
    };


    /**
     * 前置摄像头可能不会回调对焦成功，因此需要手动隐藏对焦框
     */
    private Runnable timeoutRunnable = new Runnable() {
        @Override
        public void run() {
            if (mFocusView.getVisibility() == VISIBLE) {
                mFocusView.setVisibility(INVISIBLE);
            }
        }
    };

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
            return;
        }

        int rotation = CameraUtils.calculateSensorRotation(event.values[0], event.values[1]);
        if (rotation >= 0 && rotation != mSensorRotation) {
            playRotateAnimation(mSensorRotation, rotation);
            cameraManager.setSensorRotation(rotation);
            mSensorRotation = rotation;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void onPause() {
        if (cameraManager.isOpened()) {
            cameraManager.close();
        }

        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mCameraListener = null;
    }

    private void playRotateAnimation(int oldRotation, int newRotation) {
        if (!cameraManager.hasMultiCamera()) {
            return;
        }

        int diff = newRotation - oldRotation;
        if (diff > 180) {
            diff = diff - 360;
        } else if (diff < -180) {
            diff = diff + 360;
        }
        RotateAnimation rotate = new RotateAnimation(-oldRotation, -oldRotation - diff, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(200);
        rotate.setFillAfter(true);
        mSwitchCamera.startAnimation(rotate);
    }
}

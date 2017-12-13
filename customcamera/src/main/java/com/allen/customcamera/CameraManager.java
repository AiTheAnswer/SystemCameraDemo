package com.allen.customcamera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import java.io.IOException;

import static com.allen.customcamera.R.id.info;

/**
 * Created by Allen on 2017/11/20.
 * 相机的管理类
 * 采用单例设计模式
 */

public class CameraManager {
    //后置摄像头的id
    private int CAMERA_ID_BACK = -1;
    //前置摄像头的id
    private int CAMERA_ID_FRONT = -1;
    private Context mContext;
    //当前选择的摄像头id
    private int mCameraId;
    //相机对象（在Api21之后就过时了，被android.hardware.camera这个类所取代）
    private Camera mCamera;
    //子线程Handler
    private Handler mThreadHandler;
    //UIHandler
    private Handler mUiHandler;
    //相机当前的状态
    private CameraState mState;
    //拍照预览的SurfaceView的SurfaceHandler
    private SurfaceHolder mSurfaceHolder;
    //预览SurfaceView的尺寸大小
    private Point mSurfaceSize = new Point();
    private int mSensorRotation;

    /**
     * 相机的状态
     */
    private enum CameraState {
        STATE_IDLE,//空闲
        STATE_OPENED,//打开
        STATE_SHOOTING//正在打开中
    }

    /**
     * Callback 回调接口
     *
     * @param <T>
     */
    public interface Callback<T> {
        void onEvent(T t);
    }

    private static CameraManager cameraManager;

    public static CameraManager getInstance(Context context) {
        if (null == cameraManager) {
            cameraManager = new CameraManager(context);
        }
        return cameraManager;
    }

    private CameraManager(Context context) {
        mContext = context.getApplicationContext();
        mCameraId = -1;
        mUiHandler = new Handler(Looper.getMainLooper());
        //更方便的创建一个线程，可以用它包含的Looper来创建一个handler
        HandlerThread thread = new HandlerThread("manager Thread");
        thread.start();
        mThreadHandler = new Handler(thread.getLooper());
        findCameraId();

    }

    /**
     * 获取摄像头的信息（前/后摄像头的id）
     */
    private void findCameraId() {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {//获取手机摄像头的个数
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {//后置摄像头
                CAMERA_ID_BACK = cameraInfo.facing;
            } else {//前置摄像头
                CAMERA_ID_FRONT = cameraInfo.facing;
            }
        }
    }

    /**
     * 打开相机
     *
     * @param callback
     */
    public void open(final Callback<Boolean> callback) {
        //检测初始化是否完成
        checkInitialize();
        //开启子线程打开相机
        mThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                openImmediate();
                final boolean success = (mState == CameraState.STATE_OPENED);
                //打开相机完成后用UIHandler回调结果
                mUiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onEvent(success);
                    }
                });
            }
        });

    }

    /**
     * 设置用于相机预览的SurfaceHolder
     *
     * @param surfaceHolder
     * @param width
     * @param height
     */
    public void setSurfaceHolder(SurfaceHolder surfaceHolder, int width, int height) {
        mSurfaceHolder = surfaceHolder;
        mSurfaceSize.set(height, width);
    }

    /**
     * 立即打开相机
     */
    private void openImmediate() {
        closeImmediate(); //先关闭相机
        if (mSurfaceHolder == null) {//判断SurfaceView是否创建完成
            return;
        }
        if (mCameraId < 0 && CAMERA_ID_BACK >= 0) {//默认设置后置摄像头
            mCameraId = CAMERA_ID_BACK;
        }
        if (mCameraId < 0) {//没有找到摄像头设备
            return;
        }
        try {
            mCamera = Camera.open(mCameraId);
            Camera.Parameters parameters = mCamera.getParameters();
            CameraUtils.setPreViewParameters(mSurfaceSize, parameters);
            mCamera.setParameters(parameters);
            mCamera.setDisplayOrientation(getDisplayOrientation());
            mCamera.setPreviewDisplay(mSurfaceHolder);
            mCamera.startPreview();
            setCameraState(CameraState.STATE_OPENED);
        } catch (Throwable th) {
        }
    }

    public void setFocus(final float x, final float y, final Callback<Boolean> callback) {
        checkInitialize();
        mThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mState != CameraState.STATE_OPENED) {
                    return;
                }

                mCamera.cancelAutoFocus();
                Camera.Parameters parameters = mCamera.getParameters();
                CameraUtils.setFocusArea(mSurfaceSize, parameters, x, y);
                mCamera.setParameters(parameters);
                mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(final boolean success, Camera camera) {

                        mUiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (callback != null) {
                                    callback.onEvent(success);
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    public void setZoom(final float span) {
        checkInitialize();
        mThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mState != CameraState.STATE_OPENED) {
                    return;
                }

                Camera.Parameters parameters = mCamera.getParameters();
                if (parameters.isZoomSupported()) {
                    boolean valid = CameraUtils.setZoom(mSurfaceSize, parameters, span);
                    if (valid) {
                        mCamera.setParameters(parameters);
                    }
                }
            }
        });
    }


    public void setSensorRotation(int rotation) {
        mSensorRotation = rotation;
    }

    public void takePicture(final Callback<Bitmap> callback) {
        checkInitialize();
        mThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mState != CameraState.STATE_OPENED) {
                    return;
                }
                setCameraState(CameraState.STATE_SHOOTING);
                mCamera.takePicture(null, null, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        closeImmediate();
                        final Bitmap result;
                        if (data != null && data.length > 0) {
                            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                            Matrix matrix = new Matrix();
                            int rotation = getDisplayOrientation() + mSensorRotation;
                            if (mCameraId == CAMERA_ID_BACK) {
                                matrix.setRotate(rotation);
                            } else {
                                rotation = (360 - rotation) % 360;
                                matrix.setRotate(rotation);
                                matrix.postScale(-1, 1);
                            }
                            result = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                        } else {
                            result = null;
                        }

                        mUiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (callback != null) {
                                    callback.onEvent(result);
                                }
                            }
                        });
                    }
                });
            }
        });
    }


    /**
     * 获取手机的旋转角度
     *
     * @return
     */
    private int getDisplayOrientation() {
        //Google Api提供的方法
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraId, cameraInfo);
        WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        int rotation = windowManager.getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (cameraInfo.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (cameraInfo.orientation - degrees + 360) % 360;
        }

        return result;
    }


    /**
     * 关闭相机
     */
    public void close() {
        mThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                closeImmediate();
            }
        });
    }

    /**
     * 关闭相机
     */
    private void closeImmediate() {
        if (mCamera != null) {
            mCamera.stopPreview();//停止预览
            mCamera.release();//释放相机资源
            mCamera = null;
        }
        if (mState != CameraState.STATE_IDLE) {
            setCameraState(CameraState.STATE_IDLE);
        }
    }


    /**
     * 设置相机的状态
     *
     * @param state
     */
    private void setCameraState(CameraState state) {
        mState = state;
    }

    /**
     * 相机的状态 是否打开
     *
     * @return
     */
    public boolean isOpened() {
        return mCamera != null && mState != CameraState.STATE_IDLE;
    }

    /**
     * 判断设备硬件是否有多个摄像头
     *
     * @return true 至少两个
     */
    public boolean hasMultiCamera() {
        if (Camera.getNumberOfCameras() > 1) {
            return true;
        }
        return false;
    }

    public void switchCamera(final Callback<Boolean> callback) {
        checkInitialize();
        mThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!hasMultiCamera()) {
                    return;
                }

                if (mCameraId == CAMERA_ID_BACK) {
                    mCameraId = CAMERA_ID_FRONT;
                } else if (mCameraId == CAMERA_ID_FRONT) {
                    mCameraId = CAMERA_ID_BACK;
                } else {
                    mCameraId = CAMERA_ID_BACK;
                }

                openImmediate();

                final boolean success = (mState == CameraState.STATE_OPENED);
                mUiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.onEvent(success);
                        }
                    }
                });
            }
        });
    }

    /**
     * 检测相机是否初始化完成
     */
    private void checkInitialize() {
        if (mContext == null) {
            throw new IllegalStateException("camera manager is not initialize");
        }
    }


}

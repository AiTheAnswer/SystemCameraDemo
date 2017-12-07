package com.allen.customcamera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Allen on 2017/11/20.
 * 相机工具类
 */

public class CameraUtils {

    //获取屏幕的宽度
    public static int getScreenWidth(Context context) {
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display defaultDisplay = manager.getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        defaultDisplay.getMetrics(outMetrics);
        return outMetrics.widthPixels;
    }

    /**
     * 获取屏幕的宽高，返回Point对象，getX() 对应的是width  getY()对应的是Height
     *
     * @param context
     * @return
     */
    public static Point getScreenSize(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display defaultDisplay = windowManager.getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        defaultDisplay.getMetrics(outMetrics);
        return new Point(outMetrics.widthPixels, outMetrics.heightPixels);
    }

    //将dp值转换为px
    public static int dp2px(Context context, int dpValue) {
        return (int) (dpValue * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    /**
     * 设置相机预览视图的尺寸、拍照获取图片的尺寸
     *
     * @param parameters
     */
    public static void setPreViewParameters(Point surfaceSize, Camera.Parameters parameters) {
        if (surfaceSize.x <= 0 || surfaceSize.y <= 0 || parameters == null) {
            return;
        }
        //获取相机的预览图片尺寸集合
        List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
        Camera.Size previewSize = findProperSize(surfaceSize, previewSizes);
        if (previewSize != null) {
            //设置预览尺寸
            parameters.setPreviewSize(previewSize.width, previewSize.height);
        }
        //点击拍照后的图片尺寸集合
        List<Camera.Size> pictureSizeList = parameters.getSupportedPictureSizes();
        Camera.Size pictureSize = findProperSize(surfaceSize, pictureSizeList);
        if (pictureSize != null) {
            //设置图片尺寸
            parameters.setPictureSize(pictureSize.width, pictureSize.height);
        }

        List<String> focusModeList = parameters.getSupportedFocusModes();
        if (focusModeList != null && focusModeList.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            //设置聚焦的模式
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }

        List<Integer> pictureFormatList = parameters.getSupportedPictureFormats();
        if (pictureFormatList != null && pictureFormatList.contains(ImageFormat.JPEG)) {
            //设置图片的格式
            parameters.setPictureFormat(ImageFormat.JPEG);
            parameters.setJpegQuality(100);
        }
    }

    /**
     * 找出最合适的尺寸
     * 步骤：
     * 1、 先将所有尺寸按宽高比例进行分类
     * 2、 找出和屏幕预览宽高比例最接近的一组
     * 3、 在比例最接近的一组中找出最接近屏幕尺寸且大于屏幕尺寸
     * 4、 如果没有找到，则去掉大于屏幕尺寸的条件在找一次
     *
     * @param surfaceSize
     * @param sizeList
     * @return
     */
    private static Camera.Size findProperSize(Point surfaceSize, List<Camera.Size> sizeList) {
        if (surfaceSize.x <= 0 || surfaceSize.y <= 0 || sizeList == null) {
            return null;
        }

        int surfaceWidth = surfaceSize.x;
        int surfaceHeight = surfaceSize.y;

        List<List<Camera.Size>> ratioListList = new ArrayList<>();
        for (Camera.Size size : sizeList) {
            addRatioList(ratioListList, size);
        }

        final float surfaceRatio = (float) surfaceWidth / surfaceHeight;
        List<Camera.Size> bestRatioList = null;
        float ratioDiff = Float.MAX_VALUE;
        for (List<Camera.Size> ratioList : ratioListList) {
            float ratio = (float) ratioList.get(0).width / ratioList.get(0).height;
            float newRatioDiff = Math.abs(ratio - surfaceRatio);
            Log.e("tog", newRatioDiff + "---");
            if (newRatioDiff < ratioDiff) {
                bestRatioList = ratioList;
                ratioDiff = newRatioDiff;
            }
        }

        Camera.Size bestSize = null;
        int diff = Integer.MAX_VALUE;
        assert bestRatioList != null;
        for (Camera.Size size : bestRatioList) {
            int newDiff = Math.abs(size.width - surfaceWidth) + Math.abs(size.height - surfaceHeight);
            if (size.height >= surfaceHeight && newDiff < diff) {
                bestSize = size;
                diff = newDiff;
            }
        }

        if (bestSize != null) {
            return bestSize;
        }

        diff = Integer.MAX_VALUE;
        for (Camera.Size size : bestRatioList) {
            int newDiff = Math.abs(size.width - surfaceWidth) + Math.abs(size.height - surfaceHeight);
            if (newDiff < diff) {
                bestSize = size;
                diff = newDiff;
            }
        }

        return bestSize;
    }

    private static void addRatioList(List<List<Camera.Size>> ratioListList, Camera.Size size) {
        float ratio = (float) size.width / size.height;
        for (List<Camera.Size> ratioList : ratioListList) {
            float mine = (float) ratioList.get(0).width / ratioList.get(0).height;
            if (ratio == mine) {
                ratioList.add(size);
                return;
            }
        }

        List<Camera.Size> ratioList = new ArrayList<>();
        ratioList.add(size);
        ratioListList.add(ratioList);
    }

    /**
     * 根据屏幕宽度和最大缩放倍数计算缩放单位
     */
    public static boolean setZoom(Point surfaceSize, Camera.Parameters parameters, float span) {
        if (surfaceSize.x <= 0 || surfaceSize.y <= 0 || parameters == null) {
            return false;
        }

        int maxZoom = parameters.getMaxZoom();
        int unit = surfaceSize.y / 5 / maxZoom;
        int zoom = (int) (span / unit);
        int lastZoom = parameters.getZoom();
        int currZoom = lastZoom + zoom;
        currZoom = clamp(currZoom, 0, maxZoom);
        parameters.setZoom(currZoom);
        return lastZoom != currZoom;
    }

    /**
     * 只有倾斜角度比较大时才判定为屏幕旋转
     *
     * @return -1，表示旋转角度不够大
     */
    public static int calculateSensorRotation(float x, float y) {
        if (Math.abs(x) > 6 && Math.abs(y) < 4) {
            if (x > 6) {
                return 270;
            } else {
                return 90;
            }
        } else if (Math.abs(y) > 6 && Math.abs(x) < 4) {
            if (y > 6) {
                return 0;
            } else {
                return 180;
            }
        }

        return -1;
    }

    public static void setFocusArea(Point surfaceSize, Camera.Parameters parameters, float x, float y) {
        if (surfaceSize.x <= 0 || surfaceSize.y <= 0 || parameters == null) {
            return;
        }

        if (parameters.getMaxNumFocusAreas() > 0) {
            Rect focusRect = calculateTapArea(surfaceSize, x, y, 1f);
            List<Camera.Area> focusAreas = new ArrayList<>(1);
            focusAreas.add(new Camera.Area(focusRect, 800));
            parameters.setFocusAreas(focusAreas);
        }

        if (parameters.getMaxNumMeteringAreas() > 0) {
            Rect meteringRect = calculateTapArea(surfaceSize, x, y, 1.5f);
            List<Camera.Area> meteringAreas = new ArrayList<>(1);
            meteringAreas.add(new Camera.Area(meteringRect, 800));
            parameters.setMeteringAreas(meteringAreas);
        }

        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
    }

    /**
     * 转换对焦区域
     * 范围(-1000, -1000, 1000, 1000)
     */
    private static Rect calculateTapArea(Point surfaceSize, float x, float y, float coefficient) {
        float focusAreaSize = 200;
        int areaSize = (int) (focusAreaSize * coefficient);
        int surfaceWidth = surfaceSize.x;
        int surfaceHeight = surfaceSize.y;
        int centerX = (int) (x / surfaceHeight * 2000 - 1000);
        int centerY = (int) (y / surfaceWidth * 2000 - 1000);
        int left = clamp(centerX - (areaSize / 2), -1000, 1000);
        int top = clamp(centerY - (areaSize / 2), -1000, 1000);
        int right = clamp(left + areaSize, -1000, 1000);
        int bottom = clamp(top + areaSize, -1000, 1000);
        return new Rect(left, top, right, bottom);
    }

    private static int clamp(int x, int min, int max) {
        return Math.min(Math.max(x, min), max);
    }
}

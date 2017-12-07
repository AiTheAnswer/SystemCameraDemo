package com.allen.customcamera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import java.lang.invoke.MethodHandle;

/**
 * Created by Allen on 2017/11/20.
 * 聚焦的View
 */

public class FocusView extends View {

    //画笔
    private Paint mPaint;
    //线的宽度
    private int mStrokeWidth = 1;
    //方框中四条线的长度
    private int mLineLength = 12;
    //控件的宽度
    private int mWidth;
    //控件的高度
    private int mHeight;

    public FocusView(Context context) {
        this(context, null);
    }

    public FocusView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FocusView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = getWidth();
        mHeight = getHeight();
    }

    private void init(Context context) {
        mStrokeWidth = CameraUtils.dp2px(context, 1);
        mLineLength = CameraUtils.dp2px(context, 12);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(getResources().getColor(R.color.azure));
        mPaint.setStrokeWidth(mStrokeWidth);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //画边框
        canvas.drawRect(mStrokeWidth, mStrokeWidth, mWidth, mHeight - mStrokeWidth, mPaint);
        //画左边框中间的线
        canvas.drawLine(mStrokeWidth, mHeight / 2, mLineLength, mHeight / 2, mPaint);
        //画上边框中间的线
        canvas.drawLine(mWidth / 2, mStrokeWidth, mWidth / 2, mLineLength, mPaint);
        //画右边框中间的线
        canvas.drawLine(mWidth, mHeight / 2, mWidth - mLineLength, mHeight / 2, mPaint);
        //画下边框中间的线
        canvas.drawLine(mWidth / 2, mHeight, mWidth / 2, mHeight - mLineLength, mPaint);
    }
}

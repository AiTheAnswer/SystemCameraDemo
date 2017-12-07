package com.allen.customcamera;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * Created by Allen on 2017/11/20.
 * 相机界面底部拍照、取消、重拍按钮的布局
 */

public class CaptureLayout extends FrameLayout implements View.OnClickListener {
    private Context mContext;
    //拍照、重拍、确认的布局
    private View mCaptureLayout;
    //取消按钮
    private ImageView mCancel;
    //拍照按钮
    private ImageView mCapture;
    //重拍按钮
    private ImageView mRetry;
    //确认按钮
    private ImageView mConfirm;

    //是否展开拍照、重拍、确认按钮的布局
    private boolean mIsExpand;

    public CaptureLayout(@NonNull Context context) {
        this(context, null);
    }

    public CaptureLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CaptureLayout(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
        init();
        initListener();
    }

    private void init() {
        LayoutInflater.from(mContext).inflate(R.layout.capture_layout, this, true);
        mCaptureLayout = findViewById(R.id.camera_capture_retry_layout);
        mCancel = findViewById(R.id.capture_cancel);
        mCapture = findViewById(R.id.camera_capture);
        mRetry = findViewById(R.id.camera_capture_retry);
        mConfirm = findViewById(R.id.camera_capture_confirm);
    }

    private void initListener() {
        mCancel.setOnClickListener(this);
        mCapture.setOnClickListener(this);
        mRetry.setOnClickListener(this);
        mConfirm.setOnClickListener(this);
    }

    /**
     * 回调接口
     * 取消、拍照、重拍、确认按钮点击事件监听的回调
     */
    public interface OnCaptureClickListener {
        void onCancel();

        void onCapture();

        void onRetry();

        void onConfirm();
    }

    private OnCaptureClickListener mCaptureClickListener;

    public void setOnCaptureClickListener(OnCaptureClickListener onCaptureClickListener) {
        if (null != onCaptureClickListener) {
            this.mCaptureClickListener = onCaptureClickListener;
        }
    }

    public void setIsExpand(boolean isExpand) {
        if (mIsExpand == isExpand) {
            return;
        }
        mIsExpand = isExpand;

        if (mIsExpand) {//展开
            expand();
        } else {//关闭
            fold();
        }


    }

    /**
     * 展开拍照、重拍、取消、确认按钮的布局
     */
    private void expand() {
        //使用属性动画展开布局
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {//属性动画是Api11之后添加的
            playExpandAnimation();
        } else {
            //拍照按钮不可见
            mCapture.setVisibility(View.GONE);
            //重拍按钮可见
            mRetry.setVisibility(View.VISIBLE);
            //确认按钮可见
            mConfirm.setVisibility(View.VISIBLE);
            //更改布局的宽度
            FrameLayout.LayoutParams layoutParams = (LayoutParams) mCaptureLayout.getLayoutParams();
            //布局的距离屏幕左右两边各40dp
            layoutParams.width = CameraUtils.getScreenWidth(mContext) - CameraUtils.dp2px(mContext, 80);
            layoutParams.setMargins(CameraUtils.dp2px(mContext, 40), 0, 0, 0);
            //刷新视图
            mCaptureLayout.requestLayout();
        }
    }

    /**
     * 播放展开布局动画
     */
    private void playExpandAnimation() {
        //取消按钮不可见
        mCancel.setVisibility(View.GONE);
        //拍照按钮不可见
        mCapture.setVisibility(View.GONE);
        //重拍按钮可见
        mRetry.setVisibility(View.VISIBLE);
        //确认按钮可见
        mConfirm.setVisibility(View.VISIBLE);

        int captureLayoutWidth = CameraUtils.getScreenWidth(mContext) - CameraUtils.dp2px(mContext, 80);
        ValueAnimator transAnimator = ValueAnimator.ofInt(CameraUtils.dp2px(mContext, 80), captureLayoutWidth);
        transAnimator.setInterpolator(new LinearInterpolator());
        transAnimator.setDuration(200);
        transAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int animatedValue = (int) animation.getAnimatedValue();
                LayoutParams layoutParams = (LayoutParams) mCaptureLayout.getLayoutParams();
                layoutParams.width = animatedValue;
                mCaptureLayout.requestLayout();
            }
        });
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playSequentially(transAnimator);
        animatorSet.start();
    }

    /**
     * 关闭展开的布局，显示初始化的布局
     */
    private void fold() {
        //取消按钮不可见
        mCancel.setVisibility(View.VISIBLE);
        //拍照按钮不可见
        mCapture.setVisibility(View.VISIBLE);
        //重拍按钮可见
        mRetry.setVisibility(View.GONE);
        //确认按钮可见
        mConfirm.setVisibility(View.GONE);
        LayoutParams layoutParams = (LayoutParams) mCaptureLayout.getLayoutParams();
        layoutParams.width = CameraUtils.dp2px(mContext, 80);
        mCaptureLayout.requestLayout();
    }

    @Override
    public void onClick(View view) {
        if (null == mCaptureClickListener) {
            return;
        }
        if (view.getId() == R.id.capture_cancel) {//取消
            mCaptureClickListener.onCancel();
        } else if (view.getId() == R.id.camera_capture) {//拍照
            mCaptureClickListener.onCapture();
        } else if (view.getId() == R.id.camera_capture_retry) {//重试
            mCaptureClickListener.onRetry();
        } else if (view.getId() == R.id.camera_capture_confirm) {//确认
            mCaptureClickListener.onConfirm();
        }
    }
}

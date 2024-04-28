package com.android.sherlock;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.File;

import hb.sherlock.R;

/**
 * 悬浮入口工具类
 */
public class FloatingViewUtils {

    private static final String TAG = "FloatingViewUtils";

    private Context context;
    private int screenWidth;
    private FrameLayout.LayoutParams layoutParams;

    /**
     * R.id.content
     */
    private FrameLayout mFLContent;
    private View floatView;
    private File mCacheDir;

    /**
     * 是否显示过悬浮入口的提示，控制仅显示一次
     */
    private static boolean sIsFloatEnterShow = false;

    public static void install(Activity activity) {
        View decorView = activity.getWindow().getDecorView();
        Object tag = decorView.getTag(R.id.float_layout);
        if (tag == null) {
            FloatingViewUtils floatingViewUtils = new FloatingViewUtils();
            floatingViewUtils.init(activity);
            decorView.setTag(R.id.float_layout, floatingViewUtils);
        }
    }

    public static void showFloat(Activity activity, File cacheDir) {
        View decorView = activity.getWindow().getDecorView();
        Object tag = decorView.getTag(R.id.float_layout);
        if (tag instanceof FloatingViewUtils) {
            ((FloatingViewUtils) tag).mCacheDir = cacheDir;
            ((FloatingViewUtils) tag).checkFloat(activity);
        }
    }

    private FloatingViewUtils() {
    }

    public void init(Activity context) {
        this.context = context;
        if (mFLContent != null) {
            return;
        }
        mFLContent = context.findViewById(android.R.id.content);

        //获取屏宽
        screenWidth = getScreenWidth(this.context);
    }

 
    /**
     * 展示悬浮窗
     * @param floatView 悬浮窗view
     */
    @SuppressLint("RtlHardcoded")
    public void showFloatingWindow(@NonNull View floatView) {
        if (this.floatView != null) {
            return;//有悬浮窗在显示 不再显示新的悬浮窗
        }
        // 新建悬浮窗控件
        if (floatView == null) {
            throw new NullPointerException("悬浮窗view为null 确认view不为null");
        }
        this.floatView = floatView;
        //设置触摸事件
        floatView.setOnTouchListener(new FloatingOnTouchListener());

        // 设置LayoutParam
        layoutParams = (FrameLayout.LayoutParams) floatView.getLayoutParams();
        layoutParams.gravity = Gravity.LEFT | Gravity.CENTER;
        // 将悬浮窗控件添加到content
        mFLContent.addView(floatView);
    }
 
    private class FloatingOnTouchListener implements View.OnTouchListener {
        private int x;
        private int y;
        //标记是否执行move事件 如果执行了move事件  在up事件的时候判断悬浮窗的位置让悬浮窗处于屏幕左边或者右边
        private boolean isScroll;
        //标记悬浮窗口是否移动了  防止设置点击事件的时候 窗口移动松手后触发点击事件
        private boolean isMoved;
        //事件开始时和结束的时候  X和Y坐标位置
        private int startX;
        private int startY;
        @Override
        public boolean onTouch (View view, MotionEvent event){
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    x = (int) event.getRawX();
                    y = (int) event.getRawY();
                    isMoved = false;
                    isScroll = false;
                    startX = (int) event.getRawX();
                    startY = (int) event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    int nowX = (int) event.getRawX();
                    int nowY = (int) event.getRawY();
                    int movedX = nowX - x;
                    int movedY = nowY - y;
                    x = nowX;
                    y = nowY;
                    layoutParams.leftMargin = layoutParams.leftMargin + movedX;
                    layoutParams.topMargin = layoutParams.topMargin + movedY;
                    // 更新悬浮窗控件布局
                    mFLContent.updateViewLayout(view, layoutParams);
                    isScroll = true;
                    break;
                case MotionEvent.ACTION_UP:
                    int stopX = (int) event.getRawX();
                    int stopY = (int) event.getRawY();
                    if (Math.abs(startX - stopX) >= 1 || Math.abs(startY - stopY) >= 1) {
                        isMoved = true;
                    }
                    if (isScroll) {
                        autoView(view);
                    }
                    break;
            }
            return isMoved;
        }
 
        //悬浮窗view自动停靠在屏幕左边或者右边
        private void autoView (View view){
            // 得到view在屏幕中的位置
            int[] location = new int[2];
            view.getLocationOnScreen(location);
            //判断view位置是否在屏幕中线的左侧，是的话贴屏幕左边，否则贴屏幕右边
            if (location[0] < (getScreenWidth(context))/2) {
                layoutParams.leftMargin = 0;
            } else {
                layoutParams.leftMargin = getScreenWidth(context) - view.getWidth();
            }
            mFLContent.updateViewLayout(view, layoutParams);
        }
 
    }
 
    public View getFloatView() {
        return floatView;
    }
 
 
    /**
     * 获取屏幕宽度
     * @param context
     * @return
     */
    public int getScreenWidth(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    /***
     * 检查悬浮窗开启权限
     * @return
     */
    public void checkFloat(final Activity activity) {
        if (getFloatView() == null) {
            if (!sIsFloatEnterShow) {
                Toast.makeText(context, "隐私合规检测中，点击[悬浮入口]查看检测结果！", Toast.LENGTH_SHORT).show();
                sIsFloatEnterShow = true;
            }
            ImageView imageView = new ImageView(activity);
            int width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, activity.getResources()
                    .getDisplayMetrics());
            imageView.setLayoutParams(new FrameLayout.LayoutParams(width, width));
            imageView.setImageResource(context.getApplicationInfo().icon);
            showFloatingWindow(imageView);
            getFloatView().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        PrivacyCheckResultActivity.launch(v.getContext(), mCacheDir.getAbsolutePath());
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(context, "无法跳转【隐私合规检测结果】页面. " + Log.getStackTraceString(e), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
}
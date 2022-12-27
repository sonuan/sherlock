package com.android.sherlock;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 悬浮入口工具类
 */
public class FloatingViewUtils {
    private Handler sHandler = new Handler();
    private Runnable sApplyRunnable;
    private Context context;
    private int screenWidth;
    private WindowManager.LayoutParams layoutParams;
    private static int mLastX;
    private static int mLastY;

    private WindowManager windowManager;
    private View floatView;
    private File mCacheDir;

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
            ((FloatingViewUtils) tag).checkFloatPermission(activity);
        }
    }

    private FloatingViewUtils() {
    }

    public void init(Activity context) {
        this.context = context;
        if (windowManager != null) {
            return;
        }
        //获取WindowManager服务
        windowManager = (WindowManager) this.context.getSystemService(Context.WINDOW_SERVICE);
        //获取屏宽
        screenWidth = getScreenWidth(this.context);
    }
 
    /**
     * 展示悬浮窗
     * @param layoutId 悬浮窗布局文件id
     */
    @SuppressLint("RtlHardcoded")
    public void showFloatingWindow(@LayoutRes int layoutId) {
        // 新建悬浮窗控件
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        //        View floatView = layoutInflater.inflate(R.layout.floating_view, null);
        View floatView = layoutInflater.inflate(layoutId, null);
        if (floatView == null) {
            throw new NullPointerException("悬浮窗view为null 检查布局文件是否可用");
        }
        showFloatingWindow(floatView);
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
        //悬浮窗设置点击事件
        floatView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, "点击了悬浮窗", Toast.LENGTH_SHORT).show();
                unInit();
            }
        });
 
        // 设置LayoutParam
        layoutParams = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
        layoutParams.gravity = Gravity.LEFT | Gravity.CENTER;
        //设置flags 不然悬浮窗出来后整个屏幕都无法获取焦点，
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        layoutParams.format = PixelFormat.RGBA_8888;
        layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.x = mLastX;
        layoutParams.y = mLastY;
        // 将悬浮窗控件添加到WindowManager
        windowManager.addView(floatView, layoutParams);
    }
 
 
    /**
     * 移除悬浮View
     */
    public void unInit() {
        hideFloatWindow();
        this.context = null;
        // 获取WindowManager服务
        windowManager = null;
    }
 
    /**
     * 隐藏悬浮窗
     *    
     */
    public void hideFloatWindow() {
        if (floatView != null) {
            windowManager.removeViewImmediate(floatView);
            floatView = null;
        }
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
                    layoutParams.x = layoutParams.x + movedX;
                    layoutParams.y = layoutParams.y + movedY;
                    mLastX = layoutParams.x;
                    mLastY = layoutParams.y;
                    // 更新悬浮窗控件布局
                    windowManager.updateViewLayout(view, layoutParams);
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
                layoutParams.x = 0;
            } else {
                layoutParams.x = getScreenWidth(context) - view.getWidth();
            }
            mLastX = layoutParams.x;
            windowManager.updateViewLayout(view, layoutParams);
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
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        return manager.getDefaultDisplay().getWidth();
    }

    /***
     * 检查悬浮窗开启权限
     * @return
     */
    public void checkFloatPermission(final Activity activity) {
        boolean floatPermission = checkFloatPermission(context);
        if (!floatPermission) {
            Toast.makeText(context, "隐私合规检测中，请打开悬浮窗权限，方便查看检测结果！", Toast.LENGTH_SHORT).show();

            if (sApplyRunnable != null) {
                sHandler.removeCallbacksAndMessages(sApplyRunnable);
            }
            if (sApplyRunnable == null) {
                sApplyRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if (activity.isDestroyed() || activity.isFinishing()) {
                            return;
                        }
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                        intent.setData(Uri.parse("package:" + context.getPackageName()));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    }
                };
            }
            sHandler.postDelayed(sApplyRunnable, 3000);

        } else {
            if (getFloatView() == null) {
                Toast.makeText(context, "隐私合规检测中，点击[悬浮入口]查看检测结果！", Toast.LENGTH_SHORT).show();
                ImageView imageView = new ImageView(activity);
                imageView.setLayoutParams(new WindowManager.LayoutParams(100, 100));
                imageView.setImageResource(context.getApplicationInfo().icon);
                showFloatingWindow(imageView);
                getFloatView().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            //Intent intent = new Intent();
                            //intent.setClassName(context, "hb.devtools.XDevPrivacyCheckActivity");
                            //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            //v.getContext().startActivity(intent);
                            //

                            PrivacyCheckResultActivity.launch(v.getContext(), mCacheDir.getAbsolutePath());

                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(context, "集成XDevTools组件才能查看检测结果！", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
            else {
                WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) getFloatView().getLayoutParams();
                layoutParams.x = mLastX;
                layoutParams.y = mLastY;
                windowManager.updateViewLayout(getFloatView(), layoutParams);
            }
        }
    }

    /***
     * 检查悬浮窗开启权限
     * @param context
     * @return
     */
    private static boolean checkFloatPermission(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {//19 4.4
            return true;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {//23 6.0
            try {
                Class cls = Class.forName("android.content.Context");
                Field declaredField = cls.getDeclaredField("APP_OPS_SERVICE");
                declaredField.setAccessible(true);
                Object obj = declaredField.get(cls);
                if (!(obj instanceof String)) {
                    return false;
                }
                String str2 = (String) obj;
                obj = cls.getMethod("getSystemService", String.class).invoke(context, str2);
                cls = Class.forName("android.app.AppOpsManager");
                Field declaredField2 = cls.getDeclaredField("MODE_ALLOWED");
                declaredField2.setAccessible(true);
                Method checkOp = cls.getMethod("checkOp", Integer.TYPE, Integer.TYPE, String.class);
                int result = (Integer) checkOp.invoke(obj, 24, Binder.getCallingUid(), context.getPackageName());
                return result == declaredField2.getInt(cls);
            } catch (Exception e) {
                return false;
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {//26 8.0
                AppOpsManager appOpsMgr = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
                if (appOpsMgr == null) {
                    return false;
                }
                int mode = appOpsMgr.checkOpNoThrow("android:system_alert_window", android.os.Process.myUid(), context
                        .getPackageName());
                return Settings.canDrawOverlays(context) || mode == AppOpsManager.MODE_ALLOWED || mode == AppOpsManager.MODE_IGNORED;
            } else {
                return Settings.canDrawOverlays(context);
            }
        }
    }
}
package com.android.sherlock;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Application;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Process;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import androidx.core.content.ContextCompat;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 示例：
 *         XposedHelpers.findAndHookMethod(
 *                 "需要hook的方法所在类的完整类名",
 *                 lpparam.classLoader,      // 类加载器，固定这么写就行了
 *                 "需要hook的方法名",
 *                 参数类型.class,
 *                 new XC_MethodHook() {
 *                     @Override
 *                     protected void beforeHookedMethod(MethodHookParam param) {
 *                         XposedBridge.log("调用getDeviceId()获取了imei");
 *                     }
 *
 *                     @Override
 *                     protected void afterHookedMethod(MethodHookParam param) throws Throwable {
 *                         XposedBridge.log(getMethodStack());
 *                         super.afterHookedMethod(param);
 *                     }
 *                 }
 *         );
 *
 */
public class SherLockMonitor  implements IXposedHookLoadPackage {


    public @interface Type {
        String SERIAL_NO = "SERIAL_NO";
        String DEVICE_ID = "getDeviceId";
        String DEVICE_ID_INT = "getDeviceId(INT)";
        String IMEI = "IMEI";
        String SIM_SERIAL = "SIM_SERIAL";
        String IMSI = "IMSI";

        String ANDROID_ID = "ANDROID_ID";

        String MAC = "MAC";

        String INSTALLS = "INSTALLS";
        String PROCESS = "PROCESS";
        String CLIP = "CLIP";
        String LOCATION = "LOCATION";

        String STORAGE = "STORAGE";
    }

    public Map<String, Long> mInvokeTimeMap = new HashMap<>();

    private Context mContext;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        XposedBridge.log("Xposed hook start.");

        if (lpparam == null) {
            return;
        }
        //获取context
        hookContext(lpparam);
        //hook andoidId
        hookAndroidId(lpparam);

        //hook设备号相关
        hookDeviceId(lpparam);

        //hook MAC地址
        hookMacAddress(lpparam);

        //hook定位方法
        hookLocation(lpparam);

        //hook当前运行app进程
        hookProcesses(lpparam);

        //hook应用列表
        //hookInstallList(lpparam);

        //hook剪切板
        hookClip(lpparam);

        //hook序列号
        hookSN(lpparam);
        //hook存储权限未打开时访问文件
        hookStorage(lpparam);
    }

    private void hookContext(XC_LoadPackage.LoadPackageParam lpparam) {
        XposedHelpers.findAndHookMethod(
                Application.class.getName(),
                lpparam.classLoader,
                "attach",
                Context.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        mContext = (Context) param.args[0];
                        super.afterHookedMethod(param);
                    }
                }
        );
    }

    private void hookSN(XC_LoadPackage.LoadPackageParam lpparam) {
        XposedBridge.log("hookSN");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            XposedHelpers.findAndHookMethod(
                    Build.class.getName(),
                    lpparam.classLoader,
                    "getSerial",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            getMethodStack(param, "getSerial", Type.SERIAL_NO);
                            super.afterHookedMethod(param);
                        }
                    }
            );
        }

        XposedHelpers.findAndHookMethod(
                "android.os.SystemProperties",
                lpparam.classLoader,
                "get",
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if ("ro.serialno".equals(param.args[0])) {
                            getMethodStack(param, "get(ro.serialno)", Type.SERIAL_NO);
                        }
                        super.afterHookedMethod(param);
                    }
                }
        );
    }

    private void hookLocation(XC_LoadPackage.LoadPackageParam lpparam) {
        XposedBridge.log("hookLocation");

        XposedHelpers.findAndHookMethod(
                LocationManager.class.getName(),
                lpparam.classLoader,
                "getLastKnownLocation",
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        getMethodStack(param, "getLastKnownLocation", Type.LOCATION);
                        super.afterHookedMethod(param);
                    }
                }
        );
    }

    private void hookClip(XC_LoadPackage.LoadPackageParam lpparam) {
        XposedBridge.log("hookClip");

        XposedHelpers.findAndHookMethod(
                ClipboardManager.class.getName(),
                lpparam.classLoader,
                "getPrimaryClip",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        getMethodStack(param, "getPrimaryClip", Type.CLIP);
                        super.afterHookedMethod(param);
                    }
                }
        );
    }

    private void hookAndroidId(XC_LoadPackage.LoadPackageParam lpparam) {
        XposedBridge.log("hookAndroidId");

        XposedHelpers.findAndHookMethod(
                Settings.Secure.class.getName(),
                lpparam.classLoader,
                "getString",
                ContentResolver.class,
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (Settings.Secure.ANDROID_ID.equals(param.args[1])) {
                            getMethodStack(param, "AndroidId", Type.ANDROID_ID);
                        }
                        super.afterHookedMethod(param);
                    }
                }
        );
    }

    private void hookDeviceId(XC_LoadPackage.LoadPackageParam lpparam) {
        XposedBridge.log("hookDeviceId");

        //hook获取设备信息方法
        XposedHelpers.findAndHookMethod(
                android.telephony.TelephonyManager.class.getName(),
                lpparam.classLoader,
                "getImei",
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        getMethodStack(param, "getImei", Type.IMEI);
                        super.afterHookedMethod(param);
                    }
                }
        );

        //hook获取设备信息方法
        XposedHelpers.findAndHookMethod(
                android.telephony.TelephonyManager.class.getName(),
                lpparam.classLoader,
                "getDeviceId",
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        getMethodStack(param, "getDeviceId(int)", Type.DEVICE_ID_INT);
                        super.afterHookedMethod(param);
                    }
                }
        );

        //hook获取设备信息方法
        XposedHelpers.findAndHookMethod(
                android.telephony.TelephonyManager.class.getName(),
                lpparam.classLoader,
                "getDeviceId",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        getMethodStack(param, "getDeviceId", Type.DEVICE_ID);
                        super.afterHookedMethod(param);
                    }
                }
        );

        //hook获取设备信息方法
        XposedHelpers.findAndHookMethod(
                android.telephony.TelephonyManager.class.getName(),
                lpparam.classLoader,
                "getSimSerialNumber",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        getMethodStack(param, "getSimSerialNumber", Type.SIM_SERIAL);
                        super.afterHookedMethod(param);
                    }
                }
        );

        //hook imsi获取方法
        XposedHelpers.findAndHookMethod(
                android.telephony.TelephonyManager.class.getName(),
                lpparam.classLoader,
                "getSubscriberId",
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        getMethodStack(param, "getSubscriberId", Type.IMSI);
                        super.afterHookedMethod(param);
                    }
                }
        );
    }

    private void hookMacAddress(XC_LoadPackage.LoadPackageParam lpparam) {
        XposedBridge.log("hookMacAddress");

        //hook低版本系统获取mac地方方法
        XposedHelpers.findAndHookMethod(
                android.net.wifi.WifiInfo.class.getName(),
                lpparam.classLoader,
                "getMacAddress",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        getMethodStack(param, "getMacAddress", Type.MAC);
                        super.afterHookedMethod(param);
                    }
                }
        );
        //hook获取mac地址方法
        XposedHelpers.findAndHookMethod(
                java.net.NetworkInterface.class.getName(),
                lpparam.classLoader,
                "getHardwareAddress",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        getMethodStack(param, "getHardwareAddress", Type.MAC);
                        super.afterHookedMethod(param);
                    }
                }
        );
    }

    private void hookProcesses(XC_LoadPackage.LoadPackageParam lpparam) {
        XposedBridge.log("hookProcesses");

        //hook
        XposedHelpers.findAndHookMethod(
                ActivityManager.class.getName(),
                lpparam.classLoader,
                "getRunningTasks",
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        getMethodStack(param, "getRunningTasks", Type.PROCESS);
                        super.afterHookedMethod(param);
                    }
                }
        );

        //hook
        XposedHelpers.findAndHookMethod(
                ActivityManager.class.getName(),
                lpparam.classLoader,
                "getRunningAppProcesses",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        getMethodStack(param, "getRunningAppProcesses", Type.PROCESS);
                        super.afterHookedMethod(param);
                    }
                }
        );
    }

    /**
     * 获取安装列表
     *
     * @param lpparam
     */
    private void hookInstallList(XC_LoadPackage.LoadPackageParam lpparam) {
        XposedBridge.log("hookInstallList");

        //hook
        XposedHelpers.findAndHookMethod(
                "android.app.ApplicationPackageManager",
                lpparam.classLoader,
                "getInstalledPackages",
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        getMethodStack(param, "getInstalledPackages", Type.INSTALLS);
                        super.afterHookedMethod(param);
                    }
                }
        );

        //hook
        XposedHelpers.findAndHookMethod(
                "android.app.ApplicationPackageManager",
                lpparam.classLoader,
                "queryIntentActivities",
                Intent.class,
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        getMethodStack(param, "queryIntentActivities", Type.INSTALLS);
                        super.afterHookedMethod(param);
                    }
                }


        );

        //hook
        XposedHelpers.findAndHookMethod(
                "android.app.ApplicationPackageManager",
                lpparam.classLoader,
                "queryIntentActivitiesAsUser",
                Intent.class,
                int.class,
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        getMethodStack(param, "queryIntentActivitiesAsUser", Type.INSTALLS);
                        super.afterHookedMethod(param);
                    }
                }


        );

        //hook
        XposedHelpers.findAndHookMethod(
                "android.app.ApplicationPackageManager",
                lpparam.classLoader,
                "queryIntentActivityOptions",
                ComponentName.class,
                Intent[].class,
                Intent.class,
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        getMethodStack(param, "queryIntentActivityOptions", Type.INSTALLS);
                        super.afterHookedMethod(param);
                    }
                }


        );

        //hook
        XposedHelpers.findAndHookMethod(
                "android.app.ApplicationPackageManager",
                lpparam.classLoader,
                "getLeanbackLaunchIntentForPackage",
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        getMethodStack(param, "getLeanbackLaunchIntentForPackage", Type.INSTALLS);
                        super.afterHookedMethod(param);
                    }
                }


        );

        //hook
        XposedHelpers.findAndHookMethod(
                "android.app.ApplicationPackageManager",
                lpparam.classLoader,
                "queryIntentActivities",
                Intent.class,
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        getMethodStack(param, "queryIntentActivities", Type.INSTALLS);
                        super.afterHookedMethod(param);
                    }
                }


        );
    }

    private void hookStorage(XC_LoadPackage.LoadPackageParam lpparam) {
        XposedBridge.log("hookStorage: ");
        XposedHelpers.findAndHookMethod(
                "libcore.io.IoBridge",
                lpparam.classLoader,
                "open",
                String.class,
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String name = (String) param.args[0];
                        if (!TextUtils.isEmpty(name) && (name.contains("storage") || name.contains("sdcard"))) {
                            checkStoragePermission(param, "open(" + name + ")", Type.STORAGE);
                        }
                        super.afterHookedMethod(param);
                    }
                }


        );
    }

    private void getMethodStack(XC_MethodHook.MethodHookParam param, String method, @Type String type) {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();

        StringBuilder stringBuilder = new StringBuilder();

        boolean isHit = false;
        String line;
        String packageInfo = "";
        for (StackTraceElement temp : stackTraceElements) {
            line = temp.toString();
            if (line.contains("referenceBridge")) {
                isHit = true;
                continue;
            }
            if (!isHit) {
                continue;
            }
            if (TextUtils.isEmpty(packageInfo) && !line.contains("java.lang.reflect.Method.invoke")) {
                packageInfo = line.substring(0, line.indexOf('.', line.indexOf('.') + 1));
            }
            stringBuilder.append(line + "\n");
        }

        String key = type + "-" + packageInfo;
        Long lastTime = mInvokeTimeMap.get(key);
        if (lastTime == null) {
            lastTime = 0L;
        }
        long current = System.currentTimeMillis();
        mInvokeTimeMap.put(key, current);
        // 1秒内重复调用，则打印日志并显示toast
        if (current - lastTime < 1000) {
            String msg = packageInfo + "存在超频一秒内调用两次" + method + "获取" + type + "，堆栈：\n" + stringBuilder.toString();
            Log.e("Xposed", msg);
            Toast.makeText(mContext, msg.substring(0, 400), Toast.LENGTH_LONG).show();
        }
        // 针对需要「电话」权限判断的，在没有权限通过时调用则打印日志并显示toast
        if (Type.IMEI.equals(type)
                || Type.DEVICE_ID.equals(type)
                || Type.DEVICE_ID_INT.equals(type)
                || Type.SIM_SERIAL.equals(type)
                || Type.IMSI.equals(type)
                || Type.SERIAL_NO.equals(type)) {
            boolean isGranted = checkPermission(mContext, Manifest.permission.READ_PHONE_STATE);
            if (!isGranted) {
                String msg = packageInfo + "在「电话」权限未申请时调用" + method + "获取" + type + "，堆栈：\n" + stringBuilder.toString();
                Log.e("Xposed", msg);
                Toast.makeText(mContext, msg.substring(0, 400), Toast.LENGTH_LONG).show();
            }
        }

        stringBuilder.append("package:");
        stringBuilder.append(packageInfo);
        stringBuilder.append("\n");
        stringBuilder.append("pid:");
        stringBuilder.append(Process.myPid());
        stringBuilder.append("\n");
        stringBuilder.append("thread id:");
        stringBuilder.append(Thread.currentThread().getId());
        stringBuilder.append("-");
        stringBuilder.append(Thread.currentThread().getName());
        stringBuilder.append("\n");
        stringBuilder.append("result:");
        stringBuilder.append(param.getResult());
        stringBuilder.append("\n");
        stringBuilder.append("\n");

        XposedBridge.log("调用" + method + "获取" + type + "：" + stringBuilder.toString());
    }

    private void checkStoragePermission(XC_MethodHook.MethodHookParam param, String method, @Type String type) {
        // 针对需要「存储」权限判断的，在没有权限通过时调用则打印日志并显示toast
        boolean isGranted = checkPermission(mContext, Manifest.permission.READ_EXTERNAL_STORAGE) && checkPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (isGranted) {
            return;
        }
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();

        StringBuilder stringBuilder = new StringBuilder();

        boolean isHit = false;
        String line;
        String packageInfo = "";
        for (StackTraceElement temp : stackTraceElements) {
            line = temp.toString();
            if (line.contains("referenceBridge")) {
                isHit = true;
                continue;
            }
            if (!isHit) {
                continue;
            }
            if (TextUtils.isEmpty(packageInfo) && !line.contains("java.lang.reflect.Method.invoke")) {
                packageInfo = line.substring(0, line.indexOf('.', line.indexOf('.') + 1));
            }
            stringBuilder.append(line + "\n");
        }

        String msg = packageInfo + "在「存储」权限未申请时调用" + method + "操作文件" + "，堆栈：\n" + stringBuilder.toString();
        Log.e("Xposed", msg);
        Toast.makeText(mContext, msg.substring(0, 400), Toast.LENGTH_LONG).show();

        stringBuilder.append("package:");
        stringBuilder.append(packageInfo);
        stringBuilder.append("\n");
        stringBuilder.append("pid:");
        stringBuilder.append(Process.myPid());
        stringBuilder.append("\n");
        stringBuilder.append("thread id:");
        stringBuilder.append(Thread.currentThread().getId());
        stringBuilder.append("-");
        stringBuilder.append(Thread.currentThread().getName());
        stringBuilder.append("\n");
        stringBuilder.append("result:");
        stringBuilder.append(param.getResult());
        stringBuilder.append("\n");
        stringBuilder.append("\n");

        XposedBridge.log("调用" + method + "操作文件" + "：" + stringBuilder.toString());
    }


    /**
     * 检测权限是否已授权
     */
    public static boolean checkPermission(Context context, String perm) {
        if (context == null) {
            Log.e("Xposed", "checkPermission: context is null.");
            return false;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        return ContextCompat.checkSelfPermission(context.getApplicationContext(), perm) == PackageManager.PERMISSION_GRANTED;
    }
}

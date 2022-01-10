package com.android.sherlock;

import android.app.ActivityManager;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Process;
import android.os.UserHandle;
import android.provider.Settings;

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

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        XposedBridge.log("Xposed hook start.");

        if (lpparam == null) {
            return;
        }

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
        hookInstallList(lpparam);

        //hook剪切板
        hookClip(lpparam);
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
                        XposedBridge.log("调用getLastKnownLocation获取了GPS地址");
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("调用getLastKnownLocation获取了GPS地址：" + getMethodStack());
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
                        XposedBridge.log("调用getPrimaryClip");
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("调用getPrimaryClip：" + getMethodStack());
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
                        if (Settings.Secure.ANDROID_ID.equals(param.args[1])) {
                            XposedBridge.log("获取AndroidId");
                        }
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (Settings.Secure.ANDROID_ID.equals(param.args[1])) {
                            XposedBridge.log("获取AndroidId：" + getMethodStack());
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
                        XposedBridge.log("调用getImei获取了imei");
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("调用getImei获取了imei：" + getMethodStack());
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
                        XposedBridge.log("调用getDeviceId(int)获取了imei");
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("调用getDeviceId(int)获取了imei：" + getMethodStack());
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
                        XposedBridge.log("调用getDeviceId获取了imei");
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("调用getDeviceId获取了imei：" + getMethodStack() + "---->" + param.getResult());
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
                        XposedBridge.log("调用getSimSerialNumber");
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("调用getSimSerialNumber：" + getMethodStack() + "---->" + param.getResult());
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
                        XposedBridge.log("调用getSubscriberId获取了imsi");
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("调用getSubscriberId获取了imsi：" + getMethodStack() + "---->" + param.getResult());
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
                        XposedBridge.log("调用getMacAddress()获取了mac地址");
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("调用getMacAddress()获取了mac地址：" + getMethodStack() + "---->" + param.getResult());
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
                        XposedBridge.log("调用getHardwareAddress()获取了mac地址");
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("调用getHardwareAddress()获取了mac地址：" + getMethodStack() + "---->" + param.getResult());
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
                        XposedBridge.log("调用getRunningTasks");
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("调用getRunningTasks：" + getMethodStack());
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
                        XposedBridge.log("调用getRunningAppProcesses");
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("调用getRunningAppProcesses：" + getMethodStack());
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
                        XposedBridge.log("调用getInstalledPackages");
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("调用getInstalledPackages：" + getMethodStack());
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
                        XposedBridge.log("调用queryIntentActivities");
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("调用queryIntentActivities：" + getMethodStack());
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
                        XposedBridge.log("调用queryIntentActivitiesAsUser");
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("调用queryIntentActivitiesAsUser：" + getMethodStack());
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
                        XposedBridge.log("调用queryIntentActivityOptions");
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("调用queryIntentActivityOptions：" + getMethodStack());
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
                        XposedBridge.log("调用getLeanbackLaunchIntentForPackage");
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("调用getLeanbackLaunchIntentForPackage：" + getMethodStack());
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
                        XposedBridge.log("调用queryIntentActivities");
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("调用queryIntentActivities：" + getMethodStack());
                        super.afterHookedMethod(param);
                    }
                }


        );
    }

    private String getMethodStack() {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\n");
        stringBuilder.append("pid:");
        stringBuilder.append(Process.myPid());
        stringBuilder.append("\n");
        stringBuilder.append("thread id:");
        stringBuilder.append(Thread.currentThread().getId());
        stringBuilder.append("-");
        stringBuilder.append(Thread.currentThread().getName());
        stringBuilder.append("\n");

        for (StackTraceElement temp : stackTraceElements) {
            stringBuilder.append(temp.toString() + "\n");
        }

        return stringBuilder.toString();

    }
}

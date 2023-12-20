package cn.buffcow.hypersc

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Entry of xposed hook.
 *
 * @author qingyu
 * <p>Create on 2023/12/20 22:46</p>
 */
class HookEntry : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "android") return
        XposedBridge.hookAllMethods(
            XposedHelpers.findClass(
                "com.android.server.MiuiBatteryIntelligence\$BatteryNotificationListernerService",
                lpparam.classLoader
            ),
            "isNavigationStatus",
            XC_MethodReplacement.returnConstant(true)
        )
    }
}

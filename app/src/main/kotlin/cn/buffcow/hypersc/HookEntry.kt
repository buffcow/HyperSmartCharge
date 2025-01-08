package cn.buffcow.hypersc

import cn.buffcow.hypersc.hook.ProtectFragmentHooker
import cn.buffcow.hypersc.utils.ProtectNotificationHelper
import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.factory.configs
import com.highcapable.yukihookapi.hook.factory.encase
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit

/**
 * Entry of xposed hook.
 *
 * @author qingyu
 * <p>Create on 2023/12/20 22:46</p>
 */
@InjectYukiHookWithXposed
class HookEntry : IYukiHookXposedInit {

    override fun onHook() = encase {
        loadApp("com.miui.securitycenter") {
            withProcess(mainProcessName) {
                loadHooker(ProtectFragmentHooker)
            }
            withProcess("${mainProcessName}.remote") {
                onAppLifecycle {
                    onCreate {
                        ProtectNotificationHelper.registerBatteryReceiver(this)
                    }
                }
            }
        }
    }

    override fun onInit() = configs {
        debugLog { isDebug = BuildConfig.DEBUG; tag = LOG_TAG }
    }

    companion object {
        private const val LOG_TAG = "HyperSmartCharge"
    }
}

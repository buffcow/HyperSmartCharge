package cn.buffcow.hypersc.hook

import android.app.AlertDialog
import android.content.Context
import android.widget.Toast
import cn.buffcow.hypersc.R
import cn.buffcow.hypersc.utils.ChargeProtectionUtils
import cn.buffcow.hypersc.utils.RemoteEventHelper
import cn.buffcow.hypersc.view.ChargeValueSetDialogView
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.type.android.BundleClass
import com.highcapable.yukihookapi.hook.type.java.BooleanType
import com.highcapable.yukihookapi.hook.type.java.StringClass
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.XposedHelpers.callMethod


/**
 * Hooker for charge protect fragment.
 *
 * @author qingyu
 * <p>Create on 2025/01/02 16:04</p>
 */
object ProtectFragmentHooker : YukiBaseHooker() {

    // private const val PREFERENCE_KEY_ALWAYS_PROTECT = "cb_always_charge_protect"
    private const val PREFERENCE_KEY_INTELLECT_PROTECT = "cb_intellect_charge_protect"
    private const val PREFERENCE_KEY_CATEGORY_PROTECT = "category_features_battery_protect"
    private const val PREFERENCE_KEY_SMART_CHARGE_VALUE_SET = "charge_protect_value_setting"

    override fun onHook() {
        "com.miui.powercenter.nightcharge.ChargeProtectFragment".toClass().method {
            name = "onCreatePreferences"
            paramCount = 2
            param(BundleClass, StringClass)
        }.hook().after { onCreatePreferences(instance) }
    }

    private fun onCreatePreferences(fragment: Any) {
        val smartChargeAvailable = checkSmartChargeAvailable(fragment)
        YLog.debug("onCreatePreferences() called with: fragment = $fragment, available=$smartChargeAvailable")
        if (smartChargeAvailable) {
            fragment.javaClass.apply {
                method {
                    name = "onPreferenceClick"
                    paramCount = 1
                    returnType = BooleanType
                }.hook().after { onPreferenceClick(instance, args(0).any()) }

                // method {
                //     name = "onPreferenceChange"
                //     paramCount = 2
                //     returnType = BooleanType
                // }.hook().after {
                //     onPreferenceChange(instance, args(0).any(), args(1).any())
                // }
            }
            addSmartChargeTextPreference(fragment)
        } else {
            appContext?.let { RemoteEventHelper.sendEvent(it, RemoteEventHelper.Event.UnregisterBatteryReceiver) }
        }
    }

    private fun onPreferenceClick(fragment: Any, preference: Any?) {
        YLog.debug("onPreferenceClick() called with: preference = $preference")
        if (callMethod(preference, "getKey") == PREFERENCE_KEY_SMART_CHARGE_VALUE_SET) {
            val context = getContext(fragment)
            val dialogView = ChargeValueSetDialogView(context) { moduleAppResources }
            AlertDialog.Builder(context)
                .setTitle(moduleAppResources.getString(R.string.app_name))
                .setView(dialogView)
                .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    val (suc, value) = dialogView.syncProtectValue()
                    Toast.makeText(context, "$suc($value)", Toast.LENGTH_SHORT).show()
                    callMethod(preference, "setText", getSmartChargeValueText(getContext(fragment), value))
                    appContext?.let {
                        val pv = if (suc) value?.toString() else null
                        RemoteEventHelper.sendEvent(it, RemoteEventHelper.Event.UpdateNotification(pv))
                    }
                    dialog.dismiss()
                }
                .create().show()
        }
    }

    // private fun onPreferenceChange(fragment: Any, preference: Any?, obj: Any?) {
    //     YLog.debug("onPreferenceChange() called with: preference = $preference, obj = $obj")
    //     callMethod(preference ?: return, "getKey").takeIf { k ->
    //         k == PREFERENCE_KEY_ALWAYS_PROTECT
    //                 || k == PREFERENCE_KEY_INTELLECT_PROTECT
    //     } ?: return
    //     callMethod(
    //         findPreference(getPreferenceScreen(fragment), PREFERENCE_KEY_SMART_CHARGE_VALUE_SET),
    //         "setEnabled",
    //         (obj as? Boolean)?.not() ?: checkSmartChargeShouldEnable(fragment)
    //     )
    // }

    private fun addSmartChargeTextPreference(fragment: Any) {
        val smartChargeProtectCategory = XposedHelpers.newInstance(
            "miuix.preference.PreferenceCategory".toClass(),
            getContext(fragment), // ctx
            null // attrs
        )
        callMethod(getPreferenceScreen(fragment), "addPreference", smartChargeProtectCategory)

        XposedHelpers.newInstance(
            "miuix.preference.TextPreference".toClass(),
            getContext(fragment)
        ).apply {
            callMethod(this, "setOnPreferenceClickListener", fragment)
            callMethod(this, "setKey", PREFERENCE_KEY_SMART_CHARGE_VALUE_SET)
            callMethod(this, "setEnabled", true)
            callMethod(this, "setText", getSmartChargeValueText(getContext(fragment)))
            callMethod(this, "setTitle", moduleAppResources.getString(R.string.app_name))
            callMethod(this, "setSummary", moduleAppResources.getString(R.string.smart_charge_pref_summary))
            callMethod(smartChargeProtectCategory, "addPreference", this)
        }
    }

    private fun getSmartChargeValueText(
        context: Context,
        value: Int? = ChargeProtectionUtils.getSmartChargePercentValue(context),
    ): String {
        return value?.takeIf(ChargeProtectionUtils::isSmartChargePercentValueValid)?.let {
            moduleAppResources.getString(R.string.smart_charge_value_per, it)
        } ?: moduleAppResources.getString(R.string.smart_charge_close)
    }

    private fun checkSmartChargeAvailable(fragment: Any) = getProtectCategory(fragment)?.let {
        findPreference(it, PREFERENCE_KEY_INTELLECT_PROTECT)
    } != null

    // private fun checkSmartChargeShouldEnable(fragment: Any) = try {
    //     getProtectCategory(fragment)?.run {
    //         val always = findPreference(this, PREFERENCE_KEY_ALWAYS_PROTECT)?.let {
    //             callMethod(it, "isChecked")
    //         }
    //         val intellect = findPreference(this, PREFERENCE_KEY_INTELLECT_PROTECT)?.let {
    //             callMethod(it, "isChecked")
    //         }
    //         always != true && intellect != true
    //     } == true
    // } catch (th: Throwable) {
    //     YLog.error("checkSmartChargeShouldEnable error:", th)
    //     false
    // }

    private fun getProtectCategory(fragment: Any) = findPreference(
        fragment,
        PREFERENCE_KEY_CATEGORY_PROTECT
    )

    private fun getContext(fragment: Any): Context {
        return callMethod(fragment, "requireContext") as Context
    }

    private fun getPreferenceScreen(fragment: Any): Any = callMethod(fragment, "getPreferenceScreen")

    private fun findPreference(who: Any, key: String): Any? = callMethod(who, "findPreference", key)
}

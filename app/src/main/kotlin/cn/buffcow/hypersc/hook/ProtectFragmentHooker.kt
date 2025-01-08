package cn.buffcow.hypersc.hook

import android.app.AlertDialog
import android.app.Dialog
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

    private const val PREFERENCE_KEY_ALWAYS_PROTECT = "cb_always_charge_protect"
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

                method {
                    name = "onPreferenceChange"
                    paramCount = 2
                    returnType = BooleanType
                }.hook().after {
                    onPreferenceChange(instance, args(0).any(), args(1).any())
                }
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
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.cancel() }
                .create().apply {
                    show()
                    getButton(Dialog.BUTTON_POSITIVE).setOnClickListener {
                        val (suc, value) = dialogView.syncProtectValue()
                        Toast.makeText(context, "$suc($value)", Toast.LENGTH_SHORT).show()
                        callMethod(preference, "setText", getSmartChargeValueText(getContext(fragment), value))
                        appContext?.let {
                            val pv = if (suc) value?.toString() else null
                            RemoteEventHelper.sendEvent(it, RemoteEventHelper.Event.UpdateNotification(pv))
                        }
                        dismiss()
                    }
                }
        }
    }

    private fun onPreferenceChange(fragment: Any, preference: Any?, obj: Any?) {
        YLog.debug("onPreferenceChange() called with: preference = $preference, obj = $obj")
        preference ?: return
        getProtectCategory(fragment)
            ?.let { findPreference(it, PREFERENCE_KEY_SMART_CHARGE_VALUE_SET) }
            ?.takeIf {
                callMethod(preference, "getKey").let { k ->
                    k == PREFERENCE_KEY_ALWAYS_PROTECT
                            || k == PREFERENCE_KEY_INTELLECT_PROTECT
                }
            }
            ?.let { pref ->
                callMethod(
                    pref,
                    "setEnabled",
                    (obj as? Boolean)?.not() ?: checkSmartChargeShouldEnable(fragment)
                )
            }
    }

    private fun addSmartChargeTextPreference(fragment: Any) {
        XposedHelpers.newInstance("miuix.preference.TextPreference".toClass(), getContext(fragment)).apply {
            callMethod(this, "setOnPreferenceClickListener", fragment)
            callMethod(this, "setKey", PREFERENCE_KEY_SMART_CHARGE_VALUE_SET)
            callMethod(this, "setEnabled", checkSmartChargeShouldEnable(fragment))
            callMethod(this, "setText", getSmartChargeValueText(getContext(fragment)))
            callMethod(this, "setTitle", moduleAppResources.getString(R.string.app_name))
            callMethod(this, "setSummary", moduleAppResources.getString(R.string.smart_charge_pref_summary))
            getProtectCategory(fragment)?.let { callMethod(it, "addPreference", this) }
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

    private fun checkSmartChargeShouldEnable(fragment: Any) = try {
        getProtectCategory(fragment)?.run {
            val always = findPreference(this, PREFERENCE_KEY_ALWAYS_PROTECT)?.let {
                callMethod(it, "isChecked")
            }
            val intellect = findPreference(this, PREFERENCE_KEY_INTELLECT_PROTECT)?.let {
                callMethod(it, "isChecked")
            }
            always != true && intellect != true
        } == true
    } catch (th: Throwable) {
        YLog.error("checkSmartChargeShouldEnable error:", th)
        false
    }

    private fun getProtectCategory(fragment: Any) = findPreference(
        fragment,
        PREFERENCE_KEY_CATEGORY_PROTECT
    )

    private fun getContext(fragment: Any): Context {
        return callMethod(fragment, "requireContext") as Context
    }

    private fun findPreference(who: Any, key: String): Any? = callMethod(who, "findPreference", key)
}

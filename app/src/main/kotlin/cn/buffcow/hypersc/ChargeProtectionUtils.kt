package cn.buffcow.hypersc

import android.content.Context
import android.content.SharedPreferences
import com.highcapable.yukihookapi.hook.log.YLog
import miui.util.IMiCharge

/**
 * @author qingyu
 * <p>Create on 2025/01/03 17:12</p>
 */
object ChargeProtectionUtils {

    const val MIN_CHARGE_PERCENT_VALUE = 20
    const val MAX_CHARGE_PERCENT_VALUE = 100

    private const val PREF_KEY_SMART_CHARGE_VALUE = "smart_charge_value"

    fun closeSmartCharge(): Boolean = setSmartChargeValue("0x10")

    fun openCommonProtectMode(value: Int): Boolean {
        val valueToSet = "0x${((value shl 16) or 17).toString(16)}"
        val res = setSmartChargeValue(valueToSet)
        YLog.debug("openCommonProtectMode:$res, setValue:$valueToSet")
        return res
    }

    private fun getSmartChargeValue(): String? = try {
        IMiCharge.getInstance().getMiChargePath("smart_chg").also {
            YLog.debug("getSmartChargeValue res:$it")
        }
    } catch (th: Throwable) {
        YLog.error("getSmartChargeValue error:", th)
        null
    }

    private fun setSmartChargeValue(value: String): Boolean = try {
        IMiCharge.getInstance().setMiChargePath("smart_chg", value).also {
            YLog.debug("setSmartChargeValue:$value, res:$it")
        }
    } catch (th: Throwable) {
        YLog.error("setSmartChargeValue error:", th)
        false
    }

    fun getSmartChargeValueFromSP(ctx: Context): Int? = with(ctx.getSmartChargePref()) {
        getString(PREF_KEY_SMART_CHARGE_VALUE, null)?.toIntOrNull()?.also { v ->
            if (!isChargePercentValueAvailable(v) || (getSmartChargeValue()?.toIntOrNull() ?: 1) <= 0) {
                YLog.warn("smart charge has been turned off, remove pref")
                edit().remove(PREF_KEY_SMART_CHARGE_VALUE).apply()
                return null
            }
        }
    }

    fun saveSmartChargeValueToSP(ctx: Context, value: Int?) {
        ctx.getSmartChargePref().edit().apply {
            value?.takeIf(::isChargePercentValueAvailable)?.let {
                putString(PREF_KEY_SMART_CHARGE_VALUE, "$value")
            } ?: remove(PREF_KEY_SMART_CHARGE_VALUE)
            commit()
        }
    }

    fun isChargePercentValueAvailable(perValue: Int): Boolean {
        return perValue in MIN_CHARGE_PERCENT_VALUE..MAX_CHARGE_PERCENT_VALUE
    }

    private fun Context.getSmartChargePref(): SharedPreferences {
        return applicationContext.getSharedPreferences(
            "pref_hyper_smart_charge",
            Context.MODE_PRIVATE
        )
    }
}

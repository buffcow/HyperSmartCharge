package cn.buffcow.hypersc.utils

import android.content.Context
import android.provider.Settings
import com.highcapable.yukihookapi.hook.log.YLog
import miui.util.IMiCharge

/**
 * @author qingyu
 * <p>Create on 2025/01/03 17:12</p>
 */
object ChargeProtectionUtils {

    const val MIN_CHARGE_PERCENT_VALUE = 20
    const val MAX_CHARGE_PERCENT_VALUE = 100

    private const val KEY_SMART_CHARGE_PERCENT_VALUE = "smart_charge_percent_value"

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

    fun getSmartChargePercentValue(ctx: Context): Int? {
        val value = ctx.getPercentValue() ?: return null
        return when {
            !isSmartChargePercentValueValid(value) -> {
                YLog.warn("smart charge percent value invalid, remove now")
                ctx.putPercentValue(null)
            }

            (getSmartChargeValue()?.toIntOrNull() ?: 1) <= 0 -> {
                val res = openCommonProtectMode(value)
                YLog.warn("maybe reboot or smart_chg value changed by sys, retry set:$res")
                if (res) value else ctx.putPercentValue(null)
            }

            else -> value
        }
    }

    fun putSmartChargePercentValue(ctx: Context, value: Int?) {
        ctx.putPercentValue(value?.takeIf(ChargeProtectionUtils::isSmartChargePercentValueValid))
    }

    fun isSmartChargePercentValueValid(perValue: Int): Boolean {
        return perValue in MIN_CHARGE_PERCENT_VALUE..MAX_CHARGE_PERCENT_VALUE
    }

    private fun Context.getPercentValue(): Int? {
        return Settings.System.getString(contentResolver, KEY_SMART_CHARGE_PERCENT_VALUE)?.toIntOrNull()
    }

    private fun Context.putPercentValue(value: Int?): Int? {
        return if (Settings.System.putString(
                contentResolver,
                KEY_SMART_CHARGE_PERCENT_VALUE,
                value?.toString()
            )
        ) value else null
    }
}

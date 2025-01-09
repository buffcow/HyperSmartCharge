package cn.buffcow.hypersc.view

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import cn.buffcow.hypersc.R
import cn.buffcow.hypersc.utils.ChargeProtectionUtils

/**
 * @author qingyu
 * <p>Create on 2025/01/03 18:50</p>
 */
class ChargeValueSetDialogView @JvmOverloads constructor(
    context: Context,
    private val moduleResourcesFetcher: () -> Resources = { context.resources },
) : LinearLayout(context), SeekBar.OnSeekBarChangeListener {

    private val mSeekBar: SeekBar

    private val mTvSeekBarValue: TextView

    private val moduleResources get() = moduleResourcesFetcher.invoke()

    private val Int.realProgressValue
        get() = (this + MIN_PROGRESS_VALUE).takeIf(ChargeProtectionUtils::isSmartChargePercentValueValid)

    init {
        orientation = VERTICAL
        layoutDirection = LAYOUT_DIRECTION_LTR
        addViewInLayout(
            TextView(context).also { mTvSeekBarValue = it },
            0,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
                setMargins(0, 10.dp2px(), 0, 0)
            }
        )
        addViewInLayout(
            SeekBar(context).also { mSeekBar = it },
            1,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(12.dp2px(), 28.dp2px(), 12.dp2px(), 0)
            }
        )
        addViewInLayout(
            TextView(context).apply {
                text = moduleResources.getString(R.string.smart_charge_set_note).trimIndent()
                setTextColor(Color.GRAY)
            },
            2,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(12.dp2px(), 30.dp2px(), 12.dp2px(), 0)
            }
        )
    }

    private fun initView() {
        mSeekBar.apply {
            max = MAX_PROGRESS_VALUE - MIN_PROGRESS_VALUE
            ChargeProtectionUtils.getSmartChargePercentValue(context)?.let {
                progress = (it - MIN_PROGRESS_VALUE).coerceAtLeast(0)
            }
            invalidateSeekbarValueText(progress)
            setOnSeekBarChangeListener(this@ChargeValueSetDialogView)
        }
        mTvSeekBarValue.setTextColor(Color.BLACK)
    }

    private fun invalidateSeekbarValueText(progress: Int) {
        mTvSeekBarValue.text = progress.realProgressValue?.let { pv ->
            moduleResources.getString(R.string.smart_charge_value_per, pv)
        } ?: moduleResources.getString(R.string.smart_charge_close)
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        if (fromUser) invalidateSeekbarValueText(progress)
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        initView()
    }

    fun syncProtectValue(): Pair<Boolean, Int?> {
        val percentValue = mSeekBar.progress.realProgressValue

        val res = percentValue?.let { pv ->
            ChargeProtectionUtils.openCommonProtectMode(pv)
        } ?: ChargeProtectionUtils.closeSmartCharge()

        ChargeProtectionUtils.putSmartChargePercentValue(context, percentValue)

        return res to percentValue
    }

    private fun Int.dp2px() = if (this <= 0) 0 else {
        (context.resources.displayMetrics.density * this + 0.5).toInt()
    }

    companion object {
        private const val MIN_PROGRESS_VALUE = ChargeProtectionUtils.MIN_CHARGE_PERCENT_VALUE - 1
        private const val MAX_PROGRESS_VALUE = ChargeProtectionUtils.MAX_CHARGE_PERCENT_VALUE
    }
}

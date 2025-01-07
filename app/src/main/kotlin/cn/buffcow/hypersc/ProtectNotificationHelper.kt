package cn.buffcow.hypersc

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.os.BatteryManager
import com.highcapable.yukihookapi.hook.log.YLog
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Helper for notification.
 *
 * @author qingyu
 * <p>Create on 2025/01/06 19:25</p>
 */
object ProtectNotificationHelper {

    private const val NOTIFICATION_ID = 1008611
    private const val CHANNEL_ID = "com.miui.powercenter.low"

    private var notificationShowed = false
    private val batteryRegistered = AtomicBoolean(false)

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != Intent.ACTION_BATTERY_CHANGED) return

            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 100)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)

            when (status) {
                BatteryManager.BATTERY_STATUS_FULL,
                BatteryManager.BATTERY_STATUS_CHARGING,
                    -> if (level <= 100 && plugged != 0) showNotification(context)

                else -> removeNotification(context)
            }
        }
    }

    fun registerBatteryReceiver(context: Context) {
        if (batteryRegistered.compareAndSet(false, true)) {
            context.applicationContext.registerReceiver(
                batteryReceiver,
                IntentFilter().apply {
                    addAction(Intent.ACTION_BATTERY_CHANGED)
                }
            )
            YLog.debug("registered battery changed receiver.")
        }
    }

    fun unregisterBatteryReceiver(context: Context) {
        if (batteryRegistered.compareAndSet(true, false)) {
            context.applicationContext.unregisterReceiver(batteryReceiver)
            removeNotification(context)
            YLog.debug("unregistered battery changed receiver.")
        }
    }

    @SuppressLint("NotificationPermission")
    private fun showNotification(context: Context) {
        if (notificationShowed) return
        val perChg = ChargeProtectionUtils.getSmartChargeValueFromSP(context) ?: return

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(context, "battery_and_property_ordinary_notify"),
                NotificationManager.IMPORTANCE_LOW
            )
        )

        val icon = Icon.createWithResource(
            context,
            "ic_performance_notification".resolveAsId(context, "drawable")
        )
        val intent = PendingIntent.getActivity(
            context, 0,
            Intent(
                context,
                context.classLoader.loadClass("com.miui.powercenter.nightcharge.ChargerProtectActivity")
            ),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(getString(context, "pc_health_charge_protect_title"))
            .setContentText(getString(context, "pc_health_charge_protect_noti_summary_title", "$perChg%"))
            .setAutoCancel(true)
            .setContentIntent(intent)
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)

        notificationShowed = true
    }

    private fun removeNotification(context: Context) {
        context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
        notificationShowed = false
    }

    private fun getString(context: Context, name: String, vararg args: String): String {
        return context.getString(name.resolveAsId(context, "string"), *args)
    }

    @SuppressLint("DiscouragedApi")
    private fun String.resolveAsId(context: Context, type: String): Int {
        return context.resources.getIdentifier(
            this,
            type,
            context.packageName
        )
    }
}

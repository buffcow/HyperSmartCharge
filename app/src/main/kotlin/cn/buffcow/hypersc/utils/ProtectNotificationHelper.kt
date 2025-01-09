package cn.buffcow.hypersc.utils

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
object ProtectNotificationHelper : RemoteEventHelper.EventListener {

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
                    -> if (level <= 100 && plugged != 0) createAndShowNotification(context)

                else -> removeNotification(context)
            }
        }
    }

    fun registerBatteryReceiver(context: Context) {
        if (batteryRegistered.compareAndSet(false, true)) {
            context.applicationContext.registerReceiver(
                batteryReceiver,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            RemoteEventHelper.register(context, this)
            YLog.debug("registered battery changed receiver.")
        }
    }

    override fun onReceive(context: Context, event: RemoteEventHelper.Event, intent: Intent) {
        YLog.debug("receive client event:$event, intent:$intent")
        when (event) {
            RemoteEventHelper.Event.UnregisterBatteryReceiver -> {
                unregisterBatteryReceiver(context)
            }

            is RemoteEventHelper.Event.UpdateNotification -> {
                event.percentValue?.let { value ->
                    if (notificationShowed) {
                        publishNotification(context, value)
                    } else if (batteryRegistered.get()) {
                        context.registerReceiver(
                            null,
                            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                        )?.let { batteryReceiver.onReceive(context, it) }
                    }
                } ?: removeNotification(context)
            }
        }
    }

    private fun unregisterBatteryReceiver(context: Context) {
        if (batteryRegistered.compareAndSet(true, false)) {
            context.applicationContext.unregisterReceiver(batteryReceiver)
            removeNotification(context)
            YLog.debug("unregistered battery changed receiver.")
        }
    }

    fun createAndShowNotification(context: Context) {
        if (notificationShowed) return
        val perChg = ChargeProtectionUtils.getSmartChargePercentValue(context)
        YLog.debug("showNotification-smart charge percent value:$perChg")
        perChg ?: return

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString("battery_and_property_ordinary_notify"),
                NotificationManager.IMPORTANCE_LOW
            )
        )

        publishNotification(context, "$perChg", notificationManager)

        notificationShowed = true
    }

    @SuppressLint("NotificationPermission")
    private fun publishNotification(
        context: Context,
        percentValue: String,
        notificationManager: NotificationManager = context.getSystemService(NotificationManager::class.java),
    ) {
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
        Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(context.getString("pc_health_charge_protect_title"))
            .setContentText(context.getString("pc_health_charge_protect_noti_summary_title", "$percentValue%"))
            .setAutoCancel(true)
            .setContentIntent(intent)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .build()
            .apply { notificationManager.notify(NOTIFICATION_ID, this) }
    }

    private fun removeNotification(context: Context) {
        context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
        notificationShowed = false
    }

    private fun Context.getString(name: String, vararg args: String): String {
        return getString(name.resolveAsId(this, "string"), *args)
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

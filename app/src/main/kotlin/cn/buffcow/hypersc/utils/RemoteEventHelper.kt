package cn.buffcow.hypersc.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Helper for cross process event。
 *
 * @author qingyu
 * <p>Create on 2025/01/08 15:24</p>
 */
object RemoteEventHelper {

    private val listeners = CopyOnWriteArrayList<EventListener>()

    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Event.intentOf(intent)?.let { event ->
                listeners.forEach {
                    it.onReceive(context, event, intent)
                }
            }
        }
    }

    fun register(context: Context, listener: EventListener) {
        try {
            context.unregisterReceiver(mReceiver)
        } catch (_: Exception) {
        }
        listeners.addIfAbsent(listener)
        context.registerReceiver(
            mReceiver,
            IntentFilter().apply { Event.actions.forEach(::addAction) },
            Context.RECEIVER_NOT_EXPORTED
        )
    }

    fun sendEvent(context: Context, event: Event) {
        context.sendBroadcast(
            Intent(event.action).apply { event.data?.let(::putExtras) }
        )
    }

    sealed class Event(val action: String, val data: Bundle? = null) {

        data object UnregisterBatteryReceiver : Event(ACTION_UNREGISTER_BATTERY_RECEIVER)

        data class UpdateNotification(val percentValue: String?) : Event(
            ACTION_UPDATE_NOTIFICATION,
            Bundle().apply { putString(EXTRA_UPDATE_NOTIFICATION_VALUE, percentValue) }
        )

        companion object {

            val actions = listOf(
                ACTION_UPDATE_NOTIFICATION,
                ACTION_UNREGISTER_BATTERY_RECEIVER
            )

            fun intentOf(intent: Intent): Event? {
                return when (intent.action) {
                    ACTION_UPDATE_NOTIFICATION -> UpdateNotification(intent.getStringExtra(EXTRA_UPDATE_NOTIFICATION_VALUE))
                    ACTION_UNREGISTER_BATTERY_RECEIVER -> UnregisterBatteryReceiver
                    else -> null
                }
            }
        }
    }

    fun interface EventListener {
        fun onReceive(context: Context, event: Event, intent: Intent)
    }
}

private const val ACTION_UPDATE_NOTIFICATION = "com.miui.security-center.action.UPDATE_NOTIFICATION"
private const val EXTRA_UPDATE_NOTIFICATION_VALUE = "com.miui.security-center.extra.UPDATE_NOTIFICATION_VALUE"

private const val ACTION_UNREGISTER_BATTERY_RECEIVER = "com.miui.security-center.action.UNREGISTER_BATTERY_RECEIVER"

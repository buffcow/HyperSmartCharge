package cn.buffcow.hypersc.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
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
            intent.getParcelableExtra(EXTRA_REMOTE_EVENT, Event::class.java)?.let { event ->
                listeners.forEach {
                    it.onReceive(context, event, intent)
                }
            }
        }
    }

    fun register(context: Context, listener: EventListener) {
        if (listeners.addIfAbsent(listener)) {
            try {
                context.unregisterReceiver(mReceiver)
            } catch (_: Exception) {
            }
            context.registerReceiver(
                mReceiver,
                IntentFilter(ACTION_SEND_REMOTE_EVENT),
                Context.RECEIVER_NOT_EXPORTED
            )
        }
    }

    fun sendEvent(context: Context, event: Event) {
        context.sendBroadcast(
            Intent(ACTION_SEND_REMOTE_EVENT).apply {
                setPackage(context.packageName)
                putExtra(EXTRA_REMOTE_EVENT, event)
            }
        )
    }

    @Parcelize
    sealed class Event : Parcelable {
        data object UnregisterBatteryReceiver : Event()
        data class UpdateNotification(val percentValue: String?) : Event()
    }

    fun interface EventListener {
        fun onReceive(context: Context, event: Event, intent: Intent)
    }
}

private const val EXTRA_REMOTE_EVENT = "com.miui.security-center.extra.REMOTE_EVENT"
private const val ACTION_SEND_REMOTE_EVENT = "com.miui.security-center.action.SEND_REMOTE_EVENT"

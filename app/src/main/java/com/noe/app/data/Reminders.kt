package com.noe.app.data

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.noe.app.MainActivity
import com.noe.app.R

object Reminders {
    const val CHANNEL_ID = "noe_reminders"
    const val EXTRA_NOTE_ID = "noe_note_id"

    private const val DAY_MS = 24L * 60L * 60L * 1000L
    private const val SNOOZE_MS = 60L * 60L * 1000L

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.channel_reminders),
                    NotificationManager.IMPORTANCE_HIGH,
                )
            )
        }
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    /** The next time this note's reminder should fire, or null if it never will again. */
    fun nextTrigger(note: Note, now: Long = System.currentTimeMillis()): Long? {
        val at = note.reminderAt ?: return null
        if (at > now) return at
        val period = when (note.repeat) {
            Repeat.Daily -> DAY_MS
            Repeat.Weekly -> 7L * DAY_MS
            Repeat.None -> return null
        }
        return at + ((now - at) / period + 1L) * period
    }

    private fun alarmIntent(context: Context, noteId: String, snooze: Boolean = false): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .putExtra(EXTRA_NOTE_ID, noteId)
        val code = (if (snooze) "snooze:$noteId" else noteId).hashCode()
        return PendingIntent.getBroadcast(
            context,
            code,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** Schedules (or cancels) the reminder to match the note's current state. */
    fun sync(context: Context, note: Note) {
        val at = nextTrigger(note)
        if (at != null && !note.trashed) {
            val alarms = context.getSystemService(AlarmManager::class.java)
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, alarmIntent(context, note.id))
        } else {
            cancel(context, note.id)
        }
    }

    fun cancel(context: Context, noteId: String) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        alarms.cancel(alarmIntent(context, noteId))
        alarms.cancel(alarmIntent(context, noteId, snooze = true))
    }

    /** Fires the notification again in one hour, without changing the note. */
    fun snooze(context: Context, noteId: String) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        alarms.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + SNOOZE_MS,
            alarmIntent(context, noteId, snooze = true),
        )
    }

    fun cancelNotification(context: Context, noteId: String) {
        context.getSystemService(NotificationManager::class.java).cancel(noteId.hashCode())
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getStringExtra(Reminders.EXTRA_NOTE_ID) ?: return
        val note = NoteStore(context).load().firstOrNull { it.id == noteId } ?: return
        if (note.trashed) return

        // Repeating reminders line up their next alarm right away.
        if (note.repeat != Repeat.None) Reminders.sync(context, note)

        if (!Reminders.canNotify(context)) return
        Reminders.ensureChannel(context)

        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(Reminders.EXTRA_NOTE_ID, note.id)
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            note.id.hashCode(),
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val snoozeIntent = PendingIntent.getBroadcast(
            context,
            "snooze_action:${note.id}".hashCode(),
            Intent(context, SnoozeReceiver::class.java).putExtra(Reminders.EXTRA_NOTE_ID, note.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val text = note.contentText().ifBlank { "Tap to open" }
        val notification = NotificationCompat.Builder(context, Reminders.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_noe)
            .setContentTitle(note.title.ifBlank { "Noe reminder" })
            .setContentText(text.lineSequence().firstOrNull().orEmpty())
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent)
            .addAction(R.drawable.ic_stat_noe, "Snooze 1 hour", snoozeIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(note.id.hashCode(), notification)
    }
}

class SnoozeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getStringExtra(Reminders.EXTRA_NOTE_ID) ?: return
        Reminders.cancelNotification(context, noteId)
        Reminders.snooze(context, noteId)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        NoteStore(context).load().forEach { Reminders.sync(context, it) }
    }
}

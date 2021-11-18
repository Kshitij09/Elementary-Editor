package com.kshitijpatil.elementaryeditor.util

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.WorkManager
import com.kshitijpatil.elementaryeditor.R
import java.util.*

/**
 * Create the notification and required channel (O+) for running work in a foreground service.
 */
fun createEditNotification(
    context: Context,
    workRequestId: UUID,
    notificationTitle: String
): Notification {
    val channelId = context.getString(R.string.notification_channel_id)
    val cancelText = context.getString(R.string.label_cancel_processing)
    val name = context.getString(R.string.notification_channel_name)
    // This PendingIntent can be used to cancel the Worker.
    val cancelIntent = WorkManager.getInstance(context).createCancelPendingIntent(workRequestId)

    val builder = NotificationCompat.Builder(context, channelId)
        .setContentTitle(notificationTitle)
        .setTicker(notificationTitle)
        .setSmallIcon(R.drawable.ic_work_in_progress)
        .setOngoing(true)
        .addAction(android.R.drawable.ic_delete, cancelText, cancelIntent)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        createNotificationChannel(context, channelId, name).also {
            builder.setChannelId(it.id)
        }
    }
    return builder.build()
}

/**
 * Create the required notification channel for O+ devices.
 */
@TargetApi(Build.VERSION_CODES.O)
fun createNotificationChannel(
    context: Context,
    channelId: String,
    name: String,
    notificationImportance: Int = NotificationManager.IMPORTANCE_HIGH
): NotificationChannel {
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    return NotificationChannel(
        channelId, name, notificationImportance
    ).also { channel ->
        notificationManager.createNotificationChannel(channel)
    }
}
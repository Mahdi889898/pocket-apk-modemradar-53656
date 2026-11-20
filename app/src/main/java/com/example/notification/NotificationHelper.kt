package com.example.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.ListingEntity

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "new_modem_listings_channel"
        private const val NOTIFICATION_ID_NEW_LISTINGS = 1001
        private const val NOTIFICATION_ID_TEST = 1002
    }

    fun ensureChannelCreated() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_channel_desc)
                enableVibration(true)
                enableLights(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun notifyNewListings(newListings: List<ListingEntity>) {
        if (newListings.isEmpty() || !hasNotificationPermission()) return
        ensureChannelCreated()

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_radar)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)

        if (newListings.size == 1) {
            val item = newListings.first()
            val body = if (item.priceText.isNotBlank()) {
                "${item.title}\nقیمت: ${item.priceText}"
            } else {
                item.title
            }
            builder
                .setContentTitle(context.getString(R.string.notif_single_title))
                .setContentText(item.title)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))

            if (item.listingUrl.startsWith("https://")) {
                val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(item.listingUrl))
                val viewPendingIntent = PendingIntent.getActivity(
                    context,
                    1,
                    viewIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(
                    R.drawable.ic_notification_radar,
                    context.getString(R.string.action_open_listing),
                    viewPendingIntent
                )
            }
        } else {
            val title = context.getString(R.string.notif_multi_title, newListings.size)
            val inboxStyle = NotificationCompat.InboxStyle().setBigContentTitle(title)
            newListings.take(5).forEach { item ->
                val line = if (item.priceText.isNotBlank()) {
                    "${item.title} (${item.priceText})"
                } else {
                    item.title
                }
                inboxStyle.addLine(line)
            }
            if (newListings.size > 5) {
                inboxStyle.setSummaryText("+${newListings.size - 5} آگهی جدید دیگر")
            }
            builder
                .setContentTitle(title)
                .setContentText(newListings.first().title)
                .setStyle(inboxStyle)
                .setNumber(newListings.size)
        }

        try {
            NotificationManagerCompat.from(context).notify(
                NOTIFICATION_ID_NEW_LISTINGS,
                builder.build()
            )
        } catch (_: SecurityException) {
            // Permission revoked concurrently
        }
    }

    fun sendTestNotification(): Boolean {
        if (!hasNotificationPermission()) return false
        ensureChannelCreated()

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            2,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_radar)
            .setContentTitle(context.getString(R.string.notif_test_title))
            .setContentText(context.getString(R.string.notif_test_body))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(context.getString(R.string.notif_test_body))
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_TEST, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}

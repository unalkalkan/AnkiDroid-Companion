package com.anki.notifications

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat


class Notifications {
    companion object Factory {
        fun create(): Notifications = Notifications()
    }

    fun showNotification(context: Context, card: CardInfo?, deckName: String, isSilent: Boolean) {
        Log.i("Notifications", "showNotification called")
        val builder: NotificationCompat.Builder
        val collapsedView = RemoteViews(context.packageName, R.layout.notification_collapsed)

        // There is a card to show, show a notification with the expanded view.
        if (card != null) {
            collapsedView.setTextViewText(R.id.textViewCollapsedHeader, card.q)
            collapsedView.setTextViewText(R.id.textViewCollapsedTitle, "Anki • $deckName")

            val expandedView = RemoteViews(context.packageName, R.layout.notification_expanded_full)
            expandedView.setTextViewText(R.id.textViewExpandedHeader, card.q)
            expandedView.setTextViewText(R.id.textViewContent, card.a)

            expandedView.setOnClickPendingIntent(R.id.button1, createIntent(context,"ACTION_BUTTON_1"))
            expandedView.setOnClickPendingIntent(R.id.button2, createIntent(context,"ACTION_BUTTON_2"))
            expandedView.setOnClickPendingIntent(R.id.button3, createIntent(context,"ACTION_BUTTON_3"))
            expandedView.setOnClickPendingIntent(R.id.button4, createIntent(context,"ACTION_BUTTON_4"))

            builder = NotificationCompat.Builder(context, "channel_id")
                .setSmallIcon(R.drawable.ic_notification)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(collapsedView)
                .setCustomBigContentView(expandedView)
                .setContentTitle("Anki • $deckName")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSilent(isSilent)
                .setOngoing(true)
        } else {
            collapsedView.setTextViewText(R.id.textViewCollapsedHeader, "Congrats! You've finished the deck!")
            collapsedView.setTextViewText(R.id.textViewCollapsedTitle, "Anki")

            val expandedView = RemoteViews(context.packageName, R.layout.notification_expanded_empty)
            expandedView.setTextViewText(R.id.textViewEmptyExpandedHeader, "Congrats! You've finished the deck!")
            expandedView.setTextViewText(R.id.textViewEmptyExpandedContent, "New notifications will arrive when it's time to study!")

            builder = NotificationCompat.Builder(context, "channel_id")
                .setSmallIcon(R.drawable.ic_notification)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(collapsedView)
                .setCustomBigContentView(expandedView)
                .setContentTitle("Anki")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSilent(isSilent)
                .setOngoing(false)
        }

        val notification: Notification = builder.build()

        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.cancel(1) // Cancel the current notification before sending a new one
        notificationManager.notify(1, notification)
    }

    private fun createIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, NotificationReceiver::class.java)
        intent.action = action
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }
}
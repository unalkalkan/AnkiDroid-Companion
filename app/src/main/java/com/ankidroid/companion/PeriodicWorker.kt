package com.ankidroid.companion

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters

class PeriodicWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    private val mainThreadHandler = Handler(Looper.getMainLooper())

    override fun doWork(): Result {
        Log.i("BackgroundService", "Periodic worker RAN - QQ")
        mainThreadHandler.post {
            // Update UI or perform any operation on the main thread
            Log.i("BackgroundService", "Periodic worker RAN - QA")
            checkNotifications()
        }
        return Result.success()
    }

    private fun checkNotifications() {
        Log.i("BackgroundService", "Periodic worker RAN - QB")
        var mAnkiDroid = AnkiDroidHelper(applicationContext)
        Log.i("BackgroundService", "Periodic worker RAN - 1")
        val localState = mAnkiDroid.storedState
        Log.i("BackgroundService", "Periodic worker RAN - 2")

        if (localState == null) {
            Log.i("BackgroundService", "Periodic worker - Local state is null")
            return
        }

        Log.i("BackgroundService", "Periodic worker - localState.noteID: ${localState.noteID}, localState.cardOrd: ${localState.cardOrd}")
        // This is not an empty card, don't do anything.
        if (localState.cardOrd != -1 || localState.noteID != (-1).toLong()) {
            return
        }

        Log.i("BackgroundService", "LocalState card was empty, trying to get a new card")
        // No card found on local state found, try to get the next scheduled card.
        val nextCard = mAnkiDroid.queryCurrentScheduledCard(localState.deckId)
        if (nextCard != null) {
            Log.i("BackgroundService", "next card is not null, trying to send a notification.")
            // a scheduled card is found, show it.
            mAnkiDroid.storeState(localState.deckId, nextCard)
            Notifications.create().showNotification(applicationContext, nextCard, mAnkiDroid.currentDeckName, false)
        }
        else
        {
            Log.i("BackgroundService", "next card is NULL")
        }
    }
}

package com.ankidroid.companion

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i("Notifications", "onReceive called")
        if (context == null)
            return
        Log.i("Notifications", "onReceive called - context is not null")
        when (intent?.action) {
            "ACTION_BUTTON_1" -> respondCard(context, AnkiDroidHelper.EASE_1)
            "ACTION_BUTTON_2" -> respondCard(context, AnkiDroidHelper.EASE_2)
            "ACTION_BUTTON_3" -> respondCard(context, AnkiDroidHelper.EASE_3)
            "ACTION_BUTTON_4" -> respondCard(context, AnkiDroidHelper.EASE_4)
        }
    }

    private fun respondCard(context: Context, ease: Int) {
        Log.i("Notifications", "respondCard called")
        var mAnkiDroid = AnkiDroidHelper(context)
        val localState = mAnkiDroid.storedState

        if (localState != null) {
            Log.i("Notifications", "localState.cardOrd: ${localState.cardOrd}, localState.noteID: ${localState.noteID}")
            mAnkiDroid.reviewCard(localState.noteID, localState.cardOrd, localState.cardStartTime, ease)
        }

        // Move to next card
        val nextCard = mAnkiDroid.queryCurrentScheduledCard(localState.deckId)
        if (nextCard != null) {
            Log.i("Notifications", "moving to next card.")
            mAnkiDroid.storeState(localState.deckId, nextCard)
            Notifications.create().showNotification(context, nextCard, mAnkiDroid.currentDeckName, true)
        } else {
            Log.i("Notifications", "no other cards found, showing done notification")
            // No more cards to show.
            val emptyCard = CardInfo()
            emptyCard.cardOrd = -1
            emptyCard.noteID = -1
            mAnkiDroid.storeState(localState.deckId, emptyCard)
            Notifications.create().showNotification(context, null, "", true)
        }
    }
}
package com.ankidroid.companion

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.ichi2.anki.api.AddContentApi
import java.util.concurrent.TimeUnit


class MainActivity : ComponentActivity() {
    private lateinit var mAnkiDroid:AnkiDroidHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_layout)
        createNotificationChannel()
        setup()
    }

    private fun createNotificationChannel() {
        val name = "AnkiNotificationChannel"
        val descriptionText = "Channel for anki notifications"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel("channel_id", name, importance).apply {
            description = descriptionText
        }

        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun startPeriodicWorker() {
        Log.i("BackgroundService", "startBackgroundService called from MainActivity")
        val periodicWorkRequest = PeriodicWorkRequest.Builder(
            PeriodicWorker::class.java,
            8, TimeUnit.HOURS
        ).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "WORKER_ANKI",
            ExistingPeriodicWorkPolicy.REPLACE,  // Use REPLACE to ensure only one instance is scheduled
            periodicWorkRequest
        )
    }

    private fun setup() {
        // Api is not available, either AnkiDroid is not installed or API is disabled.
        if (!AnkiDroidHelper.isApiAvailable(this)) {
            explainError("API is not available!\n" +
                    "This means either AnkiDroid is not installed or API is disabled from the AnkiDroid app")
        } else {
            mAnkiDroid = AnkiDroidHelper(this)
            if (mAnkiDroid.shouldRequestPermission()) {
                explainError("AnkiDroid Read Write permission is not granted, please make sure that it is given!")
                // requestPermissionLauncher.launch(AddContentApi.READ_WRITE_PERMISSION)
                mAnkiDroid.requestPermission(this, 0)
            } else {
                startApp()
            }
        }
    }

    private fun startApp() {
        val text = findViewById<TextView>(R.id.mainTextView)
        text.text = "Select a deck:"
        text.visibility = View.VISIBLE

        setDecksSpinner()

        val button = findViewById<Button>(R.id.mainRefreshButton)
        button.visibility = View.VISIBLE
        button.setOnClickListener{
            onClickRefresh()
        }
    }

    private fun explainError(errorText:String) {
        val text = findViewById<TextView>(R.id.mainTextView)
        text.text = errorText
        text.visibility = View.VISIBLE
        findViewById<Spinner>(R.id.spinner1).visibility = View.GONE
        findViewById<Button>(R.id.mainRefreshButton).visibility = View.GONE
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        for ((index, _) in permissions.withIndex()) {
            val permission = permissions[index]
            val grantResult = grantResults[index]
            if (permission == AddContentApi.READ_WRITE_PERMISSION) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    startApp()
                } else {
                    explainError("AnkiDroid Read Write permission is not granted, please make sure that it is given!")
                }
            }
        }
    }

    private fun setDecksSpinner() {
        val items = mutableListOf<String>()
        var startIndex = 0
        var lastDeckId:Long = -1
        val deckList = mAnkiDroid.api.deckList

        val localState = mAnkiDroid.storedState
        if (localState != null) {
            lastDeckId = localState.deckId
        }

        var count = 0
        if (deckList != null) {
            for (item in deckList) {
                items.add(item.value)
                if (item.key == lastDeckId) {
                    startIndex = count
                }
                count++
            }
        }

        val dropdown = findViewById<Spinner>(R.id.spinner1)
        dropdown.visibility = View.VISIBLE
        val adapter: ArrayAdapter<String> =
            ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, items)
        dropdown.adapter = adapter
        dropdown.setSelection(startIndex)
    }

    private fun onClickRefresh() {
        val decksDropdown = findViewById<Spinner>(R.id.spinner1)
        val deckName = decksDropdown.selectedItem.toString()
        val deckID = mAnkiDroid.findDeckIdByName(deckName)
        mAnkiDroid.storeDeckReference(deckName, deckID)

        val card = mAnkiDroid.queryCurrentScheduledCard(deckID)
        if (card != null) {
            mAnkiDroid.storeState(deckID, card)
            Notifications.create().showNotification(this, card, deckName, false)
        } else {
            // No cards to show.
            val emptyCard = CardInfo()
            emptyCard.cardOrd = -1
            emptyCard.noteID = -1
            mAnkiDroid.storeState(deckID, emptyCard)
            Notifications.create().showNotification(this, null, "", false)
        }

        // Start the periodic worker when the first card is assigned.
        startPeriodicWorker()
    }
}

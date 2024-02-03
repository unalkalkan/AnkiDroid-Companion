package com.anki.notifications;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.ichi2.anki.FlashCardsContract;
import com.ichi2.anki.api.AddContentApi;
import static com.ichi2.anki.api.AddContentApi.READ_WRITE_PERMISSION;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AnkiDroidHelper {
    public static final int EASE_1 = 1;
    public static final int EASE_2 = 2;
    public static final int EASE_3 = 3;
    public static final int EASE_4 = 4;

    public static final String[] SIMPLE_CARD_PROJECTION = {
            FlashCardsContract.Card.ANSWER_PURE,
            FlashCardsContract.Card.QUESTION_SIMPLE};
    private static final String DECK_REF_DB = "com.ichi2.anki.api.decks";
    private static final String STATE_DB = "com.ichi2.anki.api.state";

    private static final String KEY_CURRENT_STATE = "CURRENT_STATE";

    private AddContentApi mApi;
    private Context mContext;
    private final Handler uiHandler = new Handler();

    public AnkiDroidHelper(Context context) {
        Log.i("BackgroundService", "AnkiDroidHelper constructor - 1");
        mContext = context.getApplicationContext();
        Log.i("BackgroundService", "AnkiDroidHelper constructor - 2");
        mApi = new AddContentApi(mContext);
        Log.i("BackgroundService", "AnkiDroidHelper constructor - 3");
    }

    public AddContentApi getApi() {
        return mApi;
    }

    /**
     * Whether or not the API is available to use.
     * The API could be unavailable if AnkiDroid is not installed or the user explicitly disabled the API
     * @return true if the API is available to use
     */
    public static boolean isApiAvailable(Context context) {
        return AddContentApi.getAnkiDroidPackageName(context) != null;
    }

    /**
     * Whether or not we should request full access to the AnkiDroid API
     */
    public boolean shouldRequestPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false;
        }
        return ContextCompat.checkSelfPermission(mContext, READ_WRITE_PERMISSION) != PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request permission from the user to access the AnkiDroid API (for SDK 23+)
     * @param callbackActivity An Activity which implements onRequestPermissionsResult()
     * @param callbackCode The callback code to be used in onRequestPermissionsResult()
     */
    public void requestPermission(Activity callbackActivity, int callbackCode) {
        ActivityCompat.requestPermissions(callbackActivity, new String[]{READ_WRITE_PERMISSION}, callbackCode);
    }

    public boolean isPermissionGranted() {
        return ContextCompat.checkSelfPermission(mContext, READ_WRITE_PERMISSION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Save a mapping from deckName to getDeckId in the SharedPreferences
     */
    public void storeDeckReference(String deckName, long deckId) {
        final SharedPreferences decksDb = mContext.getSharedPreferences(DECK_REF_DB, Context.MODE_PRIVATE);
        decksDb.edit().putLong(deckName, deckId).apply();
    }

    /**
     * Try to find the given deck by name, accounting for potential renaming of the deck by the user as follows:
     * If there's a deck with deckName then return it's ID
     * If there's no deck with deckName, but a ref to deckName is stored in SharedPreferences, and that deck exist in
     * AnkiDroid (i.e. it was renamed), then use that deck.Note: this deck will not be found if your app is re-installed
     * If there's no reference to deckName anywhere then return null
     * @param deckName the name of the deck to find
     * @return the did of the deck in Anki
     */
    public Long findDeckIdByName(String deckName) {
        SharedPreferences decksDb = mContext.getSharedPreferences(DECK_REF_DB, Context.MODE_PRIVATE);
        // Look for deckName in the deck list
        Long did = getDeckId(deckName);
        if (did != null) {
            // If the deck was found then return it's id
            return did;
        } else {
            // Otherwise try to check if we have a reference to a deck that was renamed and return that
            did = decksDb.getLong(deckName, -1);
            if (did != -1 && mApi.getDeckName(did) != null) {
                return did;
            } else {
                // If the deck really doesn't exist then return null
                return null;
            }
        }
    }

    public String getCurrentDeckName() {
        StoredState state = getStoredState();
        return mApi.getDeckName(state.deckId);
    }

    /**
     * Get the ID of the deck which matches the name
     * @param deckName Exact name of deck (note: deck names are unique in Anki)
     * @return the ID of the deck that has given name, or null if no deck was found or API error
     */
    private Long getDeckId(String deckName) {
        Map<Long, String> deckList = mApi.getDeckList();
        if (deckList != null) {
            for (Map.Entry<Long, String> entry : deckList.entrySet()) {
                if (entry.getValue().equalsIgnoreCase(deckName)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    public void storeState(long deckId, CardInfo card) {
        final SharedPreferences cardsDb = mContext.getSharedPreferences(STATE_DB, Context.MODE_PRIVATE);

        Map<String, Object> message = new HashMap<>();
        message.put("deck_id", deckId);
        message.put("note_id", card.noteID);
        message.put("card_ord", card.cardOrd);
        message.put("start_time", card.cardStartTime);
        JSONObject js = new JSONObject(message);

        cardsDb.edit().putString(KEY_CURRENT_STATE, js.toString()).apply();
    }

    public StoredState getStoredState() {
        final SharedPreferences cardsDb = mContext.getSharedPreferences(STATE_DB, Context.MODE_PRIVATE);
        String message = cardsDb.getString(KEY_CURRENT_STATE, "");

        // No state found in local
        if (message == "") {
            return null;
        }

        JSONObject json;
        try {
            json = new JSONObject(message);
            StoredState state = new StoredState();
            state.deckId = json.getLong("deck_id");
            state.noteID = json.getLong("note_id");
            state.cardOrd = json.getInt("card_ord");
            state.cardStartTime = json.getLong("start_time");
            return state;
        } catch (JSONException e) {
            // Log.e(TAG, "JSONException " + e);
            e.printStackTrace();
        }
        return null;
    }

    @SuppressLint("Range,DirectSystemCurrentTimeMillisUsage")
    public CardInfo queryCurrentScheduledCard(long deckID) {
        // Log.d(TAG, "QueryForCurrentCard");
        String[] deckArguments = new String[deckID == -1 ? 1 : 2];
        String deckSelector = "limit=?";
        deckArguments[0] = "" + 1;
        if (deckID != -1) {
            deckSelector += ",deckID=?";
            deckArguments[1] = "" + deckID;
        }

        // This call requires com.ichi2.anki.permission.READ_WRITE_DATABASE to be granted by user as it is
        // marked as "dangerous" by ankidroid app. This permission has been asked before. Would crash if
        // not granted, so checking
        if (!isPermissionGranted()) {
            uiHandler.post(() -> Toast.makeText(mContext,
                    R.string.permission_not_granted,
                    Toast.LENGTH_SHORT).show());
        } else {
            // permission has been granted, normal case

            Cursor reviewInfoCursor =
                    mContext.getContentResolver().query(FlashCardsContract.ReviewInfo.CONTENT_URI, null, deckSelector, deckArguments, null);

            if (reviewInfoCursor == null || !reviewInfoCursor.moveToFirst()) {
                // Log.d(TAG, "query for due card info returned no result");
                if (reviewInfoCursor != null) {
                    reviewInfoCursor.close();
                }
            } else {
                ArrayList<CardInfo> cards = new ArrayList<>();

                // Walk through the cursor to get the responses.
                do {
                    CardInfo card = new CardInfo();

                    card.cardOrd = reviewInfoCursor.getInt(reviewInfoCursor.getColumnIndex(FlashCardsContract.ReviewInfo.CARD_ORD));
                    card.noteID = reviewInfoCursor.getLong(reviewInfoCursor.getColumnIndex(FlashCardsContract.ReviewInfo.NOTE_ID));
                    card.buttonCount = reviewInfoCursor.getInt(reviewInfoCursor.getColumnIndex(FlashCardsContract.ReviewInfo.BUTTON_COUNT));

                    try {
                        card.fileNames = new JSONArray(reviewInfoCursor.getString(reviewInfoCursor.getColumnIndex(FlashCardsContract.ReviewInfo.MEDIA_FILES)));
                        card.nextReviewTexts = new JSONArray(reviewInfoCursor.getString(reviewInfoCursor.getColumnIndex(FlashCardsContract.ReviewInfo.NEXT_REVIEW_TIMES)));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    card.cardStartTime = System.currentTimeMillis();
                    // Log.v(TAG, "card added to queue: " + card.fileNames);
                    cards.add(card);
                } while (reviewInfoCursor.moveToNext());

                reviewInfoCursor.close();

                if (cards.size() >= 1) {
                    for (CardInfo card : cards) {
                        Uri noteUri = Uri.withAppendedPath(FlashCardsContract.Note.CONTENT_URI, Long.toString(card.noteID));
                        Uri cardsUri = Uri.withAppendedPath(noteUri, "cards");
                        Uri specificCardUri = Uri.withAppendedPath(cardsUri, Integer.toString(card.cardOrd));
                        final Cursor specificCardCursor = mContext.getContentResolver().query(specificCardUri,
                                SIMPLE_CARD_PROJECTION,  // projection
                                null,  // selection is ignored for this URI
                                null,  // selectionArgs is ignored for this URI
                                null   // sortOrder is ignored for this URI
                        );

                        if (specificCardCursor == null || !specificCardCursor.moveToFirst()) {
                            // Log.d(TAG, "query for due card info returned no result");
                            if (specificCardCursor != null) {
                                specificCardCursor.close();
                            }
                            return null;
                        } else {
                            card.a = specificCardCursor.getString(specificCardCursor.getColumnIndex(FlashCardsContract.Card.ANSWER_PURE));
                            card.q = specificCardCursor.getString(specificCardCursor.getColumnIndex(FlashCardsContract.Card.QUESTION_SIMPLE));
                            specificCardCursor.close();
                        }
                    }
                    return cards.get(0);
                }
            }
        }
        return null;
    }

    public void reviewCard(long noteID, int cardOrd, long cardStartTime, int ease) {
        long timeTaken = System.currentTimeMillis() - cardStartTime;
        ContentResolver cr = mContext.getContentResolver();
        Uri reviewInfoUri = FlashCardsContract.ReviewInfo.CONTENT_URI;
        ContentValues values = new ContentValues();
        values.put(FlashCardsContract.ReviewInfo.NOTE_ID, noteID);
        values.put(FlashCardsContract.ReviewInfo.CARD_ORD, cardOrd);
        values.put(FlashCardsContract.ReviewInfo.EASE, ease);
        values.put(FlashCardsContract.ReviewInfo.TIME_TAKEN, timeTaken);
        // Log.d(TAG, timeTaken + " time taken " + values.getAsLong(FlashCardsContract.ReviewInfo.TIME_TAKEN));
        cr.update(reviewInfoUri, values, null, null);
    }
}


package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.FeedFilterMode
import com.example.model.FeedSortMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "luma_session")

class SessionPreferences(private val context: Context) {

    private object Keys {
        val CURRENT_USER_ID = stringPreferencesKey("current_user_id")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val FEED_SORT = stringPreferencesKey("feed_sort")
        val FEED_FILTER = stringPreferencesKey("feed_filter")
    }

    val currentUserId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.CURRENT_USER_ID]?.takeIf { it.isNotBlank() }
    }

    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.DARK_MODE] ?: true
    }

    val feedSortMode: Flow<FeedSortMode> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.FEED_SORT]
        FeedSortMode.entries.firstOrNull { it.name == raw } ?: FeedSortMode.CHRONOLOGICAL
    }

    val feedFilterMode: Flow<FeedFilterMode> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.FEED_FILTER]
        FeedFilterMode.entries.firstOrNull { it.name == raw } ?: FeedFilterMode.FOLLOWING
    }

    suspend fun setCurrentUserId(userId: String?) {
        context.dataStore.edit { prefs ->
            if (userId == null) prefs.remove(Keys.CURRENT_USER_ID)
            else prefs[Keys.CURRENT_USER_ID] = userId
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DARK_MODE] = enabled
        }
    }

    suspend fun setFeedSortMode(mode: FeedSortMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.FEED_SORT] = mode.name
        }
    }

    suspend fun setFeedFilterMode(mode: FeedFilterMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.FEED_FILTER] = mode.name
        }
    }
}

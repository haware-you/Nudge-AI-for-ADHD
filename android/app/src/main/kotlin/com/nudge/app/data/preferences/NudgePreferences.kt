package com.nudge.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nudge.app.data.remote.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "nudge_prefs")

/**
 * Local-first mirror of the `profiles` row (supabase/migrations/0001_schema.sql), so toggles
 * apply to the UI instantly. Writes are pushed to Supabase best-effort inline rather than
 * through a WorkManager queue — acceptable because a dropped settings sync just means the
 * server default applies until the next successful write, not a data-loss risk like a memo.
 */
class NudgePreferences(private val context: Context) {
    private object Keys {
        val RETAIN_TRANSCRIPT = booleanPreferencesKey("retain_transcript")
        val LOW_BATTERY_MODE = booleanPreferencesKey("low_battery_mode")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val NOTIFICATION_HOUR = intPreferencesKey("notification_hour")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val LAST_OPENED_AT = longPreferencesKey("last_opened_at")
    }

    val onboardingComplete: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.ONBOARDING_COMPLETE] ?: false }

    /** Local-only, never synced — onboarding is a per-device first-run gate, not account state. */
    suspend fun setOnboardingComplete(value: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETE] = value }
    }

    /** Epoch millis of the previous app open, or null on first run. Drives the welcome-back screen. */
    val lastOpenedAt: Flow<Long?> = context.dataStore.data.map { it[Keys.LAST_OPENED_AT] }

    suspend fun setLastOpenedAt(value: Long) {
        context.dataStore.edit { it[Keys.LAST_OPENED_AT] = value }
    }

    /** Part of account deletion: forget every local setting. */
    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }

    /** Off by default: the calm palette is the product's look; wallpaper colors are opt-in. */
    val dynamicColor: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.DYNAMIC_COLOR] ?: false }

    /** Local-only, never synced — a per-device display choice, not account state. */
    suspend fun setDynamicColor(value: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = value }
    }

    val retainTranscript: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.RETAIN_TRANSCRIPT] ?: true }

    val lowBatteryMode: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.LOW_BATTERY_MODE] ?: false }

    val notificationsEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.NOTIFICATIONS_ENABLED] ?: false }

    suspend fun setRetainTranscript(value: Boolean) {
        context.dataStore.edit { it[Keys.RETAIN_TRANSCRIPT] = value }
        syncProfile("retain_transcript" to value)
    }

    suspend fun setLowBatteryMode(value: Boolean) {
        context.dataStore.edit { it[Keys.LOW_BATTERY_MODE] = value }
        syncProfile("low_battery_mode" to value)
    }

    /** Max one push/day, off by default (PRD M4) — this only flips the flag; scheduling the
     * actual single daily notification is a follow-up (see CLAUDE.md status notes). */
    suspend fun setNotificationsEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = value }
        syncProfile("notifications_enabled" to value)
    }

    private suspend fun syncProfile(vararg fields: Pair<String, Any>) {
        runCatching {
            SupabaseClientProvider.client.postgrest.from("profiles").update(fields.toMap()) {
                filter { eq("id", requireUserId()) }
            }
        }
    }

    private fun requireUserId(): String =
        SupabaseClientProvider.client.auth.currentUserOrNull()?.id
            ?: error("syncProfile called with no signed-in user")
}

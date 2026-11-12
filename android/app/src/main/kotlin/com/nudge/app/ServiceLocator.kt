package com.nudge.app

import android.content.Context
import com.nudge.app.data.local.NudgeDatabase
import com.nudge.app.data.preferences.NudgePreferences
import com.nudge.app.data.remote.SupabaseClientProvider
import com.nudge.app.data.repository.CaptureRepository
import com.nudge.app.data.repository.TaskRepository
import com.nudge.app.widget.NextActionWidget
import androidx.glance.appwidget.updateAll
import androidx.work.WorkManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.functions.functions
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manual dependency wiring, deliberately not a DI framework (Hilt/Koin) — this app has few
 * enough dependencies that constructor injection by hand stays readable, and it avoids an
 * annotation-processor dependency this scaffold can't verify compiles anyway.
 */
object ServiceLocator {
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val database: NudgeDatabase by lazy { NudgeDatabase.getInstance(appContext) }
    val preferences: NudgePreferences by lazy { NudgePreferences(appContext) }
    val taskRepository: TaskRepository by lazy { TaskRepository(database.taskDao()) }
    val captureRepository: CaptureRepository by lazy {
        CaptureRepository(appContext, database.memoDao())
    }

    /** Re-render the home-screen widget after a setting it depends on has changed. */
    suspend fun refreshWidget() {
        NextActionWidget().updateAll(appContext)
    }

    /**
     * PRD §8 "delete-account". Order matters: the server purge (supabase/functions/
     * delete-account) runs first and throws on failure, so local data is only wiped once the
     * account is really gone. Afterwards the app is back to a fresh anonymous install.
     */
    suspend fun deleteAccount() {
        SupabaseClientProvider.client.functions.invoke("delete-account")
        WorkManager.getInstance(appContext).cancelAllWork()
        withContext(Dispatchers.IO) {
            database.clearAllTables()
            File(appContext.filesDir, "memos").deleteRecursively()
        }
        preferences.clearAll()
        runCatching { SupabaseClientProvider.client.auth.signOut() }
        runCatching { ensureSignedIn() }
        refreshWidget()
    }

    /**
     * Anonymous-first per PRD §7 ("Auth: anonymous -> email upgrade"). Called once from
     * NudgeApplication.onCreate; safe to call repeatedly since it no-ops when already signed in.
     */
    suspend fun ensureSignedIn() {
        if (SupabaseClientProvider.client.auth.currentUserOrNull() == null) {
            SupabaseClientProvider.client.auth.signInAnonymously()
        }
    }
}

package com.nudge.app

import android.content.Context
import com.nudge.app.data.local.NudgeDatabase
import com.nudge.app.data.preferences.NudgePreferences
import com.nudge.app.data.remote.SupabaseClientProvider
import com.nudge.app.data.repository.CaptureRepository
import com.nudge.app.data.repository.ReviewRepository
import com.nudge.app.data.repository.TaskRepository
import io.github.jan.supabase.auth.auth

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
    val reviewRepository: ReviewRepository by lazy {
        ReviewRepository(database.noteDao(), database.dedupMergeDao())
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

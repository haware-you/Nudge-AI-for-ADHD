package com.nudge.app.data.remote

import com.nudge.app.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

/**
 * NOTE: written without a live Gradle sync against the Google Maven repo (unavailable in the
 * sandbox this was scaffolded in — see README "Status"). supabase-kt's exact package paths
 * (`io.github.jan.supabase.auth` etc.) and plugin install syntax have shifted across major
 * versions; double-check this against whatever supabase-kt version's docs are current when
 * you first sync this project; the shape below matches the 3.x generation as of this writing.
 */
object SupabaseClientProvider {
    val client by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
        ) {
            install(Auth)
            install(Postgrest)
            install(Storage)
            install(Functions)
        }
    }
}

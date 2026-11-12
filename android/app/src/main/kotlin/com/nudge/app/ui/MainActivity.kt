package com.nudge.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import com.nudge.app.R
import com.nudge.app.ServiceLocator
import com.nudge.app.capture.CaptureActivity
import com.nudge.app.onboarding.OnboardingScreen
import com.nudge.app.review.ReviewScreen
import com.nudge.app.review.ReviewViewModel
import com.nudge.app.settings.SettingsScreen
import com.nudge.app.settings.SettingsViewModel
import com.nudge.app.ui.theme.NudgeTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private enum class Tab { REVIEW, SETTINGS }

/**
 * The in-app experience is secondary to the widget (PRD §6) — this is deliberately just a
 * two-tab shell (Review, Settings) plus a one-time onboarding gate, not a full nav graph.
 */
class MainActivity : ComponentActivity() {
    private val reviewViewModel: ReviewViewModel by viewModels { ReviewViewModel.Factory() }
    private val settingsViewModel: SettingsViewModel by viewModels { SettingsViewModel.Factory() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val preferences = ServiceLocator.preferences
            val dynamicColor by preferences.dynamicColor.collectAsState(initial = false)
            NudgeTheme(dynamicColor = dynamicColor) {
                var showOnboarding by remember { mutableStateOf<Boolean?>(null) }

                LaunchedEffect(Unit) {
                    showOnboarding = !preferences.onboardingComplete.first()
                }

                when (showOnboarding) {
                    null -> Unit // brief flash of nothing while the one DataStore read resolves
                    true -> OnboardingScreen(
                        onTryCapture = {
                            lifecycleScope.launch { preferences.setOnboardingComplete(true) }
                            startActivity(Intent(this@MainActivity, CaptureActivity::class.java))
                            showOnboarding = false
                        },
                        onSkip = {
                            lifecycleScope.launch { preferences.setOnboardingComplete(true) }
                            showOnboarding = false
                        },
                    )
                    false -> MainScaffold(reviewViewModel, settingsViewModel)
                }
            }
        }
    }
}

@Composable
private fun MainScaffold(reviewViewModel: ReviewViewModel, settingsViewModel: SettingsViewModel) {
    var tab by remember { mutableStateOf(Tab.REVIEW) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == Tab.REVIEW,
                    onClick = { tab = Tab.REVIEW },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                    label = { Text(stringResource(R.string.review_title)) },
                )
                NavigationBarItem(
                    selected = tab == Tab.SETTINGS,
                    onClick = { tab = Tab.SETTINGS },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.settings_title)) },
                )
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (tab) {
                Tab.REVIEW -> ReviewScreen(reviewViewModel)
                Tab.SETTINGS -> SettingsScreen(settingsViewModel)
            }
        }
    }
}

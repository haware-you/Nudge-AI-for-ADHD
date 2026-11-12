package com.nudge.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.work.WorkManager
import com.nudge.app.R
import com.nudge.app.ServiceLocator
import com.nudge.app.capture.CaptureActivity
import com.nudge.app.data.local.entities.TaskEntity
import com.nudge.app.data.repository.TaskRepository
import com.nudge.app.onboarding.OnboardingScreen
import com.nudge.app.review.ReviewScreen
import com.nudge.app.review.ReviewViewModel
import com.nudge.app.settings.SettingsScreen
import com.nudge.app.settings.SettingsViewModel
import com.nudge.app.sync.TaskActionSyncScheduler
import com.nudge.app.ui.components.BottomBarItem
import com.nudge.app.ui.components.NudgeBottomBar
import com.nudge.app.ui.theme.NudgeTheme
import com.nudge.app.welcome.WelcomeBackScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private enum class Tab { REVIEW, SETTINGS }

private enum class Gate { ONBOARDING, WELCOME_BACK, MAIN }

// PRD tracks "return-after-7-day-absence"; the same threshold triggers the welcome-back screen.
private const val ABSENCE_THRESHOLD_MILLIS = 7L * 24 * 60 * 60 * 1000

/**
 * The in-app experience is secondary to the widget (PRD §6) — this is deliberately just a
 * two-tab shell (Review, Settings) behind a one-time onboarding gate and the
 * return-after-absence screen, not a full nav graph.
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
                var gate by remember { mutableStateOf<Gate?>(null) }
                var welcomeTask by remember { mutableStateOf<TaskEntity?>(null) }

                LaunchedEffect(Unit) {
                    val now = System.currentTimeMillis()
                    val lastOpenedAt = preferences.lastOpenedAt.first()
                    preferences.setLastOpenedAt(now)
                    gate = when {
                        !preferences.onboardingComplete.first() -> Gate.ONBOARDING
                        lastOpenedAt != null && now - lastOpenedAt >= ABSENCE_THRESHOLD_MILLIS -> {
                            // One snapshot, same pick the widget would show.
                            welcomeTask = ServiceLocator.taskRepository
                                .observeNextAction(preferences.lowBatteryMode.first())
                                .first()
                            Gate.WELCOME_BACK
                        }
                        else -> Gate.MAIN
                    }
                }

                // Screens outside the Scaffold need the themed background too, otherwise the
                // placeholder window background shows through.
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    when (gate) {
                        null -> Unit // brief flash of nothing while the DataStore reads resolve
                        Gate.ONBOARDING -> OnboardingScreen(
                            onTryCapture = {
                                lifecycleScope.launch { preferences.setOnboardingComplete(true) }
                                startCapture()
                                gate = Gate.MAIN
                            },
                            onSkip = {
                                lifecycleScope.launch { preferences.setOnboardingComplete(true) }
                                gate = Gate.MAIN
                            },
                        )
                        Gate.WELCOME_BACK -> WelcomeBackScreen(
                            taskText = welcomeTask?.text,
                            onDone = {
                                welcomeTask?.let { task -> applyTaskAction { markDone(task) } }
                                gate = Gate.MAIN
                            },
                            onNotToday = {
                                welcomeTask?.let { task -> applyTaskAction { markSkipped(task) } }
                                gate = Gate.MAIN
                            },
                            onCapture = {
                                startCapture()
                                gate = Gate.MAIN
                            },
                            onContinue = { gate = Gate.MAIN },
                        )
                        Gate.MAIN -> MainScaffold(reviewViewModel, settingsViewModel, onCapture = ::startCapture)
                    }
                }
            }
        }
    }

    private fun startCapture() {
        startActivity(Intent(this, CaptureActivity::class.java))
    }

    /** Same three steps the widget callbacks take: apply locally, queue the sync, redraw the widget. */
    private fun applyTaskAction(action: suspend TaskRepository.() -> Unit) {
        lifecycleScope.launch {
            ServiceLocator.taskRepository.action()
            TaskActionSyncScheduler.enqueue(WorkManager.getInstance(this@MainActivity))
            ServiceLocator.refreshWidget()
        }
    }
}

@Composable
private fun MainScaffold(reviewViewModel: ReviewViewModel, settingsViewModel: SettingsViewModel, onCapture: () -> Unit) {
    var tab by remember { mutableStateOf(Tab.REVIEW) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NudgeBottomBar(
                listOf(
                    BottomBarItem(
                        label = stringResource(R.string.review_title),
                        icon = Icons.AutoMirrored.Filled.List,
                        selected = tab == Tab.REVIEW,
                        onClick = { tab = Tab.REVIEW },
                    ),
                    BottomBarItem(
                        label = stringResource(R.string.settings_title),
                        icon = Icons.Filled.Settings,
                        selected = tab == Tab.SETTINGS,
                        onClick = { tab = Tab.SETTINGS },
                    ),
                ),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (tab) {
                Tab.REVIEW -> ReviewScreen(reviewViewModel, onCapture = onCapture)
                Tab.SETTINGS -> SettingsScreen(settingsViewModel)
            }
        }
    }
}

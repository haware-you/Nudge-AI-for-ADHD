package com.nudge.app.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import com.nudge.app.R
import com.nudge.app.ServiceLocator
import com.nudge.app.capture.CaptureActivity
import com.nudge.app.data.AbsenceGate
import com.nudge.app.onboarding.OnboardingScreen
import com.nudge.app.onboarding.WidgetPromptScreen
import com.nudge.app.returning.WelcomeBackScreen
import com.nudge.app.returning.WelcomeBackViewModel
import com.nudge.app.review.ReviewScreen
import com.nudge.app.review.ReviewViewModel
import com.nudge.app.settings.SettingsScreen
import com.nudge.app.settings.SettingsViewModel
import com.nudge.app.ui.theme.NudgeTheme
import com.nudge.app.widget.requestPinNextActionWidget
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private enum class Tab { REVIEW, SETTINGS }

/** Where the top-level Compose tree is, prior to [AppStep.MAIN]. [LOADING] is the sentinel that
 * gates the one-time derivation in [MainActivity]'s LaunchedEffect — see its comment. */
private enum class AppStep { LOADING, ONBOARDING_WELCOME, ONBOARDING_WIDGET_PROMPT, WELCOME_BACK, MAIN }

/**
 * The in-app experience is secondary to the widget (PRD §6) — this is deliberately just a
 * two-tab shell (Review, Settings) plus a one-time onboarding gate, not a full nav graph.
 *
 * Ahead of the tab shell sit two more gates from the design doc's flows: Flow A's post-sample-
 * capture widget-pin prompt, and Flow E's Welcome Back screen for a user returning after a
 * >=7-day absence (PRD Open Decision 1 / Northstar-adjacent).
 */
class MainActivity : ComponentActivity() {
    private val reviewViewModel: ReviewViewModel by viewModels { ReviewViewModel.Factory() }
    private val settingsViewModel: SettingsViewModel by viewModels { SettingsViewModel.Factory() }
    private val welcomeBackViewModel: WelcomeBackViewModel by viewModels { WelcomeBackViewModel.Factory() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NudgeTheme {
                val context = LocalContext.current
                val preferences = ServiceLocator.preferences
                // rememberSaveable (not plain remember): a rotation must not re-run the
                // derivation below and land back on ONBOARDING_WIDGET_PROMPT or WELCOME_BACK's
                // one-time recordAppOpened() write a second time (see LaunchedEffect comment).
                var step by rememberSaveable { mutableStateOf(AppStep.LOADING) }

                val captureForResult = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult(),
                ) { result ->
                    lifecycleScope.launch { preferences.setOnboardingComplete(true) }
                    lifecycleScope.launch { preferences.recordAppOpened() }
                    // RESULT_OK means mic permission was granted and recording actually started
                    // (see CaptureActivity); denied/backed-out falls straight through to the
                    // main shell rather than dangling on a widget prompt with nothing to show
                    // for it — "user can still browse the app" per the design's denied branch.
                    step = if (result.resultCode == Activity.RESULT_OK) {
                        AppStep.ONBOARDING_WIDGET_PROMPT
                    } else {
                        AppStep.MAIN
                    }
                }

                LaunchedEffect(Unit) {
                    // Guard: only derive from scratch once per real cold start of this Activity.
                    // A restored `step` (rotation, or process recreation with saved state) means
                    // this has already run — re-running would call the state-mutating
                    // recordAppOpened() again and could silently clobber WELCOME_BACK/
                    // ONBOARDING_WIDGET_PROMPT back to MAIN mid-display.
                    if (step != AppStep.LOADING) return@LaunchedEffect

                    step = if (!preferences.onboardingComplete.first()) {
                        AppStep.ONBOARDING_WELCOME
                    } else {
                        val previousOpenedAt = preferences.recordAppOpened()
                        if (AbsenceGate.shouldShowWelcomeBack(previousOpenedAt)) {
                            AppStep.WELCOME_BACK
                        } else {
                            AppStep.MAIN
                        }
                    }
                }

                when (step) {
                    AppStep.LOADING -> Unit // brief flash of nothing while the DataStore read resolves
                    AppStep.ONBOARDING_WELCOME -> OnboardingScreen(
                        onTryCapture = {
                            captureForResult.launch(Intent(context, CaptureActivity::class.java))
                        },
                        onSkip = {
                            lifecycleScope.launch { preferences.setOnboardingComplete(true) }
                            lifecycleScope.launch { preferences.recordAppOpened() }
                            step = AppStep.MAIN
                        },
                    )
                    AppStep.ONBOARDING_WIDGET_PROMPT -> WidgetPromptScreen(
                        onAddWidget = {
                            requestPinNextActionWidget(context)
                            step = AppStep.MAIN
                        },
                        onContinue = { step = AppStep.MAIN },
                    )
                    AppStep.WELCOME_BACK -> WelcomeBackScreen(
                        viewModel = welcomeBackViewModel,
                        onDone = { step = AppStep.MAIN },
                    )
                    AppStep.MAIN -> MainScaffold(reviewViewModel, settingsViewModel)
                }
            }
        }
    }
}

@Composable
private fun MainScaffold(reviewViewModel: ReviewViewModel, settingsViewModel: SettingsViewModel) {
    var tab by remember { mutableStateOf(Tab.REVIEW) }
    val context = LocalContext.current

    Scaffold(
        // The design doc's Flow Overview lists "in-app capture button (<=2 taps from any
        // state)" as a standing entry point alongside the widget — this was the only screen in
        // the app with no path to CaptureActivity at all.
        floatingActionButton = {
            FloatingActionButton(onClick = { context.startActivity(Intent(context, CaptureActivity::class.java)) }) {
                Icon(Icons.Filled.Mic, contentDescription = stringResource(R.string.widget_capture_cta))
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == Tab.REVIEW,
                    onClick = { tab = Tab.REVIEW },
                    icon = { Icon(Icons.Filled.List, contentDescription = null) },
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

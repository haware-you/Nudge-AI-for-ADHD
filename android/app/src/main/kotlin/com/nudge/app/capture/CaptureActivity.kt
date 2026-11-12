package com.nudge.app.capture

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.nudge.app.R
import com.nudge.app.ServiceLocator
import com.nudge.app.ui.components.CircleActionButton
import com.nudge.app.ui.components.rememberConfirmHaptic
import com.nudge.app.ui.components.MetaLabel
import com.nudge.app.ui.theme.NudgeMotion
import com.nudge.app.ui.theme.NudgeTheme
import com.nudge.app.ui.theme.Sizing
import com.nudge.app.ui.theme.Spacing
import kotlinx.coroutines.flow.collectLatest

/**
 * Launched directly from the widget tap and from MainActivity's capture entry point. Kept as a
 * thin overlay (see Theme.Nudge.Capture) rather than the full app chrome, per M1's <1.5s
 * tap-to-recording target — there's nothing to load here beyond this screen.
 */
class CaptureActivity : ComponentActivity() {
    private val viewModel: CaptureViewModel by viewModels {
        CaptureViewModel.Factory(application)
    }

    private val requestMicPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.startRecording() else viewModel.onPermissionDenied()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)

        setContent {
            val dynamicColor by ServiceLocator.preferences.dynamicColor.collectAsState(initial = false)
            NudgeTheme(dynamicColor = dynamicColor) {
                val state by viewModel.uiState.collectAsState()
                LaunchedEffect(Unit) {
                    viewModel.captureFinished.collectLatest { finish() }
                }
                CaptureScreen(
                    state = state,
                    onStop = { viewModel.stopRecording() },
                    onOpenSettings = { openAppSettings() },
                )
            }
        }
    }

    private fun openAppSettings() {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)),
        )
    }
}

@Composable
private fun CaptureScreen(
    state: CaptureUiState,
    onStop: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    // Idle is transient (not started yet, or just finished): render nothing at all rather than
    // an empty sheet.
    if (state == CaptureUiState.Idle) return

    // The sheet rises from the bottom edge on a critically damped spring and the page dims
    // behind it (a modal task). Recording has already started by now, so the motion never
    // delays capture. The activity finishes itself when done, so only the entrance is animated.
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val confirmHaptic = rememberConfirmHaptic()

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = shown, enter = fadeIn(NudgeMotion.settle()), exit = fadeOut(NudgeMotion.settle())) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = NudgeMotion.SCRIM_ALPHA)),
            )
        }
        AnimatedVisibility(
            visible = shown,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(NudgeMotion.settle()) { it },
            exit = slideOutVertically(NudgeMotion.settle()) { it }, // leaves the way it came
        ) {
        Surface(
            modifier = Modifier.navigationBarsPadding().padding(Spacing.md).fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
        ) {
            Column(
                modifier = Modifier.padding(Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                when (state) {
                    is CaptureUiState.Recording -> {
                        MetaLabel(stringResource(R.string.capture_recording))
                        // Elapsed time is plain feedback that the mic is live; digits, not copy.
                        Text(
                            "%d:%02d".format(state.elapsedSeconds / 60, state.elapsedSeconds % 60),
                            style = MaterialTheme.typography.displayMedium,
                        )
                        if (state.showCutoffWarning) {
                            Text(
                                stringResource(R.string.capture_time_limit_warning),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        CircleActionButton(
                            icon = Icons.Filled.Stop,
                            contentDescription = stringResource(R.string.capture_tap_to_stop),
                            onClick = { confirmHaptic(); onStop() },
                            size = Sizing.stopButton,
                        )
                        Text(
                            stringResource(R.string.capture_tap_to_stop),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    CaptureUiState.Processing -> {
                        Text(stringResource(R.string.capture_processing), style = MaterialTheme.typography.titleMedium)
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                    CaptureUiState.PermissionDenied -> {
                        Text(
                            stringResource(R.string.capture_permission_denied_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            stringResource(R.string.capture_permission_denied_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Button(onClick = onOpenSettings, modifier = Modifier.heightIn(min = Sizing.minTouchTarget)) {
                            Text(stringResource(R.string.capture_permission_denied_action))
                        }
                    }
                    CaptureUiState.Idle -> Unit // handled by the early return above
                }
            }
        }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F3F1, heightDp = 360)
@Composable
private fun CaptureRecordingPreview() {
    NudgeTheme { CaptureScreen(CaptureUiState.Recording(42, showCutoffWarning = false), onStop = {}, onOpenSettings = {}) }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F3F1, heightDp = 200)
@Composable
private fun CaptureProcessingPreview() {
    NudgeTheme { CaptureScreen(CaptureUiState.Processing, onStop = {}, onOpenSettings = {}) }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F3F1, heightDp = 320)
@Composable
private fun CapturePermissionDeniedPreview() {
    NudgeTheme { CaptureScreen(CaptureUiState.PermissionDenied, onStop = {}, onOpenSettings = {}) }
}

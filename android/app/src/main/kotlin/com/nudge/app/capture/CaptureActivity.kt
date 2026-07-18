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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nudge.app.R
import com.nudge.app.ui.theme.NudgeTheme
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
        if (granted) {
            // Lets a caller launching this via startActivityForResult (onboarding's sample
            // capture, Flow A) tell "recording was attempted" apart from "permission denied,
            // user backed out" — RESULT_CANCELED stays the default for the latter.
            setResult(RESULT_OK)
            viewModel.startRecording()
        } else {
            viewModel.onPermissionDenied()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)

        setContent {
            NudgeTheme {
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
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Surface(
            modifier = Modifier.padding(24.dp),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (state) {
                    is CaptureUiState.Recording -> {
                        Text(stringResource(R.string.capture_recording), style = MaterialTheme.typography.titleMedium)
                        if (state.showCutoffWarning) {
                            Text(
                                stringResource(R.string.capture_time_limit_warning),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Button(onClick = onStop) { Text(stringResource(R.string.capture_tap_to_stop)) }
                    }
                    CaptureUiState.Processing -> {
                        Text(stringResource(R.string.capture_processing), style = MaterialTheme.typography.titleMedium)
                    }
                    CaptureUiState.PermissionDenied -> {
                        Text(
                            stringResource(R.string.capture_permission_denied_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            stringResource(R.string.capture_permission_denied_body),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Button(onClick = onOpenSettings) {
                            Text(stringResource(R.string.capture_permission_denied_action))
                        }
                    }
                    CaptureUiState.Idle -> {
                        // Transient: either not started yet or just finished; nothing to render.
                    }
                }
            }
        }
    }
}

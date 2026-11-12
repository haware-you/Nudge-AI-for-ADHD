package com.nudge.app.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.nudge.app.R
import com.nudge.app.ui.components.Overline
import com.nudge.app.ui.theme.NudgeTheme
import com.nudge.app.ui.theme.Sizing
import com.nudge.app.ui.theme.Spacing

// PRD §6: "Zero mandatory onboarding beyond mic permission + one sample capture; account
// creation deferrable." This screen is the entirety of onboarding — no multi-step carousel,
// no forced signup.
@Composable
fun OnboardingScreen(onTryCapture: () -> Unit, onSkip: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // The placeholder brand mark: one dot, for "one next action".
        Box(modifier = Modifier.size(Sizing.stopButton).background(MaterialTheme.colorScheme.primary, CircleShape))
        Overline(stringResource(R.string.app_name), modifier = Modifier.padding(top = Spacing.xl))
        Text(
            stringResource(R.string.onboarding_title),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Spacing.xs),
        )
        Text(
            stringResource(R.string.onboarding_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Spacing.lg, bottom = Spacing.xxl),
        )
        Button(onClick = onTryCapture, modifier = Modifier.heightIn(min = Sizing.minTouchTarget)) {
            Text(stringResource(R.string.onboarding_mic_cta))
        }
        TextButton(onClick = onSkip, modifier = Modifier.padding(top = Spacing.sm)) {
            Text(stringResource(R.string.onboarding_skip))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF1EFE8, heightDp = 640)
@Composable
private fun OnboardingPreview() {
    NudgeTheme { OnboardingScreen(onTryCapture = {}, onSkip = {}) }
}

package com.nudge.app.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.nudge.app.R
import com.nudge.app.ui.theme.Spacing

// PRD §6: "Zero mandatory onboarding beyond mic permission + one sample capture; account
// creation deferrable." This screen is the entirety of onboarding — no multi-step carousel,
// no forced signup.
@Composable
fun OnboardingScreen(onTryCapture: () -> Unit, onSkip: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(R.string.onboarding_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.onboarding_body),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Spacing.lg, bottom = Spacing.xxl),
        )
        Button(onClick = onTryCapture) { Text(stringResource(R.string.onboarding_mic_cta)) }
        TextButton(onClick = onSkip, modifier = Modifier.padding(top = Spacing.sm)) {
            Text(stringResource(R.string.onboarding_skip))
        }
    }
}

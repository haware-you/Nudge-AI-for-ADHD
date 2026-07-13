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
import androidx.compose.ui.unit.dp
import com.nudge.app.R

// PRD §6: "Zero mandatory onboarding beyond mic permission + one sample capture; account
// creation deferrable." This screen is the entirety of onboarding — no multi-step carousel,
// no forced signup.
@Composable
fun OnboardingScreen(onTryCapture: () -> Unit, onSkip: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.onboarding_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.onboarding_body),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 16.dp, bottom = 32.dp),
        )
        Button(onClick = onTryCapture) { Text(stringResource(R.string.onboarding_mic_cta)) }
        TextButton(onClick = onSkip, modifier = Modifier.padding(top = 8.dp)) {
            Text(stringResource(R.string.onboarding_skip))
        }
    }
}

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

/**
 * Flow A's last onboarding step: "Extracted tasks shown -> prompt to add the widget (system
 * widget picker) -> Activated - widget live." Only reached when the sample capture actually got
 * as far as recording (mic permission granted) — see MainActivity's onboarding step machine.
 */
@Composable
fun WidgetPromptScreen(onAddWidget: () -> Unit, onContinue: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.onboarding_widget_prompt_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.onboarding_widget_prompt_body),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 16.dp, bottom = 32.dp),
        )
        Button(onClick = onAddWidget) { Text(stringResource(R.string.onboarding_widget_prompt_cta)) }
        TextButton(onClick = onContinue, modifier = Modifier.padding(top = 8.dp)) {
            Text(stringResource(R.string.onboarding_widget_prompt_skip))
        }
    }
}

package com.nudge.app.welcome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.nudge.app.R
import com.nudge.app.ui.components.NudgeCard
import com.nudge.app.ui.theme.NudgeTheme
import com.nudge.app.ui.theme.Sizing
import com.nudge.app.ui.theme.Spacing

/**
 * Return-after-absence (PRD story "Return after absence", M4). OPEN DECISION 1: this screen is
 * the unvalidated retention bet, so treat its trigger and content as provisional.
 *
 * Shame-free checks: it never says how long the person was away, never shows a count or a
 * backlog, and shows at most one task. [taskText] is null when there is nothing waiting.
 */
@Composable
fun WelcomeBackScreen(
    taskText: String?,
    onDone: () -> Unit,
    onNotToday: () -> Unit,
    onCapture: () -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(Spacing.xl),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.welcome_back_title), style = MaterialTheme.typography.headlineLarge)

        if (taskText != null) {
            Text(
                stringResource(R.string.welcome_back_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.sm, bottom = Spacing.xl),
            )
            NudgeCard {
                Text(taskText, style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.padding(top = Spacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(onClick = onDone, modifier = Modifier.heightIn(min = Sizing.minTouchTarget)) {
                        Text(stringResource(R.string.widget_action_done))
                    }
                    TextButton(onClick = onNotToday, modifier = Modifier.heightIn(min = Sizing.minTouchTarget)) {
                        Text(stringResource(R.string.widget_action_not_today))
                    }
                }
            }
        } else {
            Text(
                stringResource(R.string.widget_empty_state_title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.sm, bottom = Spacing.xl),
            )
            Button(onClick = onCapture, modifier = Modifier.heightIn(min = Sizing.minTouchTarget)) {
                Text(stringResource(R.string.capture_cta))
            }
        }

        TextButton(onClick = onContinue, modifier = Modifier.padding(top = Spacing.lg)) {
            Text(stringResource(R.string.welcome_back_continue))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F3F1, heightDp = 640)
@Composable
private fun WelcomeBackPreview() {
    NudgeTheme {
        WelcomeBackScreen("Email the landlord about the tap", onDone = {}, onNotToday = {}, onCapture = {}, onContinue = {})
    }
}

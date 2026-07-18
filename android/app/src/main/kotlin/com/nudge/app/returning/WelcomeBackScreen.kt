package com.nudge.app.returning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nudge.app.R
import com.nudge.app.data.local.entities.TaskEntity

/**
 * Flow E — shown instead of MainActivity's normal shell when [com.nudge.app.data.AbsenceGate]
 * decides the user has been away >= 7 days. The whole point (PRD Open Decision 1) is that this
 * replaces a backlog, not adds one: one task, two calm outcomes, nothing marked overdue.
 */
@Composable
fun WelcomeBackScreen(viewModel: WelcomeBackViewModel, onDone: () -> Unit) {
    val topTask by viewModel.topTask.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.welcome_back_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.welcome_back_body),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 12.dp, bottom = 28.dp),
        )

        val task = topTask
        if (task != null) {
            TaskCard(task)
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            ) { Text(stringResource(R.string.welcome_back_accept_cta)) }
            TextButton(
                onClick = { viewModel.clearOldStuff(onDone) },
                modifier = Modifier.padding(top = 4.dp),
            ) { Text(stringResource(R.string.welcome_back_clear_cta)) }
        } else {
            // Nothing was parked during the absence — still a calm arrival, not a dead end.
            Text(stringResource(R.string.widget_empty_state_title), style = MaterialTheme.typography.titleMedium)
            Button(onClick = onDone, modifier = Modifier.padding(top = 20.dp)) {
                Text(stringResource(R.string.welcome_back_continue_cta))
            }
        }
    }
}

@Composable
private fun TaskCard(task: TaskEntity) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(task.text, style = MaterialTheme.typography.titleMedium)
        }
    }
}

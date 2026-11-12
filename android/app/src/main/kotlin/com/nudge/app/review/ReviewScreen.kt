package com.nudge.app.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.nudge.app.R
import com.nudge.app.ui.theme.Spacing
import com.nudge.app.data.local.entities.TaskEntity

@Composable
fun ReviewScreen(viewModel: ReviewViewModel) {
    val tasks by viewModel.tasks.collectAsState()

    Box(modifier = Modifier.fillMaxSize().padding(Spacing.lg)) {
        if (tasks.isEmpty()) {
            Text(stringResource(R.string.review_empty), modifier = Modifier.align(Alignment.Center))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                items(tasks, key = { it.id }) { task ->
                    ReviewRow(
                        task = task,
                        onDelete = { viewModel.delete(task.id) },
                        onConfirm = { viewModel.confirm(task) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewRow(task: TaskEntity, onDelete: () -> Unit, onConfirm: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(task.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.review_delete_action))
                }
            }
            Text(
                stringResource(R.string.review_source_label),
                style = MaterialTheme.typography.bodyMedium, // PRD §6: 16sp floor, so not bodySmall
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (task.lowConfidence) {
                OutlinedButton(onClick = onConfirm, modifier = Modifier.padding(top = Spacing.sm)) {
                    Text(stringResource(R.string.review_confirm_action))
                }
            }
        }
    }
}

package com.nudge.app.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.nudge.app.R
import com.nudge.app.data.local.entities.Effort
import com.nudge.app.data.local.entities.TaskEntity
import com.nudge.app.data.local.entities.TaskStatus
import com.nudge.app.ui.components.CircleActionButton
import com.nudge.app.ui.components.NudgeCard
import com.nudge.app.ui.components.Overline
import com.nudge.app.ui.components.ScreenHeader
import com.nudge.app.ui.theme.NudgeTheme
import com.nudge.app.ui.theme.Spacing
import kotlinx.coroutines.launch

/**
 * M5: the only screen where a list exists. Shame-free checks for this screen: no counts, no
 * progress, no ages or "skipped" marks on rows — a row is just what was heard and where from.
 */
@Composable
fun ReviewScreen(viewModel: ReviewViewModel, onCapture: () -> Unit) {
    val tasks by viewModel.tasks.collectAsState()
    val hasStuckMemo by viewModel.hasStuckMemo.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val removedMessage = stringResource(R.string.review_deleted)
    val undoLabel = stringResource(R.string.review_undo)

    Box(modifier = Modifier.fillMaxSize()) {
        ReviewContent(
            tasks = tasks,
            hasStuckMemo = hasStuckMemo,
            onCapture = onCapture,
            onConfirm = viewModel::confirm,
            onDelete = { task ->
                viewModel.delete(task)
                scope.launch {
                    // `finally` so the server delete still happens if this screen goes away
                    // while the snackbar is up.
                    var undone = false
                    try {
                        val result = snackbarHostState.showSnackbar(
                            message = removedMessage,
                            actionLabel = undoLabel,
                            duration = SnackbarDuration.Short,
                        )
                        undone = result == SnackbarResult.ActionPerformed
                    } finally {
                        if (undone) viewModel.undoDelete(task) else viewModel.commitDelete(task.id)
                    }
                }
            },
        )
        SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun ReviewContent(
    tasks: List<TaskEntity>,
    hasStuckMemo: Boolean,
    onCapture: () -> Unit,
    onConfirm: (TaskEntity) -> Unit,
    onDelete: (TaskEntity) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.lg)) {
        ScreenHeader(overline = stringResource(R.string.app_name), title = stringResource(R.string.review_title)) {
            CircleActionButton(Icons.Filled.Mic, stringResource(R.string.capture_cta), onClick = onCapture)
        }
        if (tasks.isEmpty() && !hasStuckMemo) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.review_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                contentPadding = PaddingValues(bottom = Spacing.lg),
            ) {
                if (hasStuckMemo) {
                    item(key = "stuck-notice") {
                        NudgeCard {
                            Text(
                                stringResource(R.string.capture_stuck_notice),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                items(tasks, key = { it.id }) { task ->
                    ReviewRow(task = task, onDelete = { onDelete(task) }, onConfirm = { onConfirm(task) })
                }
            }
        }
    }
}

@Composable
private fun ReviewRow(task: TaskEntity, onDelete: () -> Unit, onConfirm: () -> Unit) {
    NudgeCard {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                if (task.lowConfidence) {
                    // Low confidence is marked in words, not only by the outlined circle.
                    Overline(stringResource(R.string.review_confirm_action), color = MaterialTheme.colorScheme.primary)
                }
                Text(task.text, style = MaterialTheme.typography.titleMedium)
                // M5 traceability: the verbatim words from the memo this task came from.
                task.sourceSpan?.takeIf { it.isNotBlank() }?.let { span ->
                    Overline(stringResource(R.string.review_source_label), modifier = Modifier.padding(top = Spacing.sm))
                    Text(
                        stringResource(R.string.review_source_quote, span),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (task.mergeCount > 0) {
                    Overline(stringResource(R.string.review_merged_from_label), modifier = Modifier.padding(top = Spacing.sm))
                }
            }
            if (task.lowConfidence) {
                CircleActionButton(
                    Icons.Filled.Check,
                    stringResource(R.string.review_confirm_action),
                    onClick = onConfirm,
                    filled = false,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.review_delete_action),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun previewTask(id: String, text: String, span: String?, lowConfidence: Boolean = false, mergeCount: Int = 0) =
    TaskEntity(
        id = id, sourceMemoId = null, text = text, sourceSpan = span, effort = Effort.LOW, confidence = 1f,
        lowConfidence = lowConfidence, status = TaskStatus.OPEN, skipCount = 0, priorityScore = 0.0,
        createdAt = 0, updatedAt = 0, mergeCount = mergeCount,
    )

@Preview(showBackground = true, backgroundColor = 0xFFF1EFE8, heightDp = 640)
@Composable
private fun ReviewPreview() {
    NudgeTheme {
        ReviewContent(
            tasks = listOf(
                previewTask("1", "Email the landlord about the tap", "oh and the tap, I need to email him"),
                previewTask("2", "Sort the thing for Friday", "that thing for Friday", lowConfidence = true),
                previewTask("3", "Book a dentist appointment", "dentist, again", mergeCount = 1),
            ),
            hasStuckMemo = true,
            onCapture = {}, onConfirm = {}, onDelete = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF1EFE8, heightDp = 400)
@Composable
private fun ReviewEmptyPreview() {
    NudgeTheme {
        ReviewContent(tasks = emptyList(), hasStuckMemo = false, onCapture = {}, onConfirm = {}, onDelete = {})
    }
}

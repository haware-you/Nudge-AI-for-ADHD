package com.nudge.app.review

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.nudge.app.R
import com.nudge.app.data.local.entities.Effort
import com.nudge.app.data.local.entities.NoteEntity

@Composable
fun ReviewScreen(viewModel: ReviewViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    val notes by viewModel.notes.collectAsState()

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (tasks.isEmpty() && notes.isEmpty()) {
            Text(stringResource(R.string.review_empty), modifier = Modifier.align(Alignment.Center))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tasks, key = { it.task.id }) { reviewTask ->
                    ReviewRow(
                        reviewTask = reviewTask,
                        onDelete = { viewModel.delete(reviewTask.task.id) },
                        onConfirm = { viewModel.confirm(reviewTask.task) },
                    )
                }
                items(notes, key = { it.id }) { note -> NoteRow(note) }
            }
        }
    }
}

@Composable
private fun ReviewRow(reviewTask: ReviewTask, onDelete: () -> Unit, onConfirm: () -> Unit) {
    val task = reviewTask.task
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(task.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.review_delete_action))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Badge(stringResource(task.effort.labelRes()))
                // M2: "merge visible in review history" — closes the gap flagged in CLAUDE.md
                // where dedup_merges existed server-side with nothing on the client reading it.
                if (reviewTask.mergedCount > 0) {
                    Badge(
                        pluralStringResource(
                            R.plurals.review_merged_from_count,
                            reviewTask.mergedCount,
                            reviewTask.mergedCount,
                        ),
                    )
                }
            }
            Text(
                stringResource(R.string.review_source_label),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 6.dp),
            )
            if (task.lowConfidence) {
                OutlinedButton(onClick = onConfirm, modifier = Modifier.padding(top = 8.dp)) {
                    Text(stringResource(R.string.review_low_confidence_badge))
                }
            }
        }
    }
}

/** Venting/journaling the pipeline kept as a note rather than inventing a task from (M2 edge
 * case) — dashed border and italic body distinguish it at a glance from an actionable task. */
@Composable
private fun NoteRow(note: NoteEntity) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.review_note_prefix),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                note.text,
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun Badge(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun Effort.labelRes(): Int = when (this) {
    Effort.LOW -> R.string.task_effort_low
    Effort.MEDIUM -> R.string.task_effort_medium
    Effort.HIGH -> R.string.task_effort_high
}

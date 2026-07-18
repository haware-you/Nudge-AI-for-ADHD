package com.nudge.app.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nudge.app.ServiceLocator
import com.nudge.app.data.local.entities.NoteEntity
import com.nudge.app.data.local.entities.TaskEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A task paired with how many memos were merged into it, for the "merged from N memos" badge
 * (M2: "near-duplicates are merged, with merge visible in review history"). */
data class ReviewTask(val task: TaskEntity, val mergedCount: Int)

/**
 * M5: "the only screen where a list exists." Exists for trust and eval data, not as a
 * management surface — so this stays deliberately thin (no sorting/filtering/bulk actions).
 *
 * Surfaces two read-only supporting lists alongside the editable task list: dedup-merge counts
 * (was previously flagged in CLAUDE.md as server-only data nothing on the client read) and notes
 * (venting/journaling the pipeline declined to turn into a task — M2 edge case).
 */
class ReviewViewModel : ViewModel() {
    private val taskRepository = ServiceLocator.taskRepository
    private val reviewRepository = ServiceLocator.reviewRepository

    val tasks: StateFlow<List<ReviewTask>> = combine(
        taskRepository.observeReviewList(),
        reviewRepository.observeMergeCounts(),
    ) { taskList, mergeCounts ->
        taskList.map { ReviewTask(it, mergeCounts[it.id] ?: 0) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val notes: StateFlow<List<NoteEntity>> = reviewRepository.observeNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(taskId: String) {
        viewModelScope.launch { taskRepository.deleteTask(taskId) }
    }

    fun confirm(task: TaskEntity) {
        viewModelScope.launch { taskRepository.confirmLowConfidence(task) }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ReviewViewModel() as T
    }
}

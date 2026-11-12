package com.nudge.app.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nudge.app.ServiceLocator
import com.nudge.app.data.local.entities.TaskEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * M5: "the only screen where a list exists." Exists for trust and eval data, not as a
 * management surface — so this stays deliberately thin (no sorting/filtering/bulk actions).
 */
class ReviewViewModel : ViewModel() {
    private val taskRepository = ServiceLocator.taskRepository

    val tasks: StateFlow<List<TaskEntity>> = taskRepository.observeReviewList()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // PRD M1: after 3 failed uploads a memo is "stuck" and the user is told once, calmly.
    val hasStuckMemo: StateFlow<Boolean> = ServiceLocator.database.memoDao().observeStuckMemos()
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Removes the row at once; follow with [undoDelete] or [commitDelete]. */
    fun delete(task: TaskEntity) {
        viewModelScope.launch { taskRepository.deleteTaskLocally(task.id) }
    }

    fun undoDelete(task: TaskEntity) {
        viewModelScope.launch { taskRepository.restoreTask(task) }
    }

    fun commitDelete(taskId: String) {
        viewModelScope.launch { taskRepository.deleteTaskRemotely(taskId) }
    }

    fun confirm(task: TaskEntity) {
        viewModelScope.launch { taskRepository.confirmLowConfidence(task) }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ReviewViewModel() as T
    }
}

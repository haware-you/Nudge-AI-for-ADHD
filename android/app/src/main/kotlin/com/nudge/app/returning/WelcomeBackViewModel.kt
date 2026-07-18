package com.nudge.app.returning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nudge.app.ServiceLocator
import com.nudge.app.data.local.entities.TaskEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Flow E: "Two weeks away -> welcome back, one thing." Shows the same top pick the widget would
 * (never a backlog), gated by [com.nudge.app.data.AbsenceGate] in MainActivity — this ViewModel
 * only owns the screen's two possible outcomes, not the decision to show it.
 */
class WelcomeBackViewModel : ViewModel() {
    private val taskRepository = ServiceLocator.taskRepository

    // Low Battery Mode is deliberately ignored here: the return screen always offers the same
    // single top pick regardless of that toggle, matching the design's one-task welcome card.
    val topTask: StateFlow<TaskEntity?> = taskRepository.observeNextAction(lowBatteryMode = false)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** "Clear the old stuff instead" — instant, unceremonious fresh start (no confirmation
     * dialog, matching M5's delete precedent). [onCleared] runs after the local delete lands so
     * the caller can navigate on without waiting on the best-effort server round trip. */
    fun clearOldStuff(onCleared: () -> Unit) {
        viewModelScope.launch {
            taskRepository.clearAllOpenTasks()
            onCleared()
        }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = WelcomeBackViewModel() as T
    }
}

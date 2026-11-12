package com.nudge.app.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nudge.app.ServiceLocator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {
    private val preferences = ServiceLocator.preferences

    val retainTranscript: StateFlow<Boolean> =
        preferences.retainTranscript.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val lowBatteryMode: StateFlow<Boolean> =
        preferences.lowBatteryMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val notificationsEnabled: StateFlow<Boolean> =
        preferences.notificationsEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val dynamicColor: StateFlow<Boolean> =
        preferences.dynamicColor.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setDynamicColor(value: Boolean) {
        viewModelScope.launch {
            preferences.setDynamicColor(value)
            ServiceLocator.refreshWidget()
        }
    }

    /** [onResult] gets true once everything is gone, false if the server purge did not go through. */
    fun deleteAccount(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            onResult(runCatching { ServiceLocator.deleteAccount() }.isSuccess)
        }
    }

    fun setRetainTranscript(value: Boolean) {
        viewModelScope.launch { preferences.setRetainTranscript(value) }
    }

    fun setLowBatteryMode(value: Boolean) {
        viewModelScope.launch {
            preferences.setLowBatteryMode(value)
            ServiceLocator.refreshWidget() // the widget filters on this flag
        }
    }

    // Max one push/day, off by default (M4) — flips the flag; the scheduling itself is a
    // follow-up, see CLAUDE.md status notes.
    fun setNotificationsEnabled(value: Boolean) {
        viewModelScope.launch { preferences.setNotificationsEnabled(value) }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel() as T
    }
}

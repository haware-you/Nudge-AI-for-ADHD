package com.nudge.app.capture

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.nudge.app.ServiceLocator
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.UUID

// PRD M1: 5 min free / 15 min paid. Paid-tier plan isn't synced to the client anywhere yet in
// this scaffold (see CLAUDE.md status notes) — hardcoded to the free limit until it is.
private const val MAX_DURATION_SECONDS = 5 * 60
private const val CUTOFF_WARNING_AT_SECONDS = MAX_DURATION_SECONDS - 30
private const val MIN_MEANINGFUL_DURATION_SECONDS = 1

sealed interface CaptureUiState {
    data object Idle : CaptureUiState
    data class Recording(val elapsedSeconds: Int, val showCutoffWarning: Boolean) : CaptureUiState
    data object Processing : CaptureUiState
    data object PermissionDenied : CaptureUiState
}

class CaptureViewModel(private val app: Application) : AndroidViewModel(app) {
    private val captureRepository = ServiceLocator.captureRepository

    private val _uiState = MutableStateFlow<CaptureUiState>(CaptureUiState.Idle)
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    // One-shot signal distinct from uiState=Idle, which is ambiguous between "not started yet"
    // and "just finished" — the Activity collects this to know when to finish() itself.
    private val _captureFinished = Channel<Unit>(Channel.BUFFERED)
    val captureFinished = _captureFinished.receiveAsFlow()

    private var recorder: AudioRecorderController? = null
    private var currentMemoId: String? = null
    private var tickerJob: kotlinx.coroutines.Job? = null

    fun onPermissionDenied() {
        _uiState.value = CaptureUiState.PermissionDenied
    }

    fun startRecording() {
        val memoId = UUID.randomUUID().toString()
        currentMemoId = memoId
        val outputFile = captureRepository.newAudioFile(memoId)

        recorder = AudioRecorderController(app, ::onRecorderEvent).also {
            it.start(outputFile)
        }
        _uiState.value = CaptureUiState.Recording(0, showCutoffWarning = false)
        startTicker()
    }

    /** User tapped stop (tap-to-stop, per M1: "single tap starts recording; tap again to stop"). */
    fun stopRecording() = finishRecording()

    private fun onRecorderEvent(event: RecorderEvent) {
        when (event) {
            RecorderEvent.InterruptedByFocusLoss -> finishRecording(alreadyStopped = true)
        }
    }

    private fun finishRecording(alreadyStopped: Boolean = false) {
        val memoId = currentMemoId ?: return
        tickerJob?.cancel()
        val elapsed = (_uiState.value as? CaptureUiState.Recording)?.elapsedSeconds ?: 0
        if (!alreadyStopped) recorder?.stop()
        recorder = null
        currentMemoId = null

        if (elapsed < MIN_MEANINGFUL_DURATION_SECONDS) {
            viewModelScope.launch {
                captureRepository.discardMemo(memoId)
                _uiState.value = CaptureUiState.Idle
                _captureFinished.trySend(Unit)
            }
            return
        }

        _uiState.value = CaptureUiState.Processing
        viewModelScope.launch {
            val outputFile = captureRepository.newAudioFile(memoId)
            captureRepository.createPendingMemo(memoId, outputFile, elapsed)
            UploadScheduler.enqueue(WorkManager.getInstance(app), memoId)
            _uiState.value = CaptureUiState.Idle
            _captureFinished.trySend(Unit)
        }
    }

    private fun startTicker() {
        tickerJob = viewModelScope.launch {
            var seconds = 0
            while (seconds < MAX_DURATION_SECONDS) {
                delay(1_000)
                seconds += 1
                _uiState.value = CaptureUiState.Recording(
                    elapsedSeconds = seconds,
                    showCutoffWarning = seconds >= CUTOFF_WARNING_AT_SECONDS,
                )
            }
            finishRecording() // graceful cutoff at the max-length limit
        }
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CaptureViewModel(app) as T
    }
}

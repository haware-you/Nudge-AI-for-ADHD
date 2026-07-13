package com.nudge.app.capture

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Build
import java.io.File

sealed class RecorderEvent {
    /** Focus was pulled out from under us (typically an incoming call) — recording was
     * stopped and whatever was captured up to that point is finalized (PRD M1: "call
     * interruption mid-recording: partial audio saved and processed"). */
    data object InterruptedByFocusLoss : RecorderEvent()
}

/**
 * Push-to-talk only (PRD hard constraint: no wake word, no background/always-on mic). A single
 * instance is created per recording session by CaptureViewModel and discarded after stop().
 */
class AudioRecorderController(
    private val context: Context,
    private val onEvent: (RecorderEvent) -> Unit,
) {
    private var recorder: MediaRecorder? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { change ->
        if (change == AudioManager.AUDIOFOCUS_LOSS ||
            change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
        ) {
            stop()
            onEvent(RecorderEvent.InterruptedByFocusLoss)
        }
    }

    fun start(outputFile: File) {
        requestAudioFocus()

        recorder = (
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            ).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(64_000)
            setAudioSamplingRate(44_100)
            setOutputFile(outputFile.absolutePath)
            prepare()
            start()
        }
    }

    /** Idempotent: safe to call after an interruption already stopped things. */
    fun stop() {
        runCatching { recorder?.stop() }
        recorder?.release()
        recorder = null
        abandonAudioFocus()
    }

    private fun requestAudioFocus() {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener(focusChangeListener)
            .build()
        audioFocusRequest = request
        audioManager.requestAudioFocus(request)
    }

    private fun abandonAudioFocus() {
        audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        audioFocusRequest = null
    }
}

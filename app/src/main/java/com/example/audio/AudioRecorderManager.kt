package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException

class AudioRecorderManager(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentRecordFile: File? = null
    private var recordStartTime: Long = 0

    fun startRecording(): String? {
        val fileName = "REC_${System.currentTimeMillis()}.m4a"
        val outputFile = File(context.cacheDir, fileName)
        currentRecordFile = outputFile

        // Set up MediaRecorder
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile.absolutePath)

            try {
                prepare()
                start()
                recordStartTime = System.currentTimeMillis()
                Log.d("AudioRecorderManager", "Recording started: ${outputFile.absolutePath}")
            } catch (e: Exception) {
                Log.e("AudioRecorderManager", "MediaRecorder preparation or starting failed", e)
                return null
            }
        }
        return outputFile.absolutePath
    }

    /**
     * Stops the active recording.
     * Returns the duration of the recording in milliseconds, or 0 if something failed.
     */
    fun stopRecording(): Long {
        var duration: Long = 0
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            if (recordStartTime > 0) {
                duration = System.currentTimeMillis() - recordStartTime
            }
            Log.d("AudioRecorderManager", "Recording stopped. Duration: $duration ms")
        } catch (e: Exception) {
            Log.e("AudioRecorderManager", "Stopping MediaRecorder failed", e)
        } finally {
            mediaRecorder = null
            recordStartTime = 0
        }
        return duration
    }

    fun startPlayback(filePath: String, onCompletion: () -> Unit, onError: () -> Unit) {
        // Release previous player if any
        stopPlayback()

        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(filePath)
                prepare()
                start()
                setOnCompletionListener {
                    onCompletion()
                    stopPlayback()
                }
                Log.d("AudioRecorderManager", "Audio playback started: $filePath")
            } catch (e: IOException) {
                Log.e("AudioRecorderManager", "MediaPlayer preparation failed", e)
                onError()
                stopPlayback()
            }
        }
    }

    fun stopPlayback() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
    }

    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying == true
    }

    fun getCurrentPlaybackPosition(): Int {
        return mediaPlayer?.currentPosition ?: 0
    }

    fun getPlaybackDuration(): Int {
        return mediaPlayer?.duration ?: 0
    }
}

package com.goldenminute.catalyst.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

class SpeechRecognizerManager(private val context: Context) {
    private var speechRecognizer: SpeechRecognizer? = null
    private val TAG = "SpeechRecognizerManager"

    fun startListening(
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
        onListeningStateChange: (Boolean) -> Unit
    ) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition is not available on this device")
            return
        }

        // Stop any existing session
        stopListening()

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Log.d(TAG, "onReadyForSpeech")
                        onListeningStateChange(true)
                    }

                    override fun onBeginningOfSpeech() {
                        Log.d(TAG, "onBeginningOfSpeech")
                    }

                    override fun onRmsChanged(rmsdB: Float) {}

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        Log.d(TAG, "onEndOfSpeech")
                        onListeningStateChange(false)
                    }

                    override fun onError(error: Int) {
                        val message = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_CLIENT -> "Client error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissions denied"
                            SpeechRecognizer.ERROR_NETWORK -> "Network error"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice recognizer is busy"
                            SpeechRecognizer.ERROR_SERVER -> "Server error"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input detected"
                            else -> "Voice recognition failed ($error)"
                        }
                        Log.e(TAG, "onError: $message")
                        onError(message)
                        onListeningStateChange(false)
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val recognizedText = matches[0]
                            Log.d(TAG, "onResults: $recognizedText")
                            onResult(recognizedText)
                        } else {
                            onError("No text recognized")
                        }
                        onListeningStateChange(false)
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, Locale.getDefault().toString())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start speech recognizer", e)
            onError(e.localizedMessage ?: "Failed to start listening")
            onListeningStateChange(false)
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping speech recognizer", e)
        } finally {
            speechRecognizer = null
        }
    }
}

class VoiceCommandParser {
    data class ParsedCommand(
        val title: String,
        val priority: Int? = null
    )

    fun parse(text: String): ParsedCommand {
        var cleanText = text.trim()
        var detectedPriority: Int? = null

        // 1. Parse Priority (English)
        if (cleanText.contains("priority high", ignoreCase = true)) {
            detectedPriority = 2
            cleanText = cleanText.replace("priority high", "", ignoreCase = true).trim()
        } else if (cleanText.contains("priority medium", ignoreCase = true)) {
            detectedPriority = 1
            cleanText = cleanText.replace("priority medium", "", ignoreCase = true).trim()
        } else if (cleanText.contains("priority low", ignoreCase = true)) {
            detectedPriority = 0
            cleanText = cleanText.replace("priority low", "", ignoreCase = true).trim()
        }
        // Parse Priority (Persian)
        else if (cleanText.contains("اولویت بالا") || cleanText.contains("اولویت زیاد")) {
            detectedPriority = 2
            cleanText = cleanText.replace("اولویت بالا", "").replace("اولویت زیاد", "").trim()
        } else if (cleanText.contains("اولویت متوسط") || cleanText.contains("اولویت معمولی")) {
            detectedPriority = 1
            cleanText = cleanText.replace("اولویت متوسط", "").replace("اولویت معمولی", "").trim()
        } else if (cleanText.contains("اولویت پایین") || cleanText.contains("اولویت کم")) {
            detectedPriority = 0
            cleanText = cleanText.replace("اولویت پایین", "").replace("اولویت کم", "").trim()
        }

        // Clean any double spaces left behind
        cleanText = cleanText.replace("\\s+".toRegex(), " ").trim()

        // Clean trailing/leading connector words ("with", "با")
        if (cleanText.endsWith("with", ignoreCase = true)) {
            cleanText = cleanText.substring(0, cleanText.length - 4).trim()
        } else if (cleanText.endsWith("with a", ignoreCase = true)) {
            cleanText = cleanText.substring(0, cleanText.length - 6).trim()
        } else if (cleanText.endsWith("با")) {
            cleanText = cleanText.substring(0, cleanText.length - 2).trim()
        }

        val finalTitle = cleanText.ifEmpty { text }
        return ParsedCommand(
            title = finalTitle.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
            priority = detectedPriority
        )
    }
}

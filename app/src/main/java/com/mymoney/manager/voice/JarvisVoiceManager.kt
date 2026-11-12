package com.mymoney.manager.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import java.util.Locale

class JarvisVoiceManager(
    private val context: Context,
    private val onSpeechResult: (String) -> Unit,
    private val onSpeechPartial: (String) -> Unit = {},
    private val onError: (String) -> Unit = {},
    private val onListeningStateChanged: (Boolean) -> Unit = {}
) : RecognitionListener, TextToSpeech.OnInitListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    var isTtsReady = false
        private set

    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentCompletionCallback: (() -> Unit)? = null

    private val prefs = context.getSharedPreferences("jarvis_voice_prefs", Context.MODE_PRIVATE)

    companion object {
        const val PREF_PRESET = "pref_voice_preset"
        const val PREF_PITCH = "pref_voice_pitch"
        const val PREF_SPEED = "pref_voice_speed"
        const val PREF_CUSTOM_VOICE_NAME = "pref_custom_voice_name"

        val PRESET_NAMES = listOf(
            "Jarvis Deep Tech (Male)",
            "Jarvis Classic (Male)",
            "Friday Assistant (Crisp Female)",
            "British Tech (Male)",
            "Indian English (Natural)",
            "Device System Default"
        )
    }

    init {
        tts = TextToSpeech(context.applicationContext, this)
        initRecognizer()
    }

    private fun initRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(this@JarvisVoiceManager)
            }
        }
    }

    fun startListening(languageCode: String = "en-IN") {
        if (speechRecognizer == null) {
            initRecognizer()
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageCode)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            // Support mixed English, Hindi, and Gujarati recognition
            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("gu-IN", "hi-IN", "en-IN"))
        }

        try {
            speechRecognizer?.startListening(intent)
            onListeningStateChanged(true)
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Failed to start speech recognition")
            onListeningStateChanged(false)
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        onListeningStateChanged(false)
    }

    fun speak(text: String, onComplete: () -> Unit = {}) {
        if (isTtsReady && tts != null) {
            currentCompletionCallback = onComplete
            val utteranceId = "UTTERANCE_${System.currentTimeMillis()}"
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            }
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        } else {
            // TTS not available, invoke completion immediately
            onComplete()
        }
    }

    fun applyVoiceSettings(presetIndex: Int, pitch: Float, speed: Float, customVoiceName: String?) {
        prefs.edit()
            .putInt(PREF_PRESET, presetIndex)
            .putFloat(PREF_PITCH, pitch)
            .putFloat(PREF_SPEED, speed)
            .putString(PREF_CUSTOM_VOICE_NAME, customVoiceName)
            .apply()

        configureTtsVoice(presetIndex, pitch, speed, customVoiceName)
    }

    fun getSavedPreset(): Int = prefs.getInt(PREF_PRESET, 0) // Default: Jarvis Deep Tech (Male)
    fun getSavedPitch(): Float = prefs.getFloat(PREF_PITCH, 0.78f)
    fun getSavedSpeed(): Float = prefs.getFloat(PREF_SPEED, 0.95f)
    fun getSavedCustomVoiceName(): String? = prefs.getString(PREF_CUSTOM_VOICE_NAME, null)

    fun getAvailableVoices(): List<Voice> {
        return try {
            tts?.voices?.filter { voice ->
                !voice.isNetworkConnectionRequired &&
                        (voice.locale.language == "en" || voice.locale.language == "hi" || voice.locale.language == "gu")
            }?.sortedBy { it.name } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun configureTtsVoice(presetIndex: Int, pitch: Float, speed: Float, customVoiceName: String?) {
        val engine = tts ?: return

        // 1. Set Custom Specific Voice if picked
        var voiceApplied = false
        if (!customVoiceName.isNullOrBlank()) {
            val matchingVoice = engine.voices?.find { it.name == customVoiceName }
            if (matchingVoice != null) {
                engine.voice = matchingVoice
                voiceApplied = true
            }
        }

        if (!voiceApplied) {
            // Apply preset logic
            when (presetIndex) {
                0 -> { // Jarvis Deep Tech (Male)
                    findAndSetVoiceByPreference(engine, male = true, locale = Locale.ENGLISH)
                    engine.setPitch(pitch)
                    engine.setSpeechRate(speed)
                }
                1 -> { // Jarvis Classic (Male)
                    findAndSetVoiceByPreference(engine, male = true, locale = Locale.ENGLISH)
                    engine.setPitch(pitch)
                    engine.setSpeechRate(speed)
                }
                2 -> { // Friday Assistant (Crisp Female)
                    findAndSetVoiceByPreference(engine, male = false, locale = Locale.ENGLISH)
                    engine.setPitch(pitch)
                    engine.setSpeechRate(speed)
                }
                3 -> { // British Tech (Male)
                    val result = engine.setLanguage(Locale.UK)
                    if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                        findAndSetVoiceByPreference(engine, male = true, locale = Locale.UK)
                    }
                    engine.setPitch(pitch)
                    engine.setSpeechRate(speed)
                }
                4 -> { // Indian English (Natural)
                    val result = engine.setLanguage(Locale("en", "IN"))
                    if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                        engine.language = Locale("en", "IN")
                    }
                    engine.setPitch(pitch)
                    engine.setSpeechRate(speed)
                }
                else -> { // Device System Default
                    engine.language = Locale.getDefault()
                    engine.setPitch(pitch)
                    engine.setSpeechRate(speed)
                }
            }
        } else {
            engine.setPitch(pitch)
            engine.setSpeechRate(speed)
        }
    }

    private fun findAndSetVoiceByPreference(engine: TextToSpeech, male: Boolean, locale: Locale) {
        try {
            val voices = engine.voices ?: return
            val preferredVoice = voices.find { voice ->
                val matchesLocale = voice.locale.language == locale.language
                val isTargetGender = if (male) {
                    voice.name.contains("male", ignoreCase = true) ||
                            voice.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS) == false &&
                            !voice.name.contains("female", ignoreCase = true)
                } else {
                    voice.name.contains("female", ignoreCase = true)
                }
                matchesLocale && isTargetGender
            } ?: voices.find { it.locale.language == locale.language }

            if (preferredVoice != null) {
                engine.voice = preferredVoice
            }
        } catch (_: Exception) {}
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { engine ->
                // Setup UtteranceProgressListener for hands-free continuous voice interaction
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}

                    override fun onDone(utteranceId: String?) {
                        mainHandler.post {
                            val cb = currentCompletionCallback
                            currentCompletionCallback = null
                            cb?.invoke()
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        mainHandler.post {
                            val cb = currentCompletionCallback
                            currentCompletionCallback = null
                            cb?.invoke()
                        }
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        mainHandler.post {
                            val cb = currentCompletionCallback
                            currentCompletionCallback = null
                            cb?.invoke()
                        }
                    }
                })

                isTtsReady = true
                configureTtsVoice(
                    presetIndex = getSavedPreset(),
                    pitch = getSavedPitch(),
                    speed = getSavedSpeed(),
                    customVoiceName = getSavedCustomVoiceName()
                )
            }
        }
    }

    fun testVoice(sampleText: String = "Hello, I am Jarvis, your personal money manager.") {
        speak(sampleText)
    }

    override fun onReadyForSpeech(params: Bundle?) {
        onListeningStateChanged(true)
    }

    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {
        onListeningStateChanged(false)
    }

    override fun onError(error: Int) {
        onListeningStateChanged(false)
        val errorMessage = when (error) {
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected. Tap mic to try again."
            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout. Offline speech mode."
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
            else -> "Speech recognition error ($error)"
        }
        onError(errorMessage)
    }

    override fun onResults(results: Bundle?) {
        onListeningStateChanged(false)
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            onSpeechResult(matches[0])
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            onSpeechPartial(matches[0])
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    fun destroy() {
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
    }
}

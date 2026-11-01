package com.lexicon.app

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

class Speaker(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    var ready by mutableStateOf(false)
        private set
    var message by mutableStateOf("")
        private set

    init { tts = TextToSpeech(context.applicationContext, this) }

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (!ready) message = "Озвучка Android недоступна"
    }

    fun speak(text: String, tag: String) {
        val engine = tts
        if (!ready || engine == null) {
            message = "Озвучка ещё не готова"
            return
        }
        val result = engine.setLanguage(Locale.forLanguageTag(tag))
        if (result == TextToSpeech.LANG_MISSING_DATA ||
            result == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            message = "Нет голоса для этого языка. Установи его в настройках Android"
            return
        }
        message = ""
        if (engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "lexicon") == TextToSpeech.ERROR) {
            message = "Ошибка воспроизведения"
        }
    }

    fun close() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}

package com.lexicon.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class Language(val tag: String, val title: String)
val languages = listOf(
    Language("zh-CN", "中文"),
    Language("en", "English"),
    Language("fr", "Français"),
    Language("es", "Español"),
    Language("ja", "日本語")
)
data class ImportStats(val added: Int, val skipped: Int)

@OptIn(ExperimentalCoroutinesApi::class)
class LexiconViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = LexiconDatabase.get(app).cardDao()
    private val selection = MutableStateFlow("zh-CN")
    private val settingsStore = LearningSettingsStore(app)
    private val preferences = MutableStateFlow(settingsStore.load("zh-CN"))
    val learningSettings: StateFlow<LearningSettings> = preferences.asStateFlow()
    val language: StateFlow<String> = selection.asStateFlow()
    val cards: StateFlow<List<WordCard>> = selection.flatMapLatest {
        dao.observeByLanguage(it)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectLanguage(value: String) {
        if (languages.any { it.tag == value }) {
            preferences.value = settingsStore.load(value)
            selection.value = value
        }
    }

    fun setDailyLimit(value: Int) {
        if (value in LearningSettingsStore.DAILY_LIMITS) {
            updateSettings { it.copy(dailyNewWords = value) }
        }
    }

    fun setAutoSpeech(value: AutoSpeechMode) {
        updateSettings { it.copy(autoSpeech = value) }
    }

    fun setSpeechRate(value: Float) {
        val rounded = kotlin.math.round(value.coerceIn(0.7f, 1.3f) * 10f) / 10f
        updateSettings { it.copy(speechRate = rounded) }
    }

    private fun updateSettings(transform: (LearningSettings) -> LearningSettings) {
        val next = transform(preferences.value)
        preferences.value = next
        settingsStore.save(next)
    }

    fun import(rows: List<ImportCard>, onDone: (Result<ImportStats>) -> Unit) {
        val languageNow = selection.value
        viewModelScope.launch {
            val result = runCatching {
                val entities = rows.map {
                    WordCard(
                        language = languageNow,
                        word = it.word,
                        reading = it.reading,
                        translation = it.translation,
                        example = it.example,
                        exampleTranslation = it.exampleTranslation
                    )
                }
                val ids = dao.insert(entities)
                val saved = ids.count { it != -1L }
                ImportStats(saved, rows.size - saved)
            }
            onDone(result)
        }
    }

    fun rate(card: WordCard, remembered: Boolean, mode: ReviewMode) {
        viewModelScope.launch {
            dao.update(ReviewSchedule.rate(card, remembered, System.currentTimeMillis(), mode))
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { dao.delete(id) }
    }
}

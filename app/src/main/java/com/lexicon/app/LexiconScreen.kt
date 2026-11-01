package com.lexicon.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

private enum class Tab(val title: String, val symbol: String) {
    WORDS("Словарь", "▤"),
    REVIEW("Повторение", "◎"),
    IMPORT("Импорт", "+")
}

@Composable
fun LexiconScreen(vm: LexiconViewModel = viewModel()) {
    var tab by rememberSaveable { mutableStateOf(Tab.WORDS) }
    var showSettings by remember { mutableStateOf(false) }
    val selectedLanguage by vm.language.collectAsState()
    val selectedSettings by vm.learningSettings.collectAsState()
    val settings = if (selectedSettings.language == selectedLanguage) {
        selectedSettings
    } else LearningSettings(selectedLanguage)
    val cards by vm.cards.collectAsState()
    val allExamples by vm.contexts.collectAsState()
    // Avoid showing the previous language's cards during a Flow switch.
    val visibleCards = cards.filter { it.language == selectedLanguage }
    val visibleIds = visibleCards.map { it.id }.toSet()
    val visibleExamples = allExamples.filter { it.cardId in visibleIds }
    val context = LocalContext.current
    val speaker = remember(context) { Speaker(context) }
    DisposableEffect(speaker) { onDispose { speaker.close() } }
    LaunchedEffect(settings.speechRate) {
        speaker.rate = settings.speechRate
    }
    if (showSettings) LearningSettingsDialog(
        settings = settings, vm = vm, onClose = { showSettings = false }
    )

    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
        topBar = {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "Lexicon",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Слова в контексте",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { showSettings = true }) {
                        Text("⚙ Настройки")
                    }
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    languages.forEach { option ->
                        FilterChip(
                            selected = option.tag == selectedLanguage,
                            onClick = { vm.selectLanguage(option.tag) },
                            label = { Text(option.title) }
                        )
                    }
                }
                if (speaker.message.isNotEmpty()) {
                    Text(speaker.message, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { option ->
                    NavigationBarItem(
                        selected = tab == option,
                        onClick = { tab = option },
                        icon = { Text(option.symbol) },
                        label = { Text(option.title) }
                    )
                }
            }
        }
    ) { padding ->
        when (tab) {
            Tab.WORDS -> Dictionary(visibleCards, visibleExamples, selectedLanguage, speaker, vm, padding)
            Tab.REVIEW -> Review(
                visibleCards, visibleExamples, selectedLanguage, speaker, vm, padding, settings
            )
            Tab.IMPORT -> Import(selectedLanguage, vm, padding)
        }
    }
}

@Composable
private fun LearningSettingsDialog(
    settings: LearningSettings,
    vm: LexiconViewModel,
    onClose: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Text("Настройки · " + (languages.firstOrNull { it.tag == settings.language }?.title ?: ""))
        },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Новые слова в день", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Общий лимит на узнавание и письмо. Старые повторения не ограничены.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 20).forEach { n ->
                        FilterChip(
                            selected = settings.dailyNewWords == n,
                            onClick = { vm.setDailyLimit(n) },
                            label = { Text("$n") }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(30, 50).forEach { n ->
                        FilterChip(
                            selected = settings.dailyNewWords == n,
                            onClick = { vm.setDailyLimit(n) },
                            label = { Text("$n") }
                        )
                    }
                }
                HorizontalDivider()
                Text("Автоматическая озвучка", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Слово или пример звучат при появлении карточки. В режиме письма ответ озвучивается после проверки.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                AutoSpeechMode.entries.forEach { option ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            vm.setAutoSpeech(option)
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.autoSpeech == option,
                            onClick = { vm.setAutoSpeech(option) }
                        )
                        Text(option.title, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Text(
                    "Скорость · " + String.format(
                        java.util.Locale.ROOT, "%.1f×", settings.speechRate
                    ),
                    style = MaterialTheme.typography.titleSmall
                )
                Slider(
                    value = settings.speechRate,
                    onValueChange = vm::setSpeechRate,
                    valueRange = 0.7f..1.3f,
                    steps = 5
                )
                Text(
                    "Все параметры сохраняются для каждого языка отдельно.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) { Text("Готово") }
        }
    )
}

@Composable
private fun Dictionary(
    cards: List<WordCard>,
    examples: List<CardExample>,
    language: String,
    speaker: Speaker,
    vm: LexiconViewModel,
    padding: PaddingValues
) {
    var expanded by rememberSaveable(language) { mutableStateOf<Long?>(null) }
    var deleting by remember { mutableStateOf<WordCard?>(null) }
    var addingTo by remember { mutableStateOf<WordCard?>(null) }
    var contextText by remember { mutableStateOf("") }
    var contextTranslation by remember { mutableStateOf("") }
    var contextError by remember { mutableStateOf("") }
    var contextSaving by remember { mutableStateOf(false) }
    var search by rememberSaveable(language) { mutableStateOf("") }
    val filtered = remember(cards, search) {
        if (search.isBlank()) cards else cards.filter {
            it.word.contains(search.trim(), ignoreCase = true) ||
                it.translation.contains(search.trim(), ignoreCase = true)
        }
    }

    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
        Text("Мои слова · " + cards.size, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            label = { Text("Поиск по слову или переводу") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        if (cards.isEmpty()) {
            Text("Словарь пуст. Скопируй карточки из ИИ и вставь их через «Импорт».")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (filtered.isEmpty()) item { Text("Совпадений не найдено") }
                items(filtered, key = { it.id }) { card ->
                    Card(Modifier.fillMaxWidth().clickable {
                        expanded = if (expanded == card.id) null else card.id
                    }) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(card.word, style = MaterialTheme.typography.titleLarge)
                            if (card.reading.isNotEmpty()) Text(card.reading)
                            Text(card.translation)
                            if (expanded == card.id) {
                                HorizontalDivider()
                                val contexts = examples.filter { it.cardId == card.id }
                                    .ifEmpty {
                                        listOf(CardExample(
                                            cardId = card.id,
                                            example = card.example,
                                            exampleTranslation = card.exampleTranslation
                                        ))
                                    }
                                Text("Примеры · " + contexts.size,
                                    style = MaterialTheme.typography.titleSmall)
                                contexts.forEachIndexed { index, ctx ->
                                    Text(ctx.example)
                                    Text(ctx.exampleTranslation,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    TextButton(onClick = { speaker.speak(ctx.example, language) }) {
                                        Text("▶ Пример " + (index + 1))
                                    }
                                }
                                TextButton(onClick = {
                                    addingTo = card
                                    contextText = ""
                                    contextTranslation = ""
                                    contextError = ""
                                }) { Text("+ Добавить пример") }
                                Text(
                                    "Узнавание: " + card.successfulReviews +
                                        " подряд, интервал " + card.intervalDays + " дн.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    "Письмо: " + card.writingSuccessfulReviews +
                                        " подряд, интервал " + card.writingIntervalDays + " дн.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = { speaker.speak(card.word, language) }) {
                                        Text("▶ Слово")
                                    }
                                    TextButton(onClick = { speaker.speak(card.example, language) }) {
                                        Text("▶ Пример")
                                    }
                                }
                                TextButton(onClick = { deleting = card }) {
                                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    addingTo?.let { card ->
        AlertDialog(
            onDismissRequest = { if (!contextSaving) addingTo = null },
            title = { Text("Новый пример · " + card.word) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = contextText,
                        onValueChange = { contextText = it; contextError = "" },
                        label = { Text("Предложение на изучаемом языке") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = contextTranslation,
                        onValueChange = { contextTranslation = it; contextError = "" },
                        label = { Text("Перевод предложения") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (contextError.isNotEmpty()) {
                        Text(contextError, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !contextSaving &&
                        contextText.isNotBlank() && contextTranslation.isNotBlank(),
                    onClick = {
                        contextSaving = true
                        vm.addContext(card.id, contextText, contextTranslation) { result ->
                            contextSaving = false
                            result.fold(
                                onSuccess = { added ->
                                    if (added) addingTo = null
                                    else contextError = "Такой пример уже есть у этого слова."
                                },
                                onFailure = { error ->
                                    contextError = error.message ?: "Не удалось сохранить пример."
                                }
                            )
                        }
                    }
                ) { Text(if (contextSaving) "Сохранение" else "Сохранить") }
            },
            dismissButton = {
                TextButton(enabled = !contextSaving, onClick = { addingTo = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    deleting?.let { card ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Удалить карточку?") },
            text = { Text(card.word) },
            confirmButton = {
                TextButton(onClick = { vm.delete(card.id); deleting = null }) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun Import(language: String, vm: LexiconViewModel, padding: PaddingValues) {
    var text by rememberSaveable(language) { mutableStateOf("") }
    var result by rememberSaveable(language) { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    val preview = remember(text) { CardImportParser.parse(text) }

    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
        Text("Массовый импорт", style = MaterialTheme.typography.titleLarge)
        Text("слово | транскрипция | перевод | пример | перевод примера")
        Text(
            "Чтобы добавить контекст к слову, повтори слово и перевод в строке с другим примером.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it; result = "" },
            label = { Text("Вставь карточки от ИИ") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 5,
            maxLines = 9
        )
        Spacer(Modifier.height(8.dp))
        Text("Карточек: " + preview.cards.size + " · Ошибок: " + preview.problems.size)
        Button(
            enabled = !saving && preview.cards.isNotEmpty(),
            onClick = {
                saving = true
                vm.import(preview.cards) { outcome ->
                    saving = false
                    result = outcome.fold(
                        onSuccess = {
                            "Новых слов: " + it.added +
                                " · Примеров: " + it.examplesAdded +
                                " · Повторов: " + it.skipped
                        },
                        onFailure = { "Ошибка сохранения: " + (it.message ?: "неизвестна") }
                    )
                }
            }
        ) { Text(if (saving) "Сохранение..." else "Импортировать " + preview.cards.size) }
        if (result.isNotEmpty()) Text(result, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        HorizontalDivider()
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (preview.problems.isNotEmpty()) {
                item { Text("Ошибки строк", fontWeight = FontWeight.Bold) }
                items(preview.problems) { problem ->
                    Text(
                        "Строка " + problem.line + ": " + problem.message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            if (preview.cards.isNotEmpty()) {
                item { Text("Предпросмотр", fontWeight = FontWeight.Bold) }
                items(preview.cards.take(20)) { card ->
                    Column {
                        Text(card.word + " — " + card.translation)
                        Text(card.example, style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (preview.cards.size > 20) {
                    item { Text("Показаны первые 20 карточек") }
                }
            }
        }
    }
}

@Composable
private fun Review(
    cards: List<WordCard>,
    examples: List<CardExample>,
    language: String,
    speaker: Speaker,
    vm: LexiconViewModel,
    padding: PaddingValues,
    settings: LearningSettings
) {
    // Recognition and writing are scheduled independently, with one daily new-word limit.
    var writingMode by rememberSaveable(language) { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            now = System.currentTimeMillis()
        }
    }
    // Stop speech when leaving the review tab as well as when changing cards.
    DisposableEffect(speaker) {
        onDispose { speaker.stop() }
    }
    val mode = if (writingMode) ReviewMode.WRITING else ReviewMode.RECOGNITION
    val plan = ReviewQueue.build(
        cards, mode, now, dailyLimit = settings.dailyNewWords
    )
    val due = plan.due
    val current = due.firstOrNull()
    val activeExample = current?.let { ExampleSelection.choose(it, mode, examples) }

    // Answer state resets for each next card and when changing the exercise.
    var revealed by remember(current?.id, writingMode) { mutableStateOf(false) }
    var typed by remember(current?.id, writingMode) { mutableStateOf("") }
    var checked by remember(current?.id, writingMode) { mutableStateOf(false) }
    // Keep the submission guard across mode switches for the same card.
    var submitted by remember(language, current?.id) { mutableStateOf(false) }

    // Stop the previous audio when changing mode or card. Never give away
    // a written answer until the user has pressed Check.
    LaunchedEffect(
        current?.id, activeExample?.id, language, writingMode, checked,
        settings.autoSpeech, settings.speechRate, speaker.ready
    ) {
        speaker.stop()
        val card = current
        if (card != null && speaker.ready && (!writingMode || checked)) {
            speaker.rate = settings.speechRate
            speaker.speakAutomatically(
                card.word, activeExample?.example ?: card.example,
                language, settings.autoSpeech
            )
        }
    }

    Column(
        Modifier.fillMaxSize().padding(padding).imePadding()
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            (if (writingMode) "Письмо" else "Узнавание") + " · к повторению: " + due.size,
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            "Новых сегодня: " + plan.introducedToday + "/" + plan.dailyLimit +
                "   ·   Ожидают: " + plan.unseenTotal,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LinearProgressIndicator(
            progress = {
                (plan.introducedToday.toFloat() / plan.dailyLimit.coerceAtLeast(1))
                    .coerceIn(0f, 1f)
            },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !writingMode,
                enabled = !submitted,
                onClick = { writingMode = false },
                label = { Text("Карточки") }
            )
            FilterChip(
                selected = writingMode,
                enabled = !submitted,
                onClick = { writingMode = true },
                label = { Text("Написать слово") }
            )
        }

        if (current == null) {
            if (plan.unseenTotal > 0 && plan.remainingNew == 0) {
                Text("Лимит новых слов на сегодня исчерпан. Плановые повторения остаются доступны.")
            } else {
                Text("В этом режиме сейчас нет карточек к повторению.")
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (writingMode) {
                        Text(
                            "Напиши слово по переводу",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            current.translation,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            activeExample?.exampleTranslation ?: current.exampleTranslation,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        // Do not show any unmasked target-language text before checking.
                        val cloze = WritingExercise.cloze(
                            activeExample?.example ?: current.example, current.word, language
                        )
                        if (cloze != null) {
                            Text(
                                cloze,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            "Введи слово в той форме, в которой оно сохранено.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedTextField(
                            value = typed,
                            onValueChange = { typed = it },
                            enabled = !checked && !submitted,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Твой ответ") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (typed.isNotBlank() && !checked && !submitted) checked = true
                                }
                            )
                        )

                        if (!checked) {
                            Button(
                                enabled = typed.isNotBlank() && !submitted,
                                onClick = { checked = true },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Проверить") }
                        } else {
                            val correct = WritingExercise.matches(typed, current.word)
                            HorizontalDivider()
                            Text(
                                if (correct) "Верно!" else "Пока неверно",
                                color = if (correct) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text("Правильный ответ: " + current.word, fontWeight = FontWeight.Bold)
                            if (current.reading.isNotEmpty()) Text(current.reading)
                            Text(activeExample?.example ?: current.example)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { speaker.speak(current.word, language) }) {
                                    Text("▶ Слово")
                                }
                                OutlinedButton(onClick = { speaker.speak(activeExample?.example ?: current.example, language) }) {
                                    Text("▶ Фраза")
                                }
                            }
                            if (correct) {
                                Button(
                                    enabled = !submitted,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        submitted = true
                                        vm.rate(current, true, mode)
                                        now = System.currentTimeMillis()
                                    }
                                ) { Text("Следующее") }
                            } else {
                                Text(
                                    "Если ты ввёл другой допустимый вариант, можно засчитать вручную.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        enabled = !submitted,
                                        onClick = {
                                            submitted = true
                                            vm.rate(current, false, mode)
                                            now = System.currentTimeMillis()
                                        }
                                    ) { Text("Не помню") }
                                    Button(
                                        enabled = !submitted,
                                        onClick = {
                                            submitted = true
                                            vm.rate(current, true, mode)
                                            now = System.currentTimeMillis()
                                        }
                                    ) { Text("Засчитать") }
                                }
                            }
                        }
                    } else {
                        Text(
                            current.word,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.headlineLarge
                        )
                        if (current.reading.isNotEmpty()) {
                            Text(
                                current.reading,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                        Text(
                            activeExample?.example ?: current.example,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = { speaker.speak(current.word, language) }) {
                                Text("▶ Слово")
                            }
                            OutlinedButton(onClick = { speaker.speak(current.example, language) }) {
                                Text("▶ Фраза")
                            }
                        }
                        if (revealed) {
                            HorizontalDivider()
                            Text(current.translation, fontWeight = FontWeight.Bold)
                            Text(activeExample?.exampleTranslation ?: current.exampleTranslation)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    enabled = !submitted,
                                    onClick = {
                                        submitted = true
                                        vm.rate(current, false, mode)
                                        now = System.currentTimeMillis()
                                    }
                                ) { Text("Не помню") }
                                Button(
                                    enabled = !submitted,
                                    onClick = {
                                        submitted = true
                                        vm.rate(current, true, mode)
                                        now = System.currentTimeMillis()
                                    }
                                ) { Text("Помню") }
                            }
                        } else {
                            Button(
                                onClick = { revealed = true },
                                enabled = !submitted,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Показать перевод") }
                        }
                    }
                    if (current.introducedAt == 0L) {
                        TextButton(
                            enabled = !submitted,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                submitted = true
                                vm.markAlreadyKnown(current, mode)
                                now = System.currentTimeMillis()
                            }
                        ) { Text("Уже знаю · проверить через 30 дней") }
                    }
                }
            }
            Text(
                "Узнавание и письмо повторяются независимо. Ошибка: повтор через 10 минут с небольшим откатом. «Уже знаю» не расходует дневной лимит.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

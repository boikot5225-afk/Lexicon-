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
    val selectedLanguage by vm.language.collectAsState()
    val cards by vm.cards.collectAsState()
    // Avoid showing the previous language's cards during a Flow switch.
    val visibleCards = cards.filter { it.language == selectedLanguage }
    val context = LocalContext.current
    val speaker = remember(context) { Speaker(context) }
    DisposableEffect(speaker) { onDispose { speaker.close() } }

    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
        topBar = {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Lexicon", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
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
            Tab.WORDS -> Dictionary(visibleCards, selectedLanguage, speaker, vm, padding)
            Tab.REVIEW -> Review(visibleCards, selectedLanguage, speaker, vm, padding)
            Tab.IMPORT -> Import(selectedLanguage, vm, padding)
        }
    }
}

@Composable
private fun Dictionary(
    cards: List<WordCard>,
    language: String,
    speaker: Speaker,
    vm: LexiconViewModel,
    padding: PaddingValues
) {
    var expanded by rememberSaveable(language) { mutableStateOf<Long?>(null) }
    var deleting by remember { mutableStateOf<WordCard?>(null) }

    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
        Text("Слов: " + cards.size, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        if (cards.isEmpty()) {
            Text("Словарь пуст. Скопируй карточки из ИИ и вставь их через «Импорт».")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(cards, key = { it.id }) { card ->
                    Card(Modifier.fillMaxWidth().clickable {
                        expanded = if (expanded == card.id) null else card.id
                    }) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(card.word, style = MaterialTheme.typography.titleLarge)
                            if (card.reading.isNotEmpty()) Text(card.reading)
                            Text(card.translation)
                            if (expanded == card.id) {
                                HorizontalDivider()
                                Text(card.example)
                                Text(card.exampleTranslation)
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
                        onSuccess = { "Добавлено: " + it.added + " · Дубликатов: " + it.skipped },
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
    language: String,
    speaker: Speaker,
    vm: LexiconViewModel,
    padding: PaddingValues
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
    val mode = if (writingMode) ReviewMode.WRITING else ReviewMode.RECOGNITION
    val plan = ReviewQueue.build(cards, mode, now)
    val due = plan.due
    val current = due.firstOrNull()

    // Answer state resets for each next card and when changing the exercise.
    var revealed by remember(current?.id, writingMode) { mutableStateOf(false) }
    var typed by remember(current?.id, writingMode) { mutableStateOf("") }
    var checked by remember(current?.id, writingMode) { mutableStateOf(false) }
    // Keep the submission guard across mode switches for the same card.
    var submitted by remember(language, current?.id) { mutableStateOf(false) }

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
                " · Ожидают изучения: " + plan.unseenTotal,
            style = MaterialTheme.typography.bodySmall
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
            Card(Modifier.fillMaxWidth()) {
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
                            current.exampleTranslation,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        // Do not show any unmasked target-language text before checking.
                        val cloze = WritingExercise.cloze(
                            current.example, current.word, language
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
                            Text(current.example)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { speaker.speak(current.word, language) }) {
                                    Text("▶ Слово")
                                }
                                OutlinedButton(onClick = { speaker.speak(current.example, language) }) {
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
                            current.example,
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
                            Text(current.exampleTranslation)
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
                }
            }
            Text(
                "Узнавание и письмо повторяются независимо. Общий лимит: 10 новых слов в день на язык. Успех: 1, 3, 7, 14, 30, 60 дней; ошибка: через 10 минут.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

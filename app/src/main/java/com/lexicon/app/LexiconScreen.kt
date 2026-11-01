package com.lexicon.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

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
    val context = LocalContext.current
    val speaker = remember(context) { Speaker(context) }
    DisposableEffect(speaker) { onDispose { speaker.close() } }

    Scaffold(
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
            Tab.WORDS -> Dictionary(cards, selectedLanguage, speaker, vm, padding)
            Tab.REVIEW -> Review(cards, selectedLanguage, speaker, vm, padding)
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
    val due = cards.filter { it.nextReviewAt <= System.currentTimeMillis() }
        .sortedWith(compareBy<WordCard> { it.nextReviewAt }.thenBy { it.id })
    val current = due.firstOrNull()
    var revealed by remember(current?.id) { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().padding(padding).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("К повторению: " + due.size, style = MaterialTheme.typography.titleLarge)
        if (current == null) {
            Text("На сегодня всё. Добавь новые карточки через «Импорт».")
        } else {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        current.word,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineLarge
                    )
                    if (current.reading.isNotEmpty()) {
                        Text(current.reading, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    }
                    Text(current.example, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
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
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = { vm.rate(current, false) }) { Text("Не помню") }
                            Button(onClick = { vm.rate(current, true) }) { Text("Помню") }
                        }
                    } else {
                        Button(onClick = { revealed = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Показать перевод")
                        }
                    }
                }
            }
            Text(
                "Успешное повторение: через 1, 3, 7, 14, 30, 60 дней. Ошибка: через 10 минут.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

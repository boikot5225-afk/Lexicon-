package com.lexicon.app

import java.text.Normalizer
import java.util.Locale

/** Pure rules for typed recall. Independent of UI and Android services. */
object WritingExercise {
    fun matches(input: String, expected: String): Boolean =
        input.isNotBlank() && normalize(input) == normalize(expected)

    private fun normalize(text: String): String =
        Normalizer.normalize(text.trim(), Normalizer.Form.NFC)
            .replace(Regex("\\s+"), " ")
            .lowercase(Locale.ROOT)

    /**
     * Hide all exact occurrences of the card's headword.
     * For Latin-script language examples, require token boundaries so "cat"
     * cannot be masked inside "catalog". If no safe match exists, return null:
     * the UI must then hide the entire foreign-language example until grading.
     * This prevents leaking an inflected form as a supposed target answer.
     */
    fun cloze(example: String, word: String, language: String): String? {
        if (word.isBlank() || example.isBlank()) return null
        val escaped = Regex.escape(word)
        val pattern = if (language.startsWith("zh") || language.startsWith("ja")) {
            escaped
        } else {
            "(?<![\\p{L}\\p{N}])" + escaped + "(?![\\p{L}\\p{N}])"
        }
        val regex = Regex(pattern, RegexOption.IGNORE_CASE)
        if (!regex.containsMatchIn(example)) return null
        return regex.replace(example, "____")
    }
}

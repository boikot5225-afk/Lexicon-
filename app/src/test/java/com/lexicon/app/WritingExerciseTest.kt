package com.lexicon.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WritingExerciseTest {
    @Test fun acceptsChineseWordAndNormalizedPhrase() {
        assertTrue(WritingExercise.matches(" 终于 ", "终于"))
        assertTrue(WritingExercise.matches("  TAKE   OFF ", "take off"))
    }

    @Test fun checksRealSpellingIncludingAccents() {
        assertTrue(WritingExercise.matches("ÉCOLE", "école"))
        assertFalse(WritingExercise.matches("ecole", "école"))
        assertFalse(WritingExercise.matches("", "école"))
    }

    @Test fun masksEveryChineseOccurrence() {
        assertEquals(
            "他____来了。____！",
            WritingExercise.cloze("他终于来了。终于！", "终于", "zh-CN")
        )
    }

    @Test fun avoidsMaskingInsideOtherWords() {
        assertEquals(
            "The ____ is not a catalog.",
            WritingExercise.cloze("The cat is not a catalog.", "cat", "en")
        )
    }

    @Test fun avoidsGivingAwayUnmatchedInflectedExample() {
        assertNull(WritingExercise.cloze("He considered it.", "consider", "en"))
        assertNull(WritingExercise.cloze("我的朋友很好。", "终于", "zh-CN"))
    }

    @Test fun supportsFrenchPhraseAndJapaneseCharacters() {
        assertEquals(
            "Je vais ____.",
            WritingExercise.cloze("Je vais prendre soin.", "prendre soin", "fr")
        )
        assertEquals(
            "日本語を____。",
            WritingExercise.cloze("日本語を勉強する。", "勉強する", "ja")
        )
    }
}

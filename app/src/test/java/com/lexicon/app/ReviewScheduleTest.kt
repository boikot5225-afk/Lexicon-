package com.lexicon.app

import org.junit.Assert.assertEquals
import org.junit.Test

class ReviewScheduleTest {
    private val card = WordCard(
        language = "en", word = "consider", reading = "",
        translation = "рассматривать", example = "Consider this.",
        exampleTranslation = "Рассмотри это."
    )

    @Test fun successSchedulesTomorrow() {
        val result = ReviewSchedule.rate(card, true, 1000)
        assertEquals(1, result.successfulReviews)
        assertEquals(86_401_000L, result.nextReviewAt)
    }

    @Test fun mistakeReturnsAfterTenMinutes() {
        val result = ReviewSchedule.rate(card.copy(successfulReviews = 4), false, 1000)
        assertEquals(0, result.successfulReviews)
        assertEquals(601_000L, result.nextReviewAt)
    }
}

package com.lexicon.app

import org.junit.Assert.assertEquals
import org.junit.Test

class ReviewScheduleTest {
    private val card = WordCard(
        language = "en", word = "consider", reading = "",
        translation = "рассматривать", example = "Consider this.",
        exampleTranslation = "Рассмотри это."
    )

    @Test fun recognitionKeepsWritingUnchanged() {
        val result = ReviewSchedule.rate(card, true, 1000, ReviewMode.RECOGNITION)
        assertEquals(1, result.successfulReviews)
        assertEquals(86_401_000L, result.nextReviewAt)
        assertEquals(1000L, result.introducedAt)
        assertEquals(0, result.writingSuccessfulReviews)
        assertEquals(0L, result.writingNextReviewAt)
    }

    @Test fun writingKeepsRecognitionHistoryUnchanged() {
        val prior = card.copy(
            nextReviewAt = 500_000L,
            successfulReviews = 3,
            intervalDays = 7,
            introducedAt = 800L
        )
        val result = ReviewSchedule.rate(prior, true, 1000L, ReviewMode.WRITING)
        assertEquals(1, result.writingSuccessfulReviews)
        assertEquals(86_401_000L, result.writingNextReviewAt)
        assertEquals(3, result.successfulReviews)
        assertEquals(500_000L, result.nextReviewAt)
        assertEquals(800L, result.introducedAt)
    }

    @Test fun writingMistakeDoesNotResetRecognition() {
        val prior = card.copy(
            introducedAt = 500L, successfulReviews = 4, intervalDays = 14,
            writingSuccessfulReviews = 3, writingIntervalDays = 7
        )
        val result = ReviewSchedule.rate(prior, false, 1000L, ReviewMode.WRITING)
        assertEquals(0, result.writingSuccessfulReviews)
        assertEquals(601_000L, result.writingNextReviewAt)
        assertEquals(4, result.successfulReviews)
        assertEquals(14, result.intervalDays)
    }

    @Test fun recognitionMistakeResetsOnlyRecognition() {
        val prior = card.copy(
            successfulReviews = 3, writingSuccessfulReviews = 2,
            writingNextReviewAt = 33_000L
        )
        val result = ReviewSchedule.rate(prior, false, 1000L, ReviewMode.RECOGNITION)
        assertEquals(0, result.successfulReviews)
        assertEquals(601_000L, result.nextReviewAt)
        assertEquals(2, result.writingSuccessfulReviews)
        assertEquals(33_000L, result.writingNextReviewAt)
    }
}

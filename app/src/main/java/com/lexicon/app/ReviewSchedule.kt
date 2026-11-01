package com.lexicon.app

enum class ReviewMode { RECOGNITION, WRITING }

object ReviewSchedule {
    private const val DAY_MS = 86_400_000L
    private const val RETRY_MS = 600_000L
    private val steps = listOf(1, 3, 7, 14, 30, 60)

    fun dueAt(card: WordCard, mode: ReviewMode): Long = when (mode) {
        ReviewMode.RECOGNITION -> card.nextReviewAt
        ReviewMode.WRITING -> card.writingNextReviewAt
    }

    fun successCount(card: WordCard, mode: ReviewMode): Int = when (mode) {
        ReviewMode.RECOGNITION -> card.successfulReviews
        ReviewMode.WRITING -> card.writingSuccessfulReviews
    }

    fun rate(card: WordCard, remembered: Boolean, now: Long, mode: ReviewMode): WordCard {
        val firstEncounter = if (card.introducedAt == 0L) now else card.introducedAt
        val successes = if (remembered) successCount(card, mode) + 1 else 0
        val days = if (remembered) steps[(successes - 1).coerceAtMost(steps.lastIndex)] else 0
        val next = if (remembered) now + days * DAY_MS else now + RETRY_MS
        return when (mode) {
            ReviewMode.RECOGNITION -> card.copy(
                introducedAt = firstEncounter,
                nextReviewAt = next,
                successfulReviews = successes,
                intervalDays = days
            )
            ReviewMode.WRITING -> card.copy(
                introducedAt = firstEncounter,
                writingNextReviewAt = next,
                writingSuccessfulReviews = successes,
                writingIntervalDays = days
            )
        }
    }
}

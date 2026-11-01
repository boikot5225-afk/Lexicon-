package com.lexicon.app

object ReviewSchedule {
    private const val DAY_MS = 86_400_000L
    private const val RETRY_MS = 600_000L
    private val steps = listOf(1, 3, 7, 14, 30, 60)

    fun rate(card: WordCard, remembered: Boolean, now: Long): WordCard {
        if (!remembered) return card.copy(
            nextReviewAt = now + RETRY_MS,
            successfulReviews = 0,
            intervalDays = 0
        )
        val nextCount = card.successfulReviews + 1
        val days = steps[(nextCount - 1).coerceAtMost(steps.lastIndex)]
        return card.copy(
            successfulReviews = nextCount,
            intervalDays = days,
            nextReviewAt = now + days * DAY_MS
        )
    }
}

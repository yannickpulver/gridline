package com.yannickpulver.gridline.ui.feed

import kotlin.math.absoluteValue

/**
 * A utility class to calculate and validate ranks.
 */
object Rank {
    const val GAP: Long = 1L shl 50 // GAP = 2^50
    const val INITIAL_VALUE: Long = 0L
    private const val MIN_VALUE: Long = Long.MIN_VALUE
    private const val MAX_VALUE: Long = Long.MAX_VALUE

    fun calculateRank(previous: Long? = null, next: Long? = null): Long {
        return when {
            previous != null && next == null -> getNewValueAfter(previous)
            previous == null && next != null -> getNewValueBefore(next)
            previous != null && next != null -> getNewValueBetween(previous, next)
            else -> getNewValueAfter(INITIAL_VALUE)
        }
    }

    fun isBetween(rank: Long, before: Long?, after: Long?): Boolean {
        return rank in ((before ?: MIN_VALUE) + 1)..<(after ?: MAX_VALUE)
    }

    /**
     * Calculates a new value before an existing value.
     */
    private fun getNewValueBefore(targetValue: Long): Long {
        if (targetValue <= (MIN_VALUE + GAP)) {
            val remainingSpace = (targetValue - MIN_VALUE) / 2

            if (remainingSpace == 0L) {
                return MIN_VALUE
            }

            return MIN_VALUE + remainingSpace
        }
        return targetValue - GAP
    }

    /**
     * Calculates a new value after an existing value.
     */
    private fun getNewValueAfter(targetValue: Long): Long {
        if (targetValue >= MAX_VALUE - GAP) {
            val remainingSpace = (MAX_VALUE - targetValue) / 2

            if (remainingSpace == 0L) {
                return MAX_VALUE
            }

            return targetValue + remainingSpace
        }
        return targetValue + GAP
    }

    /**
     * Calculates a new value between two existing values.
     */
    private fun getNewValueBetween(firstValue: Long, secondValue: Long): Long {
        val lowerValue = firstValue.coerceAtMost(secondValue)
        val diff = (firstValue - secondValue).absoluteValue
        val remainingSpace = diff / 2

        if (remainingSpace == 0L) {
            throw IllegalStateException("No space left to insert new ranks between.")
        }

        return lowerValue + remainingSpace
    }
}

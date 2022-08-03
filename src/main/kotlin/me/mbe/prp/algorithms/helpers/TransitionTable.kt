package me.mbe.prp.algorithms.helpers

import me.mbe.prp.algorithms.helpers_temporal.TemporalSets
import me.mbe.prp.base.OneElementCacheWithDate
import me.mbe.prp.core.Capacity
import java.time.Duration
import java.time.ZonedDateTime
import java.util.ArrayList

typealias TransitionTableDurationReducer = (durations: List<Duration>, weight: Double, temporalSets: TemporalSets?,
date: ZonedDateTime?) -> Duration

/**
 * @param topN when >=1: use as integer for number of predictions; when <1: use as probability
 */
abstract class TransitionTable<K, L>(
    private val topN: Double,
    protected val reducer: TransitionTableDurationReducer,
    protected val storeDuration: Boolean,
) {
    protected abstract fun addTransitionInternal(from: K, to: L, weight: Double, duration: Duration, date: ZonedDateTime?)

    protected abstract fun getNextWithProbAllInternal(from: K, date: ZonedDateTime?): List<Triple<L, Duration, Double>>

    abstract fun computeSize(): Capacity

    protected open fun getNextInternal(from: K, date: ZonedDateTime?): List<Pair<L, Duration>> {
        val next = getNextWithProbAll(from, date).sortedByDescending { it.third }

        if (topN >= 1) {
            return next.take(topN.toInt()).map { Pair(it.first, it.second) }
        }

        var s = 0.0
        var j = 0
        val ret = ArrayList<Pair<L, Duration>>()
        while (s < topN && j < next.size) {
            @Suppress("UNCHECKED_CAST")
            ret.add(Pair(next[j].first, next[j].second))
            s += next[j].third
            j++
        }
        return ret
    }

    fun addTransition(from: K, to: L, weight: Double = 1.0, duration: Duration = Duration.ZERO, date: ZonedDateTime? = null) {
        getNextCache.invalidate()
        getNextWithProbAllCache.invalidate()

        addTransitionInternal(from, to, weight, duration, date)
    }

    private val getNextCache = OneElementCacheWithDate(::getNextInternal)
    private val getNextWithProbAllCache = OneElementCacheWithDate(::getNextWithProbAllInternal)

    fun getNext(from: K, date: ZonedDateTime?): List<Pair<L, Duration>> {
        return getNextCache.get(shallowCopy(from), date)
    }

    fun getNextWithProbAll(from: K, date: ZonedDateTime?): List<Triple<L, Duration, Double>> {
        return getNextWithProbAllCache.get(shallowCopy(from), date)
    }

    private fun shallowCopy(x: K): K {
        if (x is Collection<*>) {
            @Suppress("UNCHECKED_CAST")
            return ArrayList(x) as K
        }
        return x
    }

}
package me.mbe.prp.algorithms.helpers

import me.mbe.prp.base.percentile
import me.mbe.prp.core.Capacity
import org.openjdk.jol.info.GraphLayout
import java.time.Duration
import java.time.ZonedDateTime

val AverageReducer: TransitionTableDurationReducer = { durations, weight, _, _ ->
    val seconds = durations.sumOf { it.seconds } / weight.toLong()
    Duration.ofSeconds(seconds)
}

val PercentileReducer: (Double) -> TransitionTableDurationReducer = { p ->
    { durations, _, _, _ -> durations.percentile(p) }
}

class TransitionTableImpl<K, L>(
    topN: Double,
    reducer: TransitionTableDurationReducer,
    storeDuration: Boolean,
) : TransitionTable<K, L>(topN, reducer, storeDuration) {

    private val map = LinkedHashMap<K, LinkedHashMap<L, Pair<Double, MutableList<Duration>>>>()

    override fun addTransitionInternal(from: K, to: L, weight: Double, duration: Duration, date: ZonedDateTime?) {
        val f = map.getOrPut(from, ::LinkedHashMap)
        val v = f.getOrDefault(to, Pair(0.0, ArrayList()))
        if (storeDuration) v.second.add(duration)
        f[to] = Pair(v.first + weight, v.second)
    }

    override fun getNextWithProbAllInternal(from: K, date: ZonedDateTime?): List<Triple<L, Duration, Double>> {
        val e = map[from]?.entries ?: return emptyList()
        val s = e.sumOf { it.value.first }

        return if (storeDuration)
            e.map { Triple(it.key, reducer(it.value.second, it.value.first, null, null), it.value.first / s) }
        else
            e.map { Triple(it.key, Duration.ZERO, it.value.first / s) }
    }

    override fun toString(): String {
        return map.toString()
    }

    override fun computeSize(): Capacity {
        return GraphLayout.parseInstance(map).totalSize()
    }
}
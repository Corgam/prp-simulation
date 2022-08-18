package me.mbe.prp.algorithms.helpers_temporal


import me.mbe.prp.algorithms.helpers.*
import me.mbe.prp.base.cartesianProduct
import me.mbe.prp.core.Capacity
import me.mbe.prp.core.Node
import org.openjdk.jol.info.GraphLayout
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.LinkedHashMap
import kotlin.math.pow

data class TemporalFusionTransitionTableConfig(val maxDepth: Int, val dowSplits: List<Int>, val todSplits: List<Int>, val temporalSplits: String) {
    override fun toString(): String = "${maxDepth}_${dowSplits}_${todSplits}_${temporalSplits}"
}

class TemporalFusionTransitionTable(
    private val config: TemporalFusionTransitionTableConfig,
    topN: Double,
    reducer: TransitionTableDurationReducer,
    storeDuration: Boolean
) : TransitionTable<Triple<List<Node>, DayOfWeek, LocalTime>, Node?>(topN, reducer, storeDuration) {

    data class TTSpecification(
        val depth: Int,
        val dowSplitCount: Int, // 1: all days together; 2: Mo-Fr, Sa-Su; 7: all days alone
        val todSplitCount: Int  // 1: all together; 4: splits 0-6, 6-12, 12-18, 18-24; 12: split every two hours; 24: split every hour
    )

    private var allDurationsLong: ArrayList<Long> = ArrayList()
    private var allDurationsLongForNodes: LinkedHashMap<List<Node>, ArrayList<Long>> =  LinkedHashMap<List<Node>, ArrayList<Long>>()
    private var lastAddedStartingDate: ZonedDateTime? = null

    private val transitionTables =
        LinkedHashMap<TTSpecification, TemporalTransitionTableImpl<Triple<List<Node>, Int, Int>, Node?>>()

    init {
        cartesianProduct(
            ::TTSpecification,
            1.rangeTo(config.maxDepth).toSet(),
            config.dowSplits,
            config.todSplits,
        ).forEach {
            transitionTables[it] =
                TemporalTransitionTableImpl(topN = Double.MAX_VALUE, reducer = reducer, storeDuration = storeDuration, temporalSplit = config.temporalSplits)
        }
    }

    override fun addTransitionInternal(
        from: Triple<List<Node>, DayOfWeek, LocalTime>,
        to: Node?,
        weight: Double,
        duration: Duration,
        date: ZonedDateTime?,
    ) {
        transitionTables.forEach { (k, v) ->
            if (from.first.size >= k.depth) {
                v.addTransition(
                    Triple(
                        from.first.takeLast(k.depth),
                        dowSplitFn(from.second, k.dowSplitCount),
                        todSplitFn(from.third, k.todSplitCount)
                    ),
                    to,
                    weight,
                    duration,
                    date
                )
            }
        }
        // Add duration to HWES array
        if(from.first.isNotEmpty() && to != null) {
            allDurationsLong.add(duration.seconds)
            if (!allDurationsLongForNodes.containsKey(from.first)){
                allDurationsLongForNodes[from.first] = arrayListOf<Long>()
            }
            val durations = allDurationsLongForNodes[from.first]!!
            durations.add(duration.seconds)
            lastAddedStartingDate = date
        }
    }

    override fun getNextWithProbAllInternal(
        from: Triple<List<Node>, DayOfWeek, LocalTime>, date: ZonedDateTime?
    ): List<Triple<Node?, Duration, Double>> {

        data class TTResponse(val node: Node?, val weight: Double, val duration: Long, val prob: Double)

        val q = transitionTables
            .flatMap { (k, v) ->
                v.getNextWithProbAll(
                    Triple(
                        from.first.takeLast(k.depth),
                        dowSplitFn(from.second, k.dowSplitCount),
                        todSplitFn(from.third, k.todSplitCount)
                    ),
                    date
                ).map { TTResponse(it.first, weight(k), it.second.seconds, it.third) }
            }
            .groupBy { it.node }
            .mapValues {
                val probSum = it.value.sumOf { p -> p.prob * p.weight }
                var weightedDurationSum =
                    it.value.sumOf { p -> p.duration * p.weight } / it.value.sumOf { p -> p.weight }
                // Replace the value with HWES
                if (config.temporalSplits.contains("HWESuser") || config.temporalSplits.contains("HWESnode")){
                    var array = allDurationsLong.toLongArray()
                    if (config.temporalSplits.contains("HWESnode")){
                        if(allDurationsLongForNodes.containsKey(from.first)){
                            array = allDurationsLongForNodes[from.first]!!.toLongArray()
                        }else{
                            array = ArrayList<Long>().toLongArray()
                        }
                    }
                    val currentPeriod: Int = array.size/2
                    if (currentPeriod > 0 && array.isNotEmpty()){
                        val prediction: DoubleArray = HoltWinters.forecast(array, 0.06, 0.98, 0.48, currentPeriod, currentPeriod)
                        var duration = 0.0
                        if (lastAddedStartingDate != null){
                            val totalDifference = Duration.between(lastAddedStartingDate, date).seconds
                            var currentDifference = 0.0
                            var i = 0
                            // Move to the proper time stamp
                            while(currentDifference < totalDifference){
                                if (i >= prediction.size){
                                    break
                                }
                                val predictedDuration = prediction[i]
                                if (predictedDuration > 0.0){
                                    currentDifference += predictedDuration
                                }
                                i++
                            }
                            // Get the final value
                            if (prediction.isNotEmpty() && i > 0) {
                                duration = prediction[i - 1]
                            }
                        }
                        if (duration != 0.0 && !duration.isNaN()){
                            weightedDurationSum = duration
                        }
                    }
                }
                Pair(probSum, weightedDurationSum)
            }
            .toList()
            .sortedByDescending { it.second.first }

        val s = q.sumOf { it.second.first }

        return q.map { Triple(it.first, Duration.ofSeconds(it.second.second.toLong()), it.second.first / s) }
    }

    private fun weight(ttS: TTSpecification): Double {

        //todo: config
        val depthSpecificity = 2.0.pow(-(config.maxDepth - ttS.depth))

        //todo: config
        val dowSpecificity = when (ttS.dowSplitCount) {
            1 -> 0.3
            2 -> 0.7
            7 -> 1.0
            else -> throw IllegalArgumentException()
        }

        //todo: config
        val todSpecificity = when (ttS.todSplitCount) {
            1 -> 0.125
            4 -> 0.25
            12 -> 0.5
            24 -> 1.0
            else -> throw IllegalArgumentException()
        }

        return depthSpecificity * dowSpecificity * todSpecificity
    }

    override fun toString(): String {
        return transitionTables.toString()
    }

    override fun computeSize(): Capacity {
        //TODO: Fix the size calculation
        //return GraphLayout.parseInstance(transitionTables).totalSize()
        return 0
    }
}
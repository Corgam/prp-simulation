package me.mbe.prp.algorithms.startuppred

import me.mbe.prp.base.*
import me.mbe.prp.core.*
import org.openjdk.jol.info.GraphLayout
import smile.clustering.hclust
import java.time.*
import java.util.*
import kotlin.math.absoluteValue


class Alg015(
    p: AlgorithmParams,

    private val clusterPartitionTime: Duration,
) : Algorithm(p) {

    private val beijingZone: ZoneId = ZoneId.of("Asia/Shanghai")

    data class PossiblePattern(val node: Node, val time: LocalTime, val dow: List<DayOfWeek>, val certainty: Double)


    /*-----------------------*/

    private var lastPosition: Pair<Node, Instant>? = null

    private var startTime: Instant? = null

    private val startTimes = LinkedHashMap<Node, LinkedHashMap<DayOfWeek, ArrayList<LocalTime>>>()


    private fun possibleNumberOfOccurrences(currentTime: Instant, pattern: List<DayOfWeek>): Double {
        return Duration.between(startTime, currentTime).toDays().toDouble() / 7.0 * pattern.size
    }

    override fun onStartTrip(state: WorldState) {
        if (startTime == null) startTime = state.time

        cancelCallbacks()

        val zonedTime = state.time.atZone(beijingZone)

        val startNode = state.getClosestNode(p.user)

        val o2 = startTimes.getOrPut(startNode, ::LinkedHashMap)
        val o3 = o2.getOrPut(zonedTime.dayOfWeek, ::ArrayList)
        o3.add(zonedTime.toLocalTime())
    }

    override fun onEndTrip(state: WorldState) {
        val kg = getKeyGroup(state)
        val closestNode = state.getClosestNode(p.user)

        lastPosition = Pair(closestNode, state.time)

        val patterns = findPatterns()
        if (patterns.isEmpty()) {
            state.setKeygroupMembers(kg, listOf())
            return
        }
        val (nextPatternTime, nextNode) = findNextPattern(state.time.atZone(beijingZone), patterns)

        val callBackTime = nextPatternTime.minus(state.network.estimateTransferTime(kg, nextNode))
        if (callBackTime < state.time) {
            state.setKeygroupMembers(kg, listOf(Pair(nextNode, Duration.ZERO)))
            registerTimeCallback(state.time.plus(clusterPartitionTime), "patternPredictEnd")
        } else {
            registerTimeCallback(callBackTime, "patternPredict")
            patternPredictNode = nextNode
            state.setKeygroupMembers(kg, listOf())
        }


    }

    private var patternPredictNode: Node? = null

    override fun onTime(state: WorldState, value: String) {
        val kg = getKeyGroup(state)
        when (value) {
            "patternPredictEnd" -> {
                state.setKeygroupMembers(kg, listOf())
            }
            "patternPredict" -> {
                state.setKeygroupMembers(kg, listOf(Pair(patternPredictNode!!, Duration.ZERO)))
                registerTimeCallback(state.time.plus(clusterPartitionTime), "patternPredictEnd")
            }
            else -> throw IllegalArgumentException()
        }
    }

    private fun findNextPattern(time: ZonedDateTime, patterns: List<PossiblePattern>): Pair<Instant, Node> {
        return patterns
            .map {
                var c = LocalDateTime.of(time.toLocalDate(), it.time).atZone(time.zone)
                if (c < time) c = c.plusDays(1)
                while (c.dayOfWeek !in it.dow) c = c.plusDays(1)
                return@map Pair(c.toInstant(), it.node)
            }
            .minByOrNull { it.first }!!
    }

    override fun printState() {
        println(startTimes.mapValues { it.value.mapValues { entry -> entry.value.sorted() } })
    }

    private val clusterCache = MultiElementCache(::clusterFinder)


    private fun clusterFinder(times: List<LocalTime>): List<List<Pair<LocalTime, Int>>> {
        if (times.isEmpty()) return emptyList()

        val timesArray = times.toTypedArray()

        val c = hclust(
            timesArray,
            { o1, o2 -> Duration.between(o1, o2).seconds.absoluteValue.toDouble() },
            method = "complete"
        )

        if (c.height.isEmpty()) return emptyList()

        val cIndices =
            if (c.height.last() < clusterPartitionTime.seconds.toDouble()) IntArray(times.size) // all in cluster 0
            else c.partition(clusterPartitionTime.seconds.toDouble())

        return times
            .mapIndexed { i, t -> Pair(t, cIndices[i]) }
            .groupBy { it.second }
            .values
            .filter { it.size > 3 }
            .map { it.sortedBy { it.first } }
    }

    private fun findPatternsInternal(pair: Pair<List<LocalTime>, Double>): List<Pair<LocalTime, Double>> {
        val (times, pNOO) = pair

        val q = clusterCache.get(times)

        return q.map {
            Pair(
                it.map { it.first }
                    .minOrNull()!!,
                //.average(),
                it.size.toDouble() / pNOO
            )
        }

    }


    //todo: config
    private val patterns = listOf(
/*
        listOf(DayOfWeek.MONDAY),
        listOf(DayOfWeek.TUESDAY),
        listOf(DayOfWeek.WEDNESDAY),
        listOf(DayOfWeek.THURSDAY),
        listOf(DayOfWeek.FRIDAY),
        listOf(DayOfWeek.SATURDAY),
        listOf(DayOfWeek.SUNDAY),
*/
        listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
        listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
    )


    private fun findPatterns(): List<PossiblePattern> {

        val possiblePatterns = ArrayList<PossiblePattern>()

        startTimes.forEach { (node, startTimesAtNode) ->
            patterns.forEach { dow ->
                val times = dow.flatMap { startTimesAtNode[it].orEmpty() }
                val pNOO = possibleNumberOfOccurrences(lastPosition!!.second, dow)

                findPatternsInternal(Pair(times, pNOO))
                    .forEach { possiblePatterns.add(PossiblePattern(node, it.first, dow, it.second)) }

            }
        }

        return possiblePatterns.filter { it.certainty >= 0.5 }  //todo: config
    }

    override fun computeSize(): Capacity {
        return GraphLayout.parseInstance(startTimes).totalSize()
    }

}

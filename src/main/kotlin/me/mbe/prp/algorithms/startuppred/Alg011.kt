package me.mbe.prp.algorithms.startuppred

import me.mbe.prp.base.percentile
import me.mbe.prp.core.*
import org.openjdk.jol.info.GraphLayout
import java.time.Duration
import java.time.Instant


class Alg011(
    p: AlgorithmParams,

    private val fixedDuration: Boolean,
    private val enableShortPauseNodeSpecific: Boolean,
    private val shortPausePercentile: Double, /*  in the range ]0, 1[ */
    private val maxDuration: Duration,

    ) : Algorithm(p) {

    data class Pause(val endNode: Node, val endTime: Instant, val startNode: Node, val startTime: Instant)

    private var lastPosition: Pair<Node, Instant>? = null

    private val pauses = LinkedHashMap<Node, ArrayList<Pause>>()

    override fun onStartTrip(state: WorldState) {

        cancelCallbacks()

        val startNode = state.getClosestNode(p.user)

        if (lastPosition != null) {
            pauses.getOrPut(lastPosition!!.first, ::ArrayList)
                .add(Pause(lastPosition!!.first, lastPosition!!.second, startNode, state.time))
        }
    }

    override fun onEndTrip(state: WorldState) {
        val closestNode = state.getClosestNode(p.user)
        val kg = getKeyGroup(state)

        lastPosition = Pair(closestNode, state.time)

        state.setKeygroupMembers(kg, listOf(Pair(closestNode, Duration.ZERO)))
        registerTimeCallback(state.time.plus(getCutoff(closestNode)), "shortPauseEnd")

    }

    override fun onTime(state: WorldState, value: String) {
        val kg = getKeyGroup(state)
        when (value) {
            "shortPauseEnd" -> {
                state.setKeygroupMembers(kg, listOf())
                return
            }
            else -> throw IllegalArgumentException()
        }
    }


    private fun getCutoff(node: Node, nodeSpecific: Boolean = enableShortPauseNodeSpecific): Duration {
        if (fixedDuration) return maxDuration
        try {
            val z =
                if (!nodeSpecific) pauses.entries.flatMap { it.value }
                else pauses[node].orEmpty()

            val q = z
                .filter { it.endNode == it.startNode }
                .map { Duration.between(it.endTime, it.startTime) }
                .percentile(shortPausePercentile)
            return minOf(q, maxDuration)
        } catch (e: Exception) {
            if (!nodeSpecific) {
                return maxDuration
            }
            return getCutoff(node, nodeSpecific = false)
        }
    }

    override fun computeSize(): Capacity {
        return GraphLayout.parseInstance(pauses).totalSize()
    }

}

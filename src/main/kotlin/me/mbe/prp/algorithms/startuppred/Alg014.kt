package me.mbe.prp.algorithms.startuppred

import me.mbe.prp.algorithms.helpers.FusionTransitionTable
import me.mbe.prp.algorithms.helpers.FusionTransitionTableConfig
import me.mbe.prp.algorithms.helpers.PercentileReducer
import me.mbe.prp.core.*
import java.time.*

class Alg014(
    p: AlgorithmParams,
    private val buffer: Duration,
    percentile: Double,
    fTTC: FusionTransitionTableConfig
) : Algorithm(p) {

    private var lastPosition: Pair<Triple<List<Node>, DayOfWeek, LocalTime>, Instant>? = null

    private val beijingZone: ZoneId = ZoneId.of("Asia/Shanghai")

    private val pauses = FusionTransitionTable(fTTC, 1.0, PercentileReducer(percentile), true)

    override fun onStartTrip(state: WorldState) {
        cancelCallbacks()

        if (lastPosition == null) return

        val startNode = state.getClosestNode(p.user)
        pauses.addTransition(
            lastPosition!!.first,
            startNode,
            duration = Duration.between(lastPosition!!.second, state.time)
        )
    }

    override fun onEndTrip(state: WorldState) {
        val closestNode = state.getClosestNode(p.user)
        val kg = getKeyGroup(state)

        val zonedTimeEnd = state.time.atZone(beijingZone)
        val t = Triple(listOf(closestNode), zonedTimeEnd.dayOfWeek, zonedTimeEnd.toLocalTime())

        lastPosition = Pair(t, state.time)

        val predList = pauses.getNext(t, null)

        if (predList.isEmpty()) {
            state.setKeygroupMembers(kg, listOf())
            return
        }

        val pred = predList[0]

        if (pred.second < buffer && pred.first!! == closestNode) {
            state.setKeygroupMembers(kg, listOf(Pair(pred.first!!, Duration.ZERO)))
            val cancelAt = state.time + pred.second + buffer.dividedBy(2)
            registerTimeCallback(cancelAt, "predictEnd")
        } else {
            state.setKeygroupMembers(kg, listOf())
        }
    }

    override fun onTime(state: WorldState, value: String) {
        val kg = getKeyGroup(state)
        when (value) {
            "predictEnd" -> {
                state.setKeygroupMembers(kg, listOf())
            }
            else -> throw IllegalArgumentException()
        }
    }

    override fun computeSize(): Capacity {
        return pauses.computeSize()
    }

}
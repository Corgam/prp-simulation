package me.mbe.prp.algorithms.nextnodepred

import me.mbe.prp.algorithms.helpers.TransitionTableDurationReducer
import me.mbe.prp.algorithms.helpers_temporal.TemporalFusionTransitionTable
import me.mbe.prp.algorithms.helpers_temporal.TemporalFusionTransitionTableConfig
import me.mbe.prp.core.*
import java.time.*
import java.util.*

val TemporalSetsReducer: TransitionTableDurationReducer = { durations, weight, temporalSets, date ->
    temporalSets!!.getPrediction(date!!)
}

// T-FOOM Algorithm from Emil Balitzki's Bachelor's Thesis
class AlgT012(
    p: AlgorithmParams,
    config: TemporalFusionTransitionTableConfig,
    eP: AlgExtensionBaseParams,
) : AlgExtensionBase(p, eP) {
    private val beijingZone: ZoneId = ZoneId.of("Asia/Shanghai")

    private val transitionTable = TemporalFusionTransitionTable(config, eP.topN, TemporalSetsReducer, storeDuration)

    private var tripStartTimeZoned: ZonedDateTime? = null

    override fun onStartTrip(state: WorldState) {
        lastNodes.clear()
        tripStartTimeZoned = state.time.atZone(beijingZone)
    }


    override fun onNewPosition(state: WorldState) {
        val currentNode = state.getClosestNode(p.user)
        val kg = getKeyGroup(state)

        val correctMembers = LinkedList<Pair<Node,Duration>>()
        correctMembers.add(Pair(currentNode,Duration.ZERO))

        if (lastNodes.isEmpty() || currentNode != lastNodes.last()) {
            var date: ZonedDateTime? = null
            if(lastSwitchTime != Instant.MIN){
                date = lastSwitchTime.atZone(beijingZone)
            }
            // At the start and when the nodes are changing
            transitionTable.addTransition(
                Triple(
                    lastNodes,
                    tripStartTimeZoned!!.dayOfWeek,
                    tripStartTimeZoned!!.toLocalTime()
                ),
                currentNode,
                // In addition to the duration, also pass the date
                duration = Duration.between(lastSwitchTime, state.time),
                date = date
            )
            lastNodes.add(currentNode)
            lastSwitchTime = state.time
        }

        val nextNodes = transitionTable.getNext(
            Triple(
                ArrayList(lastNodes), // shallow copy
                tripStartTimeZoned!!.dayOfWeek,
                tripStartTimeZoned!!.toLocalTime(),
            ),
            state.time.atZone(beijingZone)
        )
        // Check if the loading can be started, or it is too late.
        correctMembers.addAll(getNodesWithinDuration(nextNodes, state))
        state.setKeygroupMembers(kg, correctMembers)
    }

    override fun onEndTrip(state: WorldState) {
        if (eP.nullTransitions) {
            transitionTable.addTransition(
                Triple(
                    lastNodes,
                    tripStartTimeZoned!!.dayOfWeek,
                    tripStartTimeZoned!!.toLocalTime(),
                ),
                null,
                duration = Duration.between(lastSwitchTime, state.time),
                date = lastSwitchTime.atZone(beijingZone)
            )
        }
    }

    override fun printState() {
        println(transitionTable)
    }

    override fun computeSize(): Capacity {
        return transitionTable.computeSize()
    }
}

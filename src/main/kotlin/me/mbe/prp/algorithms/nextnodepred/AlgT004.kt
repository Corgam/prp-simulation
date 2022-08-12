package me.mbe.prp.algorithms.nextnodepred

import me.mbe.prp.algorithms.helpers_temporal.TemporalVOTransitionTable
import me.mbe.prp.core.*
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*


class AlgT004(
    p: AlgorithmParams,
    private val maxDepth: Int,
    private val clearOnStart: Boolean,
    eP: AlgExtensionBaseParams,
    temporalSplit: String,
) : AlgExtensionBase(p, eP) {

    private val beijingZone: ZoneId = ZoneId.of("Asia/Shanghai")
    private val transitionTable = TemporalVOTransitionTable(maxDepth, eP.topN, TemporalSetsReducer, storeDuration, temporalSplit)

    override fun onStartTrip(state: WorldState) {
        if (clearOnStart) lastNodes.clear()
    }

    override fun onNewPosition(state: WorldState) {
        val currentNode = state.getClosestNode(p.user)
        val kg = getKeyGroup(state)

        val correctMembers = LinkedList<Pair<Node,Duration>>()
        correctMembers.add(Pair(currentNode,Duration.ZERO))

        if (lastNodes.isEmpty() || currentNode != lastNodes.last()) {
            // Get the date
            var date: ZonedDateTime? = null
            if(lastSwitchTime != Instant.MIN){
                date = lastSwitchTime.atZone(beijingZone)
            }
            // Add the transition to the table
            transitionTable.addTransition(
                lastNodes,
                currentNode,
                duration = Duration.between(lastSwitchTime, state.time),
                date = date
            )
            lastNodes.add(currentNode)
            lastSwitchTime = state.time
        }

        while (lastNodes.size > maxDepth) { //we dont need a history larger than maxDepth
            lastNodes.removeFirst()
        }

        val nextNodes = transitionTable.getNext(lastNodes, state.time.atZone(beijingZone))

        correctMembers.addAll(getNodesWithinDuration(nextNodes, state))

        state.setKeygroupMembers(kg, correctMembers)
    }

    override fun onEndTrip(state: WorldState) {
        if (eP.nullTransitions) {
            transitionTable.addTransition(
                lastNodes,
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


package me.mbe.prp.algorithms.nextnodepred

import me.mbe.prp.algorithms.helpers.AverageReducer
import me.mbe.prp.algorithms.helpers.FusionTransitionTable
import me.mbe.prp.algorithms.helpers.FusionTransitionTableConfig
import me.mbe.prp.core.*
import java.time.*
import java.util.*

// T-FOOM Algorithm from Emil Balitzki's Bachelor's Thesis
class Alg013(
    p: AlgorithmParams,
    config: FusionTransitionTableConfig,
    eP: AlgExtensionBaseParams,
) : AlgExtensionBase(p, eP) {
    private val beijingZone: ZoneId = ZoneId.of("Asia/Shanghai")

    private val transitionTable = FusionTransitionTable(config, eP.topN, AverageReducer, storeDuration)

    private var tripStartTimeZoned: ZonedDateTime? = null

    override fun onStartTrip(state: WorldState) {
        lastNodes.clear()
        tripStartTimeZoned = state.time.atZone(beijingZone)
    }


    override fun onNewPosition(state: WorldState) {
        val currentNode = state.getClosestNode(p.user)
        val kg = getKeyGroup(state)

        val correctMembers = LinkedList<Node>()
        correctMembers.add(currentNode)

        if (lastNodes.isEmpty() || currentNode != lastNodes.last()) {
            transitionTable.addTransition(
                Triple(
                    lastNodes,
                    tripStartTimeZoned!!.dayOfWeek,
                    tripStartTimeZoned!!.toLocalTime()
                ),
                currentNode,
                duration = Duration.between(lastSwitchTime, state.time),
            )
            lastNodes.add(currentNode)
            lastSwitchTime = state.time
        }

        val nextNodes = transitionTable.getNext(
            Triple(
                ArrayList(lastNodes), // shallow copy
                tripStartTimeZoned!!.dayOfWeek,
                tripStartTimeZoned!!.toLocalTime(),
            )
        )

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
            )
        }
    }

    override fun getNodesWithinDuration(l: List<Pair<Node?, Duration>>, state: WorldState): List<Node> {
        return listOf()
    }

    override fun printState() {
        println(transitionTable)
    }

    override fun computeSize(): Capacity {
        return transitionTable.computeSize()
    }
}

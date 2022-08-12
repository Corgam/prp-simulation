package me.mbe.prp.algorithms.nextnodepred

import me.mbe.prp.algorithms.helpers.AverageReducer
import me.mbe.prp.algorithms.helpers.VOTransitionTable
import me.mbe.prp.core.*
import java.time.Duration
import java.util.*


class Alg004(
    p: AlgorithmParams,
    private val maxDepth: Int,
    private val clearOnStart: Boolean,
    eP: AlgExtensionBaseParams
) : AlgExtensionBase(p, eP) {

    private val transitionTable = VOTransitionTable(maxDepth, eP.topN, AverageReducer, storeDuration)

    override fun onStartTrip(state: WorldState) {
        if (clearOnStart) lastNodes.clear()
    }

    override fun onNewPosition(state: WorldState) {
        val currentNode = state.getClosestNode(p.user)
        val kg = getKeyGroup(state)

        val correctMembers = LinkedList<Pair<Node,Duration>>()
        correctMembers.add(Pair(currentNode,Duration.ZERO))

        if (lastNodes.isEmpty() || currentNode != lastNodes.last()) {
            transitionTable.addTransition(
                lastNodes,
                currentNode,
                duration = Duration.between(lastSwitchTime, state.time)
            )
            lastNodes.add(currentNode)
            lastSwitchTime = state.time
        }

        while (lastNodes.size > maxDepth) { //we dont need a history larger than maxDepth
            lastNodes.removeFirst()
        }

        val nextNodes = transitionTable.getNext(lastNodes, null)

        correctMembers.addAll(getNodesWithinDuration(nextNodes, state))

        state.setKeygroupMembers(kg, correctMembers)
    }

    override fun onEndTrip(state: WorldState) {
        if (eP.nullTransitions) {
            transitionTable.addTransition(lastNodes, null, duration = Duration.between(lastSwitchTime, state.time))
        }
    }

    override fun printState() {
        println(transitionTable)
    }

    override fun computeSize(): Capacity {
        return transitionTable.computeSize()
    }
}


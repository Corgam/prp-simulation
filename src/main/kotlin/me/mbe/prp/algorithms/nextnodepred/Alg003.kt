package me.mbe.prp.algorithms.nextnodepred

import me.mbe.prp.algorithms.helpers.AverageReducer
import me.mbe.prp.algorithms.helpers.TransitionTableImpl
import me.mbe.prp.core.*
import java.time.Duration
import java.util.*

class Alg003(
    p: AlgorithmParams,
    private val noLastNodes: Int,
    private val clearOnStart: Boolean,
    eP: AlgExtensionBaseParams
) : AlgExtensionBase(p, eP) {

    private val transitionTable = TransitionTableImpl<List<Node>, Node?>(eP.topN, AverageReducer, storeDuration)

    override fun onStartTrip(state: WorldState) {
        if (clearOnStart) lastNodes.clear()
    }

    override fun onNewPosition(state: WorldState) {
        val currentNode = state.getClosestNode(p.user)
        val kg = getKeyGroup(state)

        val correctMembers = LinkedList<Pair<Node,Duration>>()
        correctMembers.add(Pair(currentNode,Duration.ZERO))

        if (lastNodes.isEmpty() || currentNode != lastNodes.last()) {
            if (lastNodes.size == noLastNodes) {
                transitionTable.addTransition(
                    ArrayList(lastNodes), /* shallow copy */
                    currentNode,
                    duration = Duration.between(lastSwitchTime, state.time)
                )
            }
            lastNodes.add(currentNode)
            lastSwitchTime = state.time
        }

        while (lastNodes.size > noLastNodes) { //we dont need a history larger than noLastNodes
            lastNodes.removeFirst()
        }

        if (lastNodes.size == noLastNodes) {
            val nextNodes = transitionTable.getNext(lastNodes, null)
            correctMembers.addAll(getNodesWithinDuration(nextNodes, state))
        }

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
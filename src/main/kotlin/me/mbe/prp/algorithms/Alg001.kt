package me.mbe.prp.algorithms

import me.mbe.prp.core.*
import java.time.Duration

class Alg001(p: AlgorithmParams) : Algorithm(p) {
    override fun onNewPosition(state: WorldState) {
        val currentNode = state.getClosestNode(p.user)
        val kg = getKeyGroup(state)
        val nodes = listOf(currentNode).map { Pair(it, Duration.ZERO) }
        state.setKeygroupMembers(kg, nodes)
    }

    override fun computeSize(): Capacity {
        return ZERO_BYTE
    }
}
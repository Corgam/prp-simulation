package me.mbe.prp.algorithms

import me.mbe.prp.core.*
import java.time.Duration

class Alg000(p: AlgorithmParams) : Algorithm(p) {
    override fun onStartTrip(state: WorldState) {
        val kg = getKeyGroup(state)
        val nodes = state.nodes.nodes.map { Pair(it, Duration.ZERO) }
        state.setKeygroupMembers(kg, nodes)
    }

    override fun computeSize(): Capacity {
        return ZERO_BYTE
    }
}
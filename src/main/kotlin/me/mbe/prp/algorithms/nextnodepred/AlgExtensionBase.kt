package me.mbe.prp.algorithms.nextnodepred

import me.mbe.prp.core.Algorithm
import me.mbe.prp.core.AlgorithmParams
import me.mbe.prp.core.Node
import me.mbe.prp.core.WorldState
import java.time.Duration
import java.time.Instant

val MAX_DURATION: Duration = Duration.ofHours(24)

data class AlgExtensionBaseParams(
    val topN: Double,
    val preloadBuffer: Duration,
    val nullTransitions: Boolean,
) {
    override fun toString(): String = "${topN}_${preloadBuffer}_${nullTransitions}"
}

abstract class AlgExtensionBase(p: AlgorithmParams, protected val eP: AlgExtensionBaseParams) : Algorithm(p) {

    protected val storeDuration: Boolean = eP.preloadBuffer < MAX_DURATION

    protected val lastNodes: MutableList<Node> = ArrayList()

    protected var lastSwitchTime: Instant = Instant.MIN

    //todo: maybe use time callback for future load instead of waiting like this
    //todo: use not only the average, but instead the time distribution with percentiles etc.
    protected open fun getNodesWithinDuration(l: List<Pair<Node?, Duration>>, state: WorldState): List<Pair<Node, Duration>> {
        if (!storeDuration) {
            return l.filter { it.first != null }.map { Pair(it.first!!, it.second) }
        }
        val kg = getKeyGroup(state)
        return l.filter { it.first != null }.filter {
            val durationAfterArrivalLoadingMustStart =
                it.second - eP.preloadBuffer - state.network.estimateTransferTime(kg, it.first!!)
            val durationSinceArrival = Duration.between(lastSwitchTime, state.time)
            durationAfterArrivalLoadingMustStart <= durationSinceArrival
        }.map { Pair(it.first!!, it.second) }
    }

}

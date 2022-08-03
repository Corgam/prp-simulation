package me.mbe.prp.algorithms.helpers

import me.mbe.prp.core.Capacity
import me.mbe.prp.core.Node
import org.openjdk.jol.info.GraphLayout
import java.time.Duration
import java.time.ZonedDateTime
import kotlin.math.min

class VOTransitionTable(
    private val maxDepth: Int,
    topN: Double,
    reducer: TransitionTableDurationReducer,
    storeDuration: Boolean
) : TransitionTable<List<Node>, Node?>(topN, reducer, storeDuration) {
    private val transitionTables = LinkedHashMap<Int, TransitionTableImpl<List<Node>, Node?>>()

    init {
        1.rangeTo(maxDepth).forEach { depth ->
            transitionTables[depth] = TransitionTableImpl(topN = topN, reducer = reducer, storeDuration = storeDuration)
        }
    }

    override fun addTransitionInternal(lastNodes: List<Node>, currentNode: Node?, weight: Double, duration: Duration, date: ZonedDateTime?) {
        transitionTables.forEach { (depth, tt) ->
            if (lastNodes.size >= depth) {
                tt.addTransition(
                    lastNodes.takeLast(depth),
                    currentNode,
                    weight,
                    duration
                )
            }
        }
    }

    private fun <T> doWork(fn: (Int, List<Node>) -> List<T>, lastNodes: List<Node>): List<T> {
        var depth = min(maxDepth, lastNodes.size)
        while (depth > 0) {
            val q = fn(depth, lastNodes.takeLast(depth))
            if (q.isNotEmpty()) {
                return q
            }
            depth--
        }
        return emptyList()
    }

    override fun getNextInternal(lastNodes: List<Node>, date: ZonedDateTime?): List<Pair<Node?, Duration>> {
        return doWork({ d, x -> transitionTables[d]!!.getNext(x, date) }, lastNodes)
    }

    override fun getNextWithProbAllInternal(lastNodes: List<Node>, date: ZonedDateTime?): List<Triple<Node?, Duration, Double>> {
        return doWork({ d, x -> transitionTables[d]!!.getNextWithProbAll(x, date) }, lastNodes)
    }

    override fun toString(): String {
        return transitionTables.toString()
    }

    override fun computeSize(): Capacity {
        return GraphLayout.parseInstance(transitionTables).totalSize()
    }
}
package me.mbe.prp.network.backend

import me.mbe.prp.core.Capacity
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.collections.LinkedHashMap

class BackendNetwork(private val simulationInterval: Duration = Duration.ofSeconds(1)) {

    private val simulationIntervalFractionSeconds =
        simulationInterval.toMillis().toDouble() / Duration.ofSeconds(1).toMillis().toDouble()

    private val ongoingTransfersLinks: MutableMap<BackendLink, Int> = LinkedHashMap()
    private val ongoingTransfers: MutableList<Transfer> = LinkedList()

    class Transfer(val item: BackendItem, val via: List<BackendLink>, var progress: Capacity = 0, var extraDuration: Duration)

    val linkFinder = LinkedHashMap<Pair<BackendNode, BackendNode>, BackendLink>()

    fun addNode(
        capacity: Capacity,
        name: String,
        fullyAvailableItems: MutableMap<String, BackendItemWithAnnotation> = LinkedHashMap()
    ): BackendNode {
        return BackendNode(capacity, name, fullyAvailableItems)
    }

    fun addLink(link: BackendLink): BackendLink {
        ongoingTransfersLinks[link] = 0
        linkFinder[Pair(link.nodeA, link.nodeB)] = link
        return link
    }

    fun estimateTransferTime(itemName: String, vararg via: BackendLink): Duration =
        estimateTransferTime(itemName, via.asList())


    fun estimateTransferTime(itemName: String, via: List<BackendLink>): Duration {
        checkValidTransfer(itemName, via)

        val item = via.first().nodeA.fullyAvailableItems[itemName]!!.item
        val minBandwidth = via.minOf { link -> link.bandwidthAB / (ongoingTransfersLinks[link]!! + 1) }

        return Duration.ofMillis((item.size.toDouble() * 1000.0 / minBandwidth.toDouble()).toLong())
    }

    fun advance(currentTime: Instant, time: Duration): List<Transfer> {
        if (time.isNegative) {
            throw IllegalArgumentException("time must be non-negative")
        }

        if (time.isZero) {
            return emptyList()
        }

        val completedTransfers = LinkedList<Transfer>()

        var doneTime = Duration.ZERO
        while (doneTime < time) {
            if (ongoingTransfers.isEmpty()) return completedTransfers
            doneTime += simulationInterval

            ongoingTransfers.forEach { transfer ->
                val minBandwidth = transfer.via
                    .minOf { link -> link.bandwidthAB / ongoingTransfersLinks[link]!! }

                transfer.progress += (minBandwidth * simulationIntervalFractionSeconds).toLong()
            }

            ongoingTransfers.forEach { transfer ->
                if (transfer.progress >= transfer.item.size) {
                    transfer.via.first().nodeA.fullyAvailableItems[transfer.item.name]!!.noTransfersDec()
                    transfer.via.forEach { link ->
                        ongoingTransfersLinks[link] = ongoingTransfersLinks[link]!! - 1
                    }

                    if (transfer.via.last().nodeB.fullyAvailableItems.containsKey(transfer.item.name))
                        throw IllegalStateException()

                    transfer.via.last().nodeB.fullyAvailableItems[transfer.item.name] =
                        BackendItemWithAnnotation(transfer.item, 0, currentTime + doneTime)

                    transfer.via.last().nodeB.reservedCapacity -= transfer.item.size
                    completedTransfers.add(transfer)
                }
            }

            ongoingTransfers.removeAll { transfer -> transfer.progress >= transfer.item.size }

        }
        if (doneTime != time) {
            throw IllegalArgumentException("time must be multiple of simulationInterval")
        }
        return completedTransfers
    }

    private fun checkValidTransfer(itemName: String, via: List<BackendLink>) {
        if (via.isEmpty()) {
            throw IllegalArgumentException("transfer must contain at least one link")
        }

        if (!via[0].nodeA.fullyAvailableItems.containsKey(itemName)) {
            throw IllegalArgumentException("StartNode does not contain item")
        }

        var i = 1
        while (i < via.size) {
            if (via[i - 1].nodeB != via[i].nodeA) {
                throw IllegalArgumentException("nodes not connected properly for this transfer")
            }
            i++
        }

        if (via.last().nodeB.freeCapacity < via.first().nodeA.fullyAvailableItems[itemName]!!.item.size) {
            throw IllegalArgumentException("not enough capacity at destination")
        }
    }

    fun cancelTransfer(t: Transfer?) {
        if (t == null) return

        if (!ongoingTransfers.remove(t)) return

        t.via.first().nodeA.fullyAvailableItems[t.item.name]!!.noTransfersDec()
        t.via.forEach { link ->
            ongoingTransfersLinks[link] = ongoingTransfersLinks[link]!! - 1
        }
        t.via.last().nodeB.reservedCapacity -= t.item.size
    }

    fun transfer(itemName: String, vararg via: BackendLink, duration: Duration): Transfer = transfer(itemName, via.asList(), duration)

    fun transfer(itemName: String, via: List<BackendLink>, extraDuration: Duration): Transfer {
        checkValidTransfer(itemName, via)

        val item = via.first().nodeA.fullyAvailableItems[itemName]!!.item

        via.last().nodeB.reservedCapacity += item.size

        via.forEach {
            ongoingTransfersLinks[it] = ongoingTransfersLinks[it]!! + 1
        }

        val transfer = Transfer(
            via.first().nodeA.fullyAvailableItems[itemName]!!.item,
            via,
            extraDuration = extraDuration
        )

        via.first().nodeA.fullyAvailableItems[itemName]!!.noTransfersInc()

        ongoingTransfers.add(transfer)

        return transfer
    }

    fun delete(name: String, node: BackendNode) {
        if (node.fullyAvailableItems[name] == null) return

        node.deleteItem(name)
    }

}
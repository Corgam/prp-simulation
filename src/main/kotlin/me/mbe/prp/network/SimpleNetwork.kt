package me.mbe.prp.network

import me.mbe.prp.core.*
import java.time.Duration
import java.time.Instant

class SimpleNetwork(
    private val transferTime: Duration,
    override val nodes: NodesGetter,
    override val keygroupSize: Capacity
) : Network {

    override val keyGroups: MutableMap<String, Keygroup> = LinkedHashMap()

    override var time: Instant = Instant.MIN

    override fun estimateTransferTime(kg: Keygroup, dst: Node): Duration {
        return transferTime
    }

    override fun addKeygroupMember(kg: Keygroup, node: Node, duration: Duration) {
        if (kg.members.contains(node.name)) return
        if (kg.size > node.freeCapacity) throw CapacityError()

        node.freeCapacity -= kg.size

        kg.members[node.name] = KeygroupMember(node, time.plus(transferTime).plus(duration))
    }

    override fun deleteKeygroupMember(kg: Keygroup, node: Node) {
        kg.members.remove(node.name) ?: return
        node.freeCapacity += kg.size
    }

    override fun isKeygroupMember(kg: Keygroup, node: Node): Boolean {
        val m = kg.members[node.name] ?: return false
        return !time.isBefore(m.availableFrom)
    }

    override fun addKeygroup(name: String) {
        keyGroups[name] = Keygroup(name, keygroupSize)
    }

}
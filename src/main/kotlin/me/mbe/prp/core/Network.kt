package me.mbe.prp.core

import java.time.Duration
import java.time.Instant

interface Network {
    val nodes: NodesGetter // returns only the edge nodes, these are relevant for the clients

    val keygroupSize: Capacity

    val keyGroups: MutableMap<String, Keygroup>

    var time: Instant

    fun advanceTimeBy(d: Duration) {}

    fun estimateTransferTime(kg: Keygroup, dst: Node): Duration

    fun addKeygroupMember(kg: Keygroup, node: Node, duration: Duration)

    fun deleteKeygroupMember(kg: Keygroup, node: Node)

    fun isKeygroupMember(kg: Keygroup, node: Node): Boolean

    fun getKeygroup(kgName: String): Keygroup {
        return keyGroups[kgName]!!
    }

    fun addKeygroup(name: String)
}
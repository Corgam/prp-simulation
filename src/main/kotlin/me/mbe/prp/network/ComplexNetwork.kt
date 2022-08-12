package me.mbe.prp.network

import me.mbe.prp.core.*
import me.mbe.prp.network.backend.BackendItem
import me.mbe.prp.network.backend.BackendLink
import me.mbe.prp.network.backend.BackendNetwork
import me.mbe.prp.network.backend.BackendNode
import me.mbe.prp.nodes.GridNodeGetter
import java.time.Duration
import java.time.Instant
import kotlin.collections.LinkedHashMap
import kotlin.math.sqrt

class ComplexNetwork : Network {
    private val transferMap = LinkedHashMap<Pair<Keygroup, Node>, BackendNetwork.Transfer?>()

    override val keyGroups: MutableMap<String, Keygroup> = LinkedHashMap()

    override var time: Instant = Instant.MIN

    private val backendNetwork: BackendNetwork

    private val node2node: MutableMap<Node, Pair<BackendNode /* intermediate */, BackendNode /* edge */>>

    private val cloudNode: BackendNode

    override val keygroupSize: Capacity

    override val nodes: GridNodeGetter

    data class BandwidthConfig(
        val cloudInter: Bandwidth,
        val interEdge: Bandwidth,
        val edgeInter: Bandwidth
    )

    constructor(
        nodes: GridNodeGetter,
        bandwidthConfig: BandwidthConfig,
        keygroupSize: Capacity
    ) : this(
        nodes,
        bandwidthConfig,
        sqrt(sqrt(nodes.nodes.size.toDouble())).toInt(),
        sqrt(sqrt(nodes.nodes.size.toDouble())).toInt(),
        keygroupSize,
    )

    constructor(
        nodes: GridNodeGetter,

        bandwidthConfig: BandwidthConfig,

        noGroupsNS: Int,
        noGroupsWE: Int,

        keygroupSize: Capacity,
    ) {
        this.nodes = nodes
        this.keygroupSize = keygroupSize

        val groupSizeNS = nodes.grid.noCentresNS / noGroupsNS
        val groupSizeWE = nodes.grid.noCentresWE / noGroupsWE

        assert(nodes.grid.noCentresNS == noGroupsNS * groupSizeNS)
        assert(nodes.grid.noCentresWE == noGroupsWE * groupSizeWE)

        backendNetwork = BackendNetwork()
        node2node = LinkedHashMap()

        cloudNode = backendNetwork.addNode(TERRA_BYTE, "-----cloud-node-----")

        0.until(noGroupsNS).forEach { groupNS ->
            0.until(noGroupsWE).forEach { groupWE ->
                val intermediateNode = backendNetwork.addNode(ZERO_BYTE, "-----inter-node-----") //just a router
                backendNetwork.addLink(BackendLink(intermediateNode, cloudNode, bandwidthConfig.cloudInter))
                backendNetwork.addLink(BackendLink(cloudNode, intermediateNode, bandwidthConfig.cloudInter))

                0.until(groupSizeNS).forEach { withinGroupIdNS ->
                    0.until(groupSizeWE).forEach { withinGroupIdWE ->

                        val edgeNode = nodes.grid.centresMap[Pair(
                            groupNS * groupSizeNS + withinGroupIdNS,
                            groupWE * groupSizeWE + withinGroupIdWE
                        )]!!

                        val backendNode = backendNetwork.addNode(edgeNode.freeCapacity, edgeNode.name)
                        if (node2node.containsKey(edgeNode)) throw IllegalStateException()
                        node2node[edgeNode] = Pair(intermediateNode, backendNode)

                        backendNetwork.addLink(BackendLink(intermediateNode, backendNode, bandwidthConfig.interEdge))
                        backendNetwork.addLink(BackendLink(backendNode, intermediateNode, bandwidthConfig.edgeInter))

                    }
                }
            }
        }

        if (node2node.size != nodes.nodes.size) throw IllegalStateException()
    }


    override fun addKeygroup(name: String) {
        keyGroups[name] = Keygroup(name, keygroupSize)
        cloudNode.fullyAvailableItems[name] = BackendItem(name, keygroupSize).bit
    }

    override fun advanceTimeBy(d: Duration) {
        val completedTransfers = backendNetwork.advance(time, d)
        completedTransfers.forEach {
            val kgName = it.item.name
            keyGroups[kgName]!!.members[it.via.last().nodeB.name]!!.availableFrom =
                it.via.last().nodeB.fullyAvailableItems[kgName]!!.availableFrom
        }
    }

    override fun estimateTransferTime(kg: Keygroup, dst: Node): Duration {
        return backendNetwork.estimateTransferTime(kg.name, computeRoute(kg, dst))
    }

    private fun computeRoute(kg: Keygroup, dst: Node): List<BackendLink> {
        //todo: also from node to node, not only from cloud (if possible and makes sense)

        val (interNode, edgeNode) = node2node[dst]!!

        val linkCloudInter = backendNetwork.linkFinder[Pair(cloudNode, interNode)]!!
        val linkInterEdge = backendNetwork.linkFinder[Pair(interNode, edgeNode)]!!

        return listOf(linkCloudInter, linkInterEdge)
    }

    override fun addKeygroupMember(kg: Keygroup, node: Node, duration: Duration) {
        if (kg.members.containsKey(node.name)) return

        transferMap[Pair(kg, node)] = backendNetwork.transfer(kg.name, computeRoute(kg, node))
        kg.members[node.name] = KeygroupMember(node, Instant.MAX)
    }

    override fun deleteKeygroupMember(kg: Keygroup, node: Node) {
        backendNetwork.delete(kg.name, node2node[node]!!.second)
        backendNetwork.cancelTransfer(transferMap[Pair(kg, node)])
        kg.members.remove(node.name)
    }

    override fun isKeygroupMember(kg: Keygroup, node: Node): Boolean {
        return node2node[node]!!.second.fullyAvailableItems.containsKey(kg.name)
    }


}
package me.mbe.prp.network.backend

import me.mbe.prp.core.GIGA_BYTE
import me.mbe.prp.core.MEGA_BYTE
import me.mbe.prp.core.TERRA_BYTE
import me.mbe.prp.core.ZERO_BYTE
import me.mbe.prp.network.MEGA_BYTE_PER_SECOND
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant


class TestNetwork {

    @Test
    fun simpleNetwork() {
        val n = BackendNetwork()

        val node1 = n.addNode(
            TERRA_BYTE,
            "node1",
            mapOf(
                BackendItem("1", 100 * MEGA_BYTE).pair,
                BackendItem("2", 200 * MEGA_BYTE).pair
            ).toMutableMap()
        ) // cloud
        val node2 = n.addNode(ZERO_BYTE, "node2") // router / switch
        val node3 = n.addNode(GIGA_BYTE, "node3") // edge node
        val node4 = n.addNode(GIGA_BYTE, "node4") // edge node

        val link_1_2 = n.addLink(BackendLink(node1, node2, 10 * MEGA_BYTE_PER_SECOND))
        val link_2_1 = n.addLink(BackendLink(node2, node1, 10 * MEGA_BYTE_PER_SECOND))

        val link_2_3 = n.addLink(BackendLink(node2, node3, 5 * MEGA_BYTE_PER_SECOND))
        val link_3_2 = n.addLink(BackendLink(node3, node2, 1 * MEGA_BYTE_PER_SECOND))

        val link_2_4 = n.addLink(BackendLink(node2, node4, 5 * MEGA_BYTE_PER_SECOND))
        val link_4_2 = n.addLink(BackendLink(node4, node2, 1 * MEGA_BYTE_PER_SECOND))

        n.advance(Instant.MIN, Duration.ofSeconds(5))

        println(n.estimateTransferTime("1", link_1_2, link_2_3))

        n.transfer("1", listOf(link_1_2, link_2_3), Duration.ZERO)
        n.advance(Instant.MIN, Duration.ofSeconds(5))

        println()

        n.advance(Instant.MIN, Duration.ofSeconds(50))

        println()
    }
}
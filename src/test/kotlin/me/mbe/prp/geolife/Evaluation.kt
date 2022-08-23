package me.mbe.prp.geolife

import me.mbe.prp.algorithms.Alg000
import me.mbe.prp.algorithms.Alg001
import me.mbe.prp.algorithms.helpers.FusionTransitionTableConfig
import me.mbe.prp.algorithms.helpers_temporal.TemporalFusionTransitionTableConfig
import me.mbe.prp.algorithms.nextnodepred.*
import me.mbe.prp.algorithms.startuppred.Alg011
import me.mbe.prp.algorithms.startuppred.Alg013
import me.mbe.prp.algorithms.startuppred.Alg014
import me.mbe.prp.algorithms.startuppred.Alg015
import me.mbe.prp.base.cartesianProduct
import me.mbe.prp.base.concat
import me.mbe.prp.base.pow
import me.mbe.prp.core.*
import me.mbe.prp.metrics.PauseTimeMetricsCollector
import me.mbe.prp.network.ComplexNetwork
import me.mbe.prp.network.MEGA_BYTE_PER_SECOND
import me.mbe.prp.network.SimpleNetwork
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.TestFactory
import java.time.Duration

class Evaluation : EvaluationBase() {

    // override val globalForceRun: Boolean = true

    private val normalBandwidthConfig = ComplexNetwork.BandwidthConfig(
        100 * MEGA_BYTE_PER_SECOND,
        5 * MEGA_BYTE_PER_SECOND,
        1 * MEGA_BYTE_PER_SECOND
    )

    private val highBandwidthConfig = ComplexNetwork.BandwidthConfig(
        100 * MEGA_BYTE_PER_SECOND,
        10 * MEGA_BYTE_PER_SECOND,
        10 * MEGA_BYTE_PER_SECOND
    )

    private val temporalSplits: List<String> = listOf("h","hPER50", "w", "wPER50","m", "mPER50","X", "HWES",
        "HWESuser","HWESnode", "PCTL0","PCTL10","PCTL20","PCTL30","PCTL40","PCTL50","PCTL60","PCTL70","PCTL80","PCTL90","PCTL100")

    override val networkSetups = listOf(10.pow(2), 15.pow(2), 20.pow(2), 25.pow(2), 30.pow(2)).flatMap {
        listOf(
            "simpleNetwork_1ns_${it}Nodes_100MB" to
                    { SimpleNetwork(Duration.ofNanos(1), getGridNodes(it), 100 * MEGA_BYTE) },
            "simpleNetwork_5min_${it}Nodes_100MB" to
                    { SimpleNetwork(Duration.ofMinutes(5), getGridNodes(it), 100 * MEGA_BYTE) },
        )
    }.associate { it }
        .concat(listOf(3.pow(4), 4.pow(4), 5.pow(4)).flatMap {
            listOf(
                "complexNetwork_${it}Nodes_normalBandwidth_100MB" to
                        { ComplexNetwork(getGridNodes(it), normalBandwidthConfig, 100 * MEGA_BYTE) },
                "complexNetwork_${it}Nodes_highBandwidth_100MB" to
                        { ComplexNetwork(getGridNodes(it), highBandwidthConfig, 100 * MEGA_BYTE) },
                "complexNetwork_${it}Nodes_normalBandwidth_1GB" to
                        { ComplexNetwork(getGridNodes(it), normalBandwidthConfig, 1 * GIGA_BYTE) },
                "complexNetwork_${it}Nodes_highBandwidth_1GB" to
                        { ComplexNetwork(getGridNodes(it), highBandwidthConfig, 1 * GIGA_BYTE) },
            )
        }.associate { it })

    private fun defineNextNodePredAlgorithms(): Map<String, AlgorithmPair> {
        val m = LinkedHashMap<String, AlgorithmPair>()

        //baselines
        m["Alg000"] = Pair(::Alg000, null)
        m["Alg001"] = Pair(::Alg001, ::Alg013)

        val aEBPOptions = cartesianProduct(
            ::AlgExtensionBaseParams,
            setOf(0.8, 0.9, 0.95, 0.98, 1.0, 2.0),
            setOf(Duration.ZERO, Duration.ofMinutes(1), Duration.ofMinutes(5), Duration.ofMinutes(10), MAX_DURATION),
            setOf(true, false),
        )

        val clearOnStartOptions = setOf(true, false)
        val noLastNodesOptions = setOf(1, 2, 3, 4, 5)

        //Alg003
        noLastNodesOptions.forEach { noLastNodes ->
            clearOnStartOptions.forEach { clearOnStart ->
                aEBPOptions.forEach { aEBP ->
                    val n = "Alg003_${noLastNodes}_${clearOnStart}_(${aEBP})"
                    m[n] = Pair(
                        { Alg003(it, noLastNodes, clearOnStart, aEBP) },
                        ::Alg013
                    )
                }
            }
        }

        //Alg004
        noLastNodesOptions.forEach { noLastNodes ->
            clearOnStartOptions.forEach { clearOnStart ->
                aEBPOptions.forEach { aEBP ->
                    val n = "Alg004_${noLastNodes}_${clearOnStart}_(${aEBP})"
                    m[n] = Pair(
                        { Alg004(it, noLastNodes, clearOnStart, aEBP) },
                        ::Alg013
                    )
                }
            }
        }

        //AlgT004
        temporalSplits.forEach { temporalSplit ->
            noLastNodesOptions.forEach { noLastNodes ->
                clearOnStartOptions.forEach { clearOnStart ->
                    aEBPOptions.forEach { aEBP ->
                        val n = "AlgT004_${noLastNodes}_${clearOnStart}_(${aEBP})_${temporalSplit}"
                        m[n] = Pair(
                            { AlgT004(it, noLastNodes, clearOnStart, aEBP, temporalSplit) },
                            ::Alg013
                        )
                    }
                }
            }
        }

        //Alg012
        val fTTOptions = cartesianProduct(
            ::FusionTransitionTableConfig,
            noLastNodesOptions,
            setOf(listOf(1), listOf(1, 2), listOf(1, 2, 7)),
            setOf(listOf(1), listOf(1, 4), listOf(1, 4, 12), listOf(1, 4, 24)),
        )

        fTTOptions.forEach { fTT ->
            aEBPOptions.forEach { aEBP ->
                val n = "Alg012_(${fTT})_(${aEBP})"
                m[n] = Pair({ Alg012(it, fTT, aEBP) }, ::Alg013)
            }
        }
        return m
    }


    private fun defineStartupPredAlgorithms(): Map<String, AlgorithmPair> {
        val m = LinkedHashMap<String, AlgorithmPair>()


        listOf(
            Duration.ofMinutes(10),
            Duration.ofMinutes(30),
            Duration.ofMinutes(60)
        ).forEach { maxDuration ->
            listOf(true, false).forEach { fixedDuration ->
                listOf(true, false).forEach { enableShortPauseNodeSpecific ->
                    val eSPNS = enableShortPauseNodeSpecific && !fixedDuration
                    val n = "Alg011_${fixedDuration}_${eSPNS}_${0.5}_${maxDuration}"
                    m[n] = ::Alg001 to {
                        Alg011(
                            it,
                            fixedDuration = fixedDuration,
                            enableShortPauseNodeSpecific = eSPNS,
                            shortPausePercentile = 0.5,
                            maxDuration = maxDuration
                        )
                    }
                }
            }
        }

        listOf(Duration.ofMinutes(30), Duration.ofMinutes(60)).forEach { clusterPartitionTime ->
            val n = "Alg015_${clusterPartitionTime}"
            m[n] = ::Alg001 to {
                Alg015(
                    it,
                    clusterPartitionTime = clusterPartitionTime,
                )
            }
        }


        cartesianProduct(
            ::FusionTransitionTableConfig,
            setOf(1), //must be one
            setOf(listOf(1), listOf(1, 2), listOf(1, 2, 7)),
            setOf(listOf(1), listOf(1, 4), listOf(1, 4, 12), listOf(1, 4, 24))
        ).forEach { fTTC ->
            cartesianProduct<Pair<Duration, Double>>(
                ::Pair,
                setOf(
                    Duration.ofMinutes(10),
                    Duration.ofSeconds(15),
                    Duration.ofMinutes(20),
                    Duration.ofMinutes(25),
                    Duration.ofMinutes(30),
                    Duration.ofMinutes(35)
                ),
                setOf(0.25, 0.5)
            ).forEach { (buffer, percentile) ->
                val n = "Alg014_(${buffer}_${percentile})_(${fTTC})"
                m[n] = ::Alg001 to { Alg014(it, buffer, percentile, fTTC) }
            }
        }
        return m
    }

    override val algorithms = defineNextNodePredAlgorithms()
        .concat(defineStartupPredAlgorithms())
        .concat(defineCombinedAlgorithms())

    private fun defineCombinedAlgorithms(): Map<String, AlgorithmPair> {
        val m = LinkedHashMap<String, AlgorithmPair>()

        m["Alg012_(5_[1, 2, 7]_[1, 4, 24])_(0.9_PT5M_true)_Alg011_true_false_0.5_PT10M"] = Pair(
            {
                Alg012(
                    it,
                    FusionTransitionTableConfig(5, listOf(1, 2, 7), listOf(1, 4, 24)),
                    AlgExtensionBaseParams(0.9, Duration.ofMinutes(5), true)
                )
            },
            {
                Alg011(
                    it,
                    fixedDuration = true,
                    enableShortPauseNodeSpecific = false,
                    shortPausePercentile = 0.5,
                    maxDuration = Duration.ofMinutes(10),
                )
            }
        )

        for (conf in temporalSplits){
            m["AlgT012_(5_[1, 2, 7]_[1, 4, 24]_${conf})_(0.9_PT5M_true)_Alg011_true_false_0.5_PT10M"] = Pair(
                {
                    AlgT012(
                        it,
                        TemporalFusionTransitionTableConfig(5, listOf(1, 2, 7), listOf(1, 4, 24),conf),
                        AlgExtensionBaseParams(0.9, Duration.ofMinutes(5), true)
                    )
                },
                {
                    Alg011(
                        it,
                        fixedDuration = true,
                        enableShortPauseNodeSpecific = false,
                        shortPausePercentile = 0.5,
                        maxDuration = Duration.ofMinutes(10),
                    )
                }
            )
        }
        return m
    }

    override val extraMetrics: Map<String, List<() -> MetricsCollector>> = mapOf(
        "simpleNetwork_5min_100Nodes_100MB/Alg001" to listOf(::PauseTimeMetricsCollector),
    )

    // ------------------------------------------------------------------------------------------------------------ //

    @TestFactory
    fun baseline(): List<DynamicContainer> {
        return generateTests(
            listOf(
                "simpleNetwork_5min_100Nodes_100MB"
            ),
            listOf(
                "Alg000",
                "Alg001",
                "Alg004_5_true_(0.9_PT5M_true)",
                "Alg012_(5_[1, 2, 7]_[1, 4, 24])_(0.9_PT5M_true)_Alg011_true_false_0.5_PT10M",
                ),
        )
    }

    @TestFactory
    fun eval_T_FOMM_basic(): List<DynamicContainer> {
        return generateTests(
            listOf(
                "simpleNetwork_5min_100Nodes_100MB",
            ),
            listOf(
                // A T-F0MM which does nothing, thus should be the same as normal FOMM
                "AlgT012_(5_[1, 2, 7]_[1, 4, 24]_X)_(0.9_PT5M_true)_Alg011_true_false_0.5_PT10M",
            ),
        )
    }
    @TestFactory
    fun eval_T_FOMM_D(): List<DynamicContainer> {
        return generateTests(
            listOf(
                "simpleNetwork_5min_100Nodes_100MB",
            ),
            listOf(
                // Discretized values
                "AlgT012_(5_[1, 2, 7]_[1, 4, 24]_h)_(0.9_PT5M_true)_Alg011_true_false_0.5_PT10M",
                "AlgT012_(5_[1, 2, 7]_[1, 4, 24]_m)_(0.9_PT5M_true)_Alg011_true_false_0.5_PT10M",
                "AlgT012_(5_[1, 2, 7]_[1, 4, 24]_w)_(0.9_PT5M_true)_Alg011_true_false_0.5_PT10M",
                "AlgT012_(5_[1, 2, 7]_[1, 4, 24]_hPER50)_(0.9_PT5M_true)_Alg011_true_false_0.5_PT10M",
                "AlgT012_(5_[1, 2, 7]_[1, 4, 24]_mPER50)_(0.9_PT5M_true)_Alg011_true_false_0.5_PT10M",
                "AlgT012_(5_[1, 2, 7]_[1, 4, 24]_wPER50)_(0.9_PT5M_true)_Alg011_true_false_0.5_PT10M",
            ),
        )
    }
    @TestFactory
    fun eval_T_FOMM_HWES(): List<DynamicContainer> {
        return generateTests(
            listOf(
                "simpleNetwork_5min_100Nodes_100MB",
            ),
            listOf(
                // HWES
                "AlgT012_(5_[1, 2, 7]_[1, 4, 24]_HWESuser)_(0.9_PT5M_true)_Alg011_true_false_0.5_PT10M",
                "AlgT012_(5_[1, 2, 7]_[1, 4, 24]_HWESnode)_(0.9_PT5M_true)_Alg011_true_false_0.5_PT10M",
                "AlgT012_(5_[1, 2, 7]_[1, 4, 24]_HWES)_(0.9_PT5M_true)_Alg011_true_false_0.5_PT10M",
            ),
        )
    }

    @TestFactory
    fun eval_T_FOMM_PCTL(): List<DynamicContainer> {
        return generateTests(
            listOf(
                "simpleNetwork_5min_100Nodes_100MB",
            ),
            listOf("0","10","20","30","40","50","60","70","80","90","100")
                .map { "AlgT012_(5_[1, 2, 7]_[1, 4, 24]_PCTL${it})_(0.9_PT5M_true)_Alg011_true_false_0.5_PT10M" }
        )
     }

// Uncomment to run the complex network tests
//    @TestFactory
//    fun eval008(): List<DynamicContainer> {
//        return generateTests(
//            listOf(
//                "complexNetwork_81Nodes_normalBandwidth_1GB",
//                "complexNetwork_256Nodes_normalBandwidth_1GB",
//                "complexNetwork_625Nodes_normalBandwidth_1GB",
//            ),
//            listOf(
//                "Alg001",
//                "Alg012_(5_[1, 2, 7]_[1, 4, 24])_(0.9_PT24H_true)",
//            )
//        )
//    }


}


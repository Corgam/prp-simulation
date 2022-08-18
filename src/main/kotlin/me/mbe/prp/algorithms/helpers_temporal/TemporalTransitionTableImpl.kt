package me.mbe.prp.algorithms.helpers_temporal

import me.mbe.prp.algorithms.helpers.TransitionTable
import me.mbe.prp.algorithms.helpers.TransitionTableDurationReducer
import me.mbe.prp.core.Capacity
import org.openjdk.jol.info.GraphLayout
import java.time.Duration
import java.time.ZonedDateTime
import java.util.*


class TemporalSets(private var temporalSplit: String){
    private val monthsSet: LinkedHashMap<Int, ArrayList<Duration>> = LinkedHashMap()
    private val weekDaySet: LinkedHashMap<Int, ArrayList<Duration>> = LinkedHashMap()
    private val hoursSet: LinkedHashMap<Int, ArrayList<Duration>> = LinkedHashMap()
    private var allDurations: ArrayList<Duration> = ArrayList()
    private var allDurationsLong: ArrayList<Long> = ArrayList()
    private var totalDuration: Long = 0
    private var numberOfDurations: Int = 0
    private var lastAddedStartingDate: ZonedDateTime? = null

    fun addDuration(duration: Duration, date: ZonedDateTime){
        // Month
        if (monthsSet[date.month.value] == null){
            monthsSet[date.month.value] = ArrayList()
        }
        monthsSet[date.month.value]?.add(duration)
        // Day of the week
        if (weekDaySet[date.dayOfWeek.value] == null){
            weekDaySet[date.dayOfWeek.value] = ArrayList()
        }
        weekDaySet[date.dayOfWeek.value]?.add(duration)
        // Hours
        if (hoursSet[date.hour] == null){
            hoursSet[date.hour] = ArrayList()
        }
        hoursSet[date.hour]?.add(duration)
        // HWES data
        allDurationsLong.add(duration.seconds)
        lastAddedStartingDate = date
        // Total average
        allDurations.add(duration)
        totalDuration += duration.seconds
        numberOfDurations += 1
    }
    fun getPrediction(date: ZonedDateTime): Duration {
        // Percentiles
        if(temporalSplit.contains("PCTL")){
            val percentile: Double = temporalSplit.replace("PCTL","").toDouble()
            if (percentile == 50.0){
                return Duration.ofSeconds(median(allDurations))
            }
            return Duration.ofSeconds(percentile(percentile, allDurationsLong))
        }
        // Holt Winter’s Exponential Smoothing
        if(temporalSplit.contains("HWES")){
            val array = allDurationsLong.toLongArray()
            val currentPeriod: Int = allDurationsLong.size/2
            if (currentPeriod <= 0){
                return Duration.ofSeconds(totalDuration / numberOfDurations)
            }
            val prediction: DoubleArray = HoltWinters.forecast(array, 0.06, 0.98, 0.48, currentPeriod, currentPeriod)
            // Select the next predicted duration
            var duration = 0.0
            if (lastAddedStartingDate != null){
                val totalDifference = Duration.between(lastAddedStartingDate, date).seconds
                var currentDifference = 0.0
                var i = 0
                // Move to the proper time stamp
                while(currentDifference < totalDifference){
                    if (i >= prediction.size){
                        break
                    }
                    val predictedDuration = prediction[i]
                    if (predictedDuration > 0.0){
                        currentDifference += predictedDuration
                    }
                    i++
                }
                // Get the final value
                if (prediction.isNotEmpty() && i > 0) {
                    duration = prediction[i - 1]
                }
            }
            if (duration == 0.0 || duration.isNaN()){
                return Duration.ofSeconds(totalDuration / numberOfDurations)
            }
            return Duration.ofSeconds(duration.toLong())
        }
        var finalDuration: Long = totalDuration / numberOfDurations
        // Discretized Values
        var set: LinkedHashMap<Int, ArrayList<Duration>>? = null
        var value: Int? = null
        var maxRightValue: Int? = null
        if(temporalSplit.contains("m")) {
            set = monthsSet
            value = date.month.value
            maxRightValue = 12
        }else if(temporalSplit.contains("w")){
            set = weekDaySet
            value = date.dayOfWeek.value
            maxRightValue = 7
        }else if(temporalSplit.contains("h")){
            set = hoursSet
            value = date.hour
            maxRightValue = 24
        }
        if (set != null && value != null && maxRightValue != null){
            var neighboursValue: Long = -1
            if (temporalSplit.contains("PER")) {
                val perc = temporalSplit.drop(4).toDouble()
                if (set[value] != null) {
                    if (perc == 50.0){
                        finalDuration = median(set[value]!!)
                    }else{
                        val durs = set[value]!!.map { it.seconds }
                        finalDuration = percentile(perc, durs as ArrayList<Long>)
                    }
                }else{
                    neighboursValue = findClosestNeighboursPercentile(set, value, maxRightValue, perc)
                    if (neighboursValue.toInt() != -1) {
                        finalDuration = neighboursValue
                    }
                }
            } else {
                if (set[value] != null) {
                    finalDuration = set[value]!!.sumOf{it.seconds} / set[value]!!.size
                }else {
                    neighboursValue = findClosestNeighboursAverage(set, value, maxRightValue)
                    if (neighboursValue.toInt() != -1) {
                        finalDuration = neighboursValue
                    }
                }
            }

        }
        // Return the final value
        return Duration.ofSeconds(finalDuration)
    }

    private fun median(list: java.util.ArrayList<Duration>): Long = list.sorted().let {
        // Source (modified): https://stackoverflow.com/questions/54187695/median-calculation-in-kotlin
        if (it.size % 2 == 0)
            (it[it.size / 2].seconds + it[(it.size - 1) / 2].seconds) / 2
        else
            it[it.size / 2].seconds
    }

    private fun findClosestNeighboursAverage(set: LinkedHashMap<Int, ArrayList<Duration>>, startingValue: Int, maxRightValue: Int): Long {
        // Find left neighbour
        var i: Int = startingValue
        var leftNeighbour: Long = -1
        while (i >= 0){
            if (set.containsKey(i)){
                leftNeighbour = set[i]!!.sumOf{it.seconds} / set[i]!!.size
                break
            }
            i--
        }
        // Find right neighbour
        i = startingValue
        var rightNeighbour: Long = -1
        while (i <= maxRightValue){
            if (set.containsKey(i)){
                rightNeighbour = set[i]!!.sumOf{it.seconds} / set[i]!!.size
                break
            }
            i++
        }
        // Returns
        return if(leftNeighbour.toInt() == -1 && rightNeighbour.toInt() == -1) {
            -1
        }else if (leftNeighbour.toInt() == -1){
            rightNeighbour
        }else if(rightNeighbour.toInt() == -1){
            leftNeighbour
        }else {
            ((leftNeighbour + rightNeighbour) / 2)
        }
    }
    fun percentile(percentile: Double, items: ArrayList<Long>): Long {
        items.sort()
        return items[Math.round(percentile / 100.0 * (items.size - 1)).toInt()]
    }
    private fun findClosestNeighboursPercentile(set: LinkedHashMap<Int, ArrayList<Duration>>, startingValue: Int, maxRightValue: Int, percentile: Double): Long {
        // Find left neighbour
        var i: Int = startingValue
        var leftNeighbour: Long = -1
        while (i > 0){
            i--
            if (set.containsKey(i)){
                val durs = set[i]!!.map { it.seconds }
                if (percentile == 50.0){
                    leftNeighbour = median(set[i]!!)
                }else {
                    leftNeighbour = percentile(percentile, durs as ArrayList<Long>)
                }
                break
            }
        }
        // Find right neighbour
        i = startingValue
        var rightNeighbour: Long = -1
        while (i < maxRightValue){
            i++
            if (set.containsKey(i)){
                val durs = set[i]!!.map { it.seconds }
                if (percentile == 50.0){
                    rightNeighbour = median(set[i]!!)
                }else {
                    rightNeighbour = percentile(percentile, durs as ArrayList<Long>)
                }
                break
            }
        }
        // Returns
        return if(leftNeighbour.toInt() == -1 && rightNeighbour.toInt() == -1) {
            -1
        }else if (leftNeighbour.toInt() == -1){
            rightNeighbour
        }else if(rightNeighbour.toInt() == -1){
            leftNeighbour
        }else {
            ((leftNeighbour + rightNeighbour) / 2)
        }
    }
}

class TemporalTransitionTableImpl<K, L>(
    topN: Double,
    reducer: TransitionTableDurationReducer,
    storeDuration: Boolean,
    private val temporalSplit: String
) : TransitionTable<K, L>(topN, reducer, storeDuration) {

    private val map = LinkedHashMap<K, LinkedHashMap<L, Pair<Double, TemporalSets>>>()

    override fun addTransitionInternal(from: K, to: L, weight: Double, duration: Duration, date: ZonedDateTime?) {
        val f = map.getOrPut(from, ::LinkedHashMap)
        val v = f.getOrDefault(to, Pair(0.0, TemporalSets(temporalSplit)))
        // Add the duration and the date
        if (storeDuration && date != null){
            v.second.addDuration(duration, date)
        }
        f[to] = Pair(v.first + weight, v.second)
    }

    override fun getNextWithProbAllInternal(from: K, date: ZonedDateTime?): List<Triple<L, Duration, Double>> {
        val e = map[from]?.entries ?: return emptyList()
        val s = e.sumOf { it.value.first }

        return if (storeDuration)
            e.map { Triple(it.key, reducer(emptyList(), 0.0, it.value.second, date), it.value.first / s) }
        else
            e.map { Triple(it.key, Duration.ZERO, it.value.first / s) }
    }

    override fun toString(): String {
        return map.toString()
    }

    override fun computeSize(): Capacity {
        return GraphLayout.parseInstance(map).totalSize()
    }
}
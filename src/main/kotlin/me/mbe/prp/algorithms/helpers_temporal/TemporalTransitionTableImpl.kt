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
        if(temporalSplit.contains("PER")){
            return Duration.ofSeconds(percentile(100.0, allDurationsLong))
        }
        if(temporalSplit.contains("HWES")){
            val array = allDurationsLong.toLongArray()
            val currentPeriod: Int = allDurationsLong.size/2
            if (currentPeriod <= 0){
                return Duration.ofSeconds(totalDuration / numberOfDurations)
            }
            val prediction: DoubleArray = HoltWinters.forecast(array, 0.06, 0.98, 0.48, currentPeriod, currentPeriod)
            // Select the next predicted duration
            var duration = 0.0
            if(temporalSplit.contains("HWESbreaks")){
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
            }else{
                for(item in prediction){
                    if(item > 0.0){
                        duration = item
                        break
                    }
                }
            }
            if (duration == 0.0 || duration.isNaN()){
                return Duration.ofSeconds(totalDuration / numberOfDurations)
            }
            return Duration.ofSeconds(duration.toLong())
        }
        if(temporalSplit.contains("MEDIAN")){
            return Duration.ofSeconds(median(allDurations))
        }
        var finalDuration: Long = totalDuration / numberOfDurations
        // Months
        if(temporalSplit.contains("m")){
            if(temporalSplit.contains("M")){
                if (monthsSet[date.month.value] != null) {
                    finalDuration = median(monthsSet[date.month.value]!!)
                } else if (temporalSplit.contains("N")) {
                    val neighboursValue = findClosestNeighboursMedian(monthsSet, date.month.value, 12)
                    if (neighboursValue.toInt() != -1) {
                        finalDuration = neighboursValue
                    }
                }
            }else {
                if (monthsSet[date.month.value] != null) {
                    finalDuration = monthsSet[date.month.value]!!.sumOf { it.seconds } / monthsSet[date.month.value]!!.size
                } else if (temporalSplit.contains("N")) {
                    val neighboursValue = findClosestNeighboursAverage(monthsSet, date.month.value, 12)
                    if (neighboursValue.toInt() != -1) {
                        finalDuration = neighboursValue
                    }
                }
            }
        }
        // Weeks of the day
        if(temporalSplit.contains("w")) {
            if(temporalSplit.contains("M")){
                if (weekDaySet[date.dayOfWeek.value] != null) {
                    finalDuration = median(weekDaySet[date.dayOfWeek.value]!!)
                }else if(temporalSplit.contains("N")){
                    val neighboursValue = findClosestNeighboursMedian(weekDaySet, date.dayOfWeek.value, 7)
                    if (neighboursValue.toInt() != -1){
                        finalDuration = neighboursValue
                    }
                }
            }else{
                if (weekDaySet[date.dayOfWeek.value] != null) {
                    finalDuration = weekDaySet[date.dayOfWeek.value]!!.sumOf { it.seconds } / weekDaySet[date.dayOfWeek.value]!!.size
                }else if(temporalSplit.contains("N")){
                    val neighboursValue = findClosestNeighboursAverage(weekDaySet, date.dayOfWeek.value, 7)
                    if (neighboursValue.toInt() != -1){
                        finalDuration = neighboursValue
                    }
                }
            }
        }
        // Hours
        if(temporalSplit.contains("h")) {
            if(temporalSplit.contains("M")){
                if (hoursSet[date.hour] != null) {
                    finalDuration = median(hoursSet[date.hour]!!)
                } else if (temporalSplit.contains("N")) {
                    val neighboursValue = findClosestNeighboursMedian(hoursSet, date.hour, 24)
                    if (neighboursValue.toInt() != -1) {
                        finalDuration = neighboursValue
                    }
                }
            }else {
                if (hoursSet[date.hour] != null) {
                    finalDuration = hoursSet[date.hour]!!.sumOf { it.seconds } / hoursSet[date.hour]!!.size
                } else if (temporalSplit.contains("N")) {
                    val neighboursValue = findClosestNeighboursAverage(hoursSet, date.hour, 24)
                    if (neighboursValue.toInt() != -1) {
                        finalDuration = neighboursValue
                    }
                }
            }
        }
        // All
        if(temporalSplit.contains("a")) {
            if(temporalSplit.contains("N")){
                var monthDuration: Long = totalDuration / numberOfDurations
                var weekDuration: Long = totalDuration / numberOfDurations
                var hourDuration: Long = totalDuration / numberOfDurations
                if (monthsSet[date.month.value] != null) {
                    monthDuration = monthsSet[date.month.value]!!.sumOf { it.seconds } / monthsSet[date.month.value]!!.size
                }else{
                    val neighboursValue = findClosestNeighboursAverage(monthsSet, date.month.value, 12)
                    if (neighboursValue.toInt() != -1){
                        monthDuration = neighboursValue
                    }
                }
                if (weekDaySet[date.dayOfWeek.value] != null) {
                    weekDuration = weekDaySet[date.dayOfWeek.value]!!.sumOf { it.seconds } / weekDaySet[date.dayOfWeek.value]!!.size
                }else{
                    val neighboursValue = findClosestNeighboursAverage(weekDaySet, date.dayOfWeek.value, 7)
                    if (neighboursValue.toInt() != -1){
                        weekDuration = neighboursValue
                    }
                }
                if (hoursSet[date.hour] != null) {
                    hourDuration = hoursSet[date.hour]!!.sumOf { it.seconds } / hoursSet[date.hour]!!.size
                }else{
                    val neighboursValue = findClosestNeighboursAverage(hoursSet, date.hour, 24)
                    if (neighboursValue.toInt() != -1){
                        hourDuration = neighboursValue
                    }
                }
                finalDuration = (monthDuration * 0.2 + weekDuration * 0.2 + hourDuration * 0.6).toLong()
            }else{
                var monthDuration: Long = totalDuration / numberOfDurations
                var weekDuration: Long = totalDuration / numberOfDurations
                var hourDuration: Long = totalDuration / numberOfDurations
                if (monthsSet[date.month.value] != null) {
                    monthDuration =
                        monthsSet[date.month.value]!!.sumOf { it.seconds } / monthsSet[date.month.value]!!.size
                }
                if (weekDaySet[date.dayOfWeek.value] != null) {
                    weekDuration =
                        weekDaySet[date.dayOfWeek.value]!!.sumOf { it.seconds } / weekDaySet[date.dayOfWeek.value]!!.size
                }
                if (hoursSet[date.hour] != null) {
                    hourDuration = hoursSet[date.hour]!!.sumOf { it.seconds } / hoursSet[date.hour]!!.size
                }
                finalDuration = (monthDuration * 0.2 + weekDuration * 0.3 + hourDuration * 0.5).toLong()
            }
        }
        // Return the final value
        return Duration.ofSeconds(finalDuration)
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
    private fun median(list: java.util.ArrayList<Duration>): Long = list.sorted().let {
        // Source (modified): https://stackoverflow.com/questions/54187695/median-calculation-in-kotlin
        if (it.size % 2 == 0)
            (it[it.size / 2].seconds + it[(it.size - 1) / 2].seconds) / 2
        else
            it[it.size / 2].seconds
    }

    fun percentile(percentile: Double, items: ArrayList<Long>): Long {
        items.sort()
        return items[Math.round(percentile / 100.0 * (items.size - 1)).toInt()]
    }
    private fun findClosestNeighboursMedian(set: LinkedHashMap<Int, ArrayList<Duration>>, startingValue: Int, maxRightValue: Int): Long {
        // Find left neighbour
        var i: Int = startingValue
        var leftNeighbour: Long = -1
        while (i >= 0){
            if (set.containsKey(i)){
                leftNeighbour = median(set[i]!!)
                break
            }
            i--
        }
        // Find right neighbour
        i = startingValue
        var rightNeighbour: Long = -1
        while (i <= maxRightValue){
            if (set.containsKey(i)){
                rightNeighbour = median(set[i]!!)
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
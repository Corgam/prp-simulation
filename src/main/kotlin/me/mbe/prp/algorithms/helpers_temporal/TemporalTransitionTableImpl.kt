package me.mbe.prp.algorithms.helpers_temporal

import me.mbe.prp.algorithms.helpers.TransitionTable
import me.mbe.prp.algorithms.helpers.TransitionTableDurationReducer
import me.mbe.prp.core.Capacity
import org.openjdk.jol.info.GraphLayout
import java.time.DayOfWeek
import java.time.Duration
import java.time.Month
import java.time.ZonedDateTime

class TemporalSets(private var temporalSplit: String){
    private val monthsSet: LinkedHashMap<Month, ArrayList<Duration>> = LinkedHashMap()
    private val weekDaySet: LinkedHashMap<DayOfWeek, ArrayList<Duration>> = LinkedHashMap()
    private val hoursSet: LinkedHashMap<Int, ArrayList<Duration>> = LinkedHashMap()
    private var totalDuration: Long = 0
    private var numberOfDurations: Int = 0

    fun addDuration(duration: Duration, date: ZonedDateTime){
        // Month
        if (monthsSet[date.month] == null){
            monthsSet[date.month] = ArrayList()
        }
        monthsSet[date.month]?.add(duration)
        // Day of the week
        if (weekDaySet[date.dayOfWeek] == null){
            weekDaySet[date.dayOfWeek] = ArrayList()
        }
        weekDaySet[date.dayOfWeek]?.add(duration)
        // Hours
        if (hoursSet[date.hour] == null){
            hoursSet[date.hour] = ArrayList()
        }
        hoursSet[date.hour]?.add(duration)
        // Total average
        totalDuration += duration.seconds
        numberOfDurations += 1
    }
    fun getPrediction(date: ZonedDateTime): Duration {
        var finalDuration: Long = totalDuration / numberOfDurations
        // Months
        if(temporalSplit == "m") {
            if (monthsSet[date.month] != null) {
                finalDuration = monthsSet[date.month]!!.sumOf { it.seconds } / monthsSet[date.month]!!.size
            }
        }
        // Weeks of the day
        if(temporalSplit == "w") {
            if (weekDaySet[date.dayOfWeek] != null) {
                finalDuration = weekDaySet[date.dayOfWeek]!!.sumOf { it.seconds } / weekDaySet[date.dayOfWeek]!!.size
            }
        }
        // Hours
        if(temporalSplit == "h") {
            if (hoursSet[date.hour] != null) {
                finalDuration = hoursSet[date.hour]!!.sumOf { it.seconds } / hoursSet[date.hour]!!.size
            }
        }
        // All
        if(temporalSplit == "a") {
            var monthDuration: Long = totalDuration / numberOfDurations
            var weekDuration: Long = totalDuration / numberOfDurations
            var hourDuration: Long = totalDuration / numberOfDurations
            if (monthsSet[date.month] != null) {
                monthDuration = monthsSet[date.month]!!.sumOf { it.seconds } / monthsSet[date.month]!!.size
            }
            if (weekDaySet[date.dayOfWeek] != null) {
                weekDuration = weekDaySet[date.dayOfWeek]!!.sumOf { it.seconds } / weekDaySet[date.dayOfWeek]!!.size
            }
            if (hoursSet[date.hour] != null) {
                hourDuration = hoursSet[date.hour]!!.sumOf { it.seconds } / hoursSet[date.hour]!!.size
            }
            finalDuration = (monthDuration * 0.2 + weekDuration * 0.3 + hourDuration * 0.5).toLong()
        }
        return Duration.ofSeconds(finalDuration)
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
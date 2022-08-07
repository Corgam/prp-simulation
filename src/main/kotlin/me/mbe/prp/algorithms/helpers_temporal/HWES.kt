package me.mbe.prp.algorithms.helpers_temporal
/**
 * Copyright 2011 Nishant Chandra <nishant.chandra at gmail dot com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
</nishant.chandra> */
/**
 * Given a time series, say a complete monthly data for 12 months, the
 * Holt-Winters smoothing and forecasting technique is built on the following
 * formulae (multiplicative version):
 *
 * St[i] = alpha * y[i] / It[i - period] + (1.0 - alpha) * (St[i - 1] + Bt[i - 1])
 * Bt[i] = gamma * (St[i] - St[i - 1]) + (1 - gamma) * Bt[i - 1]
 * It[i] = beta * y[i] / St[i] + (1.0 - beta) * It[i - period]
 * Ft[i + m] = (St[i] + (m * Bt[i])) * It[i - period + m]
 *
 * Note: Many authors suggest calculating initial values of St, Bt and It in a
 * variety of ways, but some of them are incorrect e.g. determination of It
 * parameter using regression. This implementation uses the NIST recommended methods.
 *
 * For more details, see: http://adorio-research.org/wordpress/?p=1230
 * http://www.itl.nist.gov/div898/handbook/pmc/section4/pmc435.htm
 *
 * @author Nishant Chandra
 */
object HoltWinters {
    /**
     * This method is the entry point. It calculates the initial values and
     * returns the forecast for the future m periods.
     *
     * @param y - Time series data.
     * @param alpha - Exponential smoothing coefficients for level, trend,
     * seasonal components.
     * @param beta - Exponential smoothing coefficients for level, trend,
     * seasonal components.
     * @param gamma - Exponential smoothing coefficients for level, trend,
     * seasonal components.
     * @param period - A complete season's data consists of L periods. And we need
     * to estimate the trend factor from one period to the next. To
     * accomplish this, it is advisable to use two complete seasons;
     * that is, 2L periods.
     * @param m - Extrapolated future data points.
     * - 4 quarterly,
     * - 7 weekly,
     * - 12 monthly
     *
     * @param debug - Print debug values. Useful for testing.
     */
    @JvmOverloads
    fun forecast(
        y: LongArray, alpha: Double, beta: Double,
        gamma: Double, period: Int, m: Int, debug: Boolean = false
    ): DoubleArray {
        validateArguments(y, alpha, beta, gamma, period, m)
        val seasons = y.size / period
        val a0 = calculateInitialLevel(y)
        val b0 = calculateInitialTrend(y, period)
        val initialSeasonalIndices = calculateSeasonalIndices(
            y, period,
            seasons
        )
        if (debug) {
            println(
                String.format(
                    "Total observations: %d, Seasons %d, Periods %d", y.size,
                    seasons, period
                )
            )
            println("Initial level value a0: $a0")
            println("Initial trend value b0: $b0")
            printArray("Seasonal Indices: ", initialSeasonalIndices)
        }
        val forecast = calculateHoltWinters(
            y, a0, b0, alpha, beta, gamma,
            initialSeasonalIndices, period, m, debug
        )
        if (debug) {
            printArray("Forecast", forecast)
        }
        return forecast
    }

    /**
     * Validate input.
     *
     * @param y
     * @param alpha
     * @param beta
     * @param gamma
     * @param m
     */
    private fun validateArguments(
        y: LongArray?, alpha: Double, beta: Double,
        gamma: Double, period: Int, m: Int
    ) {
        requireNotNull(y) { "Value of y should be not null" }
        require(m > 0) { "Value of m must be greater than 0." }
        require(m <= period) { "Value of m must be <= period." }
        require(!(alpha < 0.0 || alpha > 1.0)) { "Value of Alpha should satisfy 0.0 <= alpha <= 1.0" }
        require(!(beta < 0.0 || beta > 1.0)) { "Value of Beta should satisfy 0.0 <= beta <= 1.0" }
        require(!(gamma < 0.0 || gamma > 1.0)) { "Value of Gamma should satisfy 0.0 <= gamma <= 1.0" }
    }

    /**
     * This method realizes the Holt-Winters equations.
     *
     * @param y
     * @param a0
     * @param b0
     * @param alpha
     * @param beta
     * @param gamma
     * @param initialSeasonalIndices
     * @param period
     * @param m
     * @param debug
     * @return - Forecast for m periods.
     */
    private fun calculateHoltWinters(
        y: LongArray, a0: Double, b0: Double,
        alpha: Double, beta: Double, gamma: Double,
        initialSeasonalIndices: DoubleArray, period: Int, m: Int, debug: Boolean
    ): DoubleArray {
        val St = DoubleArray(y.size)
        val Bt = DoubleArray(y.size)
        val It = DoubleArray(y.size)
        val Ft = DoubleArray(y.size + m)

        // Initialize base values
        St[1] = a0
        Bt[1] = b0
        for (i in 0 until period) {
            It[i] = initialSeasonalIndices[i]
        }

        // Start calculations
        for (i in 2 until y.size) {

            // Calculate overall smoothing
            if (i - period >= 0) {
                St[i] = alpha * y[i] / It[i - period] + (1.0 - alpha) * St[i - 1] + Bt[i - 1]
            } else {
                St[i] = alpha * y[i] + (1.0 - alpha) * (St[i - 1] + Bt[i - 1])
            }

            // Calculate trend smoothing
            Bt[i] = gamma * (St[i] - St[i - 1]) + (1 - gamma) * Bt[i - 1]

            // Calculate seasonal smoothing
            if (i - period >= 0) {
                It[i] = beta * y[i] / St[i] + (1.0 - beta) * It[i - period]
            }

            // Calculate forecast
            if (i + m >= period) {
                Ft[i + m] = (St[i] + m * Bt[i]) * It[i - period + m]
            }
            if (debug) {
                println(
                    String.format(
                        "i = %d, y = %d, S = %f, Bt = %f, It = %f, F = %f", i,
                        y[i], St[i], Bt[i], It[i], Ft[i]
                    )
                )
            }
        }
        return Ft
    }

    /**
     * See: http://robjhyndman.com/researchtips/hw-initialization/ 1st period's
     * average can be taken. But y[0] works better.
     *
     * @return - Initial Level value i.e. St[1]
     */
    private fun calculateInitialLevel(y: LongArray): Double {
        return y[0].toDouble()
    }

    /**
     * See: http://www.itl.nist.gov/div898/handbook/pmc/section4/pmc435.htm
     *
     * @return - Initial trend - Bt[1]
     */
    private fun calculateInitialTrend(y: LongArray, period: Int): Double {
        var sum = 0.0
        for (i in 0 until period) {
            sum += (y[period + i] - y[i]).toDouble()
        }
        return sum / (period * period)
    }

    /**
     * See: http://www.itl.nist.gov/div898/handbook/pmc/section4/pmc435.htm
     *
     * @return - Seasonal Indices.
     */
    private fun calculateSeasonalIndices(
        y: LongArray, period: Int,
        seasons: Int
    ): DoubleArray {
        val seasonalAverage = DoubleArray(seasons)
        val seasonalIndices = DoubleArray(period)
        val averagedObservations = DoubleArray(y.size)
        for (i in 0 until seasons) {
            for (j in 0 until period) {
                seasonalAverage[i] += y[i * period + j].toDouble()
            }
            seasonalAverage[i] /= period.toDouble()
        }
        for (i in 0 until seasons) {
            for (j in 0 until period) {
                averagedObservations[i * period + j] = (y[i * period + j]
                        / seasonalAverage[i])
            }
        }
        for (i in 0 until period) {
            for (j in 0 until seasons) {
                seasonalIndices[i] += averagedObservations[j * period + i]
            }
            seasonalIndices[i] /= seasons.toDouble()
        }
        return seasonalIndices
    }

    /**
     * Utility method to print array values.
     *
     * @param description
     * @param data
     */
    private fun printArray(description: String, data: DoubleArray) {
        println(description)
        for (i in data.indices) {
            println(data[i])
        }
    }
}
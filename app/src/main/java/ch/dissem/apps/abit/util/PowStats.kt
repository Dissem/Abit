package ch.dissem.apps.abit.util

import android.content.Context
import android.preference.PreferenceManager
import ch.dissem.apps.abit.util.Constants.PREFERENCE_POW_AVERAGE
import ch.dissem.apps.abit.util.Constants.PREFERENCE_POW_COUNT
import java.math.BigInteger

/**
 * POW statistics that might help estimate the POW time, depending on
 */
object PowStats {
    private val TWO_POW_64: BigInteger = BigInteger.valueOf(2).pow(64)

    private var averagePowUnitTime = 0L
    private var powCount = 0L

    fun getExpectedPowTimeInMilliseconds(ctx: Context, target: ByteArray): Long {
        if (averagePowUnitTime == 0L) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)
            synchronized(this) {
                averagePowUnitTime = preferences.getLong(PREFERENCE_POW_AVERAGE, 0L)
                powCount = preferences.getLong(PREFERENCE_POW_COUNT, 0L)
            }
        }
        return (averagePowUnitTime * BigInteger(target) / TWO_POW_64).toLong()
    }

    fun addPow(ctx: Context, time: Long, target: ByteArray) {
        val targetBigInt = BigInteger(target)
        val powCountBefore = BigInteger.valueOf(powCount)
        synchronized(this) {
            powCount++
            averagePowUnitTime = (
                (averagePowUnitTime * powCountBefore + (time * TWO_POW_64 / targetBigInt)) / powCount
                ).toLong()

            val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)
            preferences.edit()
                .putLong(PREFERENCE_POW_AVERAGE, averagePowUnitTime)
                .putLong(PREFERENCE_POW_COUNT, powCount)
                .apply()
        }
    }

    private operator fun Long.times(other: BigInteger) = this.toBigInteger() * other
    private operator fun BigInteger.div(other: Long) = this / other.toBigInteger()
}

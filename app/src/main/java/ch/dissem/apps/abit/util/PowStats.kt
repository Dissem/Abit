package ch.dissem.apps.abit.util

import android.content.Context

/**
 * Created by chrigu on 02.08.17.
 */
object PowStats {
    var powUnitTime: Long = 0
    var powCount: Long = 0

    @JvmStatic
    fun getExpectedPowTime(ctx: Context, target: ByteArray): Long {
//        val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)
//        return preferences.getLong(Constants.PREFERENCE_POW_AVERAGE, 0L)
        return 0
    }

//    fun updatePowTelemetry(ctx: Context, averagePowTime: Long, powCount: Long) {
//        val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)
//        preferences.edit()
//            .putLong(Constants.PREFERENCE_POW_AVERAGE, averagePowTime)
//            .putLong(Constants.PREFERENCE_POW_COUNT, powCount)
//            .apply()
//    }

    @JvmStatic
    fun addPow(ctx: Context, time: Long, target: ByteArray) {
        powCount++
    }
}

package com.chesscompanion.enginedebug

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Debug

/** Reads the figures the S0-4 report needs: process memory and battery temperature. */
object DeviceProbe {
    fun pssMb(): Int = (Debug.getPss() / KB_PER_MB).toInt()

    /** Battery temperature in °C, or null if unavailable. It tracks the tablet's heat, not the SoC. */
    fun batteryTempC(context: Context): Float? {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val tenths = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE) ?: Int.MIN_VALUE
        return if (tenths == Int.MIN_VALUE) null else tenths / TENTHS
    }

    private const val KB_PER_MB = 1024
    private const val TENTHS = 10f
}

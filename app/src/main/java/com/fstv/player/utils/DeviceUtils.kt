package com.fstv.player.utils

import android.content.Context
import android.content.SharedPreferences
import java.net.NetworkInterface
import java.util.Collections
import java.util.Random

object DeviceUtils {

    private const val PREFS_NAME = "fstv_device_prefs"
    private const val KEY_DEVICE_KEY = "device_key"
    private const val KEY_MOCK_MAC = "mock_mac"

    fun getMacAddress(context: Context): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (intf.name.equals("wlan0", ignoreCase = true) || intf.name.equals("eth0", ignoreCase = true)) {
                    val macBytes = intf.hardwareAddress ?: continue
                    val res1 = StringBuilder()
                    for (b in macBytes) {
                        res1.append(String.format("%02X:", b))
                    }
                    if (res1.isNotEmpty()) {
                        res1.deleteCharAt(res1.length - 1)
                    }
                    val mac = res1.toString()
                    if (mac.isNotEmpty() && mac != "02:00:00:00:00:00") {
                        return mac
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback para Android 10+ ou emuladores onde o MAC é restrito
        return getOrCreateMockMac(context)
    }

    fun getDeviceKey(context: Context): String {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var key = prefs.getString(KEY_DEVICE_KEY, null)
        if (key.isNull_or_Empty()) {
            val random = Random()
            val randomNumber = 100000 + random.nextInt(900000)
            key = randomNumber.toString()
            prefs.edit().putString(KEY_DEVICE_KEY, key).apply()
        }
        return key
    }

    private fun String?.isNull_or_Empty(): Boolean = this == null || this.isEmpty()

    private fun getOrCreateMockMac(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var mockMac = prefs.getString(KEY_MOCK_MAC, null)
        if (mockMac.isNull_or_Empty()) {
            val r = Random()
            mockMac = String.format("%02X:%02X:%02X:%02X:%02X:%02X",
                r.nextInt(256), r.nextInt(256), r.nextInt(256),
                r.nextInt(256), r.nextInt(256), r.nextInt(256))
            prefs.edit().putString(KEY_MOCK_MAC, mockMac).apply()
        }
        return mockMac!!
    }
}

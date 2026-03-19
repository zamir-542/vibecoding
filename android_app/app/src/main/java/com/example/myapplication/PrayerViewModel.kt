package com.example.myapplication

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PrayerViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("waktu_solat_prefs", Context.MODE_PRIVATE)

    var themeMode by mutableStateOf(prefs.getInt("theme_mode", 0)) // 0=System, 1=Light, 2=Dark
        private set

    var currentZone by mutableStateOf(prefs.getString("zone", "SGR01") ?: "SGR01")
        private set

    var notifyEnabled by mutableStateOf(prefs.getBoolean("notify_enabled", false))
        private set

    var autoLocationEnabled by mutableStateOf(prefs.getBoolean("auto_location_enabled", false))
        private set

    var notifyMinutes by mutableStateOf(prefs.getInt("notify_minutes", 10))
    var hijriDate by mutableStateOf(prefs.getString("hijri", "") ?: "")
    var gregorianDate by mutableStateOf(prefs.getString("date", "") ?: "")

    private fun formatHijri(hijriStr: String): String {
        val parts = hijriStr.split("-")
        if (parts.size == 3) {
            val year = parts[0]
            val monthIdx = parts[1].toIntOrNull() ?: 1
            val day = parts[2].toIntOrNull() ?: 1
            val months = listOf("", "Muharram", "Safar", "Rabiulawal", "Rabiulakhir", "Jamadilawal", "Jamadilakhir", "Rejab", "Syaaban", "Ramadan", "Syawal", "Zulkaedah", "Zulhijjah")
            val monthName = if (monthIdx in 1..12) months[monthIdx] else ""
            return "$day $monthName ${year}H"
        }
        return hijriStr
    }

    val displayHijri: String
        get() = formatHijri(hijriDate)

    var prayerTimes by mutableStateOf<JakimPrayerTime?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    private val apiService: PrayerApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://www.e-solat.gov.my/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PrayerApiService::class.java)
    }

    fun setTheme(mode: Int) {
        prefs.edit().putInt("theme_mode", mode).apply()
        themeMode = mode
    }

    fun setZone(zone: String) {
        prefs.edit().putString("zone", zone).apply()
        currentZone = zone
        fetchPrayerTimes(forceRefresh = true)
    }

    fun setAutoLocation(enabled: Boolean) {
        prefs.edit().putBoolean("auto_location_enabled", enabled).apply()
        autoLocationEnabled = enabled
        if (enabled) {
            viewModelScope.launch {
                val newZone = LocationHelper.getAutomatedZone(getApplication())
                if (newZone != null && newZone != currentZone) {
                    setZone(newZone)
                }
            }
        }
    }

    fun setNotifications(enabled: Boolean, minutes: Int) {
        prefs.edit()
            .putBoolean("notify_enabled", enabled)
            .putInt("notify_minutes", minutes)
            .apply()
        notifyEnabled = enabled
        notifyMinutes = minutes
        
        prayerTimes?.let { p ->
            if (enabled) PrayerAlarmScheduler.scheduleAlarms(getApplication(), p, minutes)
            else PrayerAlarmScheduler.cancelAlarms(getApplication())
        }
    }

    fun fetchPrayerTimes(forceRefresh: Boolean = false) {
        if (!forceRefresh && (prayerTimes != null || isLoading)) return
        
        isLoading = true
        errorMessage = null
        
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.getPrayerTimes(zone = currentZone)
                }
                if (response.prayerTime.isNotEmpty()) {
                    val p = response.prayerTime[0]
                    prayerTimes = p
                    hijriDate = p.hijri
                    gregorianDate = p.date
                    
                    if (notifyEnabled) {
                        PrayerAlarmScheduler.scheduleAlarms(getApplication(), p, notifyMinutes)
                    }
                    PrayerAlarmScheduler.scheduleWidgetUpdates(getApplication(), p)
                    
                    // Automatically cache times for the Home Screen Widget
                    prefs.edit().apply {
                        putString("hijri", p.hijri)
                        putString("formatted_hijri", formatHijri(p.hijri))
                        putString("date", p.date)
                        putString("subuh", p.fajr)
                        putString("syuruk", p.syuruk)
                        putString("zohor", p.dhuhr)
                        putString("asar", p.asr)
                        putString("maghrib", p.maghrib)
                        putString("isyak", p.isha)
                    }.apply()
                    
                    try {
                        PrayerWidget().updateAll(getApplication())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Gagal mendapatkan waktu solat. Sila periksa sambungan internet anda."
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }
}

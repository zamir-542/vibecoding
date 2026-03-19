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

    var notifyMinutes by mutableStateOf(prefs.getInt("notify_minutes", 10))
        private set

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
                    
                    if (notifyEnabled) {
                        PrayerAlarmScheduler.scheduleAlarms(getApplication(), p, notifyMinutes)
                    }
                    PrayerAlarmScheduler.scheduleWidgetUpdates(getApplication(), p)
                    
                    // Automatically cache times for the Home Screen Widget
                    prefs.edit().apply {
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

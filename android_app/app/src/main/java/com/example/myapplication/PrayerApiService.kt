package com.example.myapplication

import retrofit2.http.GET
import retrofit2.http.Query

interface PrayerApiService {
    @GET("index.php?r=esolatApi/takwimsolat")
    suspend fun getPrayerTimes(
        @Query("period") period: String = "today",
        @Query("zone") zone: String
    ): JakimResponse
}

data class JakimResponse(
    val prayerTime: List<JakimPrayerTime>,
    val status: String,
    val zone: String,
    val serverTime: String
)

data class JakimPrayerTime(
    val hijri: String,
    val date: String,
    val day: String,
    val imsak: String,
    val fajr: String,
    val syuruk: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String
)

package com.example.myapplication

import android.location.Address

object JakimZoneMapper {

    fun getZoneCode(address: Address): String {
        // District is usually subAdminArea, State is adminArea
        val state = address.adminArea?.lowercase() ?: ""
        val district = address.subAdminArea?.lowercase() ?: ""
        val locality = address.locality?.lowercase() ?: ""
        val subLocality = address.subLocality?.lowercase() ?: ""
        val fullLocationInfo = "$district $locality $subLocality"
        
        return when {
            state.contains("kuala lumpur") || state.contains("putrajaya") || state.contains("labuan") -> "WLT01"
            
            state.contains("selangor") -> {
                when {
                    fullLocationInfo.contains("klang") || fullLocationInfo.contains("kuala langat") -> "SGR03"
                    fullLocationInfo.contains("kuala selangor") || fullLocationInfo.contains("sabak bernam") -> "SGR02"
                    else -> "SGR01" // Gombak, Petaling, Sepang, Hulu Langat, Hulu Selangor, S.Alam
                }
            }
            state.contains("johor") -> "JHR02" // Default JB
            state.contains("kedah") -> "KDH01"
            state.contains("kelantan") -> "KTN01"
            state.contains("melaka") -> "MLK01"
            state.contains("negeri sembilan") -> "NGS02"
            state.contains("pahang") -> "PHG02"
            state.contains("perak") -> "PRK02"
            state.contains("perlis") -> "PLS01"
            state.contains("pulau pinang") || state.contains("penang") -> "PNG01"
            state.contains("sabah") -> "SBH06" // Default KK
            state.contains("sarawak") -> "SWK01" // Default Kuching
            state.contains("terengganu") -> "TRG01" // Default Kuala Terengganu
            
            else -> "SGR01" // Ultimate Fallback
        }
    }
}

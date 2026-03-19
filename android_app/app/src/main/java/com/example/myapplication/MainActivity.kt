package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.MyApplicationTheme
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {
    private val viewModel: PrayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val darkTheme = when (viewModel.themeMode) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = darkTheme) {
                var currentTab by remember { mutableStateOf(0) }

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                icon = { Icon(Icons.Filled.Home, contentDescription = "Utama") },
                                label = { Text("Utama") },
                                selected = currentTab == 0,
                                onClick = { currentTab = 0 }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Filled.Settings, contentDescription = "Tetapan") },
                                label = { Text("Tetapan") },
                                selected = currentTab == 1,
                                onClick = { currentTab = 1 }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        if (currentTab == 0) {
                            PrayerScreen(viewModel)
                        } else {
                            SettingsScreen(viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PrayerScreen(viewModel: PrayerViewModel) {
    val times = viewModel.prayerTimes
    
    LaunchedEffect(Unit) {
        viewModel.fetchPrayerTimes()
    }

    var nextPrayerName by remember { mutableStateOf("") }
    var countdownText by remember { mutableStateOf("") }
    
    LaunchedEffect(times) {
        if (times == null) return@LaunchedEffect
        while(true) {
            val now = java.util.Calendar.getInstance()
            val currentSecs = now.get(java.util.Calendar.HOUR_OF_DAY) * 3600 + now.get(java.util.Calendar.MINUTE) * 60 + now.get(java.util.Calendar.SECOND)
            
            val prayerList = listOf(
                "Imsak" to times.imsak,
                "Subuh" to times.fajr,
                "Syuruk" to times.syuruk,
                "Zuhur" to times.dhuhr,
                "Asar" to times.asr,
                "Maghrib" to times.maghrib,
                "Isyak" to times.isha
            )
            
            var found = false
            for ((name, timeStr) in prayerList) {
                val parts = timeStr.split(":")
                if (parts.size >= 2) {
                    val pSecs = parts[0].toInt() * 3600 + parts[1].toInt() * 60 + (if(parts.size == 3) parts[2].toInt() else 0)
                    if (pSecs > currentSecs) {
                        nextPrayerName = name
                        val diff = pSecs - currentSecs
                        val h = diff / 3600
                        val m = (diff % 3600) / 60
                        val s = diff % 60
                        countdownText = String.format("%02d:%02d:%02d", h, m, s)
                        found = true
                        break
                    }
                }
            }
            
            if (!found) {
                val parts = times.imsak.split(":")
                if (parts.size >= 2) {
                    val pSecs = parts[0].toInt() * 3600 + parts[1].toInt() * 60 + (if(parts.size == 3) parts[2].toInt() else 0)
                    val tomorrowSecs = pSecs + 86400
                    val diff = tomorrowSecs - currentSecs
                    val h = diff / 3600
                    val m = (diff % 3600) / 60
                    val s = diff % 60
                    nextPrayerName = "Imsak"
                    countdownText = String.format("%02d:%02d:%02d", h, m, s)
                }
            }
            kotlinx.coroutines.delay(1000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                )
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Waktu Solat", 
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Zon: ${viewModel.currentZone}", 
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            when {
                viewModel.isLoading && times == null -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                viewModel.errorMessage != null && times == null -> {
                    Text(
                        text = viewModel.errorMessage!!, 
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                times != null -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        item {
                            PrayerCard("Imsak", times.imsak, nextPrayerName == "Imsak")
                            PrayerCard("Subuh", times.fajr, nextPrayerName == "Subuh")
                            PrayerCard("Syuruk", times.syuruk, nextPrayerName == "Syuruk")
                            PrayerCard("Zuhur", times.dhuhr, nextPrayerName == "Zuhur")
                            PrayerCard("Asar", times.asr, nextPrayerName == "Asar")
                            PrayerCard("Maghrib", times.maghrib, nextPrayerName == "Maghrib")
                            PrayerCard("Isyak", times.isha, nextPrayerName == "Isyak")
                        }
                    }
                }
            }
        }
        
        if (times != null && countdownText.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Seterusnya:", 
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = nextPrayerName, 
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = countdownText, 
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
fun PrayerCard(name: String, time: String, isNext: Boolean = false) {
    val containerColor = if (isNext) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isNext) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val strokeModifier = if (isNext) Modifier.fillMaxWidth().border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)) else Modifier.fillMaxWidth()

    ElevatedCard(
        modifier = strokeModifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (isNext) 12.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name, 
                style = MaterialTheme.typography.titleLarge,
                fontWeight = if (isNext) FontWeight.ExtraBold else FontWeight.Medium,
                color = textColor
            )
            Text(
                text = time, 
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isNext) textColor else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: PrayerViewModel) {
    // Grouped by State
    val groupedZones = mapOf(
        "Johor" to listOf(
            "JHR01" to "Pulau Aur dan Pulau Pemanggil",
            "JHR02" to "Johor Bahru, Kota Tinggi, Mersing",
            "JHR03" to "Kluang, Pontian",
            "JHR04" to "Batu Pahat, Muar, Segamat, Gemas Johor"
        ),
        "Kedah" to listOf(
            "KDH01" to "Kota Setar, Kubang Pasu, Pokok Sena",
            "KDH02" to "Kuala Muda, Yan, Pendang",
            "KDH03" to "Padang Terap, Sik",
            "KDH04" to "Baling",
            "KDH05" to "Bandar Baharu, Kulim",
            "KDH06" to "Langkawi"
        ),
        "Kelantan" to listOf(
            "KTN01" to "Kota Bharu, Bachok, Pasir Puteh, Tumpat...",
            "KTN03" to "Gua Musang, Jeli"
        ),
        "Melaka" to listOf(
            "MLK01" to "Seluruh Negeri Melaka"
        ),
        "Negeri Sembilan" to listOf(
            "NGS01" to "Tampin, Jempol",
            "NGS02" to "Jelebu, Kuala Pilah, Port Dickson, Rembau, Seremban"
        ),
        "Pahang" to listOf(
            "PHG01" to "Pulau Tioman",
            "PHG02" to "Kuantan, Pekan, Rompin, Muadzam Shah",
            "PHG03" to "Jerantut, Temerloh, Maran, Bera, Chenor, Jengka",
            "PHG04" to "Bentong, Lipis, Raub",
            "PHG05" to "Genting Sempah, Janda Baik, Bukit Tinggi",
            "PHG06" to "Cameron Highlands, Genting Higlands, Bukit Fraser"
        ),
        "Perak" to listOf(
            "PRK01" to "Pengkalan Hulu, Grik, Lenggong",
            "PRK02" to "Selama, Taiping, Bagan Serai, Parit Buntar",
            "PRK03" to "Kuala Kangsar, Sg. Siput, Ipoh, Batu Gajah, Kampar",
            "PRK04" to "Sitiawan, Lumut, Seri Manjung, Pangkor",
            "PRK05" to "Tapah, Slim River, Tanjung Malim",
            "PRK06" to "Teluk Intan, Bagan Datuk, Kg. Gajah...",
            "PRK07" to "Bukkit Larut"
        ),
        "Perlis" to listOf(
            "PLS01" to "Kangar, Padang Besar, Arau"
        ),
        "Pulau Pinang" to listOf(
            "PNG01" to "Seluruh Negeri Pulau Pinang"
        ),
        "Sabah" to listOf(
            "SBH01" to "Bahagian Sandakan, Bukit Garam...",
            "SBH02" to "Belingian, Kuamut, Kinabatangan...",
            "SBH03" to "Lahad Datu, Silabukan, Kunak, Sahabat...",
            "SBH04" to "Tawau, Balong, Merotai, Kalabakan...",
            "SBH05" to "Kudat, Kota Marudu, Pitas, Pulau Banggi...",
            "SBH06" to "Gunung Kinabalu",
            "SBH07" to "Kota Kinabalu, Penampang, Tuaran, Papar...",
            "SBH08" to "Pensiangan, Keningau, Tambunan, Nabawan",
            "SBH09" to "Beaufort, Kuala Penyu, Sipitang..."
        ),
        "Sarawak" to listOf(
            "SWK01" to "Limbang, Lawas, Sundar, Trusan",
            "SWK02" to "Miri, Niah, Bekenu, Sibuti, Marudi",
            "SWK03" to "Pandan, Belaga, Suai, Tatau, Sebauh, Bintulu",
            "SWK04" to "Sibu, Mukah, Dalat, Song, Igan, Oya, Balingian...",
            "SWK05" to "Belawai, Matu, Daro, Sarikei, Julau, Bintangor...",
            "SWK06" to "Lubok Antu, Sri Aman, Roban, Debak, Kabong...",
            "SWK07" to "Serian, Simunjan, Samarahan, Sebuyau, Meludam",
            "SWK08" to "Kuching, Bau, Lundu, Sematan",
            "SWK09" to "Zon Khas (Kampung Patarikan)"
        ),
        "Selangor" to listOf(
            "SGR01" to "Gombak, Petaling, Sepang, Hulu Langat, Hulu Selangor, S.Alam",
            "SGR02" to "Kuala Selangor, Sabak Bernam",
            "SGR03" to "Klang, Kuala Langat"
        ),
        "Terengganu" to listOf(
            "TRG01" to "Kuala Terengganu, Marang, Kuala Nerus",
            "TRG02" to "Besut, Setiu",
            "TRG03" to "Hulu Terengganu",
            "TRG04" to "Dungun, Kemaman"
        ),
        "Wilayah Persekutuan" to listOf(
            "WLY01" to "Kuala Lumpur, Putrajaya",
            "WLY02" to "Labuan"
        )
    )

    var showZoneDialog by remember { mutableStateOf(false) }
    var viewingState by remember { mutableStateOf<String?>(null) } // null = viewing states list

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text("Tetapan", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(32.dp))

        Text("Tema Aplikasi", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FilterChip(
                selected = viewModel.themeMode == 0,
                onClick = { viewModel.setTheme(0) },
                label = { Text("Sistem") }
            )
            FilterChip(
                selected = viewModel.themeMode == 1,
                onClick = { viewModel.setTheme(1) },
                label = { Text("Cerah") }
            )
            FilterChip(
                selected = viewModel.themeMode == 2,
                onClick = { viewModel.setTheme(2) },
                label = { Text("Gelap") }
            )
        }

        Spacer(Modifier.height(40.dp))

        Text("Lokasi (Zon Jakim)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = { 
                viewingState = null // Reset back to showing States when opened
                showZoneDialog = true 
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            val allZonesFlat = groupedZones.values.flatten()
            val currentZoneName = allZonesFlat.find { it.first == viewModel.currentZone }?.second ?: ""
            Text(
                text = "${viewModel.currentZone} - $currentZoneName",
                modifier = Modifier.padding(8.dp),
                textAlign = TextAlign.Center
            )
        }

        if (showZoneDialog) {
            AlertDialog(
                onDismissRequest = { showZoneDialog = false },
                title = { 
                    Text(
                        text = if (viewingState == null) "Pilih Negeri" else "Pilih Zon (${viewingState})", 
                        fontWeight = FontWeight.Bold
                    ) 
                },
                text = {
                    LazyColumn(modifier = Modifier.fillMaxHeight(0.8f)) {
                        if (viewingState == null) {
                            // Show List of States
                            items(groupedZones.keys.toList().size) { index ->
                                val stateName = groupedZones.keys.toList()[index]
                                TextButton(
                                    onClick = { viewingState = stateName },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = stateName, 
                                        textAlign = TextAlign.Start, 
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                HorizontalDivider()
                            }
                        } else {
                            // Show List of Zones in the selected State
                            val zonesInState = groupedZones[viewingState] ?: emptyList()
                            items(zonesInState.size) { index ->
                                val (code, name) = zonesInState[index]
                                TextButton(
                                    onClick = {
                                        viewModel.setZone(code)
                                        showZoneDialog = false
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "$code - $name", 
                                        textAlign = TextAlign.Start, 
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { 
                        if (viewingState != null) viewingState = null // Go back to state list
                        else showZoneDialog = false 
                    }) {
                        Text(if (viewingState != null) "Kembali" else "Batal")
                    }
                }
            )
        }

        Spacer(Modifier.height(40.dp))

        Text("Peringatan Solat", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        
        val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Beritahu sebelum masuk waktu", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Switch(
                checked = viewModel.notifyEnabled,
                onCheckedChange = { enabled ->
                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    viewModel.setNotifications(enabled, viewModel.notifyMinutes)
                }
            )
        }
        
        if (viewModel.notifyEnabled) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Masa peringatan", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                
                var showMinsDialog by remember { mutableStateOf(false) }
                OutlinedButton(
                    onClick = { showMinsDialog = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("${viewModel.notifyMinutes} Minit")
                }
                
                if (showMinsDialog) {
                    val minsOptions = listOf(5, 10, 15, 20, 30, 45, 60)
                    AlertDialog(
                        onDismissRequest = { showMinsDialog = false },
                        title = { Text("Pilih Minit", fontWeight = FontWeight.Bold) },
                        text = {
                            LazyColumn(modifier = Modifier.fillMaxHeight(0.5f)) {
                                items(minsOptions.size) { i ->
                                    val m = minsOptions[i]
                                    TextButton(
                                        onClick = { 
                                            viewModel.setNotifications(true, m)
                                            showMinsDialog = false 
                                        },
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    ) { Text("$m Minit", textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth()) }
                                    HorizontalDivider()
                                }
                            }
                        },
                        confirmButton = { TextButton(onClick = { showMinsDialog = false }) { Text("Batal") } }
                    )
                }
            }
        }
    }
}

package com.avero.vpn

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Navy = Color(0xFF101C2C)
private val Panel = Color(0xFF1A2B3B)
private val Emerald = Color(0xFF20BFA9)
private val Mint = Color(0xFFB8F3E5)
private val Muted = Color(0xFF9FB2C0)
private val Cities = listOf(
    "Франкфурт-на-Майне", "Берлин", "Мюнхен", "Гамбург", "Дюссельдорф",
    "Штутгарт", "Кёльн", "Лейпциг", "Дрезден", "Нюрнберг", "Ганновер",
    "Бремен", "Дортмунд", "Эссен", "Бонн", "Мангейм", "Карлсруэ"
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AveroApp() }
    }
}

@Composable
fun AveroApp() {
    val context = LocalContext.current
    var page by remember { mutableStateOf("home") }
    var selected by remember { mutableStateOf(Cities.first()) }
    var connected by remember { mutableStateOf(false) }
    var showLogin by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var favorite by remember { mutableStateOf(setOf(Cities.first(), Cities[1])) }
    var history by remember { mutableStateOf(listOf<String>()) }
    var usedMb by remember { mutableIntStateOf(0) }
    var notice by remember { mutableStateOf("") }

    val vpnPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            connected = !connected
            if (connected) history = listOf("$selected · демонстрация") + history
            notice = if (connected) "Демонстрационный режим включён. VPN-туннель ещё не настроен." else "Демонстрация остановлена"
        } else {
            notice = "Разрешение VPN не предоставлено"
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Navy, surface = Panel, primary = Emerald,
            onPrimary = Navy, onBackground = Color.White,
            onSurface = Color.White, secondary = Mint
        )
    ) {
        Surface(Modifier.fillMaxSize(), color = Navy) {
            if (showLogin) {
                LoginScreen(
                    email = email, onEmail = { email = it },
                    password = password, onPassword = { password = it },
                    onContinue = {
                        if (email.contains("@") && password.length >= 6) showLogin = false
                        else notice = "Введите корректную почту и пароль от 6 символов"
                    },
                    onGoogle = { notice = "Google-вход подключается после настройки Firebase проекта" },
                    notice = notice
                )
            } else {
                when (page) {
                    "locations" -> LocationsScreen(
                        selected, favorite,
                        onSelect = { selected = it; page = "home" },
                        onFavorite = { city -> favorite = if (city in favorite) favorite - city else favorite + city },
                        onBack = { page = "home" }
                    )
                    "stats" -> StatsScreen(usedMb, history, favorite, onBack = { page = "home" })
                    "profile" -> ProfileScreen(email, onBack = { page = "home" }, onLogout = { showLogin = true })
                    else -> HomeScreen(
                        selected = selected, connected = connected, usedMb = usedMb,
                        notice = notice,
                        onConnect = {
                            val intent = VpnService.prepare(context)
                            if (intent != null) vpnPermission.launch(intent)
                            else {
                                connected = !connected
                                if (connected) history = listOf("$selected · демонстрация") + history
                                notice = if (connected) "Демонстрационный режим. Реального VPN-туннеля пока нет." else "Демонстрация остановлена"
                            }
                        },
                        onLocations = { page = "locations" },
                        onStats = { page = "stats" },
                        onProfile = { page = "profile" }
                    )
                }
            }
        }
    }
}

@Composable
fun LoginScreen(email: String, onEmail: (String) -> Unit, password: String,
                onPassword: (String) -> Unit, onContinue: () -> Unit,
                onGoogle: () -> Unit, notice: String) {
    Column(Modifier.fillMaxSize().background(Navy).padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("AVERO", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Color.White)
        Text("VPN  ·  DIGITAL FREEDOM", color = Emerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(42.dp))
        Text("Добро пожаловать", fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text("Войдите, чтобы начать пользоваться AVERO", color = Muted)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(email, onEmail, Modifier.fillMaxWidth(), label = { Text("Электронная почта") }, singleLine = true)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(password, onPassword, Modifier.fillMaxWidth(), label = { Text("Пароль") }, singleLine = true)
        Spacer(Modifier.height(18.dp))
        Button(onClick = onContinue, Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Emerald)) {
            Text("Продолжить", color = Navy, fontWeight = FontWeight.Bold)
        }
        OutlinedButton(onClick = onGoogle, Modifier.fillMaxWidth().height(52.dp)) { Text("Продолжить с Google") }
        Text("Демо-режим: аккаунт пока проверяется локально.", color = Muted, fontSize = 11.sp)
        if (notice.isNotBlank()) Text(notice, color = Mint, fontSize = 12.sp)
    }
}

@Composable
fun HomeScreen(selected: String, connected: Boolean, usedMb: Int, notice: String,
               onConnect: () -> Unit, onLocations: () -> Unit, onStats: () -> Unit, onProfile: () -> Unit) {
    val progress = (usedMb / 5120f).coerceIn(0f, 1f)
    Column(Modifier.fillMaxSize().background(Navy).padding(horizontal = 20.dp, vertical = 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("AVERO", fontSize = 27.sp, fontWeight = FontWeight.Black)
                Text("BEYOND CONNECTIVITY", color = Emerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onProfile) { Icon(Icons.Default.Person, null, tint = Mint) }
        }
        Spacer(Modifier.height(26.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.size(184.dp).clip(CircleShape).background(
                Brush.radialGradient(listOf(Color(0xFF28645F), Color(0xFF183C48), Navy))
            ).clickable { onConnect() }, contentAlignment = Alignment.Center) {
                Icon(if (connected) Icons.Default.PowerSettingsNew else Icons.Default.Power, null, tint = Mint, modifier = Modifier.size(62.dp))
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(if (connected) "Демо включено" else "Отключено", Modifier.align(Alignment.CenterHorizontally), fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Text(if (connected) "Реальный VPN ещё не настроен" else "Нажмите кнопку для теста интерфейса",
            Modifier.align(Alignment.CenterHorizontally), color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(24.dp))
        PanelCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Пробный период", color = Muted, fontSize = 12.sp)
                    Text("5 дней · 5 ГБ", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                }
                Text("FREE", color = Emerald, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape), color = Emerald, trackColor = Color(0xFF344553))
            Spacer(Modifier.height(6.dp))
            Text("${usedMb} МБ из 5 120 МБ · демо-счётчик", color = Muted, fontSize = 11.sp)
        }
        Spacer(Modifier.height(12.dp))
        PanelCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🇩🇪", fontSize = 25.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(selected, fontWeight = FontWeight.Bold)
                    Text("Германия · 17 локаций", color = Muted, fontSize = 11.sp)
                }
                TextButton(onClick = onLocations) { Text("Выбрать", color = Mint) }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard("Скорость", "— Mbps", Modifier.weight(1f))
            MetricCard("Время", "00:00", Modifier.weight(1f))
        }
        Spacer(Modifier.weight(1f))
        if (notice.isNotBlank()) Text(notice, color = Mint, fontSize = 11.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            TextButton(onClick = onStats) { Icon(Icons.Default.Insights, null); Spacer(Modifier.width(5.dp)); Text("Статистика") }
            TextButton(onClick = onProfile) { Icon(Icons.Default.Person, null); Spacer(Modifier.width(5.dp)); Text("Профиль") }
        }
    }
}

@Composable
fun LocationsScreen(selected: String, favorites: Set<String>, onSelect: (String) -> Unit,
                    onFavorite: (String) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Text("Локации Германии", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Text("17 географических точек · демонстрационный список", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            items(Cities) { city ->
                PanelCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🇩🇪", fontSize = 23.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f).clickable { onSelect(city) }) {
                            Text(city, fontWeight = FontWeight.SemiBold)
                            Text(if (city == selected) "Выбрана" else "Германия · точка подключения", color = Muted, fontSize = 11.sp)
                        }
                        IconButton(onClick = { onFavorite(city) }) {
                            Icon(if (city in favorites) Icons.Default.Star else Icons.Default.StarBorder, null, tint = Emerald)
                        }
                        if (city == selected) Icon(Icons.Default.CheckCircle, null, tint = Emerald)
                    }
                }
            }
        }
    }
}

@Composable
fun StatsScreen(usedMb: Int, history: List<String>, favorites: Set<String>, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Text("Статистика", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        PanelCard {
            Text("Использовано трафика", color = Muted)
            Text("$usedMb МБ / 5 120 МБ", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Пробный период: 5 дней · всего 5 ГБ", color = Mint, fontSize = 12.sp)
        }
        Spacer(Modifier.height(14.dp))
        PanelCard {
            Text("Избранные локации", fontWeight = FontWeight.Bold)
            Text(if (favorites.isEmpty()) "Пока нет избранных точек" else favorites.joinToString(", "), color = Muted)
        }
        Spacer(Modifier.height(14.dp))
        PanelCard {
            Text("История подключений", fontWeight = FontWeight.Bold)
            if (history.isEmpty()) Text("Подключений пока нет", color = Muted)
            history.take(12).forEach { Text("• $it", color = Muted, modifier = Modifier.padding(top = 7.dp), fontSize = 12.sp) }
        }
    }
}

@Composable
fun ProfileScreen(email: String, onBack: () -> Unit, onLogout: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Text("Профиль", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        PanelCard {
            Text("Аккаунт", color = Muted)
            Text(email.ifBlank { "Демо-пользователь" }, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text("Пробный период · 5 дней", color = Mint)
            Text("Лимит · 5 ГБ суммарно", color = Muted)
        }
        Spacer(Modifier.height(14.dp))
        OutlinedButton(onClick = onLogout, Modifier.fillMaxWidth()) { Text("Выйти из аккаунта") }
        Text("AVERO VPN · версия 0.1 demo", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 20.dp))
    }
}

@Composable
fun PanelCard(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Panel).padding(16.dp), content = content)
}

@Composable
fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(Panel).padding(14.dp)) {
        Text(label, color = Muted, fontSize = 11.sp)
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

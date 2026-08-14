package com.yozaky.tareaintegradora.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.*
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.lazy.items
import com.yozaky.tareaintegradora.data.DataStoreManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.yozaky.tareaintegradora.api.NotificationItem
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val dataStoreManager = DataStoreManager(applicationContext)
        
        setContent {
            val authViewModel: AuthViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return AuthViewModel(dataStoreManager) as T
                    }
                }
            )
            val notificationViewModel: NotificationViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return NotificationViewModel(dataStoreManager) as T
                    }
                }
            )
            WearApp(authViewModel, notificationViewModel)
        }
    }
}

@Composable
fun WearApp(authViewModel: AuthViewModel, notificationViewModel: NotificationViewModel) {
    val authState by authViewModel.authState.collectAsState()

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            when (val state = authState) {
                is AuthState.Loading -> CircularProgressIndicator(indicatorColor = Color.Cyan)
                is AuthState.Unauthenticated -> PinLoginScreen(authViewModel, null)
                is AuthState.Authenticated -> MainScreen(authViewModel, notificationViewModel)
                is AuthState.Error -> PinLoginScreen(authViewModel, state.message)
            }
        }
    }
}

@Composable
fun PinLoginScreen(viewModel: AuthViewModel, errorMessage: String?) {
    val serviceStatus by viewModel.serviceStatus.collectAsState()
    var pin by remember { mutableStateOf("") }
    val listState = rememberScalingLazyListState()
    
    Scaffold(
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(top = 32.dp, bottom = 32.dp)
        ) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (errorMessage != null) "¡ERROR!" else "VINCULACIÓN",
                        style = MaterialTheme.typography.caption2,
                        color = if (errorMessage != null) Color.Red else Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = errorMessage ?: "INGRESE PIN",
                        style = MaterialTheme.typography.caption1,
                        color = if (errorMessage != null) Color.Red else Color.Cyan,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(4) { index ->
                            val active = index < pin.length
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(
                                        if (active) Color.Cyan else Color(0xFF333333),
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }
            }

            if (serviceStatus is ServiceStatus.Available) {
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(modifier = Modifier.height(4.dp))
                        KeyRow(listOf("1", "2", "3")) { if (pin.length < 4) pin += it }
                        KeyRow(listOf("4", "5", "6")) { if (pin.length < 4) pin += it }
                        KeyRow(listOf("7", "8", "9")) { if (pin.length < 4) pin += it }
                        KeyRow(listOf("C", "0", "✓")) { key ->
                            when (key) {
                                "C" -> pin = ""
                                "✓" -> if (pin.length == 4) viewModel.login(pin)
                                else -> if (pin.length < 4) pin += key
                            }
                        }
                    }
                }
            } else if (serviceStatus is ServiceStatus.Unavailable) {
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 10.dp)) {
                        Text("Servidor Desconectado", color = Color.Red, style = MaterialTheme.typography.caption2)
                        Spacer(modifier = Modifier.height(8.dp))
                        Chip(
                            onClick = { viewModel.checkStatus() },
                            label = { Text("Reintentar") },
                            colors = ChipDefaults.secondaryChipColors(),
                            modifier = Modifier.height(32.dp)
                        )
                    }
                }
            } else {
                item {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(top = 20.dp))
                }
            }
        }
    }
}

@Composable
fun KeyRow(keys: List<String>, onClick: (String) -> Unit) {
    Row(modifier = Modifier.padding(vertical = 1.dp)) {
        keys.forEach { key ->
            val color = when(key) {
                "C" -> Color(0xFF442222)
                "✓" -> Color(0xFF1B5E20)
                else -> Color(0xFF262626)
            }
            NumberButton(key, color, onClick)
        }
    }
}

@Composable
fun NumberButton(text: String, color: Color, onClick: (String) -> Unit) {
    Button(
        onClick = { onClick(text) },
        modifier = Modifier.padding(2.dp).size(42.dp),
        colors = ButtonDefaults.buttonColors(backgroundColor = color),
        shape = CircleShape
    ) {
        Text(
            text = text, 
            style = MaterialTheme.typography.title3,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun MainScreen(authViewModel: AuthViewModel, notificationViewModel: NotificationViewModel) {
    val notificationsState by notificationViewModel.notificationsState.collectAsState()
    val listState = rememberScalingLazyListState()

    LaunchedEffect(Unit) {
        notificationViewModel.fetchNotifications()
    }

    Scaffold(
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
    ) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(top = 32.dp, bottom = 32.dp),
        ) {
            item {
                Text(
                    "ALERTAS",
                    style = MaterialTheme.typography.caption2,
                    color = Color.Gray,
                    letterSpacing = 2.sp
                )
            }
            item {
                Text(
                    "Secuencias",
                    style = MaterialTheme.typography.title3,
                    color = Color.Cyan,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            when (val state = notificationsState) {
                is NotificationsState.Loading -> {
                    item { CircularProgressIndicator(modifier = Modifier.padding(16.dp)) }
                }
                is NotificationsState.Empty -> {
                    item { 
                        Text("Sin alertas nuevas", 
                            textAlign = TextAlign.Center, 
                            style = MaterialTheme.typography.body2,
                            modifier = Modifier.padding(top = 20.dp)) 
                    }
                }
                is NotificationsState.Success -> {
                    items(state.notifications) { notification ->
                        NotificationItemCard(notification)
                    }
                }
                is NotificationsState.Error -> {
                    item { 
                        Text(state.message, color = Color.Red, textAlign = TextAlign.Center, style = MaterialTheme.typography.caption2) 
                    }
                    item {
                        Chip(
                            onClick = { notificationViewModel.fetchNotifications() },
                            label = { Text("Reintentar") },
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            item {
                Chip(
                    onClick = { authViewModel.logout() },
                    label = { Text("DESVINCULAR", style = MaterialTheme.typography.caption2, fontWeight = FontWeight.Bold) },
                    colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF2D2D2D)),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                )
            }
        }
    }
}

@Composable
fun NotificationItemCard(notification: NotificationItem) {
    val formattedTime = remember(notification.fecha_creacion) {
        try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            parser.timeZone = TimeZone.getTimeZone("UTC")
            val date = parser.parse(notification.fecha_creacion.substring(0, 19))
            val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
            date?.let { formatter.format(it) } ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    AppCard(
        onClick = { },
        appName = { 
            Text(
                "SEC. DIDÁCTICA", 
                color = Color.Cyan, 
                style = MaterialTheme.typography.caption2,
                fontWeight = FontWeight.Bold
            ) 
        },
        time = { 
            Text(
                formattedTime, 
                style = MaterialTheme.typography.caption2,
                color = Color.LightGray
            ) 
        },
        title = { 
            Text(
                notification.titulo, 
                maxLines = 1, 
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.button
            ) 
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp, horizontal = 4.dp),
        backgroundPainter = CardDefaults.cardBackgroundPainter(
            startBackgroundColor = Color(0xFF1E1E1E),
            endBackgroundColor = Color(0xFF121212)
        )
    ) {
        Text(
            notification.mensaje, 
            style = MaterialTheme.typography.caption1, 
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}

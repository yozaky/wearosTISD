package com.yozaky.tareaintegradora.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.*
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.yozaky.tareaintegradora.data.DataStoreManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val dataStoreManager = DataStoreManager(applicationContext)
        
        setContent {
            val viewModel: AuthViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return AuthViewModel(dataStoreManager) as T
                    }
                }
            )
            WearApp(viewModel)
        }
    }
}

@Composable
fun WearApp(viewModel: AuthViewModel) {
    val authState by viewModel.authState.collectAsState()

    MaterialTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (val state = authState) {
                is AuthState.Loading -> CircularProgressIndicator()
                is AuthState.Unauthenticated -> PinLoginScreen(viewModel, null)
                is AuthState.Authenticated -> MainScreen(viewModel)
                is AuthState.Error -> PinLoginScreen(viewModel, state.message)
            }
        }
    }
}

@Composable
fun PinLoginScreen(viewModel: AuthViewModel, errorMessage: String?) {
    var pin by remember { mutableStateOf("") }
    
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(top = 20.dp, bottom = 20.dp)
    ) {
        item {
            Text(
                text = errorMessage ?: "Ingrese PIN",
                style = MaterialTheme.typography.caption1,
                color = if (errorMessage != null) Color.Red else Color.White
            )
        }
        item {
            Text(
                text = pin.replace(Regex("."), "●").ifEmpty { "____" },
                style = MaterialTheme.typography.title2,
                color = Color.Cyan
            )
        }
        
        item {
            Column {
                (1..3).forEach { row ->
                    Row {
                        (1..3).forEach { col ->
                            val num = (row - 1) * 3 + col
                            NumberButton(num.toString()) { if (pin.length < 4) pin += it }
                        }
                    }
                }
                Row {
                    NumberButton("C", Color.Red) { pin = "" }
                    NumberButton("0") { if (pin.length < 4) pin += it }
                    NumberButton("✓", Color.Green) { if (pin.length == 4) viewModel.login(pin) }
                }
            }
        }
    }
}

@Composable
fun NumberButton(text: String, color: Color = Color.DarkGray, onClick: (String) -> Unit) {
    Button(
        onClick = { onClick(text) },
        modifier = Modifier.padding(2.dp).size(40.dp),
        colors = ButtonDefaults.buttonColors(backgroundColor = color)
    ) {
        Text(text)
    }
}

@Composable
fun MainScreen(viewModel: AuthViewModel) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("✓", color = Color.Green, style = MaterialTheme.typography.display1)
        Spacer(Modifier.height(8.dp))
        Text("Dispositivo Vinculado", textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Chip(
            onClick = { viewModel.logout() },
            label = { Text("Cerrar Sesión") },
            colors = ChipDefaults.secondaryChipColors()
        )
    }
}

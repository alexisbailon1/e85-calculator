package com.example.e85calculator

import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.e85calculator.ui.theme.E85CalculatorTheme
import androidx.core.content.edit

class MainActivity : ComponentActivity() {
    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onPause() {
        super.onPause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            E85CalculatorTheme {
                val activity = LocalActivity.current
                SideEffect {
                    activity?.window?.let { window ->
                        val controller = WindowCompat.getInsetsController(window, window.decorView)
                        controller.isAppearanceLightStatusBars = false
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF2C2C2C), Color.Black)
                            )
                        )
                ) {
                    Scaffold(containerColor = Color.Transparent) { innerPadding ->
                        CalculatorScreen(modifier = Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }
}

@Composable
fun CalculatorScreen(modifier: Modifier) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("calculator_prefs", Context.MODE_PRIVATE)

    // Load saved values or use defaults
    var tankCapacity by remember {
        mutableStateOf(prefs.getString("tankCapacity", "15.9") ?: "15.9")
    }
    var currentEthanolPercentage by remember {
        mutableStateOf(prefs.getString("currentEthanolPercentage", "") ?: "")
    }
    var targetEthanolPercentage by remember {
        mutableStateOf(prefs.getString("targetEthanolPercentage", "") ?: "")
    }
    var pumpE85Percentage by remember {
        mutableStateOf(prefs.getString("pumpE85Percentage", "") ?: "")
    }
    var pumpGasPercentage by remember {
        mutableStateOf(prefs.getString("pumpGasPercentage", "") ?: "")
    }
    var currentFuelLevelPercentage by remember {
        mutableFloatStateOf(prefs.getFloat("currentFuelLevelPercentage", 0f))
    }

    // Save values when they change
    DisposableEffect(tankCapacity, currentEthanolPercentage, targetEthanolPercentage,
        pumpE85Percentage, pumpGasPercentage, currentFuelLevelPercentage) {
        prefs.edit {
            putString("tankCapacity", tankCapacity)
            putString("currentEthanolPercentage", currentEthanolPercentage)
            putString("targetEthanolPercentage", targetEthanolPercentage)
            putString("pumpE85Percentage", pumpE85Percentage)
            putString("pumpGasPercentage", pumpGasPercentage)
            putFloat("currentFuelLevelPercentage", currentFuelLevelPercentage)
        }

        onDispose {}
    }

    val focusManager = LocalFocusManager.current

    val blendResult = remember(tankCapacity, currentEthanolPercentage, targetEthanolPercentage, pumpE85Percentage, pumpGasPercentage, currentFuelLevelPercentage) {
        FuelCalculator.calculateBlend(
            tankCapacity.toDoubleOrNull() ?: 0.0, currentFuelLevelPercentage.toDouble(),
            currentEthanolPercentage.toDoubleOrNull() ?: 0.0, targetEthanolPercentage.toDoubleOrNull() ?: 0.0,
            pumpE85Percentage.toDoubleOrNull() ?: 0.0, pumpGasPercentage.toDoubleOrNull() ?: 0.0
        )
    }

    val frostedColors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
    val frostedBorder = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "E85 Blend Calculator",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            textAlign = TextAlign.Center
        )

        Card(shape = RoundedCornerShape(24.dp), colors = frostedColors, border = frostedBorder) {
            Column(modifier = Modifier.padding(16.dp)) {
                val fields = listOf("Tank Capacity" to tankCapacity, "Pump Gas %" to pumpGasPercentage, "Pump E85 %" to pumpE85Percentage, "Target Ethanol %" to targetEthanolPercentage, "Current Ethanol %" to currentEthanolPercentage)
                fields.forEachIndexed { index, (label, value) ->
                    OutlinedTextField(
                        value = value,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.toDoubleOrNull() != null) {
                                when(index) {
                                    0 -> tankCapacity = newValue
                                    1 -> pumpGasPercentage = newValue
                                    2 -> pumpE85Percentage = newValue
                                    3 -> targetEthanolPercentage = newValue
                                    4 -> currentEthanolPercentage = newValue
                                }
                            }
                        },
                        label = { Text(label, color = Color.White.copy(alpha = 0.6f)) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = if (index == 4) ImeAction.Done else ImeAction.Next
                        )
                    )
                }
            }
        }

        Card(shape = RoundedCornerShape(24.dp), colors = frostedColors, border = frostedBorder) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Slider(
                    value = currentFuelLevelPercentage,
                    onValueChange = {
                        currentFuelLevelPercentage = it
                        // This forces the keyboard to dismiss when the slider is touched
                        focusManager.clearFocus()
                    },
                    valueRange = 0f..100f
                )
                Text("Fuel Level: ${currentFuelLevelPercentage.toInt()}%", color = Color.White, style = MaterialTheme.typography.bodyLarge)
            }
        }

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = frostedColors,
            border = frostedBorder,
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Gallons E85: ${"%.2f".format(blendResult.gallonsE85Needed)}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Gallons Pump Gas: ${"%.2f".format(blendResult.gallonsPumpGasNeeded)}",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                // Added the corrected line showing resulting ethanol percentage
                val resultingEthanolPercentage = if (blendResult.totalFillVolume > 0) {
                    val currentFuelVolume = (tankCapacity.toDoubleOrNull() ?: 0.0) * (currentFuelLevelPercentage.toDouble() / 100.0)
                    val eCurrent = currentEthanolPercentage.toDoubleOrNull() ?: 0.0
                    val eE85 = pumpE85Percentage.toDoubleOrNull() ?: 0.0
                    val eGas = pumpGasPercentage.toDoubleOrNull() ?: 0.0

                    val ethanolInCurrentFuel = currentFuelVolume * (eCurrent / 100.0)
                    val ethanolFromE85 = blendResult.gallonsE85Needed * (eE85 / 100.0)
                    val ethanolFromGas = blendResult.gallonsPumpGasNeeded * (eGas / 100.0)

                    val totalEthanol = ethanolInCurrentFuel + ethanolFromE85 + ethanolFromGas
                    val totalVolume = currentFuelVolume + blendResult.totalFillVolume

                    if (totalVolume > 0) {
                        (totalEthanol / totalVolume) * 100.0
                    } else {
                        0.0
                    }
                } else {
                    0.0
                }

                Text(
                    text = "Resulting fuel mixture: ${"%.2f".format(resultingEthanolPercentage)}% ethanol",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

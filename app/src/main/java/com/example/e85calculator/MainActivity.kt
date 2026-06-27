package com.example.e85calculator

import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.Image
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import com.example.e85calculator.R
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import com.example.e85calculator.ui.theme.E85CalculatorTheme

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
                        controller.isAppearanceLightStatusBars = true
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
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

    var tankCapacity by remember { mutableStateOf(prefs.getString("tankCapacity", "15.9") ?: "15.9") }
    var currentEthanolPercentage by remember { mutableStateOf(prefs.getString("currentEthanolPercentage", "") ?: "") }
    var targetEthanolPercentage by remember { mutableStateOf(prefs.getString("targetEthanolPercentage", "") ?: "") }
    var pumpE85Percentage by remember { mutableStateOf(prefs.getString("pumpE85Percentage", "") ?: "") }
    var pumpGasPercentage by remember { mutableStateOf(prefs.getString("pumpGasPercentage", "") ?: "") }
    var currentFuelLevelPercentage by remember { mutableFloatStateOf(prefs.getFloat("currentFuelLevelPercentage", 0f)) }

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

    val validationError: String? = remember(tankCapacity, currentEthanolPercentage, targetEthanolPercentage, pumpE85Percentage, pumpGasPercentage, currentFuelLevelPercentage) {
        val capacity = tankCapacity.toDoubleOrNull() ?: 0.0
        val eCurrent = currentEthanolPercentage.toDoubleOrNull() ?: 0.0
        val eTarget = targetEthanolPercentage.toDoubleOrNull() ?: 0.0
        val eE85 = pumpE85Percentage.toDoubleOrNull() ?: 0.0
        val eGas = pumpGasPercentage.toDoubleOrNull() ?: 0.0
        val hasCapacity = tankCapacity.isNotEmpty()
        val hasCurrent = currentEthanolPercentage.isNotEmpty()
        val hasTarget = targetEthanolPercentage.isNotEmpty()
        val hasE85 = pumpE85Percentage.isNotEmpty()
        val hasGas = pumpGasPercentage.isNotEmpty()
        when {
            hasCapacity && capacity <= 0 -> "Tank capacity must be greater than 0"
            currentFuelLevelPercentage >= 100f -> "Tank is full - nothing to add"
            hasE85 && hasGas && eE85 <= eGas -> "Pump E85 % must be higher than pump gas %"
            hasTarget && hasE85 && eTarget > eE85 -> "Target % (${eTarget.toInt()}%) exceeds pump E85 % (${eE85.toInt()}%) - not achievable"
            hasTarget && hasGas && eTarget < eGas -> "Target % (${eTarget.toInt()}%) is below pump gas % (${eGas.toInt()}%) - need to drain tank first"
            hasCurrent && hasTarget && hasE85 && hasGas && eCurrent > eTarget && eCurrent > eGas ->
                "Current ethanol (${eCurrent.toInt()}%) already exceeds target - add pump gas only or drain tank"
            hasCapacity && hasTarget && hasE85 && hasGas && capacity > 0 && currentFuelLevelPercentage < 100f -> {
                val currentFuelVolume = capacity * (currentFuelLevelPercentage / 100.0)
                val totalFillVolume = capacity - currentFuelVolume
                val denominator = (eE85 - eGas) / 100.0
                if (denominator != 0.0) {
                    val numerator = (eTarget / 100.0 * capacity) - (eCurrent / 100.0 * currentFuelVolume) - (eGas / 100.0 * totalFillVolume)
                    val gallonsE85Needed = numerator / denominator
                    when {
                        gallonsE85Needed > totalFillVolume ->
                            "Not enough tank space to reach ${eTarget.toInt()}% - only ${"%.1f".format(totalFillVolume)} gal available but ${"%.1f".format(gallonsE85Needed)} gal of E85 needed. Try a lower fuel level."
                        gallonsE85Needed < 0 ->
                            "Target ${eTarget.toInt()}% is lower than what's achievable - add pump gas only or drain some fuel first."
                        else -> null
                    }
                } else null
            }
            else -> null
        }
    }

    val screenHeight = LocalConfiguration.current.screenHeightDp
    // scale factor: 1.0 at 800dp, clamped between 0.75 and 1.15
    val scale = (screenHeight / 800f).coerceIn(0.75f, 1.15f)

    val logoHeight = (56 * scale).dp
    val headerTopPadding = (16 * scale).dp
    val logoTitleSpacing = (6 * scale).dp
    val cardPadding = (16 * scale).dp
    val fieldSpacing = (10 * scale).dp
    val sectionSpacing = (12 * scale).dp

    val e85Blue = Color(0xFF0057B8)

    val filledFieldColors = TextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
    )


    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) },
        verticalArrangement = Arrangement.spacedBy(sectionSpacing)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Column(modifier = Modifier.padding(top = headerTopPadding, bottom = 4.dp)) {
            Image(
                painter = painterResource(id = R.drawable.e85logo),
                contentDescription = "E85 Logo",
                modifier = Modifier.height(logoHeight)
            )
            Spacer(modifier = Modifier.height(logoTitleSpacing))
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground)) { append("Blend ") }
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append("Calculator") }
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // ── Fuel Setup Card ───────────────────────────────────────────────────
        ElevatedCard(
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(cardPadding),
                verticalArrangement = Arrangement.spacedBy(fieldSpacing)
            ) {
                Text(
                    text = "FUEL SETUP",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                val inputFields = listOf(
                    Triple("Tank Capacity (gal)", tankCapacity, ImeAction.Next),
                    Triple("Pump Gas %", pumpGasPercentage, ImeAction.Next),
                    Triple("Pump E85 %", pumpE85Percentage, ImeAction.Next),
                    Triple("Target Ethanol %", targetEthanolPercentage, ImeAction.Next),
                    Triple("Current Ethanol %", currentEthanolPercentage, ImeAction.Done),
                )
                inputFields.forEachIndexed { index, (label, value, imeAction) ->
                    TextField(
                        value = value,
                        onValueChange = { newVal ->
                            if (newVal.isEmpty() || newVal.toDoubleOrNull() != null) {
                                when (index) {
                                    0 -> tankCapacity = newVal
                                    1 -> pumpGasPercentage = newVal
                                    2 -> pumpE85Percentage = newVal
                                    3 -> targetEthanolPercentage = newVal
                                    4 -> currentEthanolPercentage = newVal
                                }
                            }
                        },
                        label = { Text(label) },
                        colors = filledFieldColors,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = imeAction)
                    )
                }
            }
        }

        // ── Fuel Level Card ───────────────────────────────────────────────────
        ElevatedCard(
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(cardPadding)) {
                Text(
                    text = "CURRENT FUEL LEVEL",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = currentFuelLevelPercentage,
                    onValueChange = {
                        currentFuelLevelPercentage = it
                        focusManager.clearFocus()
                    },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        activeTrackColor = Color(0xFFF5C842)
                    )
                )
                val gallonsInTank = (tankCapacity.toDoubleOrNull() ?: 0.0) * (currentFuelLevelPercentage / 100.0)
                Text(
                    text = "${currentFuelLevelPercentage.toInt()}% full  ·  ${"%.1f".format(gallonsInTank)} gal in tank",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // ── Result Card ───────────────────────────────────────────────────────
        ElevatedCard(
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(bottom = 32.dp)
        ) {
            Column(modifier = Modifier.padding(cardPadding)) {
                Text(
                    text = "BLEND RESULT",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))

                Box(modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
                ) {
                Crossfade(
                    targetState = validationError != null,
                    animationSpec = tween(200),
                    label = "blend_result"
                ) { hasError ->
                    Column {
                        if (hasError) {
                            Text(
                                text = validationError ?: "",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface)) {
                                        append("${"%.2f".format(blendResult.gallonsE85Needed)} gal ")
                                    }
                                    withStyle(SpanStyle(color = e85Blue)) { append("E85") }
                                },
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "+ ${"%.2f".format(blendResult.gallonsPumpGasNeeded)} gal pump gas",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(12.dp))

                            val resultingEthanolPercentage = run {
                                val currentFuelVolume = (tankCapacity.toDoubleOrNull() ?: 0.0) * (currentFuelLevelPercentage.toDouble() / 100.0)
                                val eCurrent = currentEthanolPercentage.toDoubleOrNull() ?: 0.0
                                val eE85 = pumpE85Percentage.toDoubleOrNull() ?: 0.0
                                val eGas = pumpGasPercentage.toDoubleOrNull() ?: 0.0
                                val totalEthanol = currentFuelVolume * (eCurrent / 100.0) +
                                    blendResult.gallonsE85Needed * (eE85 / 100.0) +
                                    blendResult.gallonsPumpGasNeeded * (eGas / 100.0)
                                val totalVolume = currentFuelVolume + blendResult.totalFillVolume
                                if (totalVolume > 0) (totalEthanol / totalVolume) * 100.0 else 0.0
                            }

                            Text(
                                text = "Resulting mixture: ${"%.2f".format(resultingEthanolPercentage)}% ethanol",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                } // end Box
            }
        }
    }
}

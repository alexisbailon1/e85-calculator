package com.example.e85calculator.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Define modern "Expressive" dark colors
private val DarkColorScheme = darkColorScheme(
    primary = Color.White, // Your custom blue
    onPrimary = Color.Gray,
    surfaceContainer = Color(0xFF1E1E22), // Modern dark background
    surfaceContainerHighest = Color(0xFF333338),
    onSurface = Color.White
)

@Composable
fun E85CalculatorTheme(
    // Force true for your carbon fiber theme
    content: @Composable () -> Unit
) {
    // You can keep dynamic color, but for a carbon fiber dashboard,
    // a consistent dark theme often looks more "pro."
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography, // Ensure this exists in your project
        content = content
    )
}
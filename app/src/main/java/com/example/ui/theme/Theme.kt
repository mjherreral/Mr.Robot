package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FsocietyColorScheme = darkColorScheme(
    primary = FsocietyRed,
    onPrimary = Color.White,
    secondary = TerminalGreen,
    onSecondary = Color.Black,
    tertiary = ScanningCyan,
    onTertiary = Color.Black,
    background = CyberDarkBackground,
    onBackground = TextPrimaryGlow,
    surface = CyberPanelDark,
    onSurface = TextPrimaryGlow,
    outline = CyberBorderMuted
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // We force dark hacker mode for immersive cyber experience
    dynamicColor: Boolean = false, // Disable dynamic colors key to force Elliot Alderson design palette
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FsocietyColorScheme,
        typography = Typography,
        content = content
    )
}

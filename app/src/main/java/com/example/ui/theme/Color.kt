package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Pure hacker-terminal themed color tokens
val CyberDarkBackground = Color(0xFF050505)
val CyberPanelDark = Color(0xFF0C0C0C)
val CyberBorderMuted = Color(0x3300FF41) // #00FF41 at 20% opacity matching "border-[#00FF41]/20"

val FsocietyRed = Color(0xFFEF4444) // matches red-500 from Sleek design
val TerminalGreen = Color(0xFF00FF41) // matches classic #00FF41 from Sleek design
val ScanningCyan = Color(0xFF60A5FA) // matches blue-400
val WarningAmber = Color(0xFFFBBF24) // matches amber-400/yellow

val TextPrimaryGlow = Color(0xFF00FF41) // Green monochrome primary glow from Sleek
val TextMuted = Color(0x9900FF41) // 60% opacity green matching standard terminal text opacity

// Backward compat template color mapping definitions
val Purple80 = FsocietyRed
val PurpleGrey80 = TerminalGreen
val Pink80 = ScanningCyan

val Purple40 = FsocietyRed
val PurpleGrey40 = TerminalGreen
val Pink40 = WarningAmber

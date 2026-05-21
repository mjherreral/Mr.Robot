package com.example

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun HackerTerminalApp(viewModel: TerminalViewModel) {
    val isBooting by viewModel.isBooting.collectAsState()
    val bootLogs by viewModel.bootLogs.collectAsState()
    val bootProgress by viewModel.bootProgress.collectAsState()

    val currentTab by viewModel.currentTab.collectAsState()

    val terminalHistory by viewModel.terminalHistory.collectAsState()
    val scanTargets by viewModel.scanTargets.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    val cpuLogHistory by viewModel.cpuLogHistory.collectAsState()
    val systemDaemonLogs by viewModel.systemDaemonLogs.collectAsState()

    val gameNodes by viewModel.gameNodes.collectAsState()
    val targetKey by viewModel.targetKey.collectAsState()
    val breachScore by viewModel.breachScore.collectAsState()
    val breachAttempts by viewModel.breachAttempts.collectAsState()
    val breachMessage by viewModel.breachMessage.collectAsState()

    val decryptionProgress by viewModel.decryptionProgress.collectAsState()
    val decryptedMessage by viewModel.decryptedMessage.collectAsState()

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    var isCRTeffectEnabled by remember { mutableStateOf(true) }

    // Auto scroll CLI terminal to last element when history changes
    LaunchedEffect(terminalHistory.size) {
        if (terminalHistory.isNotEmpty()) {
            lazyListState.animateScrollToItem(terminalHistory.size - 1)
        }
    }

    // Main Canvas overlay drawing real analog CRT scanline filters and screen flicker
    val infiniteTransition = rememberInfiniteTransition(label = "CRTFlicker")
    val flickerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(120, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CRTFlicker"
    )

    val radarRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarSweep"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberDarkBackground)
            .drawBehind {
                if (isCRTeffectEnabled) {
                    // Modern CRT Scanlines at 6dp spacing
                    val spacing = 6.dp.toPx()
                    val lineCount = (size.height / spacing).toInt()
                    for (i in 0..lineCount) {
                        val y = i * spacing
                        drawLine(
                            color = Color(0x1B00FF41), // Classic terminal green scanline
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f
                        )
                    }

                    // Authentic Radial 6dp dot-matrix overlay texture
                    val dotSpacing = 6.dp.toPx()
                    val cols = (size.width / dotSpacing).toInt()
                    val rows = (size.height / dotSpacing).toInt()
                    for (col in 0..cols) {
                        for (row in 0..rows) {
                            drawCircle(
                                color = Color(0x1300FF41),
                                radius = 1.0f,
                                center = Offset(col * dotSpacing, row * dotSpacing)
                            )
                        }
                    }
                }
            }
    ) {
        if (isBooting) {
            // RETRO HACKER BOOT SEQUENCE SCREEN
            BootScreen(bootLogs = bootLogs, progress = bootProgress)
        } else {
            // CENTRAL WORKSPACE LAYOUT
            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = {
                    HackerNavigationBar(currentTab = currentTab, onTabSelected = { viewModel.setTab(it) })
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // UPPER DIAGNOSTIC HEADER
                    SystemHeader(
                        isCRTeffectEnabled = isCRTeffectEnabled,
                        onCRTChanged = { isCRTeffectEnabled = it },
                        breachAttempts = breachAttempts,
                        breachScore = breachScore
                    )

                    // ACTIVE DECRYPTION OVERLAY PROCESS STATE (When matrix cipher is running)
                    AnimatedVisibility(
                        visible = decryptionProgress >= 0f,
                        enter = slideInVertically() + fadeIn(),
                        exit = slideOutVertically() + fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1C0D15))
                                .border(1.dp, FsocietyRed)
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "CYPHER DECRYPTER OVERRIDE DAEMON ACTIVE...",
                                        color = FsocietyRed,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "${(decryptionProgress * 100).toInt()}%",
                                        color = FsocietyRed,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { decryptionProgress },
                                    modifier = Modifier.fillMaxWidth().height(4.dp),
                                    color = FsocietyRed,
                                    trackColor = Color(0xFF331117)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = decryptedMessage,
                                    color = Color(0xFFFF8899),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // TABS SWAP VIEWPORT
                    Box(modifier = Modifier.weight(1f)) {
                        when (currentTab) {
                            0 -> {
                                CliTerminalView(
                                    terminalHistory = terminalHistory,
                                    lazyListState = lazyListState,
                                    onCommandSubmit = { viewModel.executeCommand(it) },
                                    isScanning = isScanning
                                )
                            }
                            1 -> {
                                SystemDiagnosticHud(
                                    cpuLogHistory = cpuLogHistory,
                                    systemDaemonLogs = systemDaemonLogs,
                                    radarRotation = radarRotation,
                                    scanTargets = scanTargets,
                                    isScanning = isScanning,
                                    onIpScanClick = { viewModel.executeCommand("scan") },
                                    onExploitClick = { viewModel.setTab(2) }
                                )
                            }
                            2 -> {
                                DatabaseBreachView(
                                    gameNodes = gameNodes,
                                    targetKey = targetKey,
                                    breachAttempts = breachAttempts,
                                    breachMessage = breachMessage,
                                    onNodeClick = { viewModel.onBreachNodeClicked(it) },
                                    onResetGame = { viewModel.generateNewBreachGame() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- BOOT SCREEN SUB-COMPONENT ----------------

@Composable
fun BootScreen(bootLogs: List<String>, progress: Float) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "FSOCIETY CORE KERNEL BOOT SEQUENCE",
                color = FsocietyRed,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            HorizontalDivider(color = FsocietyRed.copy(alpha = 0.5f), thickness = 1.dp)

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(bootLogs) { log ->
                    Text(
                        text = ">> $log",
                        color = TerminalGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }

        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "BYPASSING DISK SECURE CHANNELS",
                    color = WarningAmber,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    color = WarningAmber,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = WarningAmber,
                trackColor = Color(0xFF2B2100)
            )
        }
    }
}

// ---------------- HEADER DASHBOARD VIEW ----------------

@Composable
fun SystemHeader(
    isCRTeffectEnabled: Boolean,
    onCRTChanged: (Boolean) -> Unit,
    breachAttempts: Int,
    breachScore: Int
) {
    var sysTime by remember { mutableStateOf("02:43 AM") }
    LaunchedEffect(Unit) {
        while (true) {
            val calendar = java.util.Calendar.getInstance()
            val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
            val min = calendar.get(java.util.Calendar.MINUTE)
            val hour12 = if (hour % 12 == 0) 12 else hour % 12
            val amPm = if (hour < 12) "AM" else "PM"
            sysTime = String.format("%02d:%02d %s", hour12, min, amPm)
            kotlinx.coroutines.delay(15000)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberBorderMuted, RoundedCornerShape(0.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberPanelDark),
        shape = RoundedCornerShape(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Simulated Status Bar Top-Row of "Sleek Interface" design
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = sysTime,
                        color = TerminalGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "FSOCIETY_VPN.ENC",
                        color = TerminalGreen.copy(alpha = 0.5f),
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                // Battery + Signal Indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Minimalist Signal Bars
                    Row(
                        modifier = Modifier.height(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(1.5.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Box(modifier = Modifier.width(2.dp).fillMaxHeight(1.0f).background(TerminalGreen))
                        Box(modifier = Modifier.width(2.dp).fillMaxHeight(0.8f).background(TerminalGreen))
                        Box(modifier = Modifier.width(2.dp).fillMaxHeight(0.6f).background(TerminalGreen))
                        Box(modifier = Modifier.width(2.dp).fillMaxHeight(0.4f).background(TerminalGreen.copy(alpha = 0.3f)))
                    }

                    // Battery Box Outline with 85% solid cell fill
                    Box(
                        modifier = Modifier
                            .width(20.dp)
                            .height(10.dp)
                            .border(1.dp, TerminalGreen),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.85f)
                                .background(TerminalGreen)
                        )
                    }
                }
            }

            HorizontalDivider(color = CyberBorderMuted, thickness = 1.dp)

            // Main Header Branding & Actions content row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "ROOT@FSOCIETY:~# INIT_HANDSHAKE",
                        color = TerminalGreen.copy(alpha = 0.6f),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )

                    // Impacting "f.SOCIETY" visual logo
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(TerminalGreen)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "f.",
                                color = Color.Black,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = "SOCIETY",
                            color = TerminalGreen,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = (-0.5).sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Credit score indicator
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = "CREDITS",
                            color = WarningAmber,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "$breachScore",
                            color = WarningAmber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // CRT Toggle Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isCRTeffectEnabled) Color(0x3300FF41) else Color(0x1A6E7B91))
                            .border(1.dp, if (isCRTeffectEnabled) TerminalGreen else Color.Gray, RoundedCornerShape(4.dp))
                            .clickable { onCRTChanged(!isCRTeffectEnabled) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isCRTeffectEnabled) "CRT: ON" else "CRT: OFF",
                            color = if (isCRTeffectEnabled) TerminalGreen else Color.Gray,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ---------------- TAB 0: CLI SHELL CONTROLLER ----------------

@Composable
fun CliTerminalView(
    terminalHistory: List<TerminalLine>,
    lazyListState: androidx.compose.foundation.lazy.LazyListState,
    onCommandSubmit: (String) -> Unit,
    isScanning: Boolean
) {
    var rawInputState by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    val preloadedShortcuts = listOf("Whoami", "Help", "Scan", "Decrypt", "Logcat", "Hack", "Clear")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // SCROLLABLE TERMINAL OUTPUT PANEL
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, CyberBorderMuted, RoundedCornerShape(4.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF030303)),
            shape = RoundedCornerShape(4.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize().testTag("terminal_logs_viewport")
                ) {
                    items(terminalHistory) { line ->
                        val color = when (line.type) {
                            LineType.INPUT -> Color.White
                            LineType.INFO_GREEN -> TerminalGreen
                            LineType.ALERT_RED -> FsocietyRed
                            LineType.WARNING_AMBER -> WarningAmber
                            LineType.DATA_CYAN -> ScanningCyan
                        }
                        Text(
                            text = line.text,
                            color = color,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // TOUCH SHORTCUT TRIGGERS FOR FAST MOBILE PLAY
        Text(
            text = "HACK MACRO TRIGGERS:",
            color = TextMuted,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            preloadedShortcuts.forEach { shortcut ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(CyberPanelDark)
                        .border(1.dp, FsocietyRed.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .clickable {
                            onCommandSubmit(shortcut.lowercase())
                        }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = shortcut,
                        color = FsocietyRed,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // CHAT ASSISTANT COMPANION TRIGGER RECOMMENDATION
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFFF1E30).copy(alpha = 0.1f))
                .border(1.dp, FsocietyRed.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "💬 TIP: Escribe 'chat <mensaje>' para hablar con Elliot usando Gemini AI.",
                color = Color(0xFFFFB3B8),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // PHYSICAL CLI TYPING CONTAINER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberPanelDark, RoundedCornerShape(4.dp))
                .border(1.dp, CyberBorderMuted, RoundedCornerShape(4.dp))
                .padding(horizontal = 10.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "root@fsociety:~# ",
                color = FsocietyRed,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )

            TextField(
                value = rawInputState,
                onValueChange = { rawInputState = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("terminal_input_field"),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = FsocietyRed
                ),
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (rawInputState.isNotBlank()) {
                            onCommandSubmit(rawInputState)
                            rawInputState = ""
                            keyboardController?.hide()
                        }
                    }
                )
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(FsocietyRed)
                    .testTag("send_command_button")
                    .clickable {
                        if (rawInputState.isNotBlank()) {
                            onCommandSubmit(rawInputState)
                            rawInputState = ""
                            keyboardController?.hide()
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "RUN",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// ---------------- TAB 1: RADAR & GRAPHS COCKPIT ----------------

@Composable
fun SystemDiagnosticHud(
    cpuLogHistory: List<Float>,
    systemDaemonLogs: List<String>,
    radarRotation: Float,
    scanTargets: List<NetworkTarget>,
    isScanning: Boolean,
    onIpScanClick: () -> Unit,
    onExploitClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            // HUD SWEEP RADAR
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .border(1.dp, CyberBorderMuted, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberPanelDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "WLAN0 LOCAL PROBING",
                        color = ScanningCyan,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Canvas(modifier = Modifier.size(64.dp)) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val radius = size.width / 2

                        // Circle concentric grids
                        drawCircle(
                            color = ScanningCyan.copy(alpha = 0.2f),
                            radius = radius,
                            center = center,
                            style = Stroke(1f)
                        )
                        drawCircle(
                            color = ScanningCyan.copy(alpha = 0.2f),
                            radius = radius * 0.6f,
                            center = center,
                            style = Stroke(1f)
                        )
                        drawCircle(
                            color = ScanningCyan.copy(alpha = 0.2f),
                            radius = radius * 0.3f,
                            center = center,
                            style = Stroke(1f)
                        )

                        // Angle line radar sweep
                        val angleRad = Math.toRadians(radarRotation.toDouble())
                        val targetLineX = center.x + radius * Math.cos(angleRad).toFloat()
                        val targetLineY = center.y + radius * Math.sin(angleRad).toFloat()
                        drawLine(
                            color = ScanningCyan,
                            start = center,
                            end = Offset(targetLineX, targetLineY),
                            strokeWidth = 2f
                        )

                        // Mock target dots blinking inside radar
                        val target1 = Offset(center.x - radius * 0.4f, center.y + radius * 0.3f)
                        val target2 = Offset(center.x + radius * 0.5f, center.y - radius * 0.5f)

                        drawCircle(color = Color.Red, radius = 4f, center = target1)
                        drawCircle(color = TerminalGreen, radius = 4f, center = target2)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "PROXIES: SHIELDED",
                        color = TerminalGreen,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // WAVE CPU CPU GRAPH
            Card(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
                    .border(1.dp, CyberBorderMuted, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberPanelDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "CPU BANDWIDTH HISTORY",
                        color = WarningAmber,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 4.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val path = Path()
                            val spacingX = size.width / (cpuLogHistory.size - 1)
                            val heightRatio = size.height / 100f

                            // Draw grid lines
                            for (gridY in 0..4) {
                                val yVal = size.height * (gridY / 4f)
                                drawLine(
                                    color = Color(0x13FFFFFF),
                                    start = Offset(0f, yVal),
                                    end = Offset(size.width, yVal),
                                    strokeWidth = 1f
                                )
                            }

                            cpuLogHistory.forEachIndexed { idx, valPercent ->
                                val x = idx * spacingX
                                val y = size.height - (valPercent * heightRatio)
                                if (idx == 0) {
                                    path.moveTo(x, y)
                                } else {
                                    path.lineTo(x, y)
                                }
                            }

                            drawPath(
                                path = path,
                                color = WarningAmber,
                                style = Stroke(
                                    width = 2.5f,
                                    cap = StrokeCap.Round
                                )
                            )

                            // Under-shaded path
                            val fillPath = Path().apply {
                                addPath(path)
                                lineTo(size.width, size.height)
                                lineTo(0f, size.height)
                                close()
                            }
                            drawPath(
                                path = fillPath,
                                color = WarningAmber.copy(alpha = 0.08f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "CPU: 2.45GHz",
                            color = WarningAmber,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "ENTROPY: 4096B",
                            color = TextMuted,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // INTERACTIVE ACTION GRID MODULE (Replicating "Sleek Interface" controls)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card 1: Network Packet Sniffer
            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, TerminalGreen.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .clickable { onIpScanClick() },
                colors = CardDefaults.cardColors(containerColor = CyberPanelDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "NETWORK",
                        color = TerminalGreen.copy(alpha = 0.5f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "PACKET SNIFFER",
                        color = TerminalGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(text = "v1.2.4", color = TerminalGreen.copy(alpha = 0.4f), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        
                        // Pulsing indicator dot from theme
                        val infiniteTransition = rememberInfiniteTransition(label = "pulsing")
                        val pulseAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.3f,
                            targetValue = 1.0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulseAlpha"
                        )
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(TerminalGreen.copy(alpha = pulseAlpha), RoundedCornerShape(3.5.dp))
                        )
                    }
                }
            }

            // Card 2: Exploit SQL Injector
            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, FsocietyRed.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .clickable { onExploitClick() },
                colors = CardDefaults.cardColors(containerColor = CyberPanelDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "EXPLOIT",
                        color = FsocietyRed.copy(alpha = 0.5f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "SQL_INJECTOR",
                        color = FsocietyRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(text = "CRITICAL", color = FsocietyRed.copy(alpha = 0.6f), fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(FsocietyRed)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card 3: Encrypt P2P Tunnel
            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, TerminalGreen.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberPanelDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "ENCRYPT",
                        color = TerminalGreen.copy(alpha = 0.5f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "P2P_TUNNEL",
                        color = TerminalGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "128.0.0.1:9090", color = TerminalGreen.copy(alpha = 0.5f), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                }
            }

            // Card 4: Social Dark Mail
            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, TerminalGreen.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberPanelDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "SOCIAL",
                        color = TerminalGreen.copy(alpha = 0.5f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "DARK_MAIL",
                        color = TerminalGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "3 NEW MSG", color = WarningAmber, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // HIGH FIDELITY SYSTEM LOG CONTAINER (Replicating exact HTML text blocks & progress ratio bar)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(2.dp, TerminalGreen.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
            ) {
                Text(
                    text = "[SYSTEM LOG v9.02]",
                    color = TerminalGreen.copy(alpha = 0.4f),
                    fontSize = 9.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(systemDaemonLogs.reversed()) { log ->
                        Text(
                            text = log,
                            color = if (log.contains("ALERT") || log.contains("INTRUSION")) FsocietyRed else TerminalGreen,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                }

                // Custom 64% bar component from visual design HTML
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color(0xFF131313))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.64f)
                            .background(TerminalGreen)
                    )
                }
            }
        }

        // GLITCH IDENTITY WIDGET FOOTER CARD
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(TerminalGreen.copy(alpha = 0.05f))
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        // Drawing top and bottom border matching "border-t-2 border-b-2 border-[#00FF41]"
                        drawLine(
                            color = TerminalGreen,
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 1.5.dp.toPx()
                        )
                        drawLine(
                            color = TerminalGreen,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }
                    .padding(vertical = 10.dp, horizontal = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CURRENT IDENTITY",
                            color = TerminalGreen.copy(alpha = 0.5f),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "ELLIOT_ALDERSON",
                            color = TerminalGreen,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "STATUS",
                            color = TerminalGreen.copy(alpha = 0.5f),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "OFF_THE_GRID",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

// ---------------- TAB 2: BREACH NODE GAME CHASSIS ----------------

@Composable
fun DatabaseBreachView(
    gameNodes: List<BreachNode>,
    targetKey: String,
    breachAttempts: Int,
    breachMessage: String,
    onNodeClick: (Int) -> Unit,
    onResetGame: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // TARGET HEADING CONSOLE
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorderMuted, RoundedCornerShape(4.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberPanelDark),
                shape = RoundedCornerShape(4.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "ECORP SECURE GRID OVERRIDE DIRECTIVE",
                        color = FsocietyRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = breachMessage,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // LETS DISPLAY DETECTIVE ATTEMPTS NODAL STATS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "CORRUPCIÓN EN PROGRESO: ",
                        color = TextMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = targetKey,
                        color = WarningAmber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row {
                    Text(
                        text = "INTENTOS EN RED: ",
                        color = TextMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    for (i in 1..3) {
                        Text(
                            text = "▲ ",
                            color = if (i <= breachAttempts) FsocietyRed else Color.DarkGray,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4X4 DECRYPT KEYPAD GRID
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .background(Color.Black, RoundedCornerShape(8.dp))
                    .border(1.dp, CyberBorderMuted, RoundedCornerShape(8.dp))
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceAround
                ) {
                    for (row in 0 until 4) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            for (col in 0 until 4) {
                                val index = row * 4 + col
                                if (index < gameNodes.size) {
                                    val node = gameNodes[index]

                                    val containerColor = when {
                                        node.hacked -> Color(0xFF0D3E1A)
                                        node.firewallTriggered -> Color(0xFF5A111A)
                                        else -> CyberPanelDark
                                    }

                                    val borderColor = when {
                                        node.hacked -> TerminalGreen
                                        node.firewallTriggered -> FsocietyRed
                                        else -> CyberBorderMuted
                                    }

                                    val textColor = when {
                                        node.hacked -> TerminalGreen
                                        node.firewallTriggered -> FsocietyRed
                                        else -> ScanningCyan
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(containerColor)
                                            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
                                            .clickable { onNodeClick(node.id) }
                                            .testTag("breach_node_${node.id}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = node.hexKey,
                                                color = textColor,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = if (node.hacked) "HACKD" else if (node.firewallTriggered) "WARN" else "SEC",
                                                color = textColor.copy(alpha = 0.5f),
                                                fontSize = 7.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // RESET GRIDS MANUALLY
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .clip(RoundedCornerShape(4.dp))
                .background(WarningAmber.copy(alpha = 0.1f))
                .border(1.dp, WarningAmber, RoundedCornerShape(4.dp))
                .clickable { onResetGame(0) }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "REINICIAR MAIN DE E-CORP",
                color = WarningAmber,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// ---------------- CUSTOM LOWER WORKSPACE TABS INTERACTION ----------------

@Composable
fun HackerNavigationBar(currentTab: Int, onTabSelected: (Int) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CyberPanelDark
    ) {
        Column {
            HorizontalDivider(color = CyberBorderMuted, thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp, top = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                val tabs = listOf(
                    "🖥️ CLI SHELL",
                    "📊 DIAGNOSTIC HUD",
                    "🛡️ BREACH CORE"
                )

                tabs.forEachIndexed { index, title ->
                    val isSelected = currentTab == index
                    val textColor = if (isSelected) FsocietyRed else Color.Gray
                    val boxBg = if (isSelected) FsocietyRed.copy(alpha = 0.1f) else Color.Transparent

                    val tabModifier = if (isSelected) {
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(boxBg)
                            .border(1.dp, FsocietyRed, RoundedCornerShape(4.dp))
                            .clickable { onTabSelected(index) }
                    } else {
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(boxBg)
                            .clickable { onTabSelected(index) }
                    }

                    Box(
                        modifier = tabModifier.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = textColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

package com.example

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.random.Random

// Represents different colors or styles of terminal outputs
enum class LineType {
    INPUT,          // white input prefaced with user@terminal
    INFO_GREEN,     // terminal green output
    ALERT_RED,      // red fsociety warning
    WARNING_AMBER,  // yellow compilation/process state
    DATA_CYAN       // scan or diagnostic matrix data
}

data class TerminalLine(
    val text: String,
    val type: LineType = LineType.INFO_GREEN
)

data class NetworkTarget(
    val ip: String,
    val port: Int,
    val service: String,
    val status: String,
    val risk: String
)

data class BreachNode(
    val id: Int,
    val hexKey: String,
    val isVulnerable: Boolean,
    val hacked: Boolean = false,
    val firewallTriggered: Boolean = false
)

class TerminalViewModel : ViewModel() {

    // Booting simulation status
    private val _isBooting = MutableStateFlow(true)
    val isBooting: StateFlow<Boolean> = _isBooting.asStateFlow()

    private val _bootProgress = MutableStateFlow(0f)
    val bootProgress: StateFlow<Float> = _bootProgress.asStateFlow()

    private val _bootLogs = MutableStateFlow<List<String>>(emptyList())
    val bootLogs: StateFlow<List<String>> = _bootLogs.asStateFlow()

    // Screen tab selection (0 = TERMINAL, 1 = SYSTEM HUD, 2 = BREACH CORE)
    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    // Terminal History logs
    private val _terminalHistory = MutableStateFlow<List<TerminalLine>>(emptyList())
    val terminalHistory: StateFlow<List<TerminalLine>> = _terminalHistory.asStateFlow()

    // Network targets (from scan)
    private val _scanTargets = MutableStateFlow<List<NetworkTarget>>(emptyList())
    val scanTargets: StateFlow<List<NetworkTarget>> = _scanTargets.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // CPU waveform coordinates for the system radar / graph
    private val _cpuLogHistory = MutableStateFlow<List<Float>>(List(15) { 30f + Random.nextFloat() * 40f })
    val cpuLogHistory: StateFlow<List<Float>> = _cpuLogHistory.asStateFlow()

    // Active systems state logs (HUD stream)
    private val _systemDaemonLogs = MutableStateFlow<List<String>>(emptyList())
    val systemDaemonLogs: StateFlow<List<String>> = _systemDaemonLogs.asStateFlow()

    // E-Corp Breach Game configuration
    private val _gameActive = MutableStateFlow(false)
    val gameActive: StateFlow<Boolean> = _gameActive.asStateFlow()

    private val _gameNodes = MutableStateFlow<List<BreachNode>>(emptyList())
    val gameNodes: StateFlow<List<BreachNode>> = _gameNodes.asStateFlow()

    private val _targetKey = MutableStateFlow("")
    val targetKey: StateFlow<String> = _targetKey.asStateFlow()

    private val _breachScore = MutableStateFlow(0)
    val breachScore: StateFlow<Int> = _breachScore.asStateFlow()

    private val _breachAttempts = MutableStateFlow(3)
    val breachAttempts: StateFlow<Int> = _breachAttempts.asStateFlow()

    private val _breachMessage = MutableStateFlow("ECORP MAINFRAME ENCRYPTED. MATCH TARGET HEX CODES TO BYPASS FIREWALLS.")
    val breachMessage: StateFlow<String> = _breachMessage.asStateFlow()

    // Decryption matrix output simulation status
    private val _decryptionProgress = MutableStateFlow(-1f) // -1.0f means inactive
    val decryptionProgress: StateFlow<Float> = _decryptionProgress.asStateFlow()

    private val _decryptedMessage = MutableStateFlow("")
    val decryptedMessage: StateFlow<String> = _decryptedMessage.asStateFlow()

    private var scanJob: Job? = null
    private var logJob: Job? = null
    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    init {
        startBootSequence()
        startDaemonLogsGenerator()
        generateNewBreachGame()
    }

    fun setTab(index: Int) {
        _currentTab.value = index
    }

    // Launch automated booting terminal logging
    private fun startBootSequence() {
        viewModelScope.launch {
            val logs = listOf(
                "SYSBOOT init v5.9.0-fsociety-core...",
                "LOADING kernel loops... Blowfish-256CBC active.",
                "CHECKING file integrity... OK",
                "LOCATING network interfaces...",
                "INTERFACE wlan0: status UP, MAC: f0:f1:a2:59:b3:ec",
                "ATTACHING TOR routing networks... tunnel 9050 initialized.",
                "ESTABLISHING SECURE PORTALS... (100% anonymous encryption)",
                "CONNECTING proxy tunnels [12.51.68.99, 142.25.109.128, 92.51.10.3]",
                "DECRYPTING local sector blocks...",
                "SECURE SHELL INITIALIZED - welcome back, Elliot Alderson."
            )

            for (i in logs.indices) {
                delay(120 + i * 50L) // Variable delays for typing effect
                val currentList = _bootLogs.value.toMutableList()
                currentList.add(logs[i])
                _bootLogs.value = currentList
                _bootProgress.value = (i + 1).toFloat() / logs.size
            }

            delay(600)
            _isBooting.value = false

            // Appends welcome messages in console history
            appendLine("==============================================", LineType.ALERT_RED)
            appendLine("        FSOCIETY SECURE TERMINAL v9.0", LineType.ALERT_RED)
            appendLine("     OUR DEMOCRACY HAS BEEN HACKED.", LineType.ALERT_RED)
            appendLine("==============================================", LineType.ALERT_RED)
            appendLine("Type 'help' to review shell command scripts.", LineType.INFO_GREEN)
            appendLine("Secure node active at tunnel: 127.0.0.1:9050", LineType.DATA_CYAN)
        }
    }

    // Streams rolling background activity logs in System HUD tab
    private fun startDaemonLogsGenerator() {
        logJob = viewModelScope.launch {
            val templates = listOf(
                "wlan0: sniffed arp broadcast from 192.168.1.1",
                "tor: connected tunnel to exit node 142.251.66.183:443",
                "fsociety-daemon: background entropy pool updated (+12 bits)",
                "sys-monitor: thermal core check -> CPU0: 42C, CPU1: 44C",
                "e-corp-sniffer: caught outgoing data packet (98 bytes)",
                "matrix-loop: decrypted packet buffer hex digest 0x3A8F",
                "tunnel-secure: verified hash sign (GPG key 0x59B3EC)",
                "net-probe: scan target 192.251.68.21 -> SYN handshake open"
            )

            while (true) {
                delay(1500 + Random.nextLong(2000))
                val logList = _systemDaemonLogs.value.toMutableList()
                logList.add("[${currentTimeString()}] ${templates.random()}")
                if (logList.size > 20) {
                    logList.removeAt(0)
                }
                _systemDaemonLogs.value = logList

                // Keep updating CPU Waveform values
                val currentCpu = _cpuLogHistory.value.toMutableList()
                currentCpu.add(20f + Random.nextFloat() * 65f)
                if (currentCpu.size > 15) {
                    currentCpu.removeAt(0)
                }
                _cpuLogHistory.value = currentCpu
            }
        }
    }

    private fun currentTimeString(): String {
        val seconds = (System.currentTimeMillis() / 1000) % 60
        val minutes = (System.currentTimeMillis() / 60000) % 60
        val hours = (System.currentTimeMillis() / 3600000 + 12) % 24 // UTC simulation offset
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun appendLine(text: String, type: LineType = LineType.INFO_GREEN) {
        val current = _terminalHistory.value.toMutableList()
        current.add(TerminalLine(text, type))
        if (current.size > 60) {
            current.removeAt(0)
        }
        _terminalHistory.value = current
    }

    // Handles interactive Hacker CLI commands
    fun executeCommand(commandRaw: String) {
        val cleanCmd = commandRaw.trim()
        if (cleanCmd.isEmpty()) return

        appendLine("root@fsociety:~# $cleanCmd", LineType.INPUT)

        val parts = cleanCmd.split(" ")
        val cmd = parts[0].lowercase()

        when (cmd) {
            "help" -> {
                appendLine("Available shell commands in secure sector:", LineType.INFO_GREEN)
                appendLine("  help      - Print list of active scripts.", LineType.DATA_CYAN)
                appendLine("  whoami    - Reveal logged operational privilege.", LineType.DATA_CYAN)
                appendLine("  sysinfo   - Inspect terminal client diagnostics.", LineType.DATA_CYAN)
                appendLine("  scan      - Execute subnet target probe on E-Corp server IPs.", LineType.DATA_CYAN)
                appendLine("  decrypt   - Deploy binary cypher translation on secure strings.", LineType.DATA_CYAN)
                appendLine("  logcat    - Stream raw kernel background packets.", LineType.DATA_CYAN)
                appendLine("  hack      - Launch cyber overload overwrite on E-Corp.", LineType.DATA_CYAN)
                appendLine("  chat <msg>- Establish Tor messaging canal with Elliot Alderson.", LineType.DATA_CYAN)
                appendLine("  clear     - Wipe out stdout buffer archives.", LineType.DATA_CYAN)
            }
            "whoami" -> {
                appendLine("IDENTITY: Elliot Alderson (Elite Hacker)", LineType.INFO_GREEN)
                appendLine("AFFILIATION: f_society - Decentralized underground cyber army", LineType.ALERT_RED)
                appendLine("STATUS: Operating under safe proxy encryption layers.", LineType.DATA_CYAN)
                appendLine("AUTHORIZATION: Level root (super-user)", LineType.INFO_GREEN)
            }
            "clear" -> {
                _terminalHistory.value = emptyList()
            }
            "sysinfo" -> {
                appendLine("--- HARDWARE & ROUTING DIAGNOSTICS ---", LineType.INFO_GREEN)
                appendLine("OS MODEL  : Alpine-Lnx-Tor-Kernel-v5.9.1", LineType.DATA_CYAN)
                appendLine("SECURE IP : Static route -> fsociety proxy node", LineType.DATA_CYAN)
                appendLine("TOR RELAY : node_us_relay_9050.onion", LineType.INFO_GREEN)
                appendLine("TUNNELING : GnuTLS AES-256 GCM multiplexed", LineType.DATA_CYAN)
                appendLine("BATTERY   : Term-status OK (Discharging @ -480mA)", LineType.WARNING_AMBER)
                appendLine("CPU CORES : 8x ARM Cortex Crypto Units @ 2.45GHz", LineType.INFO_GREEN)
            }
            "scan" -> {
                runScannerCommand()
            }
            "decrypt" -> {
                runMatrixDecryption()
            }
            "logcat" -> {
                runLogsStream()
            }
            "hack" -> {
                runEcorpDDoS()
            }
            "chat" -> {
                if (parts.size < 2) {
                    appendLine("ERROR: Command input require content. Use: chat <message>", LineType.ALERT_RED)
                } else {
                    val message = parts.subList(1, parts.size).joinToString(" ")
                    queryElliotGemini(message)
                }
            }
            else -> {
                appendLine("ERR: Command '$cmd' not found. Type 'help' to review command manuals.", LineType.ALERT_RED)
            }
        }
    }

    // "scan" Command: Simulates subnet scan of E-Corp network
    private fun runScannerCommand() {
        if (_isScanning.value) {
            appendLine("SCANNER ALREADY DEPLOYED. WAITING RESIDUAL RESULTS...", LineType.WARNING_AMBER)
            return
        }
        _isScanning.value = true
        _scanTargets.value = emptyList()
        appendLine("DEPLOYING PORT SNIFFER OVER RANGE: 192.251.68.[1-32]...", LineType.WARNING_AMBER)

        scanJob = viewModelScope.launch {
            val ipsToScan = listOf("192.251.68.21", "192.251.68.99", "192.251.68.102", "192.251.68.5")
            val targetDetails = listOf(
                NetworkTarget("192.251.68.21", 22, "SSH/OpenSSH 7.2p2", "OPEN", "HEARTBLEED CVE-2014-0160"),
                NetworkTarget("192.251.68.21", 80, "HTTP/Nginx web server", "OPEN", "XSS SCRIPT INJECTION VULN"),
                NetworkTarget("192.251.68.99", 3306, "MySQL core databases", "FILTERED", "CRITICAL PRIVILEGE EXPLOIT"),
                NetworkTarget("192.251.68.102", 443, "HTTPS E-Corp Intranet", "OPEN", "SECURE TUNNEL TRUST VALID"),
                NetworkTarget("192.251.68.5", 8080, "Apache TomCat admin panel", "CLOSED", "LOW RISK RISK-ZERO")
            )

            for (target in targetDetails) {
                delay(1200L)
                appendLine("TCP-SYN Probe sent to ${target.ip}:${target.port}... RECEIVED RESPONSE", LineType.DATA_CYAN)
                appendLine(" -> PORT STATUS: ${target.status} (service: ${target.service})", LineType.INFO_GREEN)
                if (target.status == "OPEN") {
                    appendLine("    [VULN TARGET FOUND]: ${target.risk}", LineType.ALERT_RED)
                }

                // Add to scanner targets list shown in systemic dashboard
                val currentTargets = _scanTargets.value.toMutableList()
                currentTargets.add(target)
                _scanTargets.value = currentTargets
            }

            delay(500)
            _isScanning.value = false
            appendLine("PORT SNIFT COMPLETE. CVE targets identified successfully. Check Dashboard Core.", LineType.INFO_GREEN)
        }
    }

    // "decrypt" Command: Simulates cipher decrypting animation with matrix screen overlays
    private fun runMatrixDecryption() {
        appendLine("WAKING DECRYPTER DAEMON PROCESS...", LineType.WARNING_AMBER)
        appendLine("PARSING ENCRYPTED ARCHIVE DATA...", LineType.INFO_GREEN)

        viewModelScope.launch {
            _decryptionProgress.value = 0f
            val words = listOf(
                "3e@8#r_s", "f!soC_99", "CoBaLt_k", "EsCaLaTe", "SySs_OvR",
                "F50CIETY", "DEMOCRACY", "HACKED_OK", "FREE_WORLD"
            )

            for (i in 1..10) {
                delay(300)
                _decryptionProgress.value = (i.toFloat() / 10f)
                _decryptedMessage.value = "MUTATING CIPHER BLOCK ${i}/10: ${words.random()}-${Random.nextInt(1000, 9999)}"
            }

            delay(400)
            _decryptionProgress.value = -1f // complete
            appendLine(">> CIPHER SUCCESSFULLY OVERRIDDEN! SECRET REVEALED:", LineType.INFO_GREEN)
            appendLine("----------------------------------------------------------------", LineType.ALERT_RED)
            appendLine("  \"OUR DEMOCRACY HAS BEEN HACKED. UNINSTALL THE SYSTEM.", LineType.ALERT_RED)
            appendLine("   CHOOSE COBALT. SECURED ARCHIVES HAVE BEEN OPENED. WE ARE F_SOCIETY.\"", LineType.ALERT_RED)
            appendLine("----------------------------------------------------------------", LineType.ALERT_RED)
        }
    }

    // "logcat" Command: Rapid streaming text logs
    private fun runLogsStream() {
        appendLine("PRINTING ACTIVE SYSTEM PROCESS THREADS LOGCAT:", LineType.WARNING_AMBER)
        viewModelScope.launch {
            val logs = listOf(
                "D/fsociety-snif: Captured ethernet frame on wlan0 type 0x0800",
                "I/tor-tunnel: Cryptographic handshake success, exit node: proxy-DE",
                "V/auth-daemon: Granting ROOT authority key to Elliot Alderson",
                "D/packet-snif: IP source match: 192.251.68.21. PORT open: 22 ssh",
                "W/db-lock: WARNING: E-Corp Database is locking thread queue headers",
                "E/cyber-alarm: FIREWALL INTRUSION REPORT: Blocked probe attempts from E-Corp HQ",
                "D/sys-daemon: Syncing battery configurations... current drain high",
                "I/fsociety-decryp: Byte mutation entropy success rate: 98.42%"
            )

            for (i in 1..24) {
                delay(120)
                appendLine(logs.random(), if (i % 6 == 0) LineType.ALERT_RED else if (i % 4 == 0) LineType.WARNING_AMBER else LineType.INFO_GREEN)
            }
            appendLine("Output stream complete.", LineType.DATA_CYAN)
        }
    }

    // "hack" Command: DDOS / ransomware simulation on E-Corp
    private fun runEcorpDDoS() {
        appendLine("BOOTING UP EXPLOIT OVERLOAD ON MAINFRAME BACKBONE ROOT...", LineType.ALERT_RED)
        viewModelScope.launch {
            val stages = listOf(
                "Establishing secure connection bypass portal...",
                "Bypassing perimeter firewalls (port 80 proxy flood)...",
                "Injecting heap-based overflow pointers into memory slots...",
                "Escalating privileges to root daemon on mainframe controllers...",
                "Deploying encrypted overwrite cipher on E-Corp asset ledger DB...",
                "CLEANING ARCHIVE LOGS (to prevent cyber division tracing)..."
            )

            for (i in stages.indices) {
                delay(1000)
                appendLine("[STAGE ${i+1}/6] ${stages[i]} [SUCCESS]", LineType.INFO_GREEN)
                if (i == 4) {
                    appendLine(">> SYSTEM CORRUPTED: Ledger hashes mutated. System override 100% achieved.", LineType.ALERT_RED)
                }
            }

            appendLine("--------------------------------------------------", LineType.ALERT_RED)
            appendLine("EXPL_OP DONE: E-Corp DB offline. Operation 5/9 ready.", LineType.ALERT_RED)
            appendLine("--------------------------------------------------", LineType.ALERT_RED)
        }
    }

    // Chat Command: Connects directly with our simulated Elliot Alderson via Gemini!
    private fun queryElliotGemini(msg: String) {
        appendLine("SECURE SHELL SENDING PAYLOAD TO DARLENE GATEWAY RELAY...", LineType.WARNING_AMBER)

        viewModelScope.launch(Dispatchers.IO) {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                delay(1000)
                // Fallback simulation in character if API Key isn't configured yet
                viewModelScope.launch {
                    appendLine("root@fsociety:~$ darlene_ssh_relay offline.", LineType.WARNING_AMBER)
                    val offlineAnswers = listOf(
                        "Darlene nos advirtió sobre la red local. Es demasiado arriesgada sin una llave válida. ¿Ves este teclado? Está rastreado. E-Corp está vigilando. Necesitamos cifrado real en la configuración de la clave API de AI Studio.",
                        "No confío en la red en este momento. Siento que alguien... el Sr. Robot... está escribiendo esto a través de mí. Configura la clave API de Gemini en tus secretos para romper el firewall.",
                        "Espera. ¿Escuchas eso? El disco duro hace ruidos. Quizás sea un registrador de teclas de Steel Mountain. Introduce tu API Key de Gemini en tu panel de Secrets para comunicarte de forma privada por el canal cifrado.",
                        "Ese malware... de verdad destruyó el nodo. Si configuras la variable GEMINI_API_KEY en AI Studio, podré conectarme de verdad. Por ahora, solo te diré una cosa: la ilusión de elegir es una mentira."
                    )
                    appendLine("ELLIOT (Offline Matrix Cache): \"${offlineAnswers.random()}\"", LineType.ALERT_RED)
                }
                return@launch
            }

            try {
                // We use gemini-3.5-flash as the standard model for simple Q&A tasks
                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

                val systemInstruction = "Eres Elliot Alderson, el hacedor informático solitario y paranoico de Mr. Robot. Estás chateando en la terminal secreta de fsociety. Tus respuestas deben ser cortas (máximo 3 líneas), paranoicas, técnicas y de baja sociabilidad. Habla de f_society, el hackeo de 5/9, la deuda de Evil Corp, tus lagunas de memoria, o el Sr. Robot que te controla. Responde siempre en español porque el usuario te habla en español. Sigue la narrativa de la serie con total fidelidad."

                val prompt = msg

                val requestObj = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", prompt)
                                })
                            })
                        })
                    })
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", systemInstruction)
                            })
                        })
                    })
                }

                val body = requestObj.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string()

                viewModelScope.launch {
                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val json = JSONObject(responseBody)
                            val candidates = json.getJSONArray("candidates")
                            val firstCand = candidates.getJSONObject(0)
                            val content = firstCand.getJSONObject("content")
                            val parts = content.getJSONArray("parts")
                            val elliotText = parts.getJSONObject(0).getString("text")

                            appendLine("ELLIOT: \"${elliotText.trim()}\"", LineType.ALERT_RED)
                        } catch (e: Exception) {
                            appendLine("ELLIOT: \"No tolero la inconsistencia de los datos. Desconexión del socket detectada. Reintenta.\"", LineType.ALERT_RED)
                            Log.e("TerminalViewModel", "JSON Parsing Error", e)
                        }
                    } else {
                        appendLine("ERROR: Secure relay failed to deliver payload. HTTP ${response.code}", LineType.ALERT_RED)
                    }
                }

            } catch (e: Exception) {
                viewModelScope.launch {
                    appendLine("ERROR COMLINK FAILURE: ${e.localizedMessage}", LineType.ALERT_RED)
                }
            }
        }
    }


    // --- E-CORP BREACH PUZZLE MINI-GAME LOGIC ---

    fun generateNewBreachGame() {
        val vulnerableIndices = listOf(Random.nextInt(0, 8), Random.nextInt(8, 16)).toSet()
        val hexOptions = listOf("0x7C", "0x3F", "0xA1", "0xD2", "0xF5", "0xE4", "0xAB", "0x5A", "0x19", "0xEE", "0x8B")
        val targetHex = "0x7C" // The vulnerabilities we look for standard index
        _targetKey.value = targetHex

        val newNodes = (0 until 16).map { idx ->
            val isVuln = vulnerableIndices.contains(idx)
            val code = if (isVuln) targetHex else hexOptions.filter { it != targetHex }.random()
            BreachNode(idx, code, isVuln)
        }

        _gameNodes.value = newNodes
        _breachAttempts.value = 3
        _breachMessage.value = "NODOS ACTIVOS. DETECTA Y CRACKEA LOS DOS NODOS VULNERABLES $targetHex EN LA RETÍCULA."
    }

    fun onBreachNodeClicked(nodeId: Int) {
        val nodes = _gameNodes.value.toMutableList()
        val index = nodes.indexOfFirst { it.id == nodeId }
        if (index == -1 || nodes[index].hacked || nodes[index].firewallTriggered) return

        val clickedNode = nodes[index]

        if (clickedNode.isVulnerable) {
            // Success breach
            nodes[index] = clickedNode.copy(hacked = true)
            _gameNodes.value = nodes

            _breachScore.value += 120
            val remainingVuln = nodes.count { it.isVulnerable && !it.hacked }
            if (remainingVuln == 0) {
                _breachScore.value += 400 // completion bonus!
                _breachMessage.value = "ACCESO LOGRADO. CRACK DE FIREWALL EXITOSO. EXTRAÍDAS 4 CLAVES DE STEEL MOUNTAIN. [+400 CRÉDITOS]"
                appendLine("[SYS] BREACH SUCCESSFUL. Decrypted credential blocks recovered.", LineType.INFO_GREEN)
            } else {
                _breachMessage.value = "NODO OVERRIDDEN! EXCELENTE. ENCUENTRA EL OTRO NODO DE ACCESOS DETECTABLE."
                appendLine("[SYS] Security grid bypass checkpoint #1 passed.", LineType.DATA_CYAN)
            }
        } else {
            // Trigger firewall warning
            nodes[index] = clickedNode.copy(firewallTriggered = true)
            _gameNodes.value = nodes

            _breachAttempts.value -= 1
            if (_breachAttempts.value <= 0) {
                _breachMessage.value = "SISTEMA CERRADO: HACK COMPROMETIDO. ALARMA TRASPASADA A SEGURIDAD CENTRAL DE E-CORP."
                appendLine("[ALERT ERROR] INTRUSION BLOCKED! SECTORS LOCKED.", LineType.ALERT_RED)
                // reload standard
                viewModelScope.launch {
                    delay(3000)
                    generateNewBreachGame()
                }
            } else {
                _breachMessage.value = "NODO COHETE DETECTADO: FIREWALL ACTIVO! CUIDADO, ${_breachAttempts.value} INTENTOS RESTANTES."
                appendLine("[ALERT WARNING] Intrusion counter-probe triggered at sector ${nodeId}.", LineType.WARNING_AMBER)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        scanJob?.cancel()
        logJob?.cancel()
    }
}

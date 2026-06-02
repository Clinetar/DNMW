package com.clinetar.dnmw.ui.screens.tools

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import kotlin.math.roundToInt

// ── CIDR helpers ─────────────────────────────────────────────────────────────

private data class CidrInfo(
    val network: Long,
    val firstHost: Long,
    val lastHost: Long,
    val broadcast: Long,
    val hostCount: Long,
    val prefix: Int
)

private fun ipToLong(ip: String): Long? {
    val parts = ip.split(".")
    if (parts.size != 4) return null
    return runCatching {
        parts.fold(0L) { acc, s ->
            val n = s.toInt()
            if (n !in 0..255) throw IllegalArgumentException()
            (acc shl 8) or n.toLong()
        }
    }.getOrNull()
}

private fun longToIp(n: Long) =
    "${(n shr 24) and 0xFF}.${(n shr 16) and 0xFF}.${(n shr 8) and 0xFF}.${n and 0xFF}"

private fun isValidIp(ip: String) = ipToLong(ip) != null

private fun calculateCidr(ip: String, prefix: Int): CidrInfo? {
    val ipLong = ipToLong(ip) ?: return null
    // Build the 32-bit mask; shl 32 naturally gives 0 for prefix=0
    val mask = (0xFFFFFFFFL shl (32 - prefix)) and 0xFFFFFFFFL
    val network = ipLong and mask
    val broadcast = network or (mask.inv() and 0xFFFFFFFFL)
    return when (prefix) {
        32 -> CidrInfo(network, network, network, network, 1L, prefix)
        31 -> CidrInfo(network, network, broadcast, broadcast, 2L, prefix)
        else -> CidrInfo(network, network + 1, broadcast - 1, broadcast, broadcast - network - 1, prefix)
    }
}

private fun formatHostCount(n: Long): String = when {
    n >= 1_000_000 -> "%.1fM hosts".format(n / 1_000_000.0)
    n >= 1_000 -> "%.1fK hosts".format(n / 1_000.0)
    else -> "$n host${if (n != 1L) "s" else ""}"
}

// Rough estimate: 100 concurrent workers, ~1.5 s worst-case per host
private fun estimateScanSeconds(hostCount: Long) = hostCount * 1.5 / 100

private fun formatDuration(seconds: Double): String = when {
    seconds < 60 -> "~${seconds.roundToInt()} sec"
    seconds < 3600 -> "~${(seconds / 60).roundToInt()} min"
    else -> "~${(seconds / 3600).toInt()}h ${((seconds % 3600) / 60).toInt()}m"
}

private val PRESET_PREFIXES = listOf(32, 31, 30, 29, 28, 27, 26, 25, 24, 23, 22, 21, 20, 19, 18, 17, 16, 12, 8, 0)
private const val MAX_SCANNABLE_HOSTS = 65_536L  // /16

// ── Scan result ───────────────────────────────────────────────────────────────

private data class ScannedHost(
    val ip: String,
    val hostname: String? = null,
    val mac: String? = null,
    val isCurrentDevice: Boolean = false,
    val isGateway: Boolean = false
)

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IpScannerScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()

    var scanning by remember { mutableStateOf(false) }
    var scanned by remember { mutableIntStateOf(0) }
    val hosts = remember { mutableStateListOf<ScannedHost>() }

    var ipInput by remember { mutableStateOf("") }
    var prefixSlider by remember { mutableFloatStateOf(24f) }
    var localIp by remember { mutableStateOf<String?>(null) }
    var networkError by remember { mutableStateOf<String?>(null) }

    val prefix = prefixSlider.roundToInt()
    val ipValid = isValidIp(ipInput)
    val cidr = remember(ipInput, prefix) { if (ipValid) calculateCidr(ipInput, prefix) else null }
    val hostCount = cidr?.hostCount ?: 0L
    val tooLarge = hostCount > MAX_SCANNABLE_HOSTS
    val estimatedSecs = estimateScanSeconds(hostCount)

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val info = getLocalNetworkInfo()
            if (info != null) {
                localIp = info.first
                ipInput = info.first  // fill full IP
            } else {
                networkError = "Not connected to a Wi-Fi network"
            }
        }
    }

    fun startScan() {
        val range = cidr ?: return
        val myIp = localIp
        val gwIp = longToIp(range.network + 1)

        scope.launch {
            scanning = true
            scanned = 0
            hosts.clear()

            withContext(Dispatchers.IO) {
                coroutineScope {
                    val semaphore = Semaphore(100)
                    (range.firstHost..range.lastHost).map { ipLong ->
                        async {
                            semaphore.withPermit {
                                val ip = longToIp(ipLong)
                                if (isHostAlive(ip)) {
                                    withContext(Dispatchers.Main.immediate) {
                                        hosts.add(
                                            ScannedHost(
                                                ip = ip,
                                                isCurrentDevice = ip == myIp,
                                                isGateway = ip == gwIp
                                            )
                                        )
                                    }
                                    launch {
                                        val name = runCatching {
                                            InetAddress.getByName(ip).canonicalHostName.takeIf { it != ip }
                                        }.getOrNull()
                                        if (name != null) withContext(Dispatchers.Main.immediate) {
                                            val idx = hosts.indexOfFirst { it.ip == ip }
                                            if (idx >= 0) hosts[idx] = hosts[idx].copy(hostname = name)
                                        }
                                    }
                                }
                                withContext(Dispatchers.Main.immediate) { scanned++ }
                            }
                        }
                    }.awaitAll()
                }

                // ARP sweep picks up devices that block all TCP probes
                readArpTable().forEach { (ip, mac) ->
                    val ipLong = ipToLong(ip) ?: return@forEach
                    if (ipLong in range.firstHost..range.lastHost && hosts.none { it.ip == ip }) {
                        withContext(Dispatchers.Main.immediate) {
                            hosts.add(
                                ScannedHost(
                                    ip = ip,
                                    mac = mac,
                                    isCurrentDevice = ip == myIp,
                                    isGateway = ip == gwIp
                                )
                            )
                        }
                    }
                }
            }

            scanning = false
        }
    }

    val showResults = scanning || hosts.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("IP Scanner") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            AnimatedContent(
                targetState = showResults,
                transitionSpec = {
                    if (targetState)
                        (slideInVertically { -it } + fadeIn()).togetherWith(slideOutVertically { it } + fadeOut())
                    else
                        (slideInVertically { it } + fadeIn()).togetherWith(slideOutVertically { -it } + fadeOut())
                },
                modifier = Modifier.weight(1f),
                label = "scanner_content"
            ) { active ->
                if (!active) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Icon(
                                Icons.Default.NetworkCheck,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp).alpha(0.4f),
                                tint = MaterialTheme.colorScheme.onSurface
                            )

                            if (networkError != null) {
                                Text(networkError!!, color = MaterialTheme.colorScheme.error)
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // IP address field
                                    OutlinedTextField(
                                        value = ipInput,
                                        onValueChange = { ipInput = it },
                                        label = { Text("IP Address") },
                                        placeholder = { Text("192.168.1.1") },
                                        isError = ipInput.isNotEmpty() && !ipValid,
                                        supportingText = {
                                            when {
                                                ipInput.isNotEmpty() && !ipValid ->
                                                    Text("Enter a valid IPv4 address (e.g. 192.168.1.1)")
                                                cidr != null ->
                                                    Text("Network ${longToIp(cidr.network)}/$prefix")
                                            }
                                        },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.medium
                                    )

                                    // Prefix slider
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Prefix length", style = MaterialTheme.typography.bodyMedium)
                                            Text(
                                                "/$prefix",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Slider(
                                            value = prefixSlider,
                                            onValueChange = { prefixSlider = it },
                                            valueRange = 0f..32f,
                                            steps = 31,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("/0", style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("/32", style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    // Preset chips
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        PRESET_PREFIXES.forEach { p ->
                                            FilterChip(
                                                selected = prefix == p,
                                                onClick = { prefixSlider = p.toFloat() },
                                                label = { Text("/$p") }
                                            )
                                        }
                                    }

                                    // Range summary + warning
                                    if (cidr != null) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            if (prefix < 31) {
                                                Text(
                                                    "${longToIp(cidr.firstHost)} – ${longToIp(cidr.lastHost)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                formatHostCount(hostCount),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = when {
                                                    tooLarge -> MaterialTheme.colorScheme.error
                                                    hostCount > 2048 -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                            if (tooLarge) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Warning,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text(
                                                        "Too large to scan (max /16 = 65 536 hosts)",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            } else if (hostCount > 512) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Warning,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text(
                                                        "Estimated ${formatDuration(estimatedSecs)}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Compact summary header
                        if (cidr != null) {
                            Text(
                                text = "${longToIp(cidr.network)}/$prefix  ·  ${formatHostCount(hostCount)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }

                        if (scanning) {
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { if (hostCount > 0) scanned / hostCount.toFloat() else 0f },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                            )
                            Text(
                                text = "Checked $scanned / $hostCount  ·  ${hosts.size} found",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }

                        val sorted = hosts.sortedBy { ipToLong(it.ip) ?: 0L }
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                Text(
                                    "${hosts.size} device${if (hosts.size != 1) "s" else ""} found",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                            }
                            items(sorted, key = { it.ip }) { host -> HostCard(host) }
                            item { Spacer(Modifier.height(8.dp)) }
                        }
                    }
                }
            }

            Button(
                onClick = ::startScan,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                enabled = ipValid && !tooLarge && !scanning
            ) {
                if (scanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Scanning…")
                } else {
                    Text(if (hosts.isEmpty()) "Scan Network" else "Scan Again")
                }
            }
        }
    }
}

// ── Host card ─────────────────────────────────────────────────────────────────

@Composable
private fun HostCard(host: ScannedHost) {
    val highlight = host.isCurrentDevice || host.isGateway
    val clipboard = LocalClipboardManager.current
    Card(
        onClick = { clipboard.setText(AnnotatedString(host.ip)) },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                when {
                    host.isCurrentDevice -> Icons.Default.PhoneAndroid
                    host.isGateway -> Icons.Default.Router
                    else -> Icons.Default.DeviceHub
                },
                contentDescription = null,
                tint = if (highlight) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = host.ip,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (highlight) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface
                )
                val subtitle = host.hostname ?: host.mac
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (highlight) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            when {
                host.isCurrentDevice -> "This device"
                host.isGateway -> "Gateway"
                else -> null
            }?.let { label ->
                Badge(containerColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                    Text(label, color = MaterialTheme.colorScheme.primaryContainer,
                        style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

// ── Network utilities ─────────────────────────────────────────────────────────

private fun getLocalNetworkInfo(): Pair<String, String>? {
    NetworkInterface.getNetworkInterfaces()?.toList()?.forEach { iface ->
        if (iface.isLoopback || !iface.isUp) return@forEach
        iface.interfaceAddresses.forEach { ifAddr ->
            val addr = ifAddr.address
            if (addr !is Inet4Address || addr.isLoopbackAddress) return@forEach
            val ip = addr.hostAddress ?: return@forEach
            val parts = ip.split(".")
            if (parts.size == 4) return Pair(ip, "${parts[0]}.${parts[1]}.${parts[2]}")
        }
    }
    return null
}

private fun isHostAlive(ip: String): Boolean {
    if (runCatching { InetAddress.getByName(ip).isReachable(1200) }.getOrDefault(false)) return true
    return tcpCheck(ip, 80, 500) || tcpCheck(ip, 443, 500)
}

private fun tcpCheck(ip: String, port: Int, timeoutMs: Int): Boolean {
    return try {
        Socket().use { it.connect(InetSocketAddress(ip, port), timeoutMs) }
        true
    } catch (e: java.net.ConnectException) {
        true
    } catch (e: Exception) {
        false
    }
}

private fun readArpTable(): Map<String, String> {
    val result = mutableMapOf<String, String>()
    runCatching {
        File("/proc/net/arp").readLines().drop(1).forEach { line ->
            val cols = line.trim().split(Regex("\\s+"))
            if (cols.size >= 4 && cols[2] != "0x0" && cols[3] != "00:00:00:00:00:00")
                result[cols[0]] = cols[3]
        }
    }
    return result
}

package com.clinetar.dnmw.ui.screens.tools

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private enum class TestPhase { IDLE, PING, DOWNLOAD, UPLOAD, DONE, ERROR }

private fun formatSpeed(mbps: Double): Pair<String, String> = when {
    mbps >= 1000 -> "%.2f".format(mbps / 1000) to "Gbps"
    mbps >= 1 -> "%.1f".format(mbps) to "Mbps"
    else -> "%.0f".format(mbps * 1000) to "Kbps"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedTestScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var phase by remember { mutableStateOf(TestPhase.IDLE) }
    var pingMs by remember { mutableStateOf<Long?>(null) }
    var downloadMbps by remember { mutableStateOf<Double?>(null) }
    var uploadMbps by remember { mutableStateOf<Double?>(null) }
    var liveSpeed by remember { mutableDoubleStateOf(0.0) }
    var errorMsg by remember { mutableStateOf("") }

    fun runTest() {
        scope.launch {
            phase = TestPhase.PING
            pingMs = null; downloadMbps = null; uploadMbps = null; liveSpeed = 0.0; errorMsg = ""
            try {
                // Ping — best of 3 HTTP round trips to Cloudflare
                val pings = mutableListOf<Long>()
                repeat(3) {
                    withContext(Dispatchers.IO) {
                        val t0 = System.currentTimeMillis()
                        val conn = URL("https://speed.cloudflare.com/cdn-cgi/trace")
                            .openConnection() as HttpURLConnection
                        conn.connectTimeout = 5_000
                        conn.readTimeout = 5_000
                        conn.connect()
                        conn.inputStream.use { it.readBytes() }
                        conn.disconnect()
                        pings.add(System.currentTimeMillis() - t0)
                    }
                }
                pingMs = pings.min()

                // Download — 10 MB from Cloudflare, read in 32 KB chunks for live speed
                phase = TestPhase.DOWNLOAD
                liveSpeed = 0.0
                withContext(Dispatchers.IO) {
                    val conn = URL("https://speed.cloudflare.com/__down?bytes=10485760")
                        .openConnection() as HttpURLConnection
                    conn.connectTimeout = 10_000
                    conn.readTimeout = 60_000
                    conn.connect()
                    val t0 = System.currentTimeMillis()
                    var total = 0L
                    val buf = ByteArray(32_768)
                    conn.inputStream.use { input ->
                        var n: Int
                        while (input.read(buf).also { n = it } != -1) {
                            total += n
                            val ms = System.currentTimeMillis() - t0
                            if (ms > 300) liveSpeed = (total * 8.0) / (ms * 1_000.0)
                        }
                    }
                    val ms = System.currentTimeMillis() - t0
                    downloadMbps = (total * 8.0) / (ms * 1_000.0)
                    liveSpeed = downloadMbps!!
                    conn.disconnect()
                }

                // Upload — POST 5 MB to Cloudflare, write in 16 KB chunks for live speed
                phase = TestPhase.UPLOAD
                liveSpeed = 0.0
                withContext(Dispatchers.IO) {
                    val uploadSize = 5 * 1024 * 1024
                    val chunk = ByteArray(16_384)
                    val conn = URL("https://speed.cloudflare.com/__up")
                        .openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.doOutput = true
                    conn.setFixedLengthStreamingMode(uploadSize.toLong())
                    conn.setRequestProperty("Content-Type", "application/octet-stream")
                    conn.connectTimeout = 10_000
                    conn.readTimeout = 60_000
                    conn.connect()
                    val t0 = System.currentTimeMillis()
                    var sent = 0
                    conn.outputStream.use { out ->
                        while (sent < uploadSize) {
                            val toWrite = minOf(chunk.size, uploadSize - sent)
                            out.write(chunk, 0, toWrite)
                            sent += toWrite
                            val ms = System.currentTimeMillis() - t0
                            if (ms > 300) liveSpeed = (sent * 8.0) / (ms * 1_000.0)
                        }
                    }
                    conn.responseCode // wait for server ACK
                    val ms = System.currentTimeMillis() - t0
                    uploadMbps = (sent * 8.0) / (ms * 1_000.0)
                    liveSpeed = uploadMbps!!
                    conn.disconnect()
                }

                phase = TestPhase.DONE
            } catch (e: Exception) {
                errorMsg = e.message ?: "Connection failed"
                phase = TestPhase.ERROR
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Speed Test") },
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
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Gauge
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                SpeedGauge(phase = phase, liveSpeed = liveSpeed, downloadMbps = downloadMbps)
            }

            // Results
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ResultRow(
                    icon = Icons.Default.NetworkCheck,
                    label = "Ping",
                    value = pingMs?.let { "${it} ms" } ?: "—",
                    active = phase == TestPhase.PING
                )
                ResultRow(
                    icon = Icons.Default.ArrowDownward,
                    label = "Download",
                    value = downloadMbps?.let { formatSpeed(it).let { (v, u) -> "$v $u" } } ?: "—",
                    active = phase == TestPhase.DOWNLOAD
                )
                ResultRow(
                    icon = Icons.Default.ArrowUpward,
                    label = "Upload",
                    value = uploadMbps?.let { formatSpeed(it).let { (v, u) -> "$v $u" } } ?: "—",
                    active = phase == TestPhase.UPLOAD
                )
            }

            if (phase == TestPhase.ERROR) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = errorMsg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = ::runTest,
                modifier = Modifier.fillMaxWidth(),
                enabled = phase == TestPhase.IDLE || phase == TestPhase.DONE || phase == TestPhase.ERROR
            ) {
                when (phase) {
                    TestPhase.PING, TestPhase.DOWNLOAD, TestPhase.UPLOAD -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Testing…")
                    }
                    TestPhase.DONE, TestPhase.ERROR -> Text("Run Again")
                    else -> Text("Start Test")
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SpeedGauge(phase: TestPhase, liveSpeed: Double, downloadMbps: Double?) {
    val isRunning = phase == TestPhase.PING || phase == TestPhase.DOWNLOAD || phase == TestPhase.UPLOAD

    val ringColor = when (phase) {
        TestPhase.DOWNLOAD -> MaterialTheme.colorScheme.primary
        TestPhase.UPLOAD -> MaterialTheme.colorScheme.tertiary
        TestPhase.DONE -> MaterialTheme.colorScheme.primary
        TestPhase.ERROR -> MaterialTheme.colorScheme.error
        TestPhase.PING -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    val infiniteTransition = rememberInfiniteTransition(label = "gauge")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1_200, easing = LinearEasing)),
        label = "rotation"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
        // Track ring
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 8.dp,
            color = MaterialTheme.colorScheme.surfaceVariant,
            trackColor = Color.Transparent
        )
        // Active ring
        if (isRunning) {
            CircularProgressIndicator(
                modifier = Modifier.fillMaxSize().rotate(rotation),
                strokeWidth = 8.dp,
                strokeCap = StrokeCap.Round,
                color = ringColor
            )
        } else if (phase == TestPhase.DONE || phase == TestPhase.ERROR) {
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 8.dp,
                strokeCap = StrokeCap.Round,
                color = ringColor,
                trackColor = Color.Transparent
            )
        }

        // Center text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedContent(targetState = phase, label = "gauge_text") { p ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    when (p) {
                        TestPhase.IDLE -> {
                            Text("—", fontSize = 52.sp, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Ready", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TestPhase.PING -> {
                            CircularProgressIndicator(Modifier.size(40.dp), strokeWidth = 3.dp)
                            Spacer(Modifier.height(8.dp))
                            Text("Ping", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TestPhase.DOWNLOAD, TestPhase.UPLOAD -> {
                            val (value, unit) = formatSpeed(liveSpeed)
                            Text(value, fontSize = 52.sp, fontWeight = FontWeight.Bold)
                            Text(unit, style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                if (p == TestPhase.DOWNLOAD) "Download" else "Upload",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TestPhase.DONE -> {
                            val mbps = downloadMbps ?: 0.0
                            val (value, unit) = formatSpeed(mbps)
                            Text(value, fontSize = 52.sp, fontWeight = FontWeight.Bold)
                            Text(unit, style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Download", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TestPhase.ERROR -> {
                            Text("!", fontSize = 52.sp, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error)
                            Text("Error", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultRow(icon: ImageVector, label: String, value: String, active: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (active) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon, contentDescription = null,
                tint = if (active) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                color = if (active) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (value == "—") FontWeight.Normal else FontWeight.SemiBold,
                color = if (active) MaterialTheme.colorScheme.onPrimaryContainer
                else if (value == "—") MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

package com.clinetar.dnmw.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.clinetar.dnmw.ui.screens.tools.ColorPickerScreen
import com.clinetar.dnmw.ui.screens.tools.PasswordGeneratorScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreenContent() {
    var selectedTool by remember { mutableStateOf<String?>(null) }

    AnimatedContent(
        targetState = selectedTool,
        label = "tool_nav"
    ) { toolName ->
        if (toolName == null) {
            ToolsListScreen(onSelect = { selectedTool = it })
        } else {
            when (toolName) {
                "Password Generator" -> PasswordGeneratorScreen(onBack = { selectedTool = null })
                "Color Picker" -> ColorPickerScreen(onBack = { selectedTool = null })
                else -> {
                    // Fallback for not implemented
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Tool Not Implemented Yet")
                            Button(onClick = { selectedTool = null }) { Text("Back") }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsListScreen(onSelect: (String) -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val toolGroups = listOf(
        ToolGroup("Security", listOf(
            ToolItem("Password Generator", "Create strong, random passwords", Icons.Default.Password),
            ToolItem("File Encryptor", "Secure your local files", Icons.Default.Lock)
        )),
        ToolGroup("Network", listOf(
            ToolItem("IP Scanner", "Find devices on your network", Icons.Default.NetworkCheck),
            ToolItem("Speed Test", "Check your internet connection", Icons.Default.Speed),
            ToolItem("Port Scanner", "Identify open ports on a host", Icons.Default.Router)
        )),
        ToolGroup("Developer", listOf(
            ToolItem("JSON Formatter", "Beautify or minify JSON code", Icons.Default.Code),
            ToolItem("Base64 Encoder", "Encode or decode strings", Icons.Default.Transform),
            ToolItem("Color Picker", "Find the perfect HEX/RGB values", Icons.Default.Palette)
        ))
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Tools") },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(toolGroups) { group ->
                ToolGroupSection(group, onSelect)
            }
        }
    }
}

data class ToolGroup(val name: String, val tools: List<ToolItem>)
data class ToolItem(val name: String, val description: String, val icon: ImageVector)

@Composable
fun ToolGroupSection(group: ToolGroup, onSelect: (String) -> Unit) {
    Column {
        Text(
            text = group.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column {
                group.tools.forEachIndexed { index, tool ->
                    ListItem(
                        headlineContent = { Text(tool.name) },
                        supportingContent = { Text(tool.description) },
                        leadingContent = { 
                            Icon(tool.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) 
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(tool.name) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    if (index < group.tools.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

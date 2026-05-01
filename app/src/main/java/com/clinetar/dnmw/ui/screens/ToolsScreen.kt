package com.clinetar.dnmw.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.clinetar.dnmw.data.note.Note
import com.clinetar.dnmw.ui.screens.tools.ColorPickerScreen
import com.clinetar.dnmw.ui.screens.tools.PasswordGeneratorScreen

sealed class SearchResult {
    data class Tool(val item: ToolItem, val groupName: String) : SearchResult()
    data class Calculator(val item: CalculatorItem) : SearchResult()
    data class NoteResult(val note: Note) : SearchResult()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreenContent(
    notes: List<Note> = emptyList(),
    onNavigateToCalculator: (String) -> Unit = {},
    onNavigateToNote: (Note) -> Unit = {}
) {
    var selectedTool by remember { mutableStateOf<String?>(null) }

    AnimatedContent(
        targetState = selectedTool,
        label = "tool_nav"
    ) { toolName ->
        if (toolName == null) {
            ToolsListScreen(
                notes = notes,
                onSelect = { selectedTool = it },
                onNavigateToCalculator = onNavigateToCalculator,
                onNavigateToNote = onNavigateToNote
            )
        } else {
            when (toolName) {
                "Password Generator" -> PasswordGeneratorScreen(onBack = { selectedTool = null })
                "Color Picker" -> ColorPickerScreen(onBack = { selectedTool = null })
                else -> {
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
fun ToolsListScreen(
    notes: List<Note>,
    onSelect: (String) -> Unit,
    onNavigateToCalculator: (String) -> Unit,
    onNavigateToNote: (Note) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var searchQuery by remember { mutableStateOf("") }

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

    val searchResults: List<SearchResult> = remember(searchQuery, notes) {
        if (searchQuery.isBlank()) emptyList()
        else buildList {
            toolGroups.forEach { group ->
                group.tools
                    .filter { it.name.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true) }
                    .forEach { add(SearchResult.Tool(it, group.name)) }
            }
            allCalculators
                .filter { it.name.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true) }
                .forEach { add(SearchResult.Calculator(it)) }
            notes
                .filter { it.title.contains(searchQuery, ignoreCase = true) || it.content.contains(searchQuery, ignoreCase = true) }
                .forEach { add(SearchResult.NoteResult(it)) }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                LargeTopAppBar(
                    title = { Text("Tools") },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search tools, calculators, notes...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, contentDescription = "Clear") } }
                    } else null,
                    shape = MaterialTheme.shapes.extraLarge,
                    singleLine = true
                )
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        if (searchQuery.isBlank()) {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(toolGroups) { group ->
                    ToolGroupSection(group, onSelect)
                }
            }
        } else {
            GlobalSearchResults(
                modifier = Modifier.padding(padding),
                results = searchResults,
                onSelectTool = onSelect,
                onSelectCalculator = onNavigateToCalculator,
                onSelectNote = onNavigateToNote
            )
        }
    }
}

@Composable
fun GlobalSearchResults(
    modifier: Modifier = Modifier,
    results: List<SearchResult>,
    onSelectTool: (String) -> Unit,
    onSelectCalculator: (String) -> Unit,
    onSelectNote: (Note) -> Unit
) {
    if (results.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).alpha(0.3f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("No results found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    val toolResults = results.filterIsInstance<SearchResult.Tool>()
    val calcResults = results.filterIsInstance<SearchResult.Calculator>()
    val noteResults = results.filterIsInstance<SearchResult.NoteResult>()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (toolResults.isNotEmpty()) {
            item {
                Text(
                    "Tools",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
                SearchResultCard {
                    toolResults.forEachIndexed { index, result ->
                        ListItem(
                            headlineContent = { Text(result.item.name) },
                            supportingContent = { Text(result.item.description) },
                            leadingContent = {
                                Icon(result.item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            trailingContent = {
                                Text(
                                    result.groupName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier.clickable { onSelectTool(result.item.name) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        if (index < toolResults.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }

        if (calcResults.isNotEmpty()) {
            item {
                Text(
                    "Calculators",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
                SearchResultCard {
                    calcResults.forEachIndexed { index, result ->
                        ListItem(
                            headlineContent = { Text(result.item.name) },
                            supportingContent = { Text(result.item.description) },
                            leadingContent = {
                                Icon(result.item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            modifier = Modifier.clickable { onSelectCalculator(result.item.name) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        if (index < calcResults.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }

        if (noteResults.isNotEmpty()) {
            item {
                Text(
                    "Notes",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
                SearchResultCard {
                    noteResults.forEachIndexed { index, result ->
                        ListItem(
                            headlineContent = { Text(result.note.title.ifEmpty { "Untitled" }) },
                            supportingContent = {
                                Text(
                                    result.note.content.take(80).replace('\n', ' '),
                                    maxLines = 1
                                )
                            },
                            leadingContent = {
                                Icon(Icons.AutoMirrored.Filled.Article, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            modifier = Modifier.clickable { onSelectNote(result.note) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        if (index < noteResults.size - 1) {
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
}

@Composable
private fun SearchResultCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        content = { Column(content = content) }
    )
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

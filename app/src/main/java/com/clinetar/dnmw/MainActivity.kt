package com.clinetar.dnmw

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clinetar.dnmw.ui.theme.DNMWTheme

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.input.nestedscroll.nestedScroll
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.clinetar.dnmw.data.note.Note
import com.clinetar.dnmw.ui.screens.CalculatorsScreen
import com.clinetar.dnmw.ui.screens.ToolsScreenContent
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        System.loadLibrary("sqlcipher")
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val themeState by viewModel.themeState.collectAsState()
            val customColorState by viewModel.customColorState.collectAsState()
            val pureBlackState by viewModel.pureBlackState.collectAsState()
            val databasePath by viewModel.databasePathState.collectAsState()
            val isEncrypted by viewModel.isEncryptedState.collectAsState()
            val dbPassword by viewModel.dbPasswordState.collectAsState()
            val notes by viewModel.notesState.collectAsState()
            val favoriteNotes by viewModel.favoriteNotesState.collectAsState()

            DNMWTheme(
                appTheme = themeState,
                customColor = customColorState,
                pureBlack = pureBlackState
            ) {
                DNMWApp(
                    currentTheme = themeState,
                    onThemeChange = { viewModel.setTheme(it) },
                    currentCustomColor = customColorState,
                    onCustomColorChange = { viewModel.setCustomColor(it) },
                    pureBlack = pureBlackState,
                    onPureBlackChange = { viewModel.setPureBlack(it) },
                    databasePath = databasePath,
                    onDatabaseNameChange = { viewModel.setDatabaseName(it) },
                    isEncrypted = isEncrypted,
                    onIsEncryptedChange = { viewModel.setIsEncrypted(it) },
                    dbPassword = dbPassword,
                    onDbPasswordChange = { viewModel.setDbPassword(it) },
                    notes = notes,
                    favoriteNotes = favoriteNotes,
                    onAddNote = { t, c -> viewModel.addNote(t, c) },
                    onDeleteNote = { viewModel.deleteNote(it) },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onDeleteDatabase = { viewModel.deleteDatabase() },
                    onImportDatabase = { viewModel.importDatabase(it) },
                    onExportDatabase = { uri, pwd, enc -> viewModel.exportDatabase(uri, pwd, enc) }
                )
            }
        }
    }
}

@Composable
fun DNMWApp(
    currentTheme: AppTheme = AppTheme.SYSTEM,
    onThemeChange: (AppTheme) -> Unit = {},
    currentCustomColor: CustomColor = CustomColor.DYNAMIC,
    onCustomColorChange: (CustomColor) -> Unit = {},
    pureBlack: Boolean = false,
    onPureBlackChange: (Boolean) -> Unit = {},
    databasePath: String? = null,
    onDatabaseNameChange: (String) -> Unit = {},
    isEncrypted: Boolean = false,
    onIsEncryptedChange: (Boolean) -> Unit = {},
    dbPassword: String? = null,
    onDbPasswordChange: (String?) -> Unit = {},
    notes: List<Note> = emptyList(),
    favoriteNotes: List<Note> = emptyList(),
    onAddNote: (String, String) -> Unit = { _, _ -> },
    onDeleteNote: (Note) -> Unit = {},
    onToggleFavorite: (Note) -> Unit = {},
    onDeleteDatabase: () -> Unit = {},
    onImportDatabase: (android.net.Uri) -> Unit = {},
    onExportDatabase: (android.net.Uri, String?, Boolean) -> Unit = { _, _, _ -> }
) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.TOOLS) }
    var settingsDestination by rememberSaveable { mutableStateOf(SettingsSubDestination.MAIN) }
    val isNotesEnabled = !databasePath.isNullOrBlank()

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                val enabled = it != AppDestinations.NOTES || isNotesEnabled
                item(
                    icon = {
                        Icon(
                            painterResource(it.icon),
                            contentDescription = it.label,
                            modifier = Modifier.alpha(if (enabled) 1f else 0.38f)
                        )
                    },
                    label = { 
                        Text(
                            it.label, 
                            modifier = Modifier.alpha(if (enabled) 1f else 0.38f)
                        ) 
                    },
                    selected = it == currentDestination,
                    onClick = { if (enabled) currentDestination = it },
                    enabled = enabled
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationBarContainerColor = Color.Transparent,
            navigationRailContainerColor = Color.Transparent,
            navigationDrawerContainerColor = Color.Transparent
        )
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (currentDestination) {
                    AppDestinations.TOOLS -> ToolsScreenContent()
                    AppDestinations.CALCULATORS -> CalculatorsScreen()
                    AppDestinations.NOTES -> NotesScreen(notes, onAddNote, onDeleteNote, onToggleFavorite)
                    AppDestinations.FAVORITES -> FavoritesScreen(favoriteNotes, onToggleFavorite, onDeleteNote)
                    AppDestinations.SETTINGS -> SettingsScreen(
                        currentTheme, onThemeChange,
                        currentCustomColor, onCustomColorChange,
                        pureBlack, onPureBlackChange,
                        settingsDestination, { settingsDestination = it },
                        databasePath, onDatabaseNameChange,
                        isEncrypted, onIsEncryptedChange,
                        dbPassword, onDbPasswordChange,
                        onDeleteDatabase, onImportDatabase, onExportDatabase
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    favoriteNotes: List<Note>,
    onToggleFavorite: (Note) -> Unit,
    onDeleteNote: (Note) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredNotes = remember(favoriteNotes, searchQuery) {
        favoriteNotes.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.content.contains(searchQuery, ignoreCase = true)
        }
    }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                LargeTopAppBar(
                    title = { Text("Favorites") },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
                if (favoriteNotes.isNotEmpty()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("Search favorites...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        } else null,
                        shape = MaterialTheme.shapes.extraLarge,
                        singleLine = true
                    )
                }
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (favoriteNotes.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp).alpha(0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No favorites yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(filteredNotes) { note ->
                        NoteItem(
                            note = note,
                            onDelete = { onDeleteNote(note) },
                            onToggleFavorite = { onToggleFavorite(note) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    notes: List<Note>,
    onAddNote: (String, String) -> Unit,
    onDeleteNote: (Note) -> Unit,
    onToggleFavorite: (Note) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val filteredNotes = remember(notes, searchQuery) {
        notes.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.content.contains(searchQuery, ignoreCase = true)
        }
    }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                LargeTopAppBar(
                    title = { Text("Notes") },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
                if (notes.isNotEmpty()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("Search notes...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        } else null,
                        shape = MaterialTheme.shapes.extraLarge,
                        singleLine = true
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Note")
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (notes.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painterResource(R.drawable.ic_notes),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp).alpha(0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No notes yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Tap + to add your first note",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(filteredNotes) { note ->
                        NoteItem(
                            note = note,
                            onDelete = { onDeleteNote(note) },
                            onToggleFavorite = { onToggleFavorite(note) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showAddDialog) {
        AddNoteDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, content ->
                onAddNote(title, content)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun NoteItem(note: Note, onDelete: () -> Unit, onToggleFavorite: () -> Unit) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()) }
    val dateString = dateFormatter.format(Date(note.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                Row {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            if (note.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (note.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            if (note.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AddNoteDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Note") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title, content) },
                enabled = title.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SettingsScreen(
    currentTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit,
    currentCustomColor: CustomColor,
    onCustomColorChange: (CustomColor) -> Unit,
    pureBlack: Boolean,
    onPureBlackChange: (Boolean) -> Unit,
    settingsDestination: SettingsSubDestination,
    onSettingsDestinationChange: (SettingsSubDestination) -> Unit,
    databasePath: String?,
    onDatabaseNameChange: (String) -> Unit,
    isEncrypted: Boolean,
    onIsEncryptedChange: (Boolean) -> Unit,
    dbPassword: String?,
    onDbPasswordChange: (String?) -> Unit,
    onDeleteDatabase: () -> Unit,
    onImportDatabase: (android.net.Uri) -> Unit,
    onExportDatabase: (android.net.Uri, String?, Boolean) -> Unit
) {
    AnimatedContent(
        targetState = settingsDestination,
        transitionSpec = {
            if (targetState != SettingsSubDestination.MAIN) {
                (slideInHorizontally(animationSpec = tween(300)) { it } + fadeIn(animationSpec = tween(300)))
                    .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { -it / 3 } + fadeOut(animationSpec = tween(300)))
            } else {
                (slideInHorizontally(animationSpec = tween(300)) { -it / 3 } + fadeIn(animationSpec = tween(300)))
                    .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { it } + fadeOut(animationSpec = tween(300)))
            }
        },
        label = "settings_nav"
    ) { targetDestination ->
        when (targetDestination) {
            SettingsSubDestination.THEME -> {
                BackHandler { onSettingsDestinationChange(SettingsSubDestination.MAIN) }
                ThemeSettingsSubScreen(
                    currentTheme, onThemeChange,
                    currentCustomColor, onCustomColorChange,
                    pureBlack, onPureBlackChange,
                    onBack = { onSettingsDestinationChange(SettingsSubDestination.MAIN) }
                )
            }
            SettingsSubDestination.DATABASE -> {
                BackHandler { onSettingsDestinationChange(SettingsSubDestination.MAIN) }
                DatabaseSettingsSubScreen(
                    databasePath, 
                    onCreateDatabase = { name ->
                        onIsEncryptedChange(true)
                        onDatabaseNameChange(name)
                    },
                    isEncrypted, onIsEncryptedChange,
                    dbPassword, onDbPasswordChange,
                    onDeleteDatabase, onImportDatabase, onExportDatabase,
                    onBack = { onSettingsDestinationChange(SettingsSubDestination.MAIN) }
                )
            }
            SettingsSubDestination.MAIN -> {
                MainSettingsSubScreen(
                    onNavigateToTheme = { onSettingsDestinationChange(SettingsSubDestination.THEME) },
                    onNavigateToDatabase = { onSettingsDestinationChange(SettingsSubDestination.DATABASE) }
                )
            }
        }
    }
}

enum class SettingsSubDestination {
    MAIN, THEME, DATABASE
}

@Composable
fun MainSettingsSubScreen(onNavigateToTheme: () -> Unit, onNavigateToDatabase: () -> Unit) {
    Column {
        Text("Settings", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        
        ListItem(
            headlineContent = { Text("Appearance") },
            supportingContent = { Text("Theme, colors, and dark mode") },
            modifier = Modifier.clickable { onNavigateToTheme() }
        )
        ListItem(
            headlineContent = { Text("Database") },
            supportingContent = { Text("Configure notes storage and encryption") },
            modifier = Modifier.clickable { onNavigateToDatabase() }
        )
    }
}

@Composable
fun DatabaseSettingsSubScreen(
    databasePath: String?,
    onCreateDatabase: (String) -> Unit,
    isEncrypted: Boolean,
    onIsEncryptedChange: (Boolean) -> Unit,
    dbPassword: String?,
    onDbPasswordChange: (String?) -> Unit,
    onDeleteDatabase: () -> Unit,
    onImportDatabase: (android.net.Uri) -> Unit,
    onExportDatabase: (android.net.Uri, String?, Boolean) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var pendingExportUri by remember { mutableStateOf<android.net.Uri?>(null) }
    
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> uri?.let { onImportDatabase(it) } }
    )

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/x-sqlite3"),
        onResult = { uri -> 
            uri?.let { 
                pendingExportUri = it
                showExportDialog = true
            } 
        }
    )

    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Database", style = MaterialTheme.typography.headlineMedium)
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        ListItem(
            headlineContent = { Text("Database Status") },
            supportingContent = { 
                Text(
                    if (databasePath.isNullOrBlank()) "No active database" 
                    else databasePath.substringAfterLast('/')
                ) 
            },
            leadingContent = { Icon(Icons.Default.Storage, contentDescription = null) },
            trailingContent = {
                if (!databasePath.isNullOrBlank()) {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Database", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier.weight(1f),
                enabled = databasePath.isNullOrBlank()
            ) {
                Text("Create New")
            }
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("*/*")) },
                modifier = Modifier.weight(1f),
                enabled = databasePath.isNullOrBlank()
            ) {
                Text("Import")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = { 
                databasePath?.let { path ->
                    val fileName = path.substringAfterLast('/')
                    exportLauncher.launch(fileName)
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            enabled = !databasePath.isNullOrBlank()
        ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Export Database")
        }

        if (!databasePath.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Encryption Status")
                    Text(
                        if (isEncrypted) "Protected by internal key" else "No encryption", 
                        style = MaterialTheme.typography.bodySmall, 
                        color = Color.Gray
                    )
                }
                Icon(
                    imageVector = if (isEncrypted) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = null,
                    tint = if (isEncrypted) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
        }
    }

    if (showCreateDialog) {
        CreateDatabaseDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                onCreateDatabase(name)
                showCreateDialog = false
            }
        )
    }

    if (showExportDialog && pendingExportUri != null) {
        ExportDatabaseDialog(
            onDismiss = { 
                showExportDialog = false
                pendingExportUri = null
            },
            onConfirm = { encrypt, password ->
                onExportDatabase(pendingExportUri!!, password, encrypt)
                showExportDialog = false
                pendingExportUri = null
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Database?") },
            text = { Text("This will permanently delete the database file and all notes within it. This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteDatabase()
                    showDeleteConfirm = false
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ExportDatabaseDialog(
    onDismiss: () -> Unit,
    onConfirm: (Boolean, String?) -> Unit
) {
    var encrypt by remember { mutableStateOf(true) }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Database") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Choose how you want to export your database.")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Encrypt Exported File", modifier = Modifier.weight(1f))
                    Switch(checked = encrypt, onCheckedChange = { encrypt = it })
                }
                if (encrypt) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Export Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        supportingText = { Text("This can be different from your current password.") }
                    )
                } else {
                    Text(
                        "Warning: Exporting as plain text will remove all protection from the exported file.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(encrypt, if (encrypt) password else null) },
                enabled = !encrypt || password.isNotBlank()
            ) {
                Text("Export")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CreateDatabaseDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("notes") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Database") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("File Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "The database will be encrypted using an internal key for your security.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsSubScreen(
    currentTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit,
    currentCustomColor: CustomColor,
    onCustomColorChange: (CustomColor) -> Unit,
    pureBlack: Boolean,
    onPureBlackChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val themeExpanded = remember { mutableStateOf(false) }
    val colorExpanded = remember { mutableStateOf(false) }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Appearance", style = MaterialTheme.typography.headlineMedium)
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        // Theme Dropdown
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("App Theme", modifier = Modifier.weight(1f))
            Box {
                OutlinedButton(onClick = { themeExpanded.value = true }) {
                    Text(currentTheme.name.lowercase().replaceFirstChar { it.uppercase() })
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(
                    expanded = themeExpanded.value,
                    onDismissRequest = { themeExpanded.value = false }
                ) {
                    AppTheme.entries.forEach { theme ->
                        DropdownMenuItem(
                            text = { Text(theme.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            onClick = {
                                onThemeChange(theme)
                                themeExpanded.value = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(16.dp))

        // Color Dropdown
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Theme Color", modifier = Modifier.weight(1f))
            Box {
                OutlinedButton(onClick = { colorExpanded.value = true }) {
                    Text(currentCustomColor.colorName)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(
                    expanded = colorExpanded.value,
                    onDismissRequest = { colorExpanded.value = false }
                ) {
                    CustomColor.entries.forEach { color ->
                        DropdownMenuItem(
                            text = { Text(color.colorName) },
                            onClick = {
                                onCustomColorChange(color)
                                colorExpanded.value = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(16.dp))

        // Pure Black Switch
        val isDark = when (currentTheme) {
            AppTheme.LIGHT -> false
            AppTheme.DARK -> true
            AppTheme.SYSTEM -> isSystemInDarkTheme()
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Pure Black (Dark Mode)", 
                    color = if (isDark) Color.Unspecified else Color.Gray
                )
                if (!isDark) {
                    Text(
                        "Pure Black theme requires dark mode",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            Switch(
                checked = pureBlack,
                onCheckedChange = { onPureBlackChange(it) },
                enabled = isDark
            )
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    TOOLS("Tools", R.drawable.ic_tools),
    CALCULATORS("Calculators", R.drawable.ic_calculator),
    NOTES("Notes", R.drawable.ic_notes),
    FAVORITES("Favorites", R.drawable.ic_favorite),
    SETTINGS("Settings", R.drawable.ic_account_box),
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    DNMWTheme {
        DNMWApp()
    }
}

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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val themeState by viewModel.themeState.collectAsState()
            val customColorState by viewModel.customColorState.collectAsState()
            val pureBlackState by viewModel.pureBlackState.collectAsState()

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
                    onPureBlackChange = { viewModel.setPureBlack(it) }
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
    onPureBlackChange: (Boolean) -> Unit = {}
) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.TOOLS) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            painterResource(it.icon),
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
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
            Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
                when (currentDestination) {
                    AppDestinations.TOOLS -> Text("Tools Screen", style = MaterialTheme.typography.headlineMedium)
                    AppDestinations.CALCULATORS -> Text("Calculators Screen", style = MaterialTheme.typography.headlineMedium)
                    AppDestinations.NOTES -> Text("Notes Screen", style = MaterialTheme.typography.headlineMedium)
                    AppDestinations.FAVORITES -> Text("Favorites Screen", style = MaterialTheme.typography.headlineMedium)
                    AppDestinations.SETTINGS -> SettingsScreen(
                        currentTheme, onThemeChange,
                        currentCustomColor, onCustomColorChange,
                        pureBlack, onPureBlackChange
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    currentTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit,
    currentCustomColor: CustomColor,
    onCustomColorChange: (CustomColor) -> Unit,
    pureBlack: Boolean,
    onPureBlackChange: (Boolean) -> Unit
) {
    var settingsDestination by remember { mutableStateOf(SettingsSubDestination.MAIN) }

    AnimatedContent(
        targetState = settingsDestination,
        transitionSpec = {
            if (targetState == SettingsSubDestination.THEME) {
                // Moving Forward: Slide in from right, fade in; slide out to left, fade out
                (slideInHorizontally(animationSpec = tween(300)) { it } + fadeIn(animationSpec = tween(300)))
                    .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { -it / 3 } + fadeOut(animationSpec = tween(300)))
            } else {
                // Moving Backward: Slide in from left, fade in; slide out to right, fade out
                (slideInHorizontally(animationSpec = tween(300)) { -it / 3 } + fadeIn(animationSpec = tween(300)))
                    .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { it } + fadeOut(animationSpec = tween(300)))
            }
        },
        label = "settings_nav"
    ) { targetDestination ->
        when (targetDestination) {
            SettingsSubDestination.THEME -> {
                BackHandler {
                    settingsDestination = SettingsSubDestination.MAIN
                }
                ThemeSettingsSubScreen(
                    currentTheme, onThemeChange,
                    currentCustomColor, onCustomColorChange,
                    pureBlack, onPureBlackChange,
                    onBack = { settingsDestination = SettingsSubDestination.MAIN }
                )
            }
            SettingsSubDestination.MAIN -> {
                MainSettingsSubScreen(
                    onNavigateToTheme = { settingsDestination = SettingsSubDestination.THEME }
                )
            }
        }
    }
}

enum class SettingsSubDestination {
    MAIN, THEME
}

@Composable
fun MainSettingsSubScreen(onNavigateToTheme: () -> Unit) {
    Column {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))
        
        ListItem(
            headlineContent = { Text("Appearance") },
            supportingContent = { Text("Theme, colors, and dark mode") },
            modifier = Modifier.clickable { onNavigateToTheme() }
        )
    }
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

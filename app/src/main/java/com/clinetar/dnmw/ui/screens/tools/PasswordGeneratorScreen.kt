package com.clinetar.dnmw.ui.screens.tools

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordGeneratorScreen(onBack: () -> Unit) {
    var passwordLength by remember { mutableStateOf(16f) }
    var includeUppercase by remember { mutableStateOf(true) }
    var includeLowercase by remember { mutableStateOf(true) }
    var includeNumbers by remember { mutableStateOf(true) }
    var includeSymbols by remember { mutableStateOf(true) }
    var generatedPassword by remember { mutableStateOf("") }
    
    val context = LocalContext.current

    LaunchedEffect(passwordLength, includeUppercase, includeLowercase, includeNumbers, includeSymbols) {
        generatedPassword = generatePassword(passwordLength.toInt(), includeUppercase, includeLowercase, includeNumbers, includeSymbols)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Password Generator") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = generatedPassword,
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = android.content.ClipData.newPlainText("password", generatedPassword)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                    }
                    IconButton(onClick = {
                        generatedPassword = generatePassword(passwordLength.toInt(), includeUppercase, includeLowercase, includeNumbers, includeSymbols)
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Regenerate")
                    }
                }
            }

            Text("Length: ${passwordLength.toInt()}")
            Slider(
                value = passwordLength,
                onValueChange = { passwordLength = it },
                valueRange = 4f..32f,
                steps = 27
            )

            SwitchSetting("Include Uppercase", includeUppercase) { includeUppercase = it }
            SwitchSetting("Include Lowercase", includeLowercase) { includeLowercase = it }
            SwitchSetting("Include Numbers", includeNumbers) { includeNumbers = it }
            SwitchSetting("Include Symbols", includeSymbols) { includeSymbols = it }
        }
    }
}

@Composable
fun SwitchSetting(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

fun generatePassword(length: Int, upper: Boolean, lower: Boolean, nums: Boolean, symbols: Boolean): String {
    val uppercaseChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    val lowercaseChars = "abcdefghijklmnopqrstuvwxyz"
    val numberChars = "0123456789"
    val symbolChars = "!@#$%^&*()-_=+[]{}|;:,.<>?"
    
    val allowedChars = StringBuilder()
    if (upper) allowedChars.append(uppercaseChars)
    if (lower) allowedChars.append(lowercaseChars)
    if (nums) allowedChars.append(numberChars)
    if (symbols) allowedChars.append(symbolChars)
    
    if (allowedChars.isEmpty()) return ""
    
    return (1..length)
        .map { allowedChars[Random.nextInt(allowedChars.length)] }
        .joinToString("")
}

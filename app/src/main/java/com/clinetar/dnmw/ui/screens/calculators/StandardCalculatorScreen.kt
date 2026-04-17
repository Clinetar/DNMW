package com.clinetar.dnmw.ui.screens.calculators

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandardCalculatorScreen(onBack: () -> Unit) {
    var expression by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Standard Calculator") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Display Area
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = expression,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        textAlign = TextAlign.End
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = result,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End
                    )
                }
            }

            // Keypad
            val buttons = listOf(
                listOf("C", "( )", "%", "/"),
                listOf("7", "8", "9", "×"),
                listOf("4", "5", "6", "−"),
                listOf("1", "2", "3", "+"),
                listOf("+/-", "0", ".", "=")
            )

            buttons.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { char ->
                        CalculatorButton(
                            text = char,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                when (char) {
                                    "C" -> {
                                        expression = ""
                                        result = ""
                                    }
                                    "=" -> {
                                        if (expression.isNotBlank()) {
                                            result = try {
                                                evaluateExpression(expression)
                                            } catch (e: Exception) {
                                                "Error"
                                            }
                                        }
                                    }
                                    else -> {
                                        expression += when(char) {
                                            "×" -> "*"
                                            "−" -> "-"
                                            else -> char
                                        }
                                    }
                                }
                            },
                            containerColor = when {
                                char == "=" -> MaterialTheme.colorScheme.primaryContainer
                                char in listOf("/", "×", "−", "+", "=") -> MaterialTheme.colorScheme.secondaryContainer
                                char in listOf("C", "( )", "%") -> MaterialTheme.colorScheme.tertiaryContainer
                                else -> MaterialTheme.colorScheme.surfaceContainer
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalculatorButton(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.titleLarge)
    }
}

// Simple expression evaluator (very basic for now)
fun evaluateExpression(expr: String): String {
    // This is a placeholder. For a real app, use a math library like exp4j or similar.
    // For now, let's just do very simple single-op evaluation if possible, or return a mock.
    return try {
        val result = object : Any() {
            fun eval(str: String): Double {
                return object : Any() {
                    var pos = -1
                    var ch = 0
                    fun nextChar() {
                        ch = if (++pos < str.length) str[pos].code else -1
                    }

                    fun eat(charToEat: Int): Boolean {
                        while (ch == ' '.code) nextChar()
                        if (ch == charToEat) {
                            nextChar()
                            return true
                        }
                        return false
                    }

                    fun parse(): Double {
                        nextChar()
                        val x = parseExpression()
                        if (pos < str.length) throw RuntimeException("Unexpected: " + ch.toChar())
                        return x
                    }

                    fun parseExpression(): Double {
                        var x = parseTerm()
                        while (true) {
                            if (eat('+'.code)) x += parseTerm()
                            else if (eat('-'.code)) x -= parseTerm()
                            else return x
                        }
                    }

                    fun parseTerm(): Double {
                        var x = parseFactor()
                        while (true) {
                            if (eat('*'.code)) x *= parseFactor()
                            else if (eat('/'.code)) x /= parseFactor()
                            else return x
                        }
                    }

                    fun parseFactor(): Double {
                        if (eat('+'.code)) return parseFactor()
                        if (eat('-'.code)) return -parseFactor()
                        val x: Double
                        val startPos = pos
                        if (eat('('.code)) {
                            x = parseExpression()
                            eat(')'.code)
                        } else if (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) {
                            while (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) nextChar()
                            x = str.substring(startPos, pos).toDouble()
                        } else {
                            throw RuntimeException("Unexpected: " + ch.toChar())
                        }
                        return x
                    }
                }.parse()
            }
        }.eval(expr)
        
        if (result == result.toLong().toDouble()) {
            result.toLong().toString()
        } else {
            "%.4f".format(result).trimEnd('0').trimEnd('.')
        }
    } catch (e: Exception) {
        "Error"
    }
}

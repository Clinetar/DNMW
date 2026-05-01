package com.clinetar.dnmw.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clinetar.dnmw.ui.screens.calculators.BMICalculatorScreen
import com.clinetar.dnmw.ui.screens.calculators.StandardCalculatorScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorsScreen(
    selectedCalculator: String? = null,
    onSelectedCalculatorChange: (String?) -> Unit = {}
) {
    AnimatedContent(
        targetState = selectedCalculator,
        label = "calc_nav"
    ) { calcName ->
        if (calcName == null) {
            CalculatorsListScreen(onSelect = { onSelectedCalculatorChange(it) })
        } else {
            when (calcName) {
                "Standard" -> StandardCalculatorScreen(onBack = { onSelectedCalculatorChange(null) })
                "BMI" -> BMICalculatorScreen(onBack = { onSelectedCalculatorChange(null) })
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Calculator Not Implemented Yet")
                            Button(onClick = { onSelectedCalculatorChange(null) }) { Text("Back") }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorsListScreen(onSelect: (String) -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    val calculators = allCalculators

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Calculators") },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            items(calculators) { calc ->
                CalculatorCard(calc, onClick = { onSelect(calc.name) })
            }
        }
    }
}

data class CalculatorItem(val name: String, val description: String, val icon: ImageVector)

val allCalculators = listOf(
    CalculatorItem("Standard", "Basic arithmetic", Icons.Default.Calculate),
    CalculatorItem("Scientific", "Advanced math functions", Icons.Default.Calculate),
    CalculatorItem("Unit Converter", "Length, weight, temp, etc.", Icons.Default.Calculate),
    CalculatorItem("Currency", "Real-time exchange rates", Icons.Default.Calculate),
    CalculatorItem("BMI", "Health and fitness", Icons.Default.Calculate),
    CalculatorItem("Loan", "Mortgage and interest", Icons.Default.Calculate)
)

@Composable
fun CalculatorCard(item: CalculatorItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        shape = MaterialTheme.shapes.extraLarge,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    item.icon,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Column {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}

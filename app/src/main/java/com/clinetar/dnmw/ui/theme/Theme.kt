package com.clinetar.dnmw.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.clinetar.dnmw.AppTheme
import com.clinetar.dnmw.CustomColor
import androidx.core.graphics.ColorUtils

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF1C1B1F),
    onBackground = Color.White,
    onSurface = Color.White,
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
)

@Composable
fun DNMWTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    customColor: CustomColor = CustomColor.DYNAMIC,
    pureBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (appTheme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        customColor == CustomColor.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            val scheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (darkTheme && pureBlack) {
                scheme.copy(
                    background = Color.Black,
                    surface = Color.Black,
                    surfaceVariant = Color(0xFF121212),
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            } else {
                scheme
            }
        }
        customColor.hex != null -> {
            val seedColor = Color(customColor.hex)
            if (darkTheme) {
                val baseDark = if (pureBlack) Color.Black else blend(Color(0xFF121212), seedColor, 0.05f)
                darkColorScheme(
                    primary = seedColor,
                    onPrimary = Color.Black,
                    primaryContainer = blend(Color.Black, seedColor, 0.4f),
                    onPrimaryContainer = Color.White,
                    secondary = blend(seedColor, Color.White, 0.2f),
                    secondaryContainer = blend(Color.Black, seedColor, 0.25f),
                    background = baseDark,
                    surface = baseDark,
                    surfaceVariant = blend(baseDark, seedColor, 0.15f),
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            } else {
                val baseLight = blend(Color.White, seedColor, 0.03f)
                lightColorScheme(
                    primary = seedColor,
                    onPrimary = Color.White,
                    primaryContainer = blend(Color.White, seedColor, 0.3f),
                    onPrimaryContainer = Color.Black,
                    secondary = blend(seedColor, Color.Black, 0.1f),
                    secondaryContainer = blend(Color.White, seedColor, 0.15f),
                    background = baseLight,
                    surface = baseLight,
                    surfaceVariant = blend(baseLight, seedColor, 0.1f),
                    onBackground = Color(0xFF1C1B1F),
                    onSurface = Color(0xFF1C1B1F)
                )
            }
        }
        darkTheme -> {
            if (pureBlack) {
                DarkColorScheme.copy(
                    background = Color.Black,
                    surface = Color.Black,
                    surfaceVariant = Color(0xFF121212)
                )
            } else {
                DarkColorScheme
            }
        }
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

private fun blend(color1: Color, color2: Color, ratio: Float): Color {
    val result = ColorUtils.blendARGB(color1.toArgb(), color2.toArgb(), ratio)
    return Color(result)
}

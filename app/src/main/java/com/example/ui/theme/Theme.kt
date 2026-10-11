package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppThemeMode(val labelFr: String, val descriptionFr: String) {
    SYSTEM("Suivant le système", "S'adapte automatiquement au mode clair ou sombre d'Android"),
    LIGHT("Clair (Light Material)", "Interface haute lisibilité sur fond blanc et bleu cobalt"),
    DARK("Sombre (Dark Foundry)", "Terminal d'ingénierie bas niveau carbone et cyan optique")
}

private val RomForgeDarkColorScheme = darkColorScheme(
    primary = CyanOptical,
    onPrimary = Color(0xFF001F25),
    primaryContainer = CyanOpticalDim,
    onPrimaryContainer = Color(0xFFB5F6FF),
    secondary = EmeraldTerminal,
    onSecondary = Color(0xFF00210E),
    secondaryContainer = EmeraldTerminalDim,
    onSecondaryContainer = Color(0xFFB8FFD2),
    tertiary = AmberRoot,
    onTertiary = Color(0xFF281900),
    tertiaryContainer = AmberRootDim,
    onTertiaryContainer = Color(0xFFFFDEA3),
    error = CrimsonAlert,
    background = CarbonBackground,
    onBackground = TextPrimary,
    surface = SlateSurface,
    onSurface = TextPrimary,
    surfaceVariant = SlateSurfaceHigh,
    onSurfaceVariant = TextSecondary,
    outline = SlateBorder
)

private val RomForgeLightColorScheme = lightColorScheme(
    primary = LightPrimaryCobalt,
    onPrimary = Color.White,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = Color(0xFF001D36),
    secondary = LightSecondaryEmerald,
    onSecondary = Color.White,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = Color(0xFF00210E),
    tertiary = LightTertiaryAmber,
    onTertiary = Color.White,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = Color(0xFF291800),
    error = Color(0xFFBA1A1A),
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceHigh,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder
)

@Composable
fun MyApplicationTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
    }
    val colorScheme = if (useDarkTheme) RomForgeDarkColorScheme else RomForgeLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

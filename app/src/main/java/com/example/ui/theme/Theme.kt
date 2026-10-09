package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PdfPrimaryDark,
    onPrimary = PdfOnPrimaryDark,
    primaryContainer = PdfPrimaryContainerDark,
    onPrimaryContainer = PdfOnPrimaryContainerDark,
    secondary = PdfSecondaryDark,
    onSecondary = PdfOnSecondaryDark,
    secondaryContainer = PdfSecondaryContainerDark,
    onSecondaryContainer = PdfOnSecondaryContainerDark,
    tertiary = PdfTertiaryDark,
    onTertiary = PdfOnTertiaryDark,
    tertiaryContainer = PdfTertiaryContainerDark,
    onTertiaryContainer = PdfOnTertiaryContainerDark,
    background = PdfBackgroundDark,
    onBackground = PdfOnBackgroundDark,
    surface = PdfSurfaceDark,
    onSurface = PdfOnSurfaceDark,
    surfaceVariant = PdfSurfaceVariantDark,
    onSurfaceVariant = PdfOnSurfaceVariantDark,
    outline = PdfOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = PdfPrimaryLight,
    onPrimary = PdfOnPrimaryLight,
    primaryContainer = PdfPrimaryContainerLight,
    onPrimaryContainer = PdfOnPrimaryContainerLight,
    secondary = PdfSecondaryLight,
    onSecondary = PdfOnSecondaryLight,
    secondaryContainer = PdfSecondaryContainerLight,
    onSecondaryContainer = PdfOnSecondaryContainerLight,
    tertiary = PdfTertiaryLight,
    onTertiary = PdfOnTertiaryLight,
    tertiaryContainer = PdfTertiaryContainerLight,
    onTertiaryContainer = PdfOnTertiaryContainerLight,
    background = PdfBackgroundLight,
    onBackground = PdfOnBackgroundLight,
    surface = PdfSurfaceLight,
    onSurface = PdfOnSurfaceLight,
    surfaceVariant = PdfSurfaceVariantLight,
    onSurfaceVariant = PdfOnSurfaceVariantLight,
    outline = PdfOutlineLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent PDF branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

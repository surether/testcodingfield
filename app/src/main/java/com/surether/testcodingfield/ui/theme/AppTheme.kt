package com.surether.testcodingfield.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object AppPalette {
    const val Ink = 0xFF111827L
    const val Ocean = 0xFF0F766EL
    const val Coral = 0xFFE11D48L
    const val Violet = 0xFF6D28D9L
    const val Amber = 0xFFF59E0BL
    const val Graph = 0xFF2563EBL
}

private val LightScheme = lightColorScheme(
    primary = colorFromArgb(AppPalette.Ocean),
    onPrimary = Color.White,
    secondary = colorFromArgb(AppPalette.Coral),
    tertiary = colorFromArgb(AppPalette.Violet),
    surface = Color(0xFFF8FAFC),
    background = Color(0xFFF3F4F6),
    onSurface = colorFromArgb(AppPalette.Ink),
)

@Composable
fun TestCodingFieldTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightScheme,
        typography = MaterialTheme.typography,
        content = content,
    )
}

fun colorFromArgb(argb: Long): Color {
    val alpha = ((argb shr 24) and 0xFF) / 255f
    val red = ((argb shr 16) and 0xFF) / 255f
    val green = ((argb shr 8) and 0xFF) / 255f
    val blue = (argb and 0xFF) / 255f
    return Color(red = red, green = green, blue = blue, alpha = alpha)
}

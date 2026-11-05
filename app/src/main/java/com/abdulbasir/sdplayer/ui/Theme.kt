package com.abdulbasir.sdplayer.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Accent = Color(0xFF4F8BFF)
val AccentAlt = Color(0xFF7B3FF2)

private val Colors = darkColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    background = Color(0xFF0B0D12),
    onBackground = Color(0xFFECEFF4),
    surface = Color(0xFF151922),
    onSurface = Color(0xFFECEFF4),
    surfaceVariant = Color(0xFF1E2430),
    onSurfaceVariant = Color(0xFF9AA3B2),
)

@Composable
fun SDTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}

fun formatTime(ms: Long): String {
    val total = (ms.coerceAtLeast(0L)) / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

fun formatSize(bytes: Long): String {
    val mb = bytes / 1048576.0
    return if (mb >= 1024) "%.1f GB".format(mb / 1024) else "%.0f MB".format(mb)
}

fun qualityLabel(width: Int, height: Int): String {
    val side = minOf(width, height)
    return when {
        side <= 0 -> ""
        side >= 2000 -> "4K"
        side >= 1400 -> "2K"
        side >= 1000 -> "1080p"
        side >= 700 -> "720p"
        side >= 460 -> "480p"
        else -> "SD"
    }
}

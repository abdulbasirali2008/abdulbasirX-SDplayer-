package com.abdulbasir.sdplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * The "liquid glass" surface: a translucent tinted fill, a soft sheen that is
 * brighter at the top, and a thin bright rim that fades along the edge.
 * [fill] is how strong the tinted base is; higher is easier to read on top of busy pictures.
 */
fun Modifier.glass(shape: Shape, fill: Float = 0.42f): Modifier =
    this
        .clip(shape)
        .background(Color(0xFF2C2C2E).copy(alpha = fill.coerceIn(0f, 1f)))
        .background(
            Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.03f)),
            ),
        )
        .border(
            1.dp,
            Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.55f),
                    Color.White.copy(alpha = 0.06f),
                    Color.White.copy(alpha = 0.30f),
                ),
            ),
            shape,
        )

/** Soft coloured glow behind the screen, so the glass has something to catch. */
fun Modifier.aurora(): Modifier =
    this
        .background(Color(0xFF0B0D12))
        .drawBehind {
            val w = size.width
            val h = size.height
            drawRect(
                Brush.radialGradient(
                    listOf(Accent.copy(alpha = 0.38f), Color.Transparent),
                    center = Offset(w * 0.12f, h * 0.06f),
                    radius = w * 1.0f,
                ),
            )
            drawRect(
                Brush.radialGradient(
                    listOf(AccentAlt.copy(alpha = 0.34f), Color.Transparent),
                    center = Offset(w * 0.95f, h * 0.40f),
                    radius = w * 0.95f,
                ),
            )
            drawRect(
                Brush.radialGradient(
                    listOf(Color(0xFF64D2FF).copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(w * 0.15f, h * 0.88f),
                    radius = w * 0.95f,
                ),
            )
        }

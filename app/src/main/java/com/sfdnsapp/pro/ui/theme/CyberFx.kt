package com.sfdnsapp.pro.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Shared "Cyber Glass" brushes and glow helpers used across the polished UI.
 * Pure-Compose, allocation-free at composition time (all values are static).
 */

/** Glassy card fill: subtle top-light over the cyber surface. */
val CyberGlassFill: Brush = Brush.verticalGradient(
    listOf(Color(0xFF171D30), Color(0xFF0C101B))
)

/** Accent-tinted glass border: bright leading edge fading into the card tone. */
fun cyberGlassEdge(accent: Color, widthDp: Int = 1): BorderStroke = BorderStroke(
    widthDp.dp,
    Brush.linearGradient(
        listOf(
            accent.copy(alpha = 0.75f),
            accent.copy(alpha = 0.15f),
            CyberCardBorder
        )
    )
)

/** Soft radial halo drawn behind the content (cheap glow, no blur pass). */
fun Modifier.glowHalo(color: Color, alpha: Float = 0.30f): Modifier = drawBehind {
    drawCircle(
        brush = Brush.radialGradient(
            listOf(color.copy(alpha = alpha), Color.Transparent),
            center = center,
            radius = size.minDimension / 2f
        )
    )
}

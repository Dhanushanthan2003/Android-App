package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A beautiful glassmorphic container that mimics a frosted-glass surface.
 * It uses a semi-transparent dark background, a very thin luminous border,
 * and a subtle specular glow.
 */
@Composable
fun GlassBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    borderWidth: Dp = 1.dp,
    borderColor: Color = Color(0x22FFFFFF),
    backgroundColor: Color = Color(0x3C0F0F12), // Frosted dark
    innerGlowColor: Color = Color(0x08FFFFFF),
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .drawBehind {
                // Draw a beautiful subtle diagonal glass sheen
                val sheenBrush = Brush.linearGradient(
                    colors = listOf(
                        innerGlowColor,
                        Color.Transparent,
                        Color.Transparent,
                        innerGlowColor
                    )
                )
                drawRect(brush = sheenBrush)
            }
            .background(backgroundColor)
            .border(borderWidth, borderColor, shape),
        contentAlignment = contentAlignment,
        content = content
    )
}

/**
 * An interactive glassmorphic card component with shadow and rounded corners.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(16.dp),
    borderColor: Color = Color(0x26FFFFFF),
    backgroundColor: Color = Color(0x4A121217),
    content: @Composable BoxScope.() -> Unit
) {
    val baseModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }

    GlassBox(
        modifier = baseModifier,
        shape = shape,
        borderColor = borderColor,
        backgroundColor = backgroundColor,
        content = content
    )
}

/**
 * Reusable Glassmorphic button for premium call-to-actions.
 */
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp),
    borderColor: Color = Color(0x3300FFFF), // Cyan tint border
    backgroundColor: Color = Color(0x2200E5FF), // Subtle cyan glow
    content: @Composable BoxScope.() -> Unit
) {
    val activeModifier = if (enabled) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }

    GlassBox(
        modifier = activeModifier,
        shape = shape,
        borderColor = if (enabled) borderColor else Color(0x11FFFFFF),
        backgroundColor = if (enabled) backgroundColor else Color(0x11FFFFFF),
        innerGlowColor = if (enabled) Color(0x1100FFFF) else Color.Transparent,
        content = content
    )
}

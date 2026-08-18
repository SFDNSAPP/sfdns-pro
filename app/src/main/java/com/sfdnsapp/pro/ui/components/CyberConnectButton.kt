package com.sfdnsapp.pro.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sfdnsapp.pro.ui.theme.NeonCyan
import com.sfdnsapp.pro.ui.theme.NeonGreen
import com.sfdnsapp.pro.ui.theme.TextDim
import com.sfdnsapp.pro.ui.theme.TextPrimary

@Composable
fun CyberConnectButton(
    connectionState: String, // "disconnected", "connecting", "connected"
    isPersian: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = connectionState == "connected"
    val isConnecting = connectionState == "connecting"

    val infiniteTransition = rememberInfiniteTransition(label = "connectButtonAnimations")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isConnected) 4000 else if (isConnecting) 1200 else 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringRotation"
    )

    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    val activeColor by animateColorAsState(
        targetValue = when {
            isConnected -> NeonGreen
            isConnecting -> NeonCyan
            else -> Color(0xFF334155)
        },
        animationSpec = tween(400),
        label = "activeColor"
    )

    val buttonBg by animateColorAsState(
        targetValue = when {
            isConnected -> Color(0xFF06281E)
            isConnecting -> Color(0xFF082530)
            else -> Color(0xFF0F121C)
        },
        animationSpec = tween(400),
        label = "buttonBg"
    )

    Box(
        modifier = modifier.size(190.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer glowing pulse ring
        if (isConnected || isConnecting) {
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .scale(pulseGlow)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                activeColor.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Rotating dashed cyber ring
        Box(
            modifier = Modifier
                .size(165.dp)
                .rotate(rotation)
                .drawBehind {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            listOf(
                                activeColor.copy(alpha = 0.8f),
                                Color.Transparent,
                                activeColor.copy(alpha = 0.3f),
                                activeColor.copy(alpha = 0.8f)
                            )
                        ),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
        )

        // Inner Power Button
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(buttonBg)
                .border(2.dp, activeColor.copy(alpha = 0.7f), CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = activeColor)
                ) {
                    onClick()
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = when {
                        isConnected -> Icons.Default.Bolt
                        isConnecting -> Icons.Default.PowerSettingsNew
                        else -> Icons.Default.PowerSettingsNew
                    },
                    contentDescription = "Connect Button",
                    tint = if (isConnected || isConnecting) activeColor else TextDim,
                    modifier = Modifier.size(44.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = when {
                        isConnected -> if (isPersian) "متصل شد" else "CONNECTED"
                        isConnecting -> if (isPersian) "در حال اتصال..." else "CONNECTING..."
                        else -> if (isPersian) "شروع اتصال" else "CONNECT"
                    },
                    color = if (isConnected || isConnecting) activeColor else TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

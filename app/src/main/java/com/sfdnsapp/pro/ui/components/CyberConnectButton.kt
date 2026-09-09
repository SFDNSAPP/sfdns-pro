package com.sfdnsapp.pro.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.semantics.Role
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

    // Rotation for outer cyber dashed ring
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isConnected) 5000 else if (isConnecting) 1500 else 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringRotation"
    )

    // Outer radar expanding wave pulse
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isConnected) 1800 else 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveScale"
    )

    val waveAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isConnected) 1800 else 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveAlpha"
    )

    val activeColor by animateColorAsState(
        targetValue = when {
            isConnected -> NeonGreen
            isConnecting -> NeonCyan
            else -> Color(0xFF38BDF8)
        },
        animationSpec = tween(400),
        label = "activeColor"
    )

    Box(
        modifier = modifier.size(220.dp),
        contentAlignment = Alignment.Center
    ) {
        // Concentric Cyber Ring 1 (Outermost - 212dp)
        Box(
            modifier = Modifier
                .size(212.dp)
                .clip(CircleShape)
                .border(
                    width = 1.dp,
                    color = if (isConnected) NeonGreen.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f),
                    shape = CircleShape
                )
        )

        // Concentric Cyber Ring 2 (Middle - 182dp)
        Box(
            modifier = Modifier
                .size(182.dp)
                .clip(CircleShape)
                .border(
                    width = 1.dp,
                    color = if (isConnected) NeonGreen.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f),
                    shape = CircleShape
                )
        )

        // Expanding Wave Animation when connected or connecting
        if (isConnected || isConnecting) {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .scale(waveScale)
                    .clip(CircleShape)
                    .border(
                        width = 1.5.dp,
                        color = activeColor.copy(alpha = waveAlpha),
                        shape = CircleShape
                    )
            )
        }

        // Concentric Cyber Ring 3 (Rotating Sweep Ring - 162dp)
        Box(
            modifier = Modifier
                .size(162.dp)
                .rotate(rotation)
                .drawBehind {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            listOf(
                                activeColor.copy(alpha = if (isConnected || isConnecting) 0.85f else 0.3f),
                                Color.Transparent,
                                activeColor.copy(alpha = if (isConnected || isConnecting) 0.4f else 0.15f),
                                activeColor.copy(alpha = if (isConnected || isConnecting) 0.85f else 0.3f)
                            )
                        ),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
        )

        // Main Center Button (145dp)
        val buttonBackground = if (isConnected) {
            Brush.radialGradient(
                listOf(
                    Color(0xFF0F3628),
                    Color(0xFF071C14)
                )
            )
        } else if (isConnecting) {
            Brush.radialGradient(
                listOf(
                    Color(0xFF0C384D),
                    Color(0xFF061B24)
                )
            )
        } else {
            Brush.radialGradient(
                listOf(
                    Color(0xFF141926),
                    Color(0xFF0C0F18)
                )
            )
        }

        val buttonBorderColor = if (isConnected) {
            NeonGreen
        } else if (isConnecting) {
            NeonCyan
        } else {
            Color(0xFF28344E)
        }

        Box(
            modifier = Modifier
                .size(145.dp)
                .clip(CircleShape)
                .background(buttonBackground)
                .border(2.dp, buttonBorderColor, CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = activeColor),
                    role = Role.Button
                ) {
                    onClick()
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Central 46dp Icon Badge
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            if (isConnected) {
                                NeonGreen
                            } else if (isConnecting) {
                                NeonCyan.copy(alpha = 0.25f)
                            } else {
                                Color.White.copy(alpha = 0.06f)
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = if (isConnected) Color.Transparent else activeColor.copy(alpha = 0.4f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Default.Bolt else Icons.Default.PowerSettingsNew,
                        contentDescription = "Connect State Icon",
                        tint = if (isConnected) Color(0xFF0A1017) else if (isConnecting) NeonCyan else TextPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Action Title
                Text(
                    text = when {
                        isConnected -> if (isPersian) "متصل شد" else "CONNECTED"
                        isConnecting -> if (isPersian) "در حال اتصال..." else "CONNECTING..."
                        else -> if (isPersian) "اتصال" else "CONNECT"
                    },
                    color = if (isConnected) NeonGreen else if (isConnecting) NeonCyan else TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Subtitle
                Text(
                    text = when {
                        isConnected -> if (isPersian) "برای قطع لمس کنید" else "Tap to disconnect"
                        isConnecting -> if (isPersian) "تنظیم تونل امن" else "Setting up tunnel"
                        else -> if (isPersian) "برای شروع انتخاب کن" else "Select to start"
                    },
                    color = TextDim,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

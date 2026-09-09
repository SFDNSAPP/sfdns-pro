package com.sfdnsapp.pro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sfdnsapp.pro.ui.theme.CyberCardBorder
import com.sfdnsapp.pro.ui.theme.GoldVip
import com.sfdnsapp.pro.ui.theme.NeonCyan
import com.sfdnsapp.pro.ui.theme.NeonGreen
import com.sfdnsapp.pro.ui.theme.NeonPurple
import com.sfdnsapp.pro.ui.theme.TextDim
import com.sfdnsapp.pro.ui.theme.TextPrimary
import com.sfdnsapp.pro.ui.theme.TextSecondary

@Composable
fun QuickActionHub(
    isPersian: Boolean,
    onOpenRadar: () -> Unit,
    onOpenGaming: () -> Unit,
    onOpenSplitTunnel: () -> Unit,
    onOpenCustomDns: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 4 Quick Top Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickActionButton(
                icon = Icons.Default.Radar,
                color = NeonCyan,
                label = if (isPersian) "رادار هوشمند" else "RADAR",
                onClick = onOpenRadar,
                modifier = Modifier.weight(1f)
            )

            QuickActionButton(
                icon = Icons.Default.Gamepad,
                color = GoldVip,
                label = if (isPersian) "هاب بازی‌ها" else "GAMING",
                onClick = onOpenGaming,
                modifier = Modifier.weight(1f)
            )

            QuickActionButton(
                icon = Icons.Default.Security,
                color = NeonGreen,
                label = if (isPersian) "تفکیک برنامه" else "BYPASS",
                onClick = onOpenSplitTunnel,
                modifier = Modifier.weight(1f)
            )

            QuickActionButton(
                icon = Icons.Default.Add,
                color = NeonPurple,
                label = if (isPersian) "افزودن دستی" else "CUSTOM",
                onClick = onOpenCustomDns,
                modifier = Modifier.weight(1f)
            )
        }

        // Section Title: ابزارهای ویژه و هوشمند
        Text(
            text = if (isPersian) "ابزارهای سایبری و اختصاصی" else "CYBER INTELLIGENCE TOOLS",
            color = TextDim,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp, top = 6.dp)
        )

        // Feature Card 1: Radar Arena
        CyberFeatureCard(
            icon = Icons.Default.Radar,
            iconTint = NeonGreen,
            title = if (isPersian) "رادار مسابقه دی‌ان‌اس (Radar Arena)" else "Radar Arena - Speed Test",
            subtitle = if (isPersian) "تست همزمان پینگ تمام سرورها و اتصال خودکار به سریع‌ترین DNS" else "Scan all servers concurrently and auto-connect to fastest",
            badgeText = if (isPersian) "👑 تست ۳۰ سرور" else "👑 30 SERVERS",
            badgeColor = NeonGreen,
            onClick = onOpenRadar
        )

        // Feature Card 2: Game Hub Launcher
        CyberFeatureCard(
            icon = Icons.Default.Gamepad,
            iconTint = Color(0xFFC084FC),
            title = if (isPersian) "لانچر هوشمند و بوست بازی‌ها (Game Hub)" else "Smart Game Hub & Ping Booster",
            subtitle = if (isPersian) "پینگ پایین و بدون تحریم ویژه پابجی، کلش، کالاف و فیفا" else "Optimized low latency routes for competitive online games",
            badgeText = if (isPersian) "⚡ اجرای مستقیم" else "⚡ BOOST",
            badgeColor = Color(0xFFC084FC),
            onClick = onOpenGaming
        )

        // Feature Card 3: Split Tunnel Bypass
        CyberFeatureCard(
            icon = Icons.Default.Security,
            iconTint = NeonCyan,
            title = if (isPersian) "عبور هوشمند برنامه‌ها (Split Tunnel)" else "Smart App Bypass (Split Tunnel)",
            subtitle = if (isPersian) "تفکیک برنامه‌های بانکی، اسنپ و دولتی بدون قطع VPN" else "Bypass banking, ride-hailing & local domestic services",
            badgeText = if (isPersian) "🔒 بدون قطعی" else "🔒 BYPASS",
            badgeColor = NeonCyan,
            onClick = onOpenSplitTunnel
        )

        // Feature Card 4: Custom DNS
        CyberFeatureCard(
            icon = Icons.Default.Add,
            iconTint = Color(0xFFF59E0B),
            title = if (isPersian) "افزودن سرور دستی (Custom DNS)" else "Custom DNS Configuration",
            subtitle = if (isPersian) "پشتیبانی از سرورهای اختصاصی، AdGuard و پروتکل IPv6" else "Add your private DNS endpoints with IPv4 and IPv6 support",
            badgeText = if (isPersian) "✨ IPv4 & IPv6" else "✨ CUSTOM",
            badgeColor = Color(0xFFF59E0B),
            onClick = onOpenCustomDns
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    color: Color,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF131724))
            .border(1.dp, CyberCardBorder, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = color)
            ) { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.14f))
                    .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = label,
                color = TextPrimary,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CyberFeatureCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    badgeText: String,
    badgeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        iconTint.copy(alpha = 0.08f),
                        Color(0xFF111520)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = iconTint.copy(alpha = 0.3f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = iconTint)
            ) { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icon Box
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconTint.copy(alpha = 0.15f))
                        .border(1.dp, iconTint.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Text Column
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = title,
                            color = TextPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = subtitle,
                        color = TextDim,
                        fontSize = 10.5.sp,
                        lineHeight = 14.sp
                    )
                }
            }

            // Right Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeColor.copy(alpha = 0.14f))
                    .border(1.dp, badgeColor.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 7.dp, vertical = 4.dp)
            ) {
                Text(
                    text = badgeText,
                    color = badgeColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

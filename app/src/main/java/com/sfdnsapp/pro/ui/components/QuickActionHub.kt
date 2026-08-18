package com.sfdnsapp.pro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.sfdnsapp.pro.ui.theme.TextPrimary

@Composable
fun QuickActionHub(
    isPersian: Boolean,
    onOpenRadar: () -> Unit,
    onOpenGaming: () -> Unit,
    onOpenSplitTunnel: () -> Unit,
    onOpenCustomDns: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
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
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF101420))
            .border(1.dp, CyberCardBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(18.dp)
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

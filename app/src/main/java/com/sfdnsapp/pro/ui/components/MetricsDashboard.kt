package com.sfdnsapp.pro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sfdnsapp.pro.ui.theme.CyberCardBorder
import com.sfdnsapp.pro.ui.theme.NeonCyan
import com.sfdnsapp.pro.ui.theme.NeonGreen
import com.sfdnsapp.pro.ui.theme.NeonPurple
import com.sfdnsapp.pro.ui.theme.TextDim
import com.sfdnsapp.pro.ui.theme.TextPrimary
import com.sfdnsapp.pro.viewmodel.UiMetrics

@Composable
fun MetricsDashboard(
    metrics: UiMetrics,
    isPersian: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard(
                icon = Icons.Default.Speed,
                iconTint = NeonCyan,
                label = if (isPersian) "پینگ زنده" else "LIVE PING",
                value = metrics.ping,
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                icon = Icons.Default.Timer,
                iconTint = NeonGreen,
                label = if (isPersian) "مدت اتصال" else "DURATION",
                value = metrics.durationFormatted,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard(
                icon = Icons.Default.ArrowDownward,
                iconTint = Color(0xFF38BDF8),
                label = if (isPersian) "سرعت دانلود" else "DOWNLOAD",
                value = metrics.downloadSpeed,
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                icon = Icons.Default.ArrowUpward,
                iconTint = NeonPurple,
                label = if (isPersian) "سرعت آپلود" else "UPLOAD",
                value = metrics.uploadSpeed,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MetricCard(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF101420))
            .border(1.dp, CyberCardBorder, RoundedCornerShape(14.dp))
            .padding(vertical = 10.dp, horizontal = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column {
                Text(
                    text = label,
                    color = TextDim,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

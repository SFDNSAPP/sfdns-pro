package com.sfdnsapp.pro.ui.components

import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sfdnsapp.pro.ui.theme.CyberBgDarker
import com.sfdnsapp.pro.ui.theme.CyberCardBorder
import com.sfdnsapp.pro.ui.theme.CyberSurface
import com.sfdnsapp.pro.ui.theme.NeonCyan
import com.sfdnsapp.pro.ui.theme.NeonGreen
import com.sfdnsapp.pro.ui.theme.TextDim
import com.sfdnsapp.pro.ui.theme.TextPrimary
import com.sfdnsapp.pro.ui.theme.TextSecondary
import com.sfdnsapp.pro.viewmodel.AppSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    settings: AppSettings,
    isPersian: Boolean,
    onToggleDoh: (Boolean) -> Unit,
    onToggleIpv6: (Boolean) -> Unit,
    onToggleAntiDpi: (Boolean) -> Unit,
    onToggleAutoReconnect: (Boolean) -> Unit = {},
    onToggleAutoConnect: (Boolean) -> Unit = onToggleAutoReconnect,
    onToggleKillSwitch: (Boolean) -> Unit = {},
    onSelectCarrier: (String) -> Unit = {},
    onSelectLanguage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CyberBgDarker,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 28.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(NeonCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                    }

                    Column {
                        Text(
                            text = if (isPersian) "تنظیمات پیشرفته شبکه" else "ADVANCED SETTINGS",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = if (isPersian) "پیکربندی امنیت، اپراتور و کلید قطع" else "Security, Carrier & Kill Switch Options",
                            color = TextDim,
                            fontSize = 10.5.sp
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF141824))
                        .border(1.dp, CyberCardBorder, RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextDim, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Language Selector Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CyberSurface)
                    .border(1.dp, CyberCardBorder, RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        Text(
                            text = if (isPersian) "زبان برنامه (Language)" else "App Language",
                            color = TextPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isPersian) NeonCyan else Color(0xFF141824))
                                .border(1.dp, if (isPersian) NeonCyan else CyberCardBorder, RoundedCornerShape(8.dp))
                                .clickable { onSelectLanguage("fa") }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("فارسی", color = if (isPersian) Color.Black else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (!isPersian) NeonCyan else Color(0xFF141824))
                                .border(1.dp, if (!isPersian) NeonCyan else CyberCardBorder, RoundedCornerShape(8.dp))
                                .clickable { onSelectLanguage("en") }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("English", color = if (!isPersian) Color.Black else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Carrier MTU Optimization Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CyberSurface)
                    .border(1.dp, CyberCardBorder, RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CellTower, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        Column {
                            Text(
                                text = if (isPersian) "بهینه‌سازی اپراتور و MTU" else "Carrier & MTU Optimization",
                                color = TextPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isPersian) "تنظیم اندازه بسته‌های شبکه بر اساس بستر اینترنت" else "Tune packet sizes for lowest latency",
                                color = TextDim,
                                fontSize = 10.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val carrierList = listOf(
                        "auto" to if (isPersian) "خودکار" else "Auto",
                        "mci" to if (isPersian) "همراه اول" else "MCI",
                        "mtn" to if (isPersian) "ایرانسل" else "Irancell",
                        "wifi" to if (isPersian) "وای‌فای" else "Wi-Fi"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        carrierList.forEach { (key, label) ->
                            val isSelected = settings.carrierOpt == key
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) NeonCyan else Color(0xFF141824))
                                    .border(1.dp, if (isSelected) NeonCyan else CyberCardBorder, RoundedCornerShape(8.dp))
                                    .clickable { onSelectCarrier(key) }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.Black else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Kill Switch Toggle
            SettingToggleCard(
                icon = Icons.Default.GppBad,
                title = if (isPersian) "قطع خودکار ترافیک (Kill Switch)" else "Kill Switch Protection",
                description = if (isPersian) "مسدودسازی نشت ترافیک در صورت قطع اتصال تونل" else "Block unencrypted leaks when connection drops",
                isChecked = settings.isKillSwitchEnabled,
                onCheckedChange = onToggleKillSwitch
            )

            Spacer(modifier = Modifier.height(8.dp))

            // DoH Toggle
            SettingToggleCard(
                icon = Icons.Default.Lock,
                title = if (isPersian) "رمزنگاری DNS-over-HTTPS (DoH)" else "DNS-over-HTTPS (DoH)",
                description = if (isPersian) "رمزگذاری درخواست‌های DNS جهت جلوگیری از شنود و جعل" else "Encrypt DNS queries via HTTPS",
                isChecked = settings.isDohEnabled,
                onCheckedChange = onToggleDoh
            )

            Spacer(modifier = Modifier.height(8.dp))

            // IPv6 Toggle
            SettingToggleCard(
                icon = Icons.Default.VpnKey,
                title = if (isPersian) "پشتیبانی از پروتکل IPv6" else "IPv6 Protocol Support",
                description = if (isPersian) "مسیریابی آدرس‌های DNS بر بستر شبکه IPv6" else "Route DNS queries over IPv6 addresses",
                isChecked = settings.isIpv6Enabled,
                onCheckedChange = onToggleIpv6
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Anti-DPI Mode
            SettingToggleCard(
                icon = Icons.Default.Security,
                title = if (isPersian) "حالت ضد فیلترینگ هوشمند (Anti-DPI)" else "Smart Anti-DPI Bypass Mode",
                description = if (isPersian) "تغییر ساختار پکت‌های DNS برای عبور از فیلترینگ عمیق" else "Fragment packets to evade Deep Packet Inspection",
                isChecked = settings.isAntiDpiEnabled,
                onCheckedChange = onToggleAntiDpi
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Auto Reconnect & Boot Auto Connect
            SettingToggleCard(
                icon = Icons.Default.Refresh,
                title = if (isPersian) "اتصال خودکار (روشن شدن گوشی و تغییر اینترنت)" else "Auto-Connect on Boot & Network Change",
                description = if (isPersian) "شروع خودکار پس از بوت دستگاه و اتصال مجدد پس از قطعی اینترنت" else "Auto-start on device boot and reconnect on network switches",
                isChecked = settings.isAutoConnectEnabled,
                onCheckedChange = onToggleAutoConnect
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Share App Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF131826))
                    .border(1.dp, CyberCardBorder, RoundedCornerShape(14.dp))
                    .clickable { shareApp(context, isPersian) }
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(20.dp))
                        Column {
                            Text(
                                text = if (isPersian) "اشتراک‌گذاری SFDNS PRO" else "Share SFDNS PRO",
                                color = TextPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isPersian) "ارسال لینک دانلود به دوستان" else "Send app download link to friends",
                                color = TextDim,
                                fontSize = 10.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingToggleCard(
    icon: ImageVector,
    title: String,
    description: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CyberSurface)
            .border(1.dp, CyberCardBorder, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(icon, contentDescription = null, tint = if (isChecked) NeonCyan else TextDim, modifier = Modifier.size(20.dp))
                Column {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        color = TextDim,
                        fontSize = 10.5.sp,
                        lineHeight = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = NeonGreen,
                    uncheckedThumbColor = TextDim,
                    uncheckedTrackColor = Color(0xFF161B28),
                    uncheckedBorderColor = CyberCardBorder
                )
            )
        }
    }
}

private fun shareApp(context: Context, isPersian: Boolean) {
    val text = if (isPersian) {
        "🚀 برنامه SFDNS PRO Cyber Edition\nسریع‌ترین دی‌ان‌اس گیمینگ و دورزدن تحریم‌های آنلاین برای بازی‌ها و وب‌سایت‌ها با پینگ فوق‌العاده پایین."
    } else {
        "🚀 Download SFDNS PRO Cyber Edition\nThe fastest gaming DNS and anti-sanction bypass app for Android with ultra-low latency."
    }
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "SFDNS PRO"))
}

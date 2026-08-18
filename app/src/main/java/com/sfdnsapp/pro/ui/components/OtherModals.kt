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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sfdnsapp.pro.ui.theme.CyberBgDarker
import com.sfdnsapp.pro.ui.theme.CyberCardBorder
import com.sfdnsapp.pro.ui.theme.CyberSurface
import com.sfdnsapp.pro.ui.theme.GoldVip
import com.sfdnsapp.pro.ui.theme.NeonCyan
import com.sfdnsapp.pro.ui.theme.NeonGreen
import com.sfdnsapp.pro.ui.theme.NeonPink
import com.sfdnsapp.pro.ui.theme.TextDim
import com.sfdnsapp.pro.ui.theme.TextPrimary
import com.sfdnsapp.pro.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VipModal(
    isPersian: Boolean,
    onDismiss: () -> Unit
) {
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
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(GoldVip.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = GoldVip, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        text = if (isPersian) "عضویت و امکانات VIP" else "SFDNS VIP CLUB",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black
                    )
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

            Spacer(modifier = Modifier.height(14.dp))

            VipFeatureCard("⚡", if (isPersian) "سرورهای اختصاصی گیمینگ بدون جیتر" else "Ultra-low Jitter Dedicated Servers")
            Spacer(modifier = Modifier.height(8.dp))
            VipFeatureCard("🛡️", if (isPersian) "مسیریابی هوشمند پکت‌های UDP بازی" else "Smart UDP Packet Routing")
            Spacer(modifier = Modifier.height(8.dp))
            VipFeatureCard("🚀", if (isPersian) "بدون هیچ‌گونه تبلیغات و با بالاترین اولویت پینگ" else "Zero Ads with Highest Ping Priority")
            Spacer(modifier = Modifier.height(8.dp))
            VipFeatureCard("👑", if (isPersian) "دسترسی مادام‌العمر به تمامی امکانات نسخه سایبر" else "Lifetime Access to All Cyber Features")
        }
    }
}

@Composable
private fun VipFeatureCard(icon: String, text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberSurface)
            .border(1.dp, CyberCardBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(icon, fontSize = 20.sp)
            Text(text, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportModal(
    isPersian: Boolean,
    onDismiss: () -> Unit
) {
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
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(NeonPink.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = NeonPink, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        text = if (isPersian) "حمایت از توسعه‌دهنده" else "SUPPORT DEVELOPER",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black
                    )
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

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF161120))
                    .border(1.dp, NeonPink.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = if (isPersian) "توسعه داده شده با عشق برای گیمرهای ایرانی ❤️" else "Crafted with passion for high performance gaming ❤️",
                        color = TextPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isPersian) "برنامه SFDNS PRO به صورت کاملاً مستقل و رایگان توسعه داده می‌شود. از همراهی و حمایت شما بی‌نهایت سپاسگزاریم." else "SFDNS PRO is developed independently to provide fast and secure DNS access. Thank you for your support!",
                        color = TextDim,
                        fontSize = 11.5.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogModal(
    isPersian: Boolean,
    onDismiss: () -> Unit
) {
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
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(NeonCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        text = if (isPersian) "تغییرات نسخه ۲.۵ (Changelog)" else "WHAT'S NEW IN v2.5",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black
                    )
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

            Spacer(modifier = Modifier.height(14.dp))

            VipFeatureCard("⚡", if (isPersian) "موتور پینگ واقعی زنده UDP و TCP سوکت بومی" else "Real Socket UDP/TCP Ping Engine")
            Spacer(modifier = Modifier.height(8.dp))
            VipFeatureCard("🚀", if (isPersian) "معماری کاملاً بومی Jetpack Compose با نرخ ۱۲۰ هرتز" else "100% Native Jetpack Compose Architecture")
            Spacer(modifier = Modifier.height(8.dp))
            VipFeatureCard("🎮", if (isPersian) "هاب بازی‌ها و اجرای مستقیم بازی‌های نصب‌شده" else "Gaming Latency Hub & 1-tap Launch")
            Spacer(modifier = Modifier.height(8.dp))
            VipFeatureCard("📡", if (isPersian) "رادار بنچمارک هوشمند و اتصال به سریع‌ترین سرور" else "Smart Radar Benchmark & Auto-Connect")
        }
    }
}

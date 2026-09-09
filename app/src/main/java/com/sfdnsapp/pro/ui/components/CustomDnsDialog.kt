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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sfdnsapp.pro.IpValidator
import com.sfdnsapp.pro.ui.theme.CyberBgDarker
import com.sfdnsapp.pro.ui.theme.CyberCardBorder
import com.sfdnsapp.pro.ui.theme.NeonCyan
import com.sfdnsapp.pro.ui.theme.NeonGreen
import com.sfdnsapp.pro.ui.theme.NeonPurple
import com.sfdnsapp.pro.ui.theme.TextDim
import com.sfdnsapp.pro.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDnsDialog(
    isPersian: Boolean,
    onSave: (name: String, primary: String, secondary: String, primaryV6: String, secondaryV6: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var primary by remember { mutableStateOf("") }
    var secondary by remember { mutableStateOf("") }
    var primaryV6 by remember { mutableStateOf("") }
    var secondaryV6 by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

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
                            .background(NeonPurple.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(20.dp))
                    }

                    Column {
                        Text(
                            text = if (isPersian) "افزودن دی‌ان‌اس اختصاصی" else "ADD CUSTOM DNS",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = if (isPersian) "آدرس‌های IPv4 و IPv6 دلخواه" else "Custom IPv4 & IPv6 addresses",
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

            // Name Field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; errorText = null },
                label = { Text(if (isPersian) "نام سرور (مثلا: Fast DNS)" else "Server Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors()
            )

            Spacer(modifier = Modifier.height(8.dp))

            val isPrimaryInvalid = primary.isNotBlank() && !IpValidator.isValidIp(primary)
            val isSecondaryInvalid = secondary.isNotBlank() && !IpValidator.isValidIp(secondary)
            val isPrimaryV6Invalid = primaryV6.isNotBlank() && !IpValidator.isValidIpv6(primaryV6)

            val isFormValid = name.isNotBlank() &&
                    primary.isNotBlank() &&
                    IpValidator.isValidIp(primary) &&
                    (secondary.isBlank() || IpValidator.isValidIp(secondary)) &&
                    (primaryV6.isBlank() || IpValidator.isValidIpv6(primaryV6))

            // Primary DNS
            OutlinedTextField(
                value = primary,
                onValueChange = { primary = it; errorText = null },
                label = { Text(if (isPersian) "دی‌ان‌اس اصلی (Primary IPv4)" else "Primary DNS (IPv4)") },
                placeholder = { Text("1.1.1.1", color = TextDim) },
                isError = isPrimaryInvalid,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors()
            )
            if (isPrimaryInvalid) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isPersian) "فرمت آی‌پی نامعتبر است" else "Invalid IP address format",
                    color = Color(0xFFEF4444),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Secondary DNS
            OutlinedTextField(
                value = secondary,
                onValueChange = { secondary = it },
                label = { Text(if (isPersian) "دی‌ان‌اس دوم (Secondary IPv4)" else "Secondary DNS (IPv4)") },
                placeholder = { Text("1.0.0.1", color = TextDim) },
                isError = isSecondaryInvalid,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors()
            )
            if (isSecondaryInvalid) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isPersian) "فرمت آی‌پی نامعتبر است" else "Invalid IP address format",
                    color = Color(0xFFEF4444),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Primary IPv6 (Optional)
            OutlinedTextField(
                value = primaryV6,
                onValueChange = { primaryV6 = it },
                label = { Text(if (isPersian) "آدرس اختیاری Primary IPv6" else "Primary IPv6 (Optional)") },
                isError = isPrimaryV6Invalid,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors()
            )
            if (isPrimaryV6Invalid) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isPersian) "فرمت IPv6 نامعتبر است" else "Invalid IPv6 format",
                    color = Color(0xFFEF4444),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            errorText?.let { err ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(err, color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (!isFormValid) return@Button
                    onSave(name, primary, secondary, primaryV6, secondaryV6)
                    onDismiss()
                },
                enabled = isFormValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonGreen,
                    contentColor = Color.Black,
                    disabledContainerColor = Color(0xFF1E2638),
                    disabledContentColor = TextDim
                )
            ) {
                Text(
                    text = if (isPersian) "ذخیره و انتخاب سرور" else "SAVE & ACTIVATE",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NeonCyan,
    unfocusedBorderColor = CyberCardBorder,
    focusedContainerColor = Color(0xFF101420),
    unfocusedContainerColor = Color(0xFF101420),
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedLabelColor = NeonCyan,
    unfocusedLabelColor = TextDim
)

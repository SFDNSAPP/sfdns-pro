package com.sfdnsapp.pro

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.sfdnsapp.pro.data.DnsRepository
import com.sfdnsapp.pro.ui.components.ActiveServerCard
import com.sfdnsapp.pro.ui.components.CyberConnectButton
import com.sfdnsapp.pro.ui.components.CyberHeader
import com.sfdnsapp.pro.ui.components.MetricsDashboard
import com.sfdnsapp.pro.ui.components.QuickActionHub
import com.sfdnsapp.pro.ui.theme.MyApplicationTheme
import com.sfdnsapp.pro.viewmodel.UiMetrics
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DnsComposeUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `cyber header renders correctly in Persian`() {
        composeTestRule.setContent {
            MyApplicationTheme {
                CyberHeader(
                    isPersian = true,
                    onOpenVip = {},
                    onOpenChangelog = {},
                    onOpenSupport = {},
                    onOpenSettings = {}
                )
            }
        }
        composeTestRule.onNodeWithText("SFDNS").assertIsDisplayed()
        composeTestRule.onNodeWithText("نسخه سایبر ۲.۵").assertIsDisplayed()
    }

    @Test
    fun `active server card renders server name`() {
        val server = DnsRepository.defaultServers.first()
        composeTestRule.setContent {
            MyApplicationTheme {
                ActiveServerCard(
                    server = server,
                    livePing = 18,
                    isPersian = true,
                    onClick = {}
                )
            }
        }
        composeTestRule.onNodeWithText(server.faName).assertIsDisplayed()
    }

    @Test
    fun `connect button renders state text`() {
        composeTestRule.setContent {
            MyApplicationTheme {
                CyberConnectButton(
                    connectionState = "disconnected",
                    isPersian = true,
                    onClick = {}
                )
            }
        }
        composeTestRule.onNodeWithText("شروع اتصال").assertIsDisplayed()
    }

    @Test
    fun `metrics dashboard renders values`() {
        val metrics = UiMetrics(
            ping = "22ms",
            downloadSpeed = "1.5 MB/s",
            uploadSpeed = "0.8 MB/s",
            durationFormatted = "00:05:30"
        )
        composeTestRule.setContent {
            MyApplicationTheme {
                MetricsDashboard(
                    metrics = metrics,
                    isPersian = true
                )
            }
        }
        composeTestRule.onNodeWithText("22ms").assertIsDisplayed()
        composeTestRule.onNodeWithText("00:05:30").assertIsDisplayed()
    }

    @Test
    fun `quick action hub renders shortcut buttons`() {
        composeTestRule.setContent {
            MyApplicationTheme {
                QuickActionHub(
                    isPersian = true,
                    onOpenRadar = {},
                    onOpenGaming = {},
                    onOpenSplitTunnel = {},
                    onOpenCustomDns = {}
                )
            }
        }
        composeTestRule.onNodeWithText("رادار هوشمند").assertIsDisplayed()
        composeTestRule.onNodeWithText("هاب بازی‌ها").assertIsDisplayed()
        composeTestRule.onNodeWithText("تفکیک برنامه").assertIsDisplayed()
        composeTestRule.onNodeWithText("افزودن دستی").assertIsDisplayed()
    }
}

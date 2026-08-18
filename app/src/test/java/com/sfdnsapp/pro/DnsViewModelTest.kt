package com.sfdnsapp.pro

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.sfdnsapp.pro.viewmodel.DnsViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DnsViewModelTest {

    private lateinit var app: Application
    private lateinit var viewModel: DnsViewModel

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        viewModel = DnsViewModel(app)
    }

    @Test
    fun `initial state is disconnected`() {
        assertEquals("disconnected", viewModel.connectionState.value)
        assertNotNull(viewModel.selectedDns.value)
        assertTrue(viewModel.dnsList.value.isNotEmpty())
    }

    @Test
    fun `select DNS server updates selected server`() {
        val secondServer = viewModel.dnsList.value.getOrNull(1)
        assertNotNull(secondServer)
        viewModel.selectDns(secondServer!!)
        assertEquals(secondServer.id, viewModel.selectedDns.value.id)
    }

    @Test
    fun `set connection status updates state`() {
        viewModel.setConnectionStatus("connecting")
        assertEquals("connecting", viewModel.connectionState.value)

        viewModel.setConnectionStatus("connected")
        assertEquals("connected", viewModel.connectionState.value)

        viewModel.setConnectionStatus("disconnected")
        assertEquals("disconnected", viewModel.connectionState.value)
    }

    @Test
    fun `add custom DNS updates dnsList and selects it`() {
        val success = viewModel.addCustomDns("My DNS", "8.8.8.8", "8.8.4.4", "", "")
        assertTrue(success)
        assertEquals("My DNS", viewModel.selectedDns.value.name)
        assertTrue(viewModel.dnsList.value.any { it.name == "My DNS" })
    }

    @Test
    fun `settings toggles update state`() {
        val initialDoh = viewModel.settings.value.isDohEnabled
        viewModel.toggleDoh(!initialDoh)
        assertEquals(!initialDoh, viewModel.settings.value.isDohEnabled)

        viewModel.updateLanguage("en")
        assertEquals("en", viewModel.settings.value.language)
    }
}

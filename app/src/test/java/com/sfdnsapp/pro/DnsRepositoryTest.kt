package com.sfdnsapp.pro

import com.sfdnsapp.pro.data.DnsRepository
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DnsRepositoryTest {

    @Test
    fun `default server list contains popular servers`() {
        val servers = DnsRepository.defaultServers
        assertTrue(servers.isNotEmpty())
        assertTrue(servers.any { it.id == "electro" })
        assertTrue(servers.any { it.id == "shecan" })
        assertTrue(servers.any { it.id == "radar" })
        assertTrue(servers.any { it.id == "cloudflare" })
        assertTrue(servers.any { it.id == "google" })
    }

    @Test
    fun `popular games list is defined`() {
        val games = DnsRepository.popularGames
        assertTrue(games.isNotEmpty())
        assertTrue(games.any { it.name.contains("PUBG", ignoreCase = true) })
        assertTrue(games.any { it.name.contains("Valorant", ignoreCase = true) })
        assertTrue(games.any { it.name.contains("Warzone", ignoreCase = true) })
    }
}

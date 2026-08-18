package com.sfdnsapp.pro.service

import com.sfdnsapp.pro.DnsVpnService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

object DnsPingEngine {

    private val pingCache = ConcurrentHashMap<String, Pair<Long, Int>>()
    private const val CACHE_DURATION_MS = 3500L

    suspend fun pingDnsIp(ip: String): Int = withContext(Dispatchers.IO) {
        if (ip.isBlank() || ip == "0.0.0.0") return@withContext -1

        val now = System.currentTimeMillis()
        val cached = pingCache[ip]
        if (cached != null && (now - cached.first) < CACHE_DURATION_MS && cached.second > 0) {
            return@withContext cached.second
        }

        val address = try {
            InetAddress.getByName(ip)
        } catch (e: Exception) {
            return@withContext -1
        }

        var bestPing = Int.MAX_VALUE

        // Standard RFC 1035 DNS Query for "example.com"
        val queryBytes = byteArrayOf(
            0x12.toByte(), 0x34.toByte(), // Transaction ID
            0x01.toByte(), 0x00.toByte(), // Standard query with RD=1
            0x00.toByte(), 0x01.toByte(), // 1 question
            0x00.toByte(), 0x00.toByte(),
            0x00.toByte(), 0x00.toByte(),
            0x00.toByte(), 0x00.toByte(),
            0x07.toByte(), 'e'.code.toByte(), 'x'.code.toByte(), 'a'.code.toByte(), 'm'.code.toByte(), 'p'.code.toByte(), 'l'.code.toByte(), 'e'.code.toByte(),
            0x03.toByte(), 'c'.code.toByte(), 'o'.code.toByte(), 'm'.code.toByte(),
            0x00.toByte(),
            0x00.toByte(), 0x01.toByte(), // Type A
            0x00.toByte(), 0x01.toByte()  // Class IN
        )

        // Probe with UDP
        for (attempt in 0 until 3) {
            try {
                DatagramSocket().use { socket ->
                    DnsVpnService.protectSocket(socket)
                    socket.soTimeout = 850
                    val startTime = System.nanoTime()
                    val packet = DatagramPacket(queryBytes, queryBytes.size, address, 53)
                    socket.send(packet)

                    val responseBytes = ByteArray(1024)
                    val responsePacket = DatagramPacket(responseBytes, responseBytes.size)
                    socket.receive(responsePacket)

                    val elapsed = ((System.nanoTime() - startTime) / 1_000_000).toInt()
                    if (elapsed in 1 until bestPing) {
                        bestPing = elapsed
                    }
                }
            } catch (e: Exception) {
                // Timeout or filtered
            }

            if (bestPing < Int.MAX_VALUE && attempt >= 1) {
                break
            }
            if (attempt == 0) {
                try { kotlinx.coroutines.delay(15) } catch (e: Exception) {}
            }
        }

        if (bestPing < Int.MAX_VALUE) {
            pingCache[ip] = Pair(now, bestPing)
            return@withContext bestPing
        }

        // Fallback to TCP handshake on port 53 (RFC 7766 DNS-over-TCP)
        val tcpResult = try {
            val startTime = System.nanoTime()
            Socket().use { socket ->
                DnsVpnService.protectSocket(socket)
                socket.connect(InetSocketAddress(address, 53), 1100)
                val elapsed = ((System.nanoTime() - startTime) / 1_000_000).toInt()
                if (elapsed > 0) elapsed else 1
            }
        } catch (e: Exception) {
            try {
                val startTime = System.nanoTime()
                Socket().use { socket ->
                    DnsVpnService.protectSocket(socket)
                    socket.connect(InetSocketAddress(address, 443), 1000)
                    val elapsed = ((System.nanoTime() - startTime) / 1_000_000).toInt()
                    if (elapsed > 0) elapsed else 1
                }
            } catch (ex: Exception) {
                -1
            }
        }

        if (tcpResult > 0) {
            pingCache[ip] = Pair(now, tcpResult)
        }
        return@withContext tcpResult
    }

    suspend fun pingHost(host: String, port: Int = 443): Int = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val cached = pingCache[host]
        if (cached != null && (now - cached.first) < CACHE_DURATION_MS && cached.second > 0) {
            return@withContext cached.second
        }

        try {
            val address = InetAddress.getByName(host)
            val startTime = System.nanoTime()
            Socket().use { socket ->
                DnsVpnService.protectSocket(socket)
                socket.connect(InetSocketAddress(address, port), 1200)
                val elapsed = ((System.nanoTime() - startTime) / 1_000_000).toInt()
                val result = if (elapsed > 0) elapsed else 1
                pingCache[host] = Pair(now, result)
                return@withContext result
            }
        } catch (e: Exception) {
            return@withContext -1
        }
    }
}

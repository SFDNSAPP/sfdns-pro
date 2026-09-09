package com.sfdnsapp.pro

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IpValidatorTest {

    @Test
    fun testValidIpv4() {
        assertTrue(IpValidator.isValidIpv4("1.1.1.1"))
        assertTrue(IpValidator.isValidIpv4("8.8.8.8"))
        assertTrue(IpValidator.isValidIpv4("10.201.201.201"))
        assertTrue(IpValidator.isValidIpv4("10.201.201.202"))
        assertTrue(IpValidator.isValidIpv4("192.168.1.1"))
        assertTrue(IpValidator.isValidIpv4("255.255.255.255"))
        assertTrue(IpValidator.isValidIpv4("0.0.0.0"))
    }

    @Test
    fun testInvalidIpv4() {
        assertFalse(IpValidator.isValidIpv4(null))
        assertFalse(IpValidator.isValidIpv4(""))
        assertFalse(IpValidator.isValidIpv4("   "))
        assertFalse(IpValidator.isValidIpv4("256.0.0.1"))
        assertFalse(IpValidator.isValidIpv4("1.1.1"))
        assertFalse(IpValidator.isValidIpv4("1.1.1.1.1"))
        assertFalse(IpValidator.isValidIpv4("1.1.1.abc"))
        assertFalse(IpValidator.isValidIpv4("1.1.1.-1"))
    }

    @Test
    fun testValidIpv6() {
        assertTrue(IpValidator.isValidIpv6("2606:4700:4700::1111"))
        assertTrue(IpValidator.isValidIpv6("2001:4860:4860::8888"))
        assertTrue(IpValidator.isValidIpv6("::1"))
        assertTrue(IpValidator.isValidIpv6("fe80::1"))
        assertTrue(IpValidator.isValidIpv6("2001:0db8:85a3:0000:0000:8a2e:0370:7334"))
        assertTrue(IpValidator.isValidIpv6("::"))
    }

    @Test
    fun testInvalidIpv6() {
        assertFalse(IpValidator.isValidIpv6(null))
        assertFalse(IpValidator.isValidIpv6(""))
        assertFalse(IpValidator.isValidIpv6("   "))
        assertFalse(IpValidator.isValidIpv6("1.1.1.1"))
        assertFalse(IpValidator.isValidIpv6("2001:4860:4860:::8888"))
        assertFalse(IpValidator.isValidIpv6("gggg::1"))
    }

    @Test
    fun testRadarGameIps() {
        // Confirm official Radar Game IPs validate perfectly
        assertTrue(IpValidator.isValidIp("10.201.201.201"))
        assertTrue(IpValidator.isValidIp("10.201.201.202"))
    }
}

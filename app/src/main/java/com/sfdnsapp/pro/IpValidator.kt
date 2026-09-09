package com.sfdnsapp.pro

import java.net.Inet6Address
import java.net.InetAddress

/**
 * Shared IP address validator for IPv4 and IPv6.
 * Provides unified, strict validation for Compose UI, ViewModel, and DnsVpnService.
 */
object IpValidator {

    /**
     * Returns true if the given string is a valid IPv4 or IPv6 address.
     */
    fun isValidIp(ip: String?): Boolean {
        if (ip.isNullOrBlank()) return false
        val clean = ip.trim()
        return isValidIpv4(clean) || isValidIpv6(clean)
    }

    /**
     * Validates standard dot-decimal IPv4 address format (e.g. 1.1.1.1, 192.168.1.1).
     * Strictly rejects leading zeroes (except standalone "0"), numbers > 255, and non-numeric parts.
     */
    fun isValidIpv4(ip: String?): Boolean {
        if (ip.isNullOrBlank()) return false
        val clean = ip.trim()
        val parts = clean.split('.')
        if (parts.size != 4) return false

        for (part in parts) {
            if (part.isEmpty() || part.length > 3) return false
            if (!part.all { it.isDigit() }) return false
            if (part.length > 1 && part.startsWith('0')) return false
            val num = part.toIntOrNull() ?: return false
            if (num !in 0..255) return false
        }
        return true
    }

    /**
     * Validates IPv6 address format, including compressed (::) notation.
     * Prevents accidental network DNS lookups by pre-checking hex characters and colons,
     * then verifying through InetAddress numeric parsing.
     */
    fun isValidIpv6(ip: String?): Boolean {
        if (ip.isNullOrBlank()) return false
        val clean = ip.trim()

        // IPv6 must contain at least one colon and cannot contain spaces
        if (!clean.contains(':') || clean.contains(' ')) return false

        // Ensure characters are strictly hexadecimal or colon
        if (!clean.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' || it == ':' }) {
            return false
        }

        // Cannot contain triple or more colons (":::")
        if (clean.contains(":::")) return false

        // Cannot contain more than one double colon ("::")
        val doubleColonCount = clean.windowed(2).count { it == "::" }
        if (doubleColonCount > 1) return false

        // Unspecified address "::"
        if (clean == "::") return true

        return try {
            // Because the string contains strictly [0-9a-fA-F:] and a colon,
            // getByName parses it purely as an IPv6 literal without network DNS lookups.
            val addr = InetAddress.getByName(clean)
            addr is Inet6Address
        } catch (_: Exception) {
            false
        }
    }
}

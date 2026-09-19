package ru.coffeemaniavpn.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Hysteria2ParserTest {
    @Test
    fun parsesSimpleHy2Uri() {
        val node = Hysteria2Parser.parseUri(
            "hy2://secret@fi.example.com:443?sni=fi.example.com#Finland%20%E2%84%962",
        )
        assertNotNull(node)
        assertEquals("hysteria2", node!!.protocol)
        assertEquals("fi.example.com", node.host)
        assertEquals(443, node.port)
        assertEquals("secret", node.password)
        assertEquals("Finland №2", node.name)
        assertTrue(node.isHysteria2)
    }

    @Test
    fun parsesMultiPortRange() {
        val node = Hysteria2Parser.parseUri(
            "hysteria2://pass@nl.example.com:20000-50000/?sni=nl.example.com#NL2",
        )
        assertNotNull(node)
        assertEquals(20000, node!!.port)
        assertEquals("nl.example.com", node.host)
    }

    @Test
    fun parsesMultiPortListAndRange() {
        val node = Hysteria2Parser.parseUri(
            "hy2://pass@nl.example.com:1234,5000-6000,7044#NL6",
        )
        assertNotNull(node)
        assertEquals(1234, node!!.port)
    }

    @Test
    fun parsesAuthFromQueryWhenUserInfoMissing() {
        val node = Hysteria2Parser.parseUri(
            "hy2://fi.example.com:443?auth=token&sni=fi.example.com#FI",
        )
        assertNotNull(node)
        assertEquals("token", node!!.password)
        assertEquals(443, node.port)
    }
}

class PortSpecTest {
    @Test
    fun parsesPlainAndHopFormats() {
        assertEquals(443, PortSpec.firstPortFromText("443"))
        assertEquals(1234, PortSpec.firstPortFromText("1234,5678"))
        assertEquals(20000, PortSpec.firstPortFromText("20000-50000"))
        assertEquals(1234, PortSpec.firstPortFromText("1234,5000-6000"))
        assertEquals(2080, PortSpec.firstPortFromText("2080:3000"))
        assertEquals(443, PortSpec.firstPort(443))
        assertEquals(8443, PortSpec.firstPort("8443"))
    }
}

package ru.coffeemaniavpn.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RoutingRuleInputTest {

    @Test
    fun classifyDomainCidrAndInvalid() {
        assertEquals(RoutingRuleLineKind.Domain, RoutingRuleInput.classifyLine("Ya.ru"))
        assertEquals(RoutingRuleLineKind.Domain, RoutingRuleInput.classifyLine("*.youtube.com"))
        assertEquals(RoutingRuleLineKind.Cidr, RoutingRuleInput.classifyLine("192.168.0.0/16"))
        assertEquals(RoutingRuleLineKind.Cidr, RoutingRuleInput.classifyLine("2001:db8::/32"))
        assertEquals(RoutingRuleLineKind.Blank, RoutingRuleInput.classifyLine("   "))
        assertEquals(RoutingRuleLineKind.Invalid, RoutingRuleInput.classifyLine("not a rule"))
    }

    @Test
    fun normalizeWebsiteFromUrl() {
        val rule = RoutingRuleInput.toStoredRule(
            "https://www.YouTube.com/watch?v=1",
            RoutingRuleLineKind.Domain,
            RoutingRuleTarget.Direct,
        )
        assertNotNull(rule)
        assertEquals("youtube.com", rule!!.value)
        assertEquals(RoutingRuleMatcher.DomainSuffix, rule.matcher)
        assertEquals(RoutingRuleTarget.Direct, rule.target)
    }

    @Test
    fun rejectInvalidStoredRule() {
        assertNull(
            RoutingRuleInput.toStoredRule(
                "hello world",
                RoutingRuleLineKind.Invalid,
                RoutingRuleTarget.Block,
            ),
        )
    }
}

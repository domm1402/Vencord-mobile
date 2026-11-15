package com.vencordmobile

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscordUrlPolicyTest {
    @Test fun acceptsDiscordDomainsAndTheirSubdomains() {
        listOf(
            "discord.com", "canary.discord.com", "discordapp.com",
            "cdn.discordapp.com", "discordapp.net", "media.discordapp.net",
            "discord.gg", "discord.media", "discord.gift", "discord.new"
        ).forEach { assertTrue("$it should stay in Discord WebView", DiscordUrlPolicy.isTrustedWebViewHost(it)) }
    }

    @Test fun rejectsLookalikeAndUnrelatedHosts() {
        listOf(
            "discord.com.attacker.example", "notdiscord.com", "evil-discord.com",
            "discordapp.com.example", "discord-gift.com", "example.com"
        ).forEach { assertFalse("$it must open externally", DiscordUrlPolicy.isTrustedWebViewHost(it)) }
    }

    @Test fun normalizesCaseAndTrailingDot() {
        assertTrue(DiscordUrlPolicy.isTrustedWebViewHost("CANARY.DISCORD.COM."))
        assertFalse(DiscordUrlPolicy.isTrustedWebViewHost("discord.com..attacker"))
    }
}

package com.vencordmobile

/** Explicit host boundary for pages allowed to remain inside the signed-in WebView. */
internal object DiscordUrlPolicy {
    private val exactHosts = setOf(
        "discord.gg", "discord.media", "discord.gift", "discord.new"
    )
    private val suffixHosts = setOf("discord.com", "discordapp.com", "discordapp.net")

    fun isTrustedWebViewHost(host: String): Boolean {
        val normalized = host.trimEnd('.').lowercase(java.util.Locale.ROOT)
        return normalized in exactHosts || suffixHosts.any {
            normalized == it || normalized.endsWith(".$it")
        }
    }
}

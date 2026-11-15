package com.vencordmobile

/** Strict allowlist and type/range checks for user-pasted settings backups. */
internal object BackupPreferenceValidator {
    private val booleanKeys = setOf(
        "desktop_mode", "profile_overlay", "reader_mode", "compact", "upsells",
        "noavatars", "cards", "bigtext", "compact_channels", "hide_members",
        "hide_activity", "hide_embeds", "hide_attachments", "reduce_motion",
        "hide_sticker_buttons", "rpc_timestamps_enabled"
    )
    private val integerKeys = setOf("theme", "font_choice", "rpc_type")
    private val stringKeys = setOf(
        "custom_accent", "css", "snippet_vault", "message_studio_draft",
        "bookmarks", "profile_badges"
    )

    /** Returns a correctly typed value, or null when a backup entry is unsafe/invalid. */
    fun parse(key: String, declaredType: String, value: String): Any? {
        if (key == "custom_plugins") return null
        val expectedType = when {
            key in booleanKeys || key.matches(Regex("feature_[a-z_]+")) -> "boolean"
            key in integerKeys -> "int"
            key in stringKeys || key.matches(Regex("rpc_[a-z0-9_]+")) -> "string"
            else -> return null
        }
        if (declaredType != expectedType) return null
        return when (expectedType) {
            "boolean" -> when (value) { "true" -> true; "false" -> false; else -> null }
            "int" -> value.toIntOrNull()?.takeIf {
                when (key) {
                    "theme" -> it in 0..10
                    "font_choice" -> it in 0..5
                    "rpc_type" -> it in 0..5
                    else -> false
                }
            }
            else -> value.takeIf { it.length <= 100000 }
        }
    }
}

package com.vencordmobile

/**
 * Heuristic warnings for untrusted extension code. This is not a sandbox or malware scanner:
 * plugin JavaScript still runs with the privileges of the Discord page.
 */
internal object PluginRiskAnalyzer {
    data class Finding(val label: String, val detail: String)

    fun inspect(source: String): List<Finding> {
        val checks = listOf(
            Regex("""\beval\s*\(""") to Finding("Dynamic code execution", "eval() can execute generated or concealed JavaScript."),
            Regex("""\bnew\s+Function\s*\(""") to Finding("Dynamic code execution", "Function() compiles strings into executable code."),
            Regex("""\b(fetch|XMLHttpRequest|GM_xmlhttpRequest)\b""") to Finding("Network access", "The plugin can send or retrieve data from remote servers."),
            Regex("""\b(localStorage|sessionStorage|indexedDB)\b""") to Finding("Persistent browser storage", "The plugin can read or write data stored by the page."),
            Regex("""\b(document\.cookie|Authorization|Bearer\s)""", RegexOption.IGNORE_CASE) to Finding("Credential-related APIs", "Review any access to cookies or authorization headers carefully."),
            Regex("""atob\s*\(|String\.fromCharCode|\\x[0-9a-f]{2}""", RegexOption.IGNORE_CASE) to Finding("Encoded strings", "Encoded text may hide behavior; inspect the source before installing."),
            Regex("""https?://""", RegexOption.IGNORE_CASE) to Finding("Embedded URLs", "The source contains URLs. Verify destinations are expected.")
        )
        return checks.filter { (pattern, _) -> pattern.containsMatchIn(source) }.map { it.second }
    }

    fun summary(source: String): String {
        val findings = inspect(source)
        if (findings.isEmpty()) return "No common risk indicators detected. This does NOT prove the plugin is safe."
        return findings.joinToString("\n") { "• " + it.label + ": " + it.detail } +
            "\n\nHeuristic only: absence of warnings does not mean safe."
    }
}

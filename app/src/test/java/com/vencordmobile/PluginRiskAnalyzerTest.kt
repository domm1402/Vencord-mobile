package com.vencordmobile

import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class PluginRiskAnalyzerTest {
    @Test fun flagsDynamicExecutionAndNetworkAccess() {
        val findings = PluginRiskAnalyzer.inspect("eval(payload); fetch('https://example.invalid')")
        assertTrue(findings.any { it.label == "Dynamic code execution" })
        assertTrue(findings.any { it.label == "Network access" })
        assertTrue(findings.any { it.label == "Embedded URLs" })
    }

    @Test fun flagsCredentialAndObfuscationIndicators() {
        val findings = PluginRiskAnalyzer.inspect("document.cookie; atob('encoded')")
        assertTrue(findings.any { it.label == "Credential-related APIs" })
        assertTrue(findings.any { it.label == "Encoded strings" })
    }

    @Test fun explicitlySaysNoFindingsIsNotProofOfSafety() {
        val summary = PluginRiskAnalyzer.summary("document.querySelector('#app')")
        assertFalse(PluginRiskAnalyzer.inspect("document.querySelector('#app')").isNotEmpty())
        assertTrue(summary.contains("does NOT prove"))
    }
}

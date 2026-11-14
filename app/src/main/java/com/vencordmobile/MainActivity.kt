package com.vencordmobile

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.Button
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient

class MainActivity : Activity() {
    private lateinit var web: WebView
    private lateinit var root: FrameLayout
    private val prefs by lazy { getSharedPreferences("vcm_prefs", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = FrameLayout(this)
        web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.userAgentString = web.settings.userAgentString.replace("; wv", "")
        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                applyTheme()
            }
        }
        web.webChromeClient = WebChromeClient()
        root.addView(web, FrameLayout.LayoutParams(-1, -1))

        val menu = Button(this).apply {
            text = "V"
            textSize = 18f
            contentDescription = "Vencord Mobile settings"
            setOnClickListener { showSettings() }
        }
        val lp = FrameLayout.LayoutParams(dp(54), dp(54), Gravity.BOTTOM or Gravity.END)
        lp.setMargins(dp(16), dp(16), dp(18), dp(24))
        root.addView(menu, lp)
        setContentView(root)

        if (savedInstanceState == null) web.loadUrl("https://discord.com/app")
        else web.restoreState(savedInstanceState)
    }

    private fun showSettings() {
        val items = arrayOf(
            if (prefs.getBoolean("amoled", false)) "✓ AMOLED black theme" else "AMOLED black theme",
            if (prefs.getBoolean("compact", false)) "✓ Compact chat" else "Compact chat",
            "Reload Discord",
            "About Vencord Mobile"
        )
        AlertDialog.Builder(this)
            .setTitle("Vencord Mobile")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> { prefs.edit().putBoolean("amoled", !prefs.getBoolean("amoled", false)).apply(); applyTheme() }
                    1 -> { prefs.edit().putBoolean("compact", !prefs.getBoolean("compact", false)).apply(); applyTheme() }
                    2 -> web.reload()
                    3 -> AlertDialog.Builder(this).setTitle("About")
                        .setMessage("Vencord Mobile is an experimental Discord WebView shell. These are local visual settings, not Vencord plugins or Discord account changes.")
                        .setPositiveButton("OK", null).show()
                }
            }.show()
    }

    private fun applyTheme() {
        val amoled = prefs.getBoolean("amoled", false)
        val compact = prefs.getBoolean("compact", false)
        val css = buildString {
            if (amoled) append("html,body,#app-mount{background:#000!important;} [class*=sidebar], [class*=chat], [class*=channel]{background-color:#050505!important;}")
            if (compact) append("[class*=messageListItem]{padding-top:2px!important;padding-bottom:2px!important;} [class*=messageContent]{line-height:1.25!important;}")
        }
        val escaped = css.replace("\\", "\\\\").replace("'", "\\'")
        web.evaluateJavascript("(function(){var s=document.getElementById('vcm-style');if(!s){s=document.createElement('style');s.id='vcm-style';document.head.appendChild(s);}s.textContent='$escaped';})();", null)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    override fun onSaveInstanceState(outState: Bundle) { web.saveState(outState); super.onSaveInstanceState(outState) }
    @Deprecated("Deprecated in Android")
    override fun onBackPressed() { if (web.canGoBack()) web.goBack() else super.onBackPressed() }
}
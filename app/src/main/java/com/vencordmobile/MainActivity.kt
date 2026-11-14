package com.vencordmobile

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.*
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

class MainActivity : Activity() {
    private lateinit var web: WebView
    private lateinit var root: FrameLayout
    private lateinit var swipe: SwipeRefreshLayout
    private val prefs by lazy { getSharedPreferences("vcm_prefs", MODE_PRIVATE) }
    private val plugins = listOf("Message Logger (local UI)", "Translator button", "Typing indicator tools", "Custom profile decorations", "Better message spacing", "Quick reactions", "Hide nitro upsells", "Developer tools")
    private val pluginKeys = listOf("logger", "translator", "typing", "decor", "spacing", "quickreact", "upsells", "devtools")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = FrameLayout(this)
        web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.databaseEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.useWideViewPort = true
        web.settings.loadWithOverviewMode = true
        web.settings.mediaPlaybackRequiresUserGesture = false
        web.settings.setSupportZoom(true)
        web.settings.builtInZoomControls = true
        web.settings.displayZoomControls = false
        if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) WebSettingsCompat.setSafeBrowsingEnabled(web.settings, true)
        web.settings.userAgentString = web.settings.userAgentString.replace("; wv", "")
        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) { swipe.isRefreshing = false; applyCustomizations(); runPlugins() }
        }
        web.webChromeClient = WebChromeClient()
        swipe = SwipeRefreshLayout(this).apply {
            setOnRefreshListener { web.reload() }
            setColorSchemeResources(android.R.color.holo_blue_bright, android.R.color.holo_green_light, android.R.color.holo_orange_light)
            addView(web, SwipeRefreshLayout.LayoutParams(-1, -1))
        }
        root.addView(swipe, FrameLayout.LayoutParams(-1, -1))
        val menu = Button(this).apply {
            text = "V"
            textSize = 18f
            contentDescription = "Vencord Mobile settings"
            setOnClickListener { showHome() }
        }
        val lp = FrameLayout.LayoutParams(dp(54), dp(54), Gravity.BOTTOM or Gravity.END)
        lp.setMargins(dp(16), dp(16), dp(18), dp(24))
        root.addView(menu, lp)
        setContentView(root)
        if (savedInstanceState == null) web.loadUrl("https://discord.com/app") else web.restoreState(savedInstanceState)
    }

    private fun showHome() {
        val sections = arrayOf("Plugins", "Themes & appearance", "Custom CSS editor", "Rich Presence", "Chat & messages", "Privacy", "Backup / restore", "Reload Discord", "Open Discord in browser", "Clear WebView cache", "About")
        AlertDialog.Builder(this).setTitle("Vencord Mobile • Control Centre")
            .setItems(sections) { _, which ->
                when(which) {
                    0 -> PluginManager(this) { feature -> when (feature) { 0 -> showRPC(); 1 -> showThemes(); 2 -> editCss(); 3 -> showChatTools(); 4 -> showPrivacy(); 5 -> CustomPlugins(this) { runPlugins() }.open() } }.open()
                    1 -> showThemes()
                    2 -> editCss()
                    3 -> showRPC()
                    4 -> showChatTools()
                    5 -> showPrivacy()
                    6 -> backup()
                    7 -> web.reload()
                    8 -> startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://discord.com/app")))
                    9 -> AlertDialog.Builder(this).setTitle("Clear cache?").setMessage("Clear cached web files. You will remain logged in unless site data is cleared separately.").setPositiveButton("Clear") { _, _ -> web.clearCache(true); Toast.makeText(this,"Cache cleared",Toast.LENGTH_SHORT).show() }.setNegativeButton("Cancel",null).show()
                    10 -> AlertDialog.Builder(this).setTitle("About Vencord Mobile").setMessage("Experimental Android Discord WebView shell with local customization controls. This is not the official Vencord project; desktop plugins and account-side cosmetics cannot be installed or granted by this app.").setPositiveButton("OK", null).show()
                }
            }.show()
    }

    private fun showPlugins() {
        val labels = plugins.mapIndexed { i, p -> (if (prefs.getBoolean(pluginKeys[i], false)) "✓ " else "○ ") + p }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Plugin toggles • experimental")
            .setMultiChoiceItems(plugins.toTypedArray(), pluginKeys.map { prefs.getBoolean(it, false) }.toBooleanArray()) { d, i, checked ->
                prefs.edit().putBoolean(pluginKeys[i], checked).apply()
                (d as AlertDialog).listView.setItemChecked(i, checked)
            }.setPositiveButton("Apply") { _, _ -> applyCustomizations() }
            .setNeutralButton("Plugin sources") { _, _ ->
                AlertDialog.Builder(this).setTitle("Plugin sources")
                    .setMessage("This build has local feature switches only. Arbitrary third-party JavaScript plugins are not loaded for security and compatibility reasons. A safe marketplace needs signed/allowlisted plugins and a Discord-compatible injection layer.")
                    .setPositiveButton("OK", null).show()
            }.show()
    }

    private fun showThemes() {
        val themes = arrayOf("Default Discord", "AMOLED black", "Midnight purple", "Ocean blue", "Forest green", "Sunset orange")
        val current = prefs.getInt("theme", 0)
        AlertDialog.Builder(this).setTitle("Theme")
            .setSingleChoiceItems(themes, current) { d, which -> prefs.edit().putInt("theme", which).apply(); applyCustomizations(); d.dismiss() }
            .setPositiveButton("Done", null).show()
        val options = arrayOf("Compact messages", "Hide message avatars", "Rounded message cards", "Use larger text")
        AlertDialog.Builder(this).setTitle("Appearance options")
            .setMultiChoiceItems(options, booleanArrayOf(prefs.getBoolean("compact", false), prefs.getBoolean("noavatars", false), prefs.getBoolean("cards", false), prefs.getBoolean("bigtext", false))) { _, i, checked ->
                prefs.edit().putBoolean(arrayOf("compact","noavatars","cards","bigtext")[i], checked).apply(); applyCustomizations()
            }.setPositiveButton("Done", null).show()
    }

    private fun editCss() {
        val input = EditText(this).apply { minLines = 8; gravity = Gravity.TOP; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE; setText(prefs.getString("css", "")); hint = "/* Your CSS */\nbody { }" }
        AlertDialog.Builder(this).setTitle("Custom CSS (local)")
            .setView(input).setPositiveButton("Save") { _, _ -> prefs.edit().putString("css", input.text.toString()).apply(); applyCustomizations() }
            .setNeutralButton("Clear") { _, _ -> prefs.edit().remove("css").apply(); applyCustomizations() }.setNegativeButton("Cancel", null).show()
    }

    private fun showRPC() {
        val name = EditText(this).apply { setText(prefs.getString("rpc_name", "Playing a game")); hint = "Activity name" }
        val details = EditText(this).apply { setText(prefs.getString("rpc_details", "Custom activity")); hint = "Details" }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), 0); addView(name); addView(details) }
        AlertDialog.Builder(this).setTitle("Rich Presence • preview settings").setView(box)
            .setPositiveButton("Save") { _, _ -> prefs.edit().putString("rpc_name", name.text.toString()).putString("rpc_details", details.text.toString()).apply(); Toast.makeText(this, "Saved locally. Android WebView cannot publish Discord Rich Presence.", Toast.LENGTH_LONG).show() }
            .setNegativeButton("Cancel", null).show()
    }

    private fun showChatTools() {
        val opts = arrayOf("Enable quick-reaction helper UI", "Compact message spacing", "Hide Nitro upsell panels", "Enable local translation helper")
        val keys = arrayOf("quickreact","compact","upsells","translator")
        AlertDialog.Builder(this).setTitle("Chat & message tools").setMultiChoiceItems(opts, keys.map { prefs.getBoolean(it,false) }.toBooleanArray()) { _, i, checked -> prefs.edit().putBoolean(keys[i],checked).apply(); applyCustomizations() }.setPositiveButton("Done", null).show()
    }

    private fun showPrivacy() {
        val text = "This app is a WebView around Discord. These switches only affect local app behavior; they cannot override Discord server logging, account telemetry, or Discord's own policies."
        AlertDialog.Builder(this).setTitle("Privacy & safety").setMessage(text)
            .setPositiveButton("Understood", null).show()
    }

    private fun backup() {
        val all = prefs.all.toString()
        val edit = EditText(this).apply { setText(all); minLines = 5; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE }
        AlertDialog.Builder(this).setTitle("Settings snapshot").setMessage("Copy this local settings snapshot for backup:")
            .setView(edit).setPositiveButton("Copy") { _, _ -> val cm = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager; cm.setPrimaryClip(android.content.ClipData.newPlainText("Vencord Mobile settings", edit.text)); Toast.makeText(this,"Copied",Toast.LENGTH_SHORT).show() }.setNegativeButton("Close",null).show()
    }

    private fun runPlugins() {
        val scripts = prefs.getString("custom_plugins", "[]") ?: "[]"
        val arr = try { org.json.JSONArray(scripts) } catch (_: Exception) { org.json.JSONArray() }
        for (i in 0 until arr.length()) {
            val p = arr.optJSONObject(i) ?: continue
            if (p.optBoolean("enabled", false)) {
                val code = p.optString("code", "")
                web.evaluateJavascript("(function(){try{" + code + "}catch(e){console.error('VCM plugin error',e)}})();", null)
            }
        }
    }

    private fun applyCustomizations() {
        val theme = prefs.getInt("theme", 0)
        val bg = arrayOf("#36393f","#000000","#120d20","#071827","#07170f","#241007")[theme.coerceIn(0,5)]
        val compact = prefs.getBoolean("compact", false)
        val noavatars = prefs.getBoolean("noavatars", false)
        val cards = prefs.getBoolean("cards", false)
        val bigtext = prefs.getBoolean("bigtext", false)
        val custom = prefs.getString("css", "") ?: ""
        val css = "html,body,#app-mount{background:$bg!important;}" +
            (if(compact) "[class*=messageListItem]{padding-top:2px!important;padding-bottom:2px!important;} [class*=messageContent]{line-height:1.2!important;}" else "") +
            (if(noavatars) "[class*=avatar]{display:none!important;}" else "") +
            (if(cards) "[class*=messageListItem]{border-radius:10px!important;margin:3px!important;background:rgba(255,255,255,.025)!important;}" else "") +
            (if(bigtext) "[class*=messageContent]{font-size:16px!important;}" else "") + custom
        val escaped = css.replace("\\", "\\\\").replace("'", "\\'").replace("\n"," ")
        web.evaluateJavascript("(function(){var s=document.getElementById('vcm-style');if(!s){s=document.createElement('style');s.id='vcm-style';document.head.appendChild(s);}s.textContent='$escaped';})();", null)
    }

    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    override fun onSaveInstanceState(outState: Bundle) { web.saveState(outState); super.onSaveInstanceState(outState) }
    @Deprecated("Deprecated in Android")
    override fun onBackPressed() { if(web.canGoBack()) web.goBack() else super.onBackPressed() }
}
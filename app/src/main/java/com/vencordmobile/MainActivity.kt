package com.vencordmobile

import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.os.Bundle
import android.os.Environment
import android.text.InputType
import android.view.Gravity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.widget.*
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.webkit.CookieManager
import android.webkit.URLUtil
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

class MainActivity : Activity() {
    private lateinit var web: WebView
    private lateinit var root: FrameLayout
    private lateinit var swipe: SwipeRefreshLayout
    private var mobileUserAgent = ""
    private val prefs by lazy { getSharedPreferences("vcm_prefs", MODE_PRIVATE) }
    private val plugins = listOf("Message Logger (local UI)", "Translator button", "Typing indicator tools", "Custom profile decorations", "Better message spacing", "Quick reactions", "Hide nitro upsells", "Developer tools")
    private val pluginKeys = listOf("logger", "translator", "typing", "decor", "spacing", "quickreact", "upsells", "devtools")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = FrameLayout(this)
        web = WebView(this)
        mobileUserAgent = web.settings.userAgentString.replace("; wv", "")
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
        web.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            try {
                val filename = URLUtil.guessFileName(url, contentDisposition, mimeType)
                val request = DownloadManager.Request(android.net.Uri.parse(url)).apply {
                    setMimeType(mimeType)
                    addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url) ?: "")
                    addRequestHeader("User-Agent", userAgent)
                    setTitle(filename)
                    setDescription("Downloading from Discord")
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
                    setAllowedOverMetered(true)
                    setAllowedOverRoaming(false)
                }
                (getSystemService(DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
                Toast.makeText(this, "Download started: $filename", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Could not start download: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        swipe = SwipeRefreshLayout(this).apply {
            setOnRefreshListener { web.reload() }
            setColorSchemeResources(android.R.color.holo_blue_bright, android.R.color.holo_green_light, android.R.color.holo_orange_light)
            addView(web, FrameLayout.LayoutParams(-1, -1))
        }
        root.addView(swipe, FrameLayout.LayoutParams(-1, -1))
        val menu = Button(this).apply {
            text = "V"
            textSize = 21f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            contentDescription = "Vencord Mobile settings"
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                colors = intArrayOf(Color.rgb(137, 91, 255), Color.rgb(76, 53, 190))
                setStroke(dp(2), Color.argb(110, 255, 255, 255))
            }
            elevation = dp(9).toFloat()
            setOnClickListener { showHome() }
        }
        val lp = FrameLayout.LayoutParams(dp(54), dp(54), Gravity.BOTTOM or Gravity.END)
        lp.setMargins(dp(16), dp(16), dp(18), dp(24))
        root.addView(menu, lp)
        setContentView(root)
        if (savedInstanceState == null) web.loadUrl("https://discord.com/app") else web.restoreState(savedInstanceState)
    }

    private fun showHome() {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(16))
            background = GradientDrawable().apply { setColor(Color.rgb(31, 32, 37)); cornerRadius = dp(20).toFloat() }
        }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val logo = TextView(this).apply {
            text = "V"; textSize = 25f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; colors = intArrayOf(Color.rgb(149, 105, 255), Color.rgb(83, 57, 194)) }
        }
        header.addView(logo, LinearLayout.LayoutParams(dp(50), dp(50)))
        val titleBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), 0, 0, 0) }
        titleBox.addView(TextView(this).apply { text = "Vencord Mobile"; textSize = 20f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) })
        titleBox.addView(TextView(this).apply { text = "CONTROL CENTRE"; textSize = 10f; setTextColor(Color.rgb(171, 164, 192)) })
        header.addView(titleBox)
        panel.addView(header)
        panel.addView(TextView(this).apply { text = "Customise your Discord experience"; textSize = 13f; setTextColor(Color.LTGRAY); setPadding(0, dp(12), 0, dp(8)) })
        fun group(name: String) { panel.addView(TextView(this).apply { text = name.uppercase(); textSize = 10f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.rgb(174, 151, 255)); setPadding(0, dp(12), 0, dp(6)) }) }
        fun item(name: String, info: String, click: () -> Unit) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(11), dp(14), dp(11)); background = GradientDrawable().apply { setColor(Color.rgb(47, 49, 56)); cornerRadius = dp(13).toFloat() }; isClickable = true; isFocusable = true; setOnClickListener { click() } }
            row.addView(TextView(this).apply { text = name; textSize = 15f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) })
            row.addView(TextView(this).apply { text = info; textSize = 11f; setTextColor(Color.rgb(184, 186, 193)); setPadding(0, dp(3), 0, 0) })
            val lp = LinearLayout.LayoutParams(-1, -2); lp.bottomMargin = dp(7); panel.addView(row, lp)
        }
        group("Customise")
        item("Plugins & extensions", "Install, enable and manage your add-ons") { PluginManager(this) { feature -> when(feature) { 0 -> CustomPlugins(this) { runPlugins() }.open(); 1 -> showThemes(); 2 -> editCss(); 3 -> showChatTools(); 4 -> cosmeticLab(); 5 -> showRPC() } }.open() }
        item("Themes & appearance", "Colours, message spacing, avatars and layout") { showThemes() }
        item("Cosmetic lab", "Local badge and Nitro-style profile mock-ups") { cosmeticLab() }
        item("Custom CSS", "Style the Discord page yourself") { editCss() }
        group("Utilities")
        item("Rich Presence", "Configure a local activity preview") { showRPC() }
        item("Chat tools", "Quick reactions, hide upsells and compact chat") { showChatTools() }
        item("Backup settings", "Copy a backup of your preferences") { backup() }
        item("Browser tools", "Back, forward, find, desktop mode, reload and cache") {
            val opts = arrayOf("Back", "Forward", "Find on page", "Copy page URL", "Desktop / mobile mode", "Reload", "Clear cache")
            AlertDialog.Builder(this).setTitle("Browser tools").setItems(opts) { _, i -> when(i) { 0 -> if(web.canGoBack()) web.goBack(); 1 -> if(web.canGoForward()) web.goForward(); 2 -> findOnPage(); 3 -> { val cm=getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager; cm.setPrimaryClip(android.content.ClipData.newPlainText("Page URL",web.url ?: "")); Toast.makeText(this,"Copied",Toast.LENGTH_SHORT).show() }; 4 -> toggleDesktopMode(); 5 -> web.reload(); 6 -> AlertDialog.Builder(this).setMessage("Clear cached web files?").setPositiveButton("Clear") { _, _ -> web.clearCache(true) }.setNegativeButton("Cancel",null).show() } }.show()
        }
        item("Open Discord in browser", "Use your external browser") { startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://discord.com/app"))) }
        item("About & limitations", "Compatibility, privacy and feature details") { AlertDialog.Builder(this).setTitle("About Vencord Mobile").setMessage("Independent Android Discord WebView with local customisations. It cannot grant genuine Discord badges or Nitro. Discord mobile does not expose the desktop RPC IPC interface, so Rich Presence here is a local preview. Vencord, Revenge and Aliucord plugins use different runtimes and are not directly compatible.").setPositiveButton("OK",null).show() }
        val scroll = ScrollView(this).apply { addView(panel) }
        val dialog = AlertDialog.Builder(this).setView(scroll).create()
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.91).toInt(), (resources.displayMetrics.heightPixels * 0.86).toInt())
    }

    private fun cosmeticLab() {
        val panel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(16), dp(18), dp(16)); background = GradientDrawable().apply { setColor(Color.rgb(43,45,51)); cornerRadius = dp(16).toFloat() } }
        panel.addView(TextView(this).apply { text = "PROFILE COSMETIC PREVIEW"; textSize = 10f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.rgb(180,157,255)) })
        panel.addView(TextView(this).apply { text = "Discord user"; textSize = 22f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); setPadding(0,dp(10),0,dp(4)) })
        panel.addView(TextView(this).apply { text = "✦ Nitro  •  ✓ Verified  •  ★ Early Supporter"; textSize = 12f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.rgb(206,190,255)) })
        panel.addView(TextView(this).apply { text = "These are visual mock-ups inside this app only. They do not grant account entitlements or appear to other Discord users."; textSize = 12f; setTextColor(Color.LTGRAY); setPadding(0,dp(12),0,0) })
        AlertDialog.Builder(this).setTitle("Cosmetic lab").setView(panel).setPositiveButton("Close",null).show()
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
        val themes = arrayOf("Discord modern dark", "AMOLED black", "Midnight purple", "Ocean blue", "Forest green", "Sunset orange")
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
        val app = EditText(this).apply { setText(prefs.getString("rpc_name", "Playing a game")); hint = "Activity name" }
        val details = EditText(this).apply { setText(prefs.getString("rpc_details", "Custom activity")); hint = "Details" }
        val state = EditText(this).apply { setText(prefs.getString("rpc_state", "Available")); hint = "State" }
        val large = EditText(this).apply { setText(prefs.getString("rpc_large", "")); hint = "Large image key (optional)" }
        val small = EditText(this).apply { setText(prefs.getString("rpc_small", "")); hint = "Small image key (optional)" }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), 0); addView(app); addView(details); addView(state); addView(large); addView(small) }
        AlertDialog.Builder(this).setTitle("Rich Presence • activity builder").setMessage("Build and preview an activity configuration. Android Discord does not expose desktop RPC, so this cannot publish a live status by itself.").setView(box)
            .setPositiveButton("Save & preview") { _, _ ->
                prefs.edit().putString("rpc_name",app.text.toString()).putString("rpc_details",details.text.toString()).putString("rpc_state",state.text.toString()).putString("rpc_large",large.text.toString()).putString("rpc_small",small.text.toString()).apply()
                showRPCPreview()
            }
            .setNeutralButton("Copy JSON") { _, _ ->
                val json = org.json.JSONObject().put("name",app.text.toString()).put("details",details.text.toString()).put("state",state.text.toString()).put("largeImageKey",large.text.toString()).put("smallImageKey",small.text.toString()).toString(2)
                val cm = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                cm.setPrimaryClip(android.content.ClipData.newPlainText("Rich Presence config",json))
                Toast.makeText(this,"Activity JSON copied",Toast.LENGTH_SHORT).show()
            }.setNegativeButton("Cancel",null).show()
    }

    private fun showRPCPreview() {
        val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18),dp(16),dp(18),dp(16)); background = GradientDrawable().apply { setColor(Color.rgb(43,45,51)); cornerRadius = dp(14).toFloat() } }
        card.addView(TextView(this).apply { text = "PLAYING A GAME"; textSize = 10f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.rgb(177,160,255)) })
        card.addView(TextView(this).apply { text = prefs.getString("rpc_name","Playing a game"); textSize = 20f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); setPadding(0,dp(8),0,dp(4)) })
        card.addView(TextView(this).apply { text = prefs.getString("rpc_details",""); textSize = 13f; setTextColor(Color.LTGRAY) })
        card.addView(TextView(this).apply { text = prefs.getString("rpc_state",""); textSize = 12f; setTextColor(Color.LTGRAY); setPadding(0,dp(3),0,0) })
        AlertDialog.Builder(this).setTitle("Activity preview").setView(card).setPositiveButton("Edit") { _, _ -> showRPC() }.setNegativeButton("Close",null).show()
    }

    private fun showChatTools() {
        val options = arrayOf("Quick reaction palette", "Translate selected text", "Compact message spacing", "Hide Nitro promotional panels")
        AlertDialog.Builder(this).setTitle("Chat tools").setItems(options) { _, index ->
            when(index) {
                0 -> quickReactionPalette()
                1 -> translateSelection()
                2 -> { val enabled = !prefs.getBoolean("compact", false); prefs.edit().putBoolean("compact", enabled).apply(); applyCustomizations(); Toast.makeText(this, if(enabled) "Compact spacing on" else "Compact spacing off", Toast.LENGTH_SHORT).show() }
                3 -> { val enabled = !prefs.getBoolean("upsells", false); prefs.edit().putBoolean("upsells", enabled).apply(); applyCustomizations(); Toast.makeText(this, if(enabled) "Promotional panels hidden where supported" else "Promotional panels shown", Toast.LENGTH_SHORT).show() }
            }
        }.show()
    }

    private fun quickReactionPalette() {
        val emojis = arrayOf("👍", "❤️", "😂", "😭", "🔥", "🎉", "👀", "🤝", "💀", "✨", "🙏", "🚀")
        AlertDialog.Builder(this).setTitle("Quick reactions • tap to copy")
            .setItems(emojis) { _, which ->
                val cm = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                cm.setPrimaryClip(android.content.ClipData.newPlainText("Reaction", emojis[which]))
                Toast.makeText(this, "${emojis[which]} copied", Toast.LENGTH_SHORT).show()
            }.show()
    }

    private fun translateSelection() {
        web.evaluateJavascript("encodeURIComponent(window.getSelection().toString())") { result ->
            try {
                val encoded = org.json.JSONTokener(result).nextValue() as? String ?: ""
                if (encoded.isBlank()) { Toast.makeText(this, "Select some text in Discord first", Toast.LENGTH_SHORT).show(); return@evaluateJavascript }
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://translate.google.com/?sl=auto&tl=en&text=$encoded&op=translate"))
                startActivity(intent)
            } catch (_: Exception) { Toast.makeText(this, "Could not read selected text", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun showPrivacy() {
        val text = "This app is a WebView around Discord. These switches only affect local app behavior; they cannot override Discord server logging, account telemetry, or Discord's own policies."
        AlertDialog.Builder(this).setTitle("Privacy & safety").setMessage(text)
            .setPositiveButton("Understood", null).show()
    }

    private fun backup() {
        val snapshot = org.json.JSONObject()
        val values = org.json.JSONObject()
        for ((key, value) in prefs.all) {
            val type = when(value) { is Boolean -> "boolean"; is Int -> "int"; is Long -> "long"; is Float -> "float"; else -> "string" }
            values.put(key, org.json.JSONObject().put("type",type).put("value",value.toString()))
        }
        snapshot.put("format","vencord-mobile-settings").put("version",1).put("preferences",values)
        val editor = EditText(this).apply { setText(snapshot.toString(2)); minLines=8; gravity=Gravity.TOP; inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE }
        AlertDialog.Builder(this).setTitle("Backup & restore")
            .setMessage("Copy this JSON to back up your settings, or paste a previous Vencord Mobile backup and choose Restore.")
            .setView(editor)
            .setPositiveButton("Copy backup") { _, _ ->
                val cm=getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                cm.setPrimaryClip(android.content.ClipData.newPlainText("Vencord Mobile backup",editor.text.toString()))
                Toast.makeText(this,"Backup copied",Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Restore") { _, _ ->
                try {
                    val root=org.json.JSONObject(editor.text.toString())
                    require(root.optString("format")=="vencord-mobile-settings") { "This is not a Vencord Mobile backup" }
                    val values=root.getJSONObject("preferences")
                    val out=prefs.edit()
                    val keys=values.keys()
                    while(keys.hasNext()) {
                        val key=keys.next(); val item=values.getJSONObject(key); val value=item.optString("value");
                        when(item.optString("type")) {
                            "boolean" -> out.putBoolean(key,value.toBoolean())
                            "int" -> out.putInt(key,value.toInt())
                            "long" -> out.putLong(key,value.toLong())
                            "float" -> out.putFloat(key,value.toFloat())
                            else -> out.putString(key,value)
                        }
                    }
                    out.apply(); applyCustomizations(); Toast.makeText(this,"Settings restored",Toast.LENGTH_LONG).show()
                } catch(e:Exception) { AlertDialog.Builder(this).setTitle("Restore failed").setMessage(e.message ?: "Invalid backup").setPositiveButton("OK",null).show() }
            }.setNegativeButton("Close",null).show()
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
        val bg = arrayOf("#313338","#000000","#120d20","#071827","#07170f","#241007")[theme.coerceIn(0,5)]
        val compact = prefs.getBoolean("compact", false)
        val noavatars = prefs.getBoolean("noavatars", false)
        val cards = prefs.getBoolean("cards", false)
        val bigtext = prefs.getBoolean("bigtext", false)
        val custom = prefs.getString("css", "") ?: ""
        val hideUpsells = prefs.getBoolean("upsells", false)
        val css = "html,body,#app-mount{background:$bg!important;}" +
            (if(compact) "[class*=messageListItem]{padding-top:2px!important;padding-bottom:2px!important;} [class*=messageContent]{line-height:1.2!important;}" else "") +
            (if(noavatars) "[class*=avatar]{display:none!important;}" else "") +
            (if(cards) "[class*=messageListItem]{border-radius:10px!important;margin:3px!important;background:rgba(255,255,255,.025)!important;}" else "") +
            (if(bigtext) "[class*=messageContent]{font-size:16px!important;}" else "") +
            (if(hideUpsells) "[class*=premiumPromo],[class*=nitroTopDivider],[class*=upsell],[class*=premiumUpsell]{display:none!important;}" else "") + custom
        val escaped = css.replace("\\", "\\\\").replace("'", "\\'").replace("\n"," ")
        web.evaluateJavascript("(function(){var s=document.getElementById('vcm-style');if(!s){s=document.createElement('style');s.id='vcm-style';document.head.appendChild(s);}s.textContent='$escaped';})();", null)
    }

    private fun findOnPage() {
        val query = EditText(this).apply { hint = "Search visible page text" }
        AlertDialog.Builder(this).setTitle("Find on page").setView(query)
            .setPositiveButton("Find") { _, _ ->
                val term = query.text.toString()
                if (term.isNotBlank()) {
                    web.findAllAsync(term)
                    web.setFindListener { active, total, _ ->
                        Toast.makeText(this, if (total == 0) "No matches" else "$active of $total matches", Toast.LENGTH_SHORT).show()
                    }
                }
            }.setNeutralButton("Next") { _, _ -> web.findNext(true) }
            .setNegativeButton("Close") { _, _ -> web.clearMatches() }.show()
    }

    private fun toggleDesktopMode() {
        val enabled = !prefs.getBoolean("desktop_mode", false)
        prefs.edit().putBoolean("desktop_mode", enabled).apply()
        web.settings.userAgentString = if (enabled) {
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
        } else {
            mobileUserAgent
        }
        web.settings.useWideViewPort = enabled
        web.settings.loadWithOverviewMode = enabled
        Toast.makeText(this, if (enabled) "Desktop mode enabled" else "Mobile mode enabled", Toast.LENGTH_SHORT).show()
        web.reload()
    }

    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    override fun onSaveInstanceState(outState: Bundle) { web.saveState(outState); super.onSaveInstanceState(outState) }
    @Deprecated("Deprecated in Android")
    override fun onBackPressed() { if(web.canGoBack()) web.goBack() else super.onBackPressed() }
}
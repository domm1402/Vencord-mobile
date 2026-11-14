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
import androidx.webkit.WebViewCompat
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
        installVencord()
        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) { swipe.isRefreshing = false; applyCustomizations(); applyProfileEnhancements(); runPlugins() }
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

    private fun installVencord() {
        try {
            val bundle = assets.open("vencord.user.js").bufferedReader().use { it.readText() }
            val bridge = """
                (function(){
                  if (window.__vcmVencordBridge) return;
                  window.__vcmVencordBridge = true;
                  window.unsafeWindow = window;
                  window.GM_xmlhttpRequest = function(options) {
                    var xhr = new XMLHttpRequest();
                    xhr.open(options.method || 'GET', options.url, true);
                    xhr.responseType = 'blob';
                    if (options.timeout) xhr.timeout = options.timeout;
                    if (options.headers) Object.keys(options.headers).forEach(function(k){ xhr.setRequestHeader(k, options.headers[k]); });
                    xhr.onload = function(){ if(options.onload) options.onload({response:xhr.response,status:xhr.status,statusText:xhr.statusText,responseHeaders:xhr.getAllResponseHeaders(),finalUrl:xhr.responseURL}); };
                    xhr.onerror = function(){ if(options.onerror) options.onerror({status:xhr.status}); };
                    xhr.ontimeout = function(){ if(options.ontimeout) options.ontimeout(); };
                    xhr.onabort = function(){ if(options.onabort) options.onabort(); };
                    xhr.send(options.data || null);
                    return {abort:function(){xhr.abort();}};
                  };
                })();
            """.trimIndent()
            val source = bridge + "\n" + bundle
            if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                WebViewCompat.addDocumentStartJavaScript(web, source, setOf("https://discord.com", "https://*.discord.com"))
            } else {
                Toast.makeText(this, "This Android System WebView is too old for Vencord injection. Update Android System WebView.", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Vencord bundle is missing. Build again with the Vencord bundle step. ${'$'}{e.message}", Toast.LENGTH_LONG).show()
        }
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
        item("My profile cosmetics", "Show your local badges and activity on your own profile panel") { showProfileStudio() }
        item("Cosmetic lab", "Local badge and Nitro-style profile mock-ups") { cosmeticLab() }
        item("Custom CSS", "Style the Discord page yourself") { editCss() }
        group("Utilities")
        item("Rich Presence", "Configure Discord Rich Presence") { showRPC() }
        item("Chat tools", "Quick reactions, hide upsells and compact chat") { showChatTools() }
        item("Backup settings", "Copy a backup of your preferences") { backup() }
        item("Browser tools", "Back, forward, find, desktop mode, reload and cache") {
            val opts = arrayOf("Back", "Forward", "Find on page", "Copy page URL", "Desktop / mobile mode", "Reload", "Clear cache")
            AlertDialog.Builder(this).setTitle("Browser tools").setItems(opts) { _, i -> when(i) { 0 -> if(web.canGoBack()) web.goBack(); 1 -> if(web.canGoForward()) web.goForward(); 2 -> findOnPage(); 3 -> { val cm=getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager; cm.setPrimaryClip(android.content.ClipData.newPlainText("Page URL",web.url ?: "")); Toast.makeText(this,"Copied",Toast.LENGTH_SHORT).show() }; 4 -> toggleDesktopMode(); 5 -> web.reload(); 6 -> AlertDialog.Builder(this).setMessage("Clear cached web files?").setPositiveButton("Clear") { _, _ -> web.clearCache(true) }.setNegativeButton("Cancel",null).show() } }.show()
        }
        item("Open Discord in browser", "Use your external browser") { startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://discord.com/app"))) }
        item("About & limitations", "Compatibility, privacy and feature details") { AlertDialog.Builder(this).setTitle("About Vencord Mobile").setMessage("Independent Android Discord WebView with the official Vencord browser userscript bundled at build time. Vencord plugins that depend on desktop-only native APIs may not work. Genuine Nitro and account badges cannot be granted by a client mod. Live Android Rich Presence is supported by Discord Social SDK 1.10+ and needs a registered Application ID and SDK package.").setPositiveButton("OK",null).show() }
        val scroll = ScrollView(this).apply { addView(panel) }
        val dialog = AlertDialog.Builder(this).setView(scroll).create()
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.91).toInt(), (resources.displayMetrics.heightPixels * 0.86).toInt())
    }

    private fun showProfileStudio() {
        val badges = EditText(this).apply {
            hint = "Nitro, Verified, Early Supporter"
            setText(prefs.getString("profile_badges", "Nitro, Early Supporter"))
        }
        val activityName = EditText(this).apply { hint = "Activity name"; setText(prefs.getString("rpc_name", "Playing a game")) }
        val activityDetails = EditText(this).apply { hint = "Activity details"; setText(prefs.getString("rpc_details", "Custom activity")) }
        val activityState = EditText(this).apply { hint = "Activity state"; setText(prefs.getString("rpc_state", "Available")) }
        val enabled = CheckBox(this).apply { text = "Show local cosmetics and activity on my own profile panel"; isChecked = prefs.getBoolean("profile_overlay", false) }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), 0); addView(TextView(this@MainActivity).apply { text = "Badge labels (comma separated)"; setTextColor(Color.LTGRAY) }); addView(badges); addView(TextView(this@MainActivity).apply { text = "Local activity display"; setTextColor(Color.LTGRAY); setPadding(0,dp(8),0,0) }); addView(activityName); addView(activityDetails); addView(activityState); addView(enabled) }
        AlertDialog.Builder(this).setTitle("My profile • local cosmetics")
            .setMessage("These decorations are injected only into your own view of Discord. They are not real Discord badges, Nitro or published Rich Presence.")
            .setView(box)
            .setPositiveButton("Save & apply") { _, _ ->
                prefs.edit().putString("profile_badges", badges.text.toString()).putString("rpc_name",activityName.text.toString()).putString("rpc_details",activityDetails.text.toString()).putString("rpc_state",activityState.text.toString()).putBoolean("profile_overlay",enabled.isChecked).apply()
                applyProfileEnhancements()
                Toast.makeText(this,"Your local profile decorations were saved",Toast.LENGTH_SHORT).show()
            }.setNeutralButton("Preview") { _, _ -> applyProfileEnhancements(); showRPCPreview() }.setNegativeButton("Close",null).show()
    }

    private fun applyProfileEnhancements() {
        // The previous experimental overlay injected fake badges/activity into
        // Discord's profile DOM and could land in the wrong container. Remove it.
        if (!::web.isInitialized) return
        web.evaluateJavascript("""
            (function() {
                document.querySelectorAll('#vcm-profile-local, #vcm-profile-local-css').forEach(function(node) {
                    node.remove();
                });
            })();
        """.trimIndent(), null)
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
        val themes = arrayOf("Discord blurple dark", "AMOLED black", "Midnight purple", "Ocean blue", "Forest green", "Sunset orange", "Discord light")
        val current = prefs.getInt("theme", 0).coerceIn(0, themes.lastIndex)
        AlertDialog.Builder(this).setTitle("Colour theme")
            .setSingleChoiceItems(themes, current) { d, which -> prefs.edit().putInt("theme", which).apply(); applyCustomizations(); d.dismiss() }
            .setNegativeButton("Close", null).show()
        val options = arrayOf("Compact messages", "Hide message avatars", "Rounded message cards", "Use larger text", "Hide Nitro promotional panels", "Compact channel list", "Hide member list", "Hide profile activity panels", "Hide message embeds", "Hide attachment previews", "Reduce animations", "Hide sticker and GIF buttons")
        val keys = arrayOf("compact","noavatars","cards","bigtext","upsells","compact_channels","hide_members","hide_activity","hide_embeds","hide_attachments","reduce_motion","hide_sticker_buttons")
        AlertDialog.Builder(this).setTitle("Interface options")
            .setMultiChoiceItems(options, keys.map { prefs.getBoolean(it, false) }.toBooleanArray()) { _, i, checked -> prefs.edit().putBoolean(keys[i], checked).apply(); applyCustomizations() }
            .setPositiveButton("Done", null).show()
    }
    private fun editCss() {
        val input = EditText(this).apply { minLines = 8; gravity = Gravity.TOP; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE; setText(prefs.getString("css", "")); hint = "/* Your CSS */\nbody { }" }
        AlertDialog.Builder(this).setTitle("Custom CSS (local)")
            .setView(input).setPositiveButton("Save") { _, _ -> prefs.edit().putString("css", input.text.toString()).apply(); applyCustomizations() }
            .setNeutralButton("Clear") { _, _ -> prefs.edit().remove("css").apply(); applyCustomizations() }.setNegativeButton("Cancel", null).show()
    }

    private fun showRPC() {
        val fields = linkedMapOf<String, EditText>()
        lateinit var form: LinearLayout
        fun field(key: String, label: String, hint: String = "", type: Int = InputType.TYPE_CLASS_TEXT): EditText {
            val input = EditText(this).apply {
                setText(prefs.getString("rpc_$key", ""))
                this.hint = hint.ifBlank { label }
                inputType = type
                textSize = 15f
                setTextColor(Color.WHITE)
                setHintTextColor(Color.LTGRAY)
                setPadding(dp(12), dp(9), dp(12), dp(9))
                background = GradientDrawable().apply {
                    setColor(Color.rgb(35, 36, 42))
                    cornerRadius = dp(7).toFloat()
                    setStroke(dp(1), Color.rgb(65, 67, 76))
                }
            }
            fields[key] = input
            val labelView = TextView(this).apply {
                text = label.uppercase()
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.LTGRAY)
                setPadding(0, dp(10), 0, dp(4))
            }
            form.addView(labelView)
            form.addView(input, LinearLayout.LayoutParams(-1, dp(48)))
            return input
        }
        form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(10), dp(16), dp(18))
            background = GradientDrawable().apply {
                setColor(Color.rgb(43, 45, 51))
                cornerRadius = dp(12).toFloat()
            }
        }
        fun section(title: String) {
            form.addView(TextView(this).apply {
                text = title.uppercase()
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.rgb(177, 160, 255))
                setPadding(0, dp(18), 0, dp(4))
            })
        }
        section("Basic")
        field("name", "Application name", "GTA VI")
        field("application_id", "Application ID", "Discord application ID", InputType.TYPE_CLASS_NUMBER)
        val typeLabel = TextView(this).apply {
            text = "ACTIVITY TYPE"
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.LTGRAY)
            setPadding(0, dp(10), 0, dp(4))
        }
        form.addView(typeLabel)
        val types = arrayOf("Playing", "Streaming", "Listening", "Watching", "Custom", "Competing")
        val typeSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, types)
            setSelection(prefs.getInt("rpc_type", 0).coerceIn(0, types.lastIndex))
        }
        form.addView(typeSpinner, LinearLayout.LayoutParams(-1, dp(48)))
        field("state", "State", "Playing Solo")
        field("details", "Details", "Competitive")
        section("Timestamps")
        val timestamps = CheckBox(this).apply {
            text = "Enable timestamps"
            setTextColor(Color.WHITE)
            isChecked = prefs.getBoolean("rpc_timestamps_enabled", false)
        }
        form.addView(timestamps)
        field("start", "Start timestamp (milliseconds)", "e.g. 1731600000000", InputType.TYPE_CLASS_NUMBER)
        field("end", "End timestamp (milliseconds)", "Optional", InputType.TYPE_CLASS_NUMBER)
        section("Images")
        field("large", "Large image key or URL", "asset key or image URL")
        field("large_text", "Large image text", "Shown when hovering")
        field("small", "Small image key or URL", "Optional")
        field("small_text", "Small image text", "Shown when hovering")
        section("Party")
        field("party_id", "Party ID", "Optional party identifier")
        field("party_size", "Party size", "1", InputType.TYPE_CLASS_NUMBER)
        field("party_max", "Party max", "5", InputType.TYPE_CLASS_NUMBER)
        field("join_secret", "Join secret", "Optional")
        section("Buttons")
        field("button1_label", "Button 1 label", "Ask to Join")
        field("button1_url", "Button 1 URL", "https://example.com")
        field("button2_label", "Button 2 label", "Optional")
        field("button2_url", "Button 2 URL", "https://example.com")
        val scroll = ScrollView(this).apply { isFillViewport = true; addView(form) }
        AlertDialog.Builder(this)
            .setTitle("Rich Presence Builder")
            .setView(scroll)
            .setPositiveButton("Save & preview") { _, _ ->
                val editor = prefs.edit()
                fields.forEach { (key, input) -> editor.putString("rpc_$key", input.text.toString()) }
                editor.putInt("rpc_type", typeSpinner.selectedItemPosition)
                    .putBoolean("rpc_timestamps_enabled", timestamps.isChecked)
                    .apply()
                showRPCPreview()
            }
            .setNeutralButton("Copy JSON") { _, _ ->
                val json = org.json.JSONObject()
                    .put("name", fields["name"]?.text.toString())
                    .put("application_id", fields["application_id"]?.text.toString())
                    .put("type", typeSpinner.selectedItemPosition)
                    .put("state", fields["state"]?.text.toString())
                    .put("details", fields["details"]?.text.toString())
                    .put("timestamps", org.json.JSONObject()
                        .put("_enabled", timestamps.isChecked)
                        .put("start", fields["start"]?.text.toString().toLongOrNull() ?: 0L)
                        .put("end", fields["end"]?.text.toString().toLongOrNull() ?: 0L))
                    .put("assets", org.json.JSONObject()
                        .put("large_image", fields["large"]?.text.toString())
                        .put("large_text", fields["large_text"]?.text.toString())
                        .put("small_image", fields["small"]?.text.toString())
                        .put("small_text", fields["small_text"]?.text.toString()))
                    .put("party", org.json.JSONObject()
                        .put("id", fields["party_id"]?.text.toString())
                        .put("size", fields["party_size"]?.text.toString().toIntOrNull() ?: 1)
                        .put("max", fields["party_max"]?.text.toString().toIntOrNull() ?: 5))
                    .put("join_secret", fields["join_secret"]?.text.toString())
                    .put("buttons", org.json.JSONArray().put(
                        org.json.JSONObject().put("label", fields["button1_label"]?.text.toString()).put("url", fields["button1_url"]?.text.toString())
                    ).put(
                        org.json.JSONObject().put("label", fields["button2_label"]?.text.toString()).put("url", fields["button2_url"]?.text.toString())
                    )).toString(2)
                val cm = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                cm.setPrimaryClip(android.content.ClipData.newPlainText("Rich Presence JSON", json))
                Toast.makeText(this, "Rich Presence JSON copied", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showRPCPreview() {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = GradientDrawable().apply {
                setColor(Color.rgb(88, 101, 242))
                cornerRadius = dp(12).toFloat()
            }
        }
        card.addView(TextView(this).apply {
            text = "PLAYING A GAME"
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        })
        card.addView(TextView(this).apply {
            text = prefs.getString("rpc_name", "My new game")
            textSize = 21f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(0, dp(8), 0, dp(8))
        })
        card.addView(TextView(this).apply {
            val details = prefs.getString("rpc_details", "").orEmpty()
            val state = prefs.getString("rpc_state", "").orEmpty()
            text = listOf(details, state).filter { it.isNotBlank() }.joinToString("\n")
            textSize = 13f
            setTextColor(Color.WHITE)
        })
        val metadata = listOf(
            "Large image: " + prefs.getString("rpc_large", "").orEmpty(),
            "Small image: " + prefs.getString("rpc_small", "").orEmpty(),
            "Party: " + prefs.getString("rpc_party_size", "1") + " / " + prefs.getString("rpc_party_max", "5")
        ).filterNot { it.endsWith(": ") }
        card.addView(TextView(this).apply {
            text = metadata.joinToString("\n")
            textSize = 11f
            setTextColor(Color.rgb(230, 232, 255))
            setPadding(0, dp(12), 0, 0)
        })
        AlertDialog.Builder(this)
            .setTitle("Full Profile • Preview")
            .setView(card)
            .setPositiveButton("Edit") { _, _ -> showRPC() }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showChatTools() {
        val options = arrayOf("Quick reaction palette", "Translate selected text", "Compact message spacing", "Hide Nitro promotional panels", "Format a message", "Create Discord timestamp", "Markdown reference", "Copy selected text")
        AlertDialog.Builder(this).setTitle("Chat tools").setItems(options) { _, index ->
            when(index) {
                0 -> quickReactionPalette()
                1 -> translateSelection()
                2 -> { val enabled = !prefs.getBoolean("compact", false); prefs.edit().putBoolean("compact", enabled).apply(); applyCustomizations(); Toast.makeText(this, if(enabled) "Compact spacing on" else "Compact spacing off", Toast.LENGTH_SHORT).show() }
                3 -> { val enabled = !prefs.getBoolean("upsells", false); prefs.edit().putBoolean("upsells", enabled).apply(); applyCustomizations(); Toast.makeText(this, if(enabled) "Promotional panels hidden where supported" else "Promotional panels shown", Toast.LENGTH_SHORT).show() }
                4 -> formatDiscordMessage()
                5 -> createDiscordTimestamp()
                6 -> showMarkdownReference()
                7 -> copySelection()
            }
        }.show()
    }

    private fun copySelection() {
        web.evaluateJavascript("window.getSelection().toString()") { result ->
            try {
                val selected = org.json.JSONTokener(result).nextValue() as? String ?: ""
                if(selected.isBlank()) { Toast.makeText(this,"Select text in Discord first",Toast.LENGTH_SHORT).show(); return@evaluateJavascript }
                val cm = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                cm.setPrimaryClip(android.content.ClipData.newPlainText("Discord selection",selected))
                Toast.makeText(this,"Selected text copied",Toast.LENGTH_SHORT).show()
            } catch(_:Exception) { Toast.makeText(this,"Could not copy selection",Toast.LENGTH_SHORT).show() }
        }
    }

    private fun formatDiscordMessage() {
        val input = EditText(this).apply { hint = "Write or paste your message"; minLines = 4; gravity = Gravity.TOP; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE }
        val formats = arrayOf("Bold", "Italic", "Underline", "Strikethrough", "Spoiler", "Inline code", "Code block", "Quote")
        val picker = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, formats) }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20),dp(8),dp(20),0); addView(input); addView(picker) }
        AlertDialog.Builder(this).setTitle("Message formatter").setView(box)
            .setPositiveButton("Copy formatted text") { _, _ ->
                val value = input.text.toString()
                if(value.isBlank()) { Toast.makeText(this,"Enter some text first",Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                val formatted = when(picker.selectedItemPosition) { 0 -> "**$value**"; 1 -> "*$value*"; 2 -> "__${value}__"; 3 -> "~~$value~~"; 4 -> "||$value||"; 5 -> "`$value`"; 6 -> "```\n$value\n```"; else -> value.lines().joinToString("\n") { "> $it" } }
                val cm = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                cm.setPrimaryClip(android.content.ClipData.newPlainText("Formatted Discord message",formatted))
                Toast.makeText(this,"Formatted text copied",Toast.LENGTH_SHORT).show()
            }.setNegativeButton("Cancel",null).show()
    }

    private fun createDiscordTimestamp() {
        val calendar = java.util.Calendar.getInstance()
        android.app.DatePickerDialog(this,{ _, year, month, day ->
            calendar.set(year,month,day)
            android.app.TimePickerDialog(this,{ _, hour, minute ->
                calendar.set(java.util.Calendar.HOUR_OF_DAY,hour); calendar.set(java.util.Calendar.MINUTE,minute); calendar.set(java.util.Calendar.SECOND,0)
                val epoch = calendar.timeInMillis/1000
                val formats = arrayOf("Default: <t:$epoch>", "Short time: <t:$epoch:t>", "Long time: <t:$epoch:T>", "Short date: <t:$epoch:d>", "Long date: <t:$epoch:D>", "Date and time: <t:$epoch:f>", "Full date: <t:$epoch:F>", "Relative: <t:$epoch:R>")
                AlertDialog.Builder(this).setTitle("Copy Discord timestamp").setItems(formats) { _, i ->
                    val cm=getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    cm.setPrimaryClip(android.content.ClipData.newPlainText("Discord timestamp",formats[i].substringAfter(": ")))
                    Toast.makeText(this,"Timestamp copied",Toast.LENGTH_SHORT).show()
                }.show()
            },calendar.get(java.util.Calendar.HOUR_OF_DAY),calendar.get(java.util.Calendar.MINUTE),true).show()
        },calendar.get(java.util.Calendar.YEAR),calendar.get(java.util.Calendar.MONTH),calendar.get(java.util.Calendar.DAY_OF_MONTH)).show()
    }

    private fun showMarkdownReference() {
        AlertDialog.Builder(this).setTitle("Discord Markdown")
            .setMessage("**bold**\n*italic*\n__underline__\n~~strikethrough~~\n||spoiler||\n`inline code`\n```\ncode block\n```\n> quote\n-# subtext\n[link text](https://example.com)\n<t:UNIX:F> timestamp")
            .setPositiveButton("Close",null).show()
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
        val theme = prefs.getInt("theme", 0).coerceIn(0, 6)
        val palettes = arrayOf(
            arrayOf("#313338","#2b2d31","#1e1f22","#5865f2","#f2f3f5","#b5bac1"),
            arrayOf("#000000","#080808","#101010","#8b5cf6","#f5f5f5","#a1a1aa"),
            arrayOf("#171321","#211a30","#100d18","#a78bfa","#f5efff","#c4b5fd"),
            arrayOf("#10202d","#162a3a","#0b1722","#38bdf8","#e5f4ff","#a8c9df"),
            arrayOf("#12231b","#1a3025","#0b1710","#34d399","#e8fff0","#a9d7b8"),
            arrayOf("#2a1b14","#38251b","#1d120d","#fb923c","#fff2e8","#e7b99b"),
            arrayOf("#ffffff","#f2f3f5","#e3e5e8","#5865f2","#232428","#5c5e66")
        )
        val p = palettes[theme]
        val rules = StringBuilder()
        rules.append(":root{color-scheme:" + (if(theme == 6) "light" else "dark") + ";}:root,body,#app-mount,.theme-dark,.theme-light{--background-primary:"+p[0]+"!important;--background-secondary:"+p[1]+"!important;--background-secondary-alt:"+p[1]+"!important;--background-tertiary:"+p[2]+"!important;--background-floating:"+p[1]+"!important;--channeltextarea-background:"+p[2]+"!important;--modal-background:"+p[1]+"!important;--header-primary:"+p[4]+"!important;--text-normal:"+p[4]+"!important;--header-secondary:"+p[5]+"!important;--text-muted:"+p[5]+"!important;--interactive-normal:"+p[5]+"!important;--brand-experiment:"+p[3]+"!important;--brand-experiment-560:"+p[3]+"!important;--button-secondary-background:"+p[2]+"!important;--scrollbar-thin-thumb:"+p[5]+"!important;}")
        rules.append("html,body,#app-mount{background:"+p[0]+"!important;color:"+p[4]+"!important;}")
        if(prefs.getBoolean("compact",false)) rules.append("[class*=messageListItem]{padding-top:2px!important;padding-bottom:2px!important}[class*=messageContent]{line-height:1.25!important}")
        if(prefs.getBoolean("noavatars",false)) rules.append("[class*=messageListItem] [class*=avatar]{display:none!important}")
        if(prefs.getBoolean("cards",false)) rules.append("[class*=messageListItem]{border-radius:10px!important;margin:3px!important;background:rgba(255,255,255,.035)!important}")
        if(prefs.getBoolean("bigtext",false)) rules.append("[class*=messageContent]{font-size:16px!important;line-height:1.45!important}")
        if(prefs.getBoolean("upsells",false)) rules.append("[class*=premiumPromo],[class*=nitroTopDivider],[class*=upsell],[class*=premiumUpsell]{display:none!important}")
        if(prefs.getBoolean("compact_channels",false)) rules.append("[class*=sidebar] [class*=containerDefault]{padding-top:0!important;padding-bottom:0!important}[class*=sidebar] [class*=name]{font-size:12px!important}")
        if(prefs.getBoolean("hide_members",false)) rules.append("[class*=membersWrap]{display:none!important}")
        if(prefs.getBoolean("hide_activity",false)) rules.append("[class*=activityPanel],[class*=nowPlayingColumn]{display:none!important}")
        if(prefs.getBoolean("hide_embeds",false)) rules.append("[class*=embedWrapper],[class*=embedFull]{display:none!important}")
        if(prefs.getBoolean("hide_attachments",false)) rules.append("[class*=attachmentContentContainer],[class*=imageContent],[class*=videoContainer]{display:none!important}")
        if(prefs.getBoolean("reduce_motion",false)) rules.append("*,*::before,*::after{animation-duration:.001ms!important;animation-iteration-count:1!important;transition-duration:.001ms!important;scroll-behavior:auto!important}")
        if(prefs.getBoolean("hide_sticker_buttons",false)) rules.append("button[aria-label*=GIF],button[aria-label*=sticker],[class*=stickerButton],[class*=gifButton]{display:none!important}")
        rules.append(prefs.getString("css", "") ?: "")
        val escaped = rules.toString().replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ")
        web.evaluateJavascript("(function(){var s=document.getElementById('vcm-style');if(!s){s=document.createElement('style');s.id='vcm-style';document.head.appendChild(s);}s.textContent='"+escaped+"';})();", null)
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
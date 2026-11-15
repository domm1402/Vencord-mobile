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
    private val fileChooserRequestCode = 4107
    private var pendingFileChooser: android.webkit.ValueCallback<Array<android.net.Uri>>? = null
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
        // Discord uploads use content:// URIs; local file:// access is not needed.
        web.settings.allowFileAccess = false
        web.settings.allowContentAccess = true
        web.settings.javaScriptCanOpenWindowsAutomatically = false
        web.settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
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
            override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                val uri = request?.url ?: return false
                val scheme = uri.scheme?.lowercase(java.util.Locale.ROOT)
                if (scheme != "https" && scheme != "http") return true
                val host = uri.host?.lowercase(java.util.Locale.ROOT) ?: return true
                val discordHost = host == "discord.com" || host.endsWith(".discord.com") ||
                    host == "discordapp.com" || host.endsWith(".discordapp.com") ||
                    host == "discordapp.net" || host.endsWith(".discordapp.net") ||
                    host == "discord.gg" || host == "discord.media" ||
                    host == "discord.gift" || host == "discord.new"
                if (scheme == "https" && discordHost) return false
                // Keep third-party pages out of the signed-in Discord WebView.
                return try {
                    startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri))
                    true
                } catch (_: Exception) {
                    Toast.makeText(this@MainActivity, "No browser is available for this link", Toast.LENGTH_SHORT).show()
                    true
                }
            }
            override fun onReceivedError(
                view: WebView?,
                request: android.webkit.WebResourceRequest?,
                error: android.webkit.WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    swipe.isRefreshing = false
                    Toast.makeText(this@MainActivity, "Discord couldn't load. Check your connection and pull down to retry.", Toast.LENGTH_LONG).show()
                }
            }
            override fun onReceivedHttpError(
                view: WebView?,
                request: android.webkit.WebResourceRequest?,
                errorResponse: android.webkit.WebResourceResponse?
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                val status = errorResponse?.statusCode ?: return
                if (request?.isForMainFrame == true && status >= 500) {
                    swipe.isRefreshing = false
                    Toast.makeText(this@MainActivity, "Discord returned a server error ($status). Try again shortly.", Toast.LENGTH_LONG).show()
                }
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                swipe.isRefreshing = false
                applyCustomizations()
                applyProfileEnhancements()
                runPlugins()
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                view: WebView?,
                filePathCallback: android.webkit.ValueCallback<Array<android.net.Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                pendingFileChooser?.onReceiveValue(null)
                pendingFileChooser = filePathCallback
                val intent = try {
                    fileChooserParams?.createIntent() ?: android.content.Intent(android.content.Intent.ACTION_GET_CONTENT).apply {
                        addCategory(android.content.Intent.CATEGORY_OPENABLE)
                        type = "*/*"
                    }
                } catch (_: Exception) {
                    android.content.Intent(android.content.Intent.ACTION_GET_CONTENT).apply {
                        addCategory(android.content.Intent.CATEGORY_OPENABLE)
                        type = "*/*"
                    }
                }
                return try {
                    startActivityForResult(android.content.Intent.createChooser(intent, "Choose a file"), fileChooserRequestCode)
                    true
                } catch (e: Exception) {
                    pendingFileChooser?.onReceiveValue(null)
                    pendingFileChooser = null
                    Toast.makeText(this@MainActivity, "No file picker is available: ${e.message}", Toast.LENGTH_LONG).show()
                    false
                }
            }
        }
        web.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            try {
                val downloadUri = android.net.Uri.parse(url)
                require(downloadUri.scheme.equals("https", ignoreCase = true)) { "Only secure HTTPS downloads are allowed" }
                val filename = safeDownloadFileName(URLUtil.guessFileName(url, contentDisposition, mimeType))
                val request = DownloadManager.Request(downloadUri).apply {
                    setMimeType(mimeType)
                    // Never forward Discord session cookies to third-party download hosts.
                    val host = downloadUri.host?.lowercase(java.util.Locale.ROOT).orEmpty()
                    if (host == "discord.com" || host.endsWith(".discord.com") ||
                        host == "discordapp.com" || host.endsWith(".discordapp.com")) {
                        CookieManager.getInstance().getCookie(url)?.takeIf { it.isNotBlank() }?.let {
                            addRequestHeader("Cookie", it)
                        }
                    }
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
            text = "N"
            textSize = 21f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            contentDescription = "NEXORA control deck"
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                colors = intArrayOf(Color.rgb(137, 91, 255), Color.rgb(76, 53, 190))
                setStroke(dp(2), Color.argb(110, 255, 255, 255))
            }
            elevation = dp(9).toFloat()
            setOnClickListener { showNexoraHub() }
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
    private fun showNexoraHub() {
        val dialog = android.app.Dialog(this)
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(10, 12, 20))
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(22), dp(20), dp(22))
        }
        fun label(text: String, size: Float, color: Int, bold: Boolean = false): TextView =
            TextView(this).apply {
                this.text = text; textSize = size; setTextColor(color)
                if (bold) typeface = Typeface.DEFAULT_BOLD
            }
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(80, 56, 190), Color.rgb(23, 116, 150), Color.rgb(13, 22, 43))).apply { cornerRadius = dp(25).toFloat() }
        }
        val brandRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val mark = TextView(this).apply {
            text = "N"; textSize = 28f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.argb(55,255,255,255)); setStroke(dp(1),Color.argb(130,255,255,255)) }
        }
        brandRow.addView(mark, LinearLayout.LayoutParams(dp(54),dp(54)))
        val brandText = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(13),0,0,0) }
        brandText.addView(label("NEXORA",22f,Color.WHITE,true))
        brandText.addView(label("YOUR DISCORD, REIMAGINED",9f,Color.rgb(219,224,255),true))
        brandRow.addView(brandText)
        hero.addView(brandRow)
        hero.addView(label("Your space. Your rules.",23f,Color.WHITE,true).apply { setPadding(0,dp(22),0,dp(5)) })
        hero.addView(label("A faster control deck for the way you use Discord.",13f,Color.rgb(224,230,255)))
        val pageStatus = label("CONNECTED EXPERIENCE  •  DISCORD WEB",10f,Color.rgb(191,220,255),true).apply { setPadding(0,dp(17),0,0) }
        hero.addView(pageStatus)
        content.addView(hero)
        val tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0,dp(16),0,dp(12)) }
        val sections = listOf("Home","Appearance","Tools","Extensions")
        val sectionBody = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        fun tile(title: String, subtitle: String, action: () -> Unit) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; setPadding(dp(15),dp(14),dp(15),dp(14))
                background = GradientDrawable().apply { setColor(Color.rgb(25,29,43)); cornerRadius = dp(17).toFloat(); setStroke(dp(1),Color.rgb(43,52,76)) }
                isClickable = true; isFocusable = true; setOnClickListener { dialog.dismiss(); action() }
            }
            card.addView(label(title,15f,Color.WHITE,true))
            card.addView(label(subtitle,11f,Color.rgb(166,177,202)).apply { setPadding(0,dp(5),0,0) })
            val lp = LinearLayout.LayoutParams(-1,-2); lp.bottomMargin = dp(9); sectionBody.addView(card,lp)
        }
        fun render(section: String) {
            sectionBody.removeAllViews()
            when(section) {
                "Home" -> {
                    tile("Continue to Discord","Close this deck and return to your current server or DM") { }
                    tile("My profile studio","Personalise your local profile preview") { showProfileStudio() }
                    tile("Quick navigation","Jump to Discord settings, DMs and discovery") { showQuickNavigation() }
                    tile("Bookmarks","Open your saved pages") { showBookmarks() }
                    tile("What's inside Nexora","Themes, tools, plugins and customisation in one place") { showNexoraAbout() }
                }
                "Appearance" -> {
                    tile("Theme gallery","Eleven colour themes and layout controls") { showThemes() }
                    tile("Accent & typography","Custom colours and font choices") { showTypographySettings() }
                    tile("Style presets","One-tap chat layout presets") { showStylePresets() }
                    tile("Custom CSS","Build your own visual style") { editCss() }
                    tile("Profile cosmetics","Local-only profile decoration preview") { cosmeticLab() }
                }
                "Tools" -> {
                    tile("Message studio","Format messages before copying them") { showMessageStudio() }
                    tile("Snippet vault","Save reusable replies and templates") { showSnippetVault() }
                    tile("Emoji library","Browse and copy categorised emoji") { showEmojiLibrary() }
                    tile("Developer toolbox","JSON, Base64, URLs and Discord ID tools") { showDeveloperToolbox() }
                    tile("Rich Presence","Configure and preview activity data") { showRPC() }
                    tile("Browser tools","Reader mode, zoom, find and reload") { showBrowserTools() }
                    tile("Backup & restore","Export your Nexora settings") { backup() }
                }
                "Extensions" -> {
                    tile("Plugin manager","Install and manage extensions") { PluginManager(this) { feature -> when(feature) { 0 -> CustomPlugins(this) { web.reload() }.open(); 1 -> showThemes(); 2 -> editCss(); 3 -> showChatTools(); 4 -> cosmeticLab(); 5 -> showRPC() } }.open() }
                    tile("Feature library","Toggle interface enhancements") { showFeatureLibrary() }
                    tile("Chat tools","Reactions, translation and message helpers") { showChatTools() }
                    tile("About compatibility","Understand what works and what is experimental") { showNexoraAbout() }
                }
            }
        }
        sections.forEach { section ->
            val tab = Button(this).apply {
                text = section; textSize = 10f; isAllCaps = false
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply { setColor(Color.rgb(29,35,53)); cornerRadius = dp(12).toFloat() }
                setOnClickListener { render(section); pageStatus.text = "NEXORA  •  " + section.uppercase() }
            }
            val lp = LinearLayout.LayoutParams(0,dp(43),1f); lp.setMargins(dp(2),0,dp(2),0); tabs.addView(tab,lp)
        }
        content.addView(tabs)
        sectionBody.setPadding(0,dp(3),0,0)
        content.addView(sectionBody)
        content.addView(label("NEXORA IS AN INDEPENDENT CLIENT SHELL. DISCORD ACCOUNT FEATURES REMAIN SUBJECT TO DISCORD.",9f,Color.rgb(111,123,150)).apply { setPadding(0,dp(15),0,0) })
        render("Home")
        scroll.addView(content)
        outer.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        val footer = Button(this).apply {
            text = "BACK TO DISCORD"; isAllCaps = false; textSize = 12f; setTextColor(Color.WHITE)
            background = GradientDrawable().apply { setColor(Color.rgb(91,75,210)); cornerRadius = dp(15).toFloat() }
            setOnClickListener { dialog.dismiss() }
        }
        val footerWrap = LinearLayout(this).apply { setPadding(dp(18),dp(8),dp(18),dp(15)); addView(footer,LinearLayout.LayoutParams(-1,dp(48))) }
        outer.addView(footerWrap)
        dialog.setContentView(outer)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
        dialog.window?.setLayout(-1,-1)
        dialog.window?.setStatusBarColor(Color.rgb(10,12,20))
        dialog.window?.setNavigationBarColor(Color.rgb(10,12,20))
    }

    private fun showBrowserTools() {
        val opts = arrayOf("Back","Forward","Find on page","Copy page URL","Desktop / mobile mode","Toggle reader mode","Page zoom","Reload","Clear cache","Download history","Page information")
        AlertDialog.Builder(this).setTitle("Browser tools").setItems(opts) { _, i ->
            when(i) {
                0 -> if(web.canGoBack()) web.goBack()
                1 -> if(web.canGoForward()) web.goForward()
                2 -> findOnPage()
                3 -> { val cm=getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager; cm.setPrimaryClip(android.content.ClipData.newPlainText("Page URL",web.url ?: "")); Toast.makeText(this,"Copied",Toast.LENGTH_SHORT).show() }
                4 -> toggleDesktopMode()
                5 -> toggleReaderMode()
                6 -> showPageZoom()
                7 -> web.reload()
                8 -> AlertDialog.Builder(this).setMessage("Clear cached web files?").setPositiveButton("Clear") { _, _ -> web.clearCache(true) }.setNegativeButton("Cancel",null).show()
                9 -> showDownloads()
                10 -> showPageInfo()
            }
        }.show()
    }

    private fun showPageInfo() {
        val engine = if (android.os.Build.VERSION.SDK_INT >= 26) {
            WebView.getCurrentWebViewPackage()?.versionName ?: "Unknown"
        } else "Android System WebView"
        val info = "Page: " + (web.title ?: "Untitled") +
            "\nURL: " + (web.url ?: "Not loaded") +
            "\nWebView engine: " + engine +
            "\nDisplay mode: " + (if (prefs.getBoolean("desktop_mode", false)) "Desktop" else "Mobile") +
            "\nText zoom: " + web.settings.textZoom + "%" +
            "\nJavaScript: enabled" +
            "\nSafe Browsing: enabled where supported"
        AlertDialog.Builder(this).setTitle("Page information").setMessage(info)
            .setPositiveButton("Copy details") { _, _ ->
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("NEXORA page information", info))
                Toast.makeText(this, "Page details copied", Toast.LENGTH_SHORT).show()
            }.setNegativeButton("Close", null).show()
    }

    private fun showDownloads() {
        val manager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        val cursor = manager.query(DownloadManager.Query())
        val ids = ArrayList<Long>()
        val labels = ArrayList<String>()
        cursor.use {
            val idCol = it.getColumnIndex(DownloadManager.COLUMN_ID)
            val titleCol = it.getColumnIndex(DownloadManager.COLUMN_TITLE)
            val statusCol = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val reasonCol = it.getColumnIndex(DownloadManager.COLUMN_REASON)
            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val title = it.getString(titleCol) ?: "Downloaded file"
                val status = it.getInt(statusCol)
                val statusText = when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> "Complete"
                    DownloadManager.STATUS_FAILED -> "Failed (code " + it.getInt(reasonCol) + ")"
                    DownloadManager.STATUS_RUNNING -> "Downloading"
                    DownloadManager.STATUS_PAUSED -> "Paused"
                    else -> "Queued"
                }
                ids.add(id)
                labels.add(title + "\n" + statusText)
            }
        }
        if (ids.isEmpty()) {
            Toast.makeText(this, "No downloads yet", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this).setTitle("Downloads")
            .setItems(labels.toTypedArray()) { _, index ->
                val id = ids[index]
                val uri = manager.getUriForDownloadedFile(id)
                if (uri == null) {
                    Toast.makeText(this, "This download is not ready to open", Toast.LENGTH_SHORT).show()
                } else {
                    val mime = manager.getMimeTypeForDownloadedFile(id) ?: "*/*"
                    try {
                        startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW).setDataAndType(uri, mime)
                            .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION))
                    } catch (_: Exception) {
                        Toast.makeText(this, "No app can open this file", Toast.LENGTH_LONG).show()
                    }
                }
            }.setNegativeButton("Close", null).show()
    }

    private fun showNexoraAbout() {
        AlertDialog.Builder(this).setTitle("NEXORA")
            .setMessage("A redesigned control deck for your Discord experience. Nexora is an independent Android client shell, not an official Discord product. It does not grant Nitro or badges, and live Rich Presence remains dependent on supported Discord integration.")
            .setPositiveButton("Got it",null).show()
    }

    private fun showHome() {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(16))
            background = GradientDrawable().apply { setColor(Color.rgb(31, 32, 37)); cornerRadius = dp(20).toFloat() }
        }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val logo = TextView(this).apply {
            text = "N"; textSize = 25f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; colors = intArrayOf(Color.rgb(149, 105, 255), Color.rgb(83, 57, 194)) }
        }
        header.addView(logo, LinearLayout.LayoutParams(dp(50), dp(50)))
        val titleBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), 0, 0, 0) }
        titleBox.addView(TextView(this).apply { text = "NEXORA"; textSize = 20f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) })
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
        item("Plugins & extensions", "Install, enable and manage your add-ons") { PluginManager(this) { feature -> when(feature) { 0 -> CustomPlugins(this) { web.reload() }.open(); 1 -> showThemes(); 2 -> editCss(); 3 -> showChatTools(); 4 -> cosmeticLab(); 5 -> showRPC() } }.open() }
        item("Themes & appearance", "Colours, message spacing, avatars and layout") { showThemes() }
        item("Accent colour & typography", "Choose a custom accent and interface font") { showTypographySettings() }
        item("Feature library", "A larger collection of practical chat and interface controls") { showFeatureLibrary() }
        item("My profile cosmetics", "Show your local badges and activity on your own profile panel") { showProfileStudio() }
        item("Cosmetic lab", "Local badge and Nitro-style profile mock-ups") { cosmeticLab() }
        item("Custom CSS", "Style the Discord page yourself") { editCss() }
        item("Style preset gallery", "Apply ready-made chat and interface CSS styles") { showStylePresets() }
        group("Utilities")
        item("Rich Presence", "Configure Discord Rich Presence") { showRPC() }
        item("Chat tools", "Quick reactions, hide upsells and compact chat") { showChatTools() }
        item("Message studio", "Format messages, manage reusable snippets and copy templates") { showMessageStudio() }
        item("Snippet vault", "Save reusable replies, templates and formatted text") { showSnippetVault() }
        item("Developer toolbox", "Format JSON, encode text, decode Base64 and inspect Discord IDs") { showDeveloperToolbox() }
        item("Backup settings", "Copy a backup of your preferences") { backup() }
        item("Bookmarks", "Save, search and reopen Discord pages") { showBookmarks() }
        item("Discord quick navigation", "Jump to DMs, friends, settings, Nitro and discovery") { showQuickNavigation() }
        item("Browser tools", "Back, forward, find, desktop mode, reload and cache") {
            val opts = arrayOf("Back", "Forward", "Find on page", "Copy page URL", "Desktop / mobile mode", "Toggle reader mode", "Page zoom", "Reload", "Clear cache")
            AlertDialog.Builder(this).setTitle("Browser tools").setItems(opts) { _, i -> when(i) { 0 -> if(web.canGoBack()) web.goBack(); 1 -> if(web.canGoForward()) web.goForward(); 2 -> findOnPage(); 3 -> { val cm=getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager; cm.setPrimaryClip(android.content.ClipData.newPlainText("Page URL",web.url ?: "")); Toast.makeText(this,"Copied",Toast.LENGTH_SHORT).show() }; 4 -> toggleDesktopMode(); 5 -> toggleReaderMode(); 6 -> showPageZoom(); 7 -> web.reload(); 8 -> AlertDialog.Builder(this).setMessage("Clear cached web files?").setPositiveButton("Clear") { _, _ -> web.clearCache(true) }.setNegativeButton("Cancel",null).show() } }.show()
        }
        item("Open Discord in browser", "Use your external browser") { startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://discord.com/app"))) }
        item("About & limitations", "Compatibility, privacy and feature details") { AlertDialog.Builder(this).setTitle("About NEXORA").setMessage("Independent Android Discord WebView with the official Vencord browser userscript bundled at build time. Vencord plugins that depend on desktop-only native APIs may not work. Genuine Nitro and account badges cannot be granted by a client mod. Live Android Rich Presence is supported by Discord Social SDK 1.10+ and needs a registered Application ID and SDK package.").setPositiveButton("OK",null).show() }
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

    private fun showTypographySettings() {
        val accent = EditText(this).apply {
            hint = "#8B5CF6"
            setText(prefs.getString("custom_accent", ""))
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val fonts = arrayOf("System default", "Arial", "Georgia", "Trebuchet MS", "Verdana", "monospace")
        val current = prefs.getInt("font_choice", 0).coerceIn(0, fonts.lastIndex)
        val spinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, fonts)
            setSelection(current)
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(8), dp(18), 0)
            addView(TextView(this@MainActivity).apply { text = "ACCENT COLOUR (HEX)"; setTextColor(Color.LTGRAY); textSize = 11f })
            addView(accent)
            addView(TextView(this@MainActivity).apply { text = "INTERFACE FONT"; setTextColor(Color.LTGRAY); textSize = 11f; setPadding(0, dp(12), 0, dp(4)) })
            addView(spinner)
        }
        AlertDialog.Builder(this).setTitle("Accent & typography").setView(box)
            .setPositiveButton("Apply") { _, _ ->
                val value = accent.text.toString().trim()
                if (value.isNotEmpty() && !Regex("^#[0-9a-fA-F]{6}$").matches(value)) {
                    Toast.makeText(this, "Use a six-digit hex colour such as #8B5CF6", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                prefs.edit().putString("custom_accent", value).putInt("font_choice", spinner.selectedItemPosition).apply()
                applyCustomizations()
                Toast.makeText(this, "Appearance updated", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Reset") { _, _ ->
                prefs.edit().remove("custom_accent").putInt("font_choice", 0).apply()
                applyCustomizations()
            }.setNegativeButton("Close", null).show()
    }

    private fun showThemes() {
        val themes = arrayOf("Discord blurple dark", "AMOLED black", "Midnight purple", "Ocean blue", "Forest green", "Sunset orange", "Discord light", "Graphite", "Rose", "Crimson", "Mint")
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
    private fun showStylePresets() {
        val presets = arrayOf(
            "Soft rounded messages",
            "Minimal compact chat",
            "Bubble message layout",
            "Hide visual clutter",
            "High contrast readability",
            "Monospace developer chat",
            "Soft glass panels",
            "Reset custom styles"
        )
        val css = arrayOf(
            "[class*=messageListItem]{border-radius:14px!important;margin:4px 8px!important;padding:5px 8px!important}[class*=messageContent]{line-height:1.5!important}",
            "[class*=messageListItem]{padding-top:1px!important;padding-bottom:1px!important}[class*=messageContent]{font-size:13px!important;line-height:1.3!important}[class*=divider]{margin-top:4px!important;margin-bottom:4px!important}",
            "[class*=messageListItem]{margin:5px 10px!important;padding:8px 12px!important;border-radius:16px!important;background:rgba(127,127,127,.09)!important}[class*=contents]{border-radius:14px!important}",
            "[class*=nitroMessage], [class*=premiumPromo], [class*=upsell], [class*=shopBanner], [class*=giftButton]{display:none!important}",
            "[class*=messageContent]{font-size:17px!important;line-height:1.7!important;letter-spacing:.01em!important}[class*=markup]{color:var(--text-normal)!important}",
            "[class*=messageContent],pre,code{font-family:ui-monospace,SFMono-Regular,Consolas,monospace!important}[class*=messageContent]{font-size:13px!important;line-height:1.6!important}",
            "[class*=messageListItem],[class*=containerDefault],[class*=membersWrap]{backdrop-filter:blur(12px)!important}[class*=messageListItem]{background:rgba(255,255,255,.045)!important;border:1px solid rgba(255,255,255,.06)!important;border-radius:12px!important;margin:4px!important}",
            ""
        )
        AlertDialog.Builder(this).setTitle("Style preset gallery")
            .setItems(presets) { _, which ->
                if (which == presets.lastIndex) {
                    prefs.edit().remove("css").apply()
                    applyCustomizations()
                    Toast.makeText(this, "Custom CSS cleared", Toast.LENGTH_SHORT).show()
                } else {
                    prefs.edit().putString("css", css[which]).apply()
                    applyCustomizations()
                    Toast.makeText(this, presets[which] + " applied", Toast.LENGTH_SHORT).show()
                }
            }.setNegativeButton("Close", null).show()
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
            .setPositiveButton("Save & test dispatch") { _, _ ->
                val editor = prefs.edit()
                fields.forEach { (key, input) -> editor.putString("rpc_$key", input.text.toString()) }
                editor.putInt("rpc_type", typeSpinner.selectedItemPosition)
                    .putBoolean("rpc_timestamps_enabled", timestamps.isChecked)
                    .apply()
                publishRichPresence()
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

    private fun publishRichPresence() {
        val activity = org.json.JSONObject()
            .put("name", prefs.getString("rpc_name", ""))
            .put("application_id", prefs.getString("rpc_application_id", ""))
            .put("type", prefs.getInt("rpc_type", 0))
            .put("details", prefs.getString("rpc_details", ""))
            .put("state", prefs.getString("rpc_state", ""))
        if (prefs.getBoolean("rpc_timestamps_enabled", false)) {
            val timestamps = org.json.JSONObject()
            prefs.getString("rpc_start", "")?.toLongOrNull()?.takeIf { it > 0 }?.let { timestamps.put("start", it) }
            prefs.getString("rpc_end", "")?.toLongOrNull()?.takeIf { it > 0 }?.let { timestamps.put("end", it) }
            if (timestamps.length() > 0) activity.put("timestamps", timestamps)
        }
        val assets = org.json.JSONObject()
        listOf("large" to "large_image", "large_text" to "large_text", "small" to "small_image", "small_text" to "small_text").forEach { (pref, key) ->
            prefs.getString("rpc_$pref", "")?.takeIf { it.isNotBlank() }?.let { assets.put(key, it) }
        }
        if (assets.length() > 0) activity.put("assets", assets)
        val party = org.json.JSONObject()
        prefs.getString("rpc_party_id", "")?.takeIf { it.isNotBlank() }?.let { party.put("id", it) }
        val partySize = prefs.getString("rpc_party_size", "")?.toIntOrNull()
        val partyMax = prefs.getString("rpc_party_max", "")?.toIntOrNull()
        if (partySize != null && partyMax != null) party.put("size", org.json.JSONArray().put(partySize).put(partyMax))
        if (party.length() > 0) activity.put("party", party)
        val buttons = org.json.JSONArray()
        for (n in 1..2) {
            val label = prefs.getString("rpc_button${n}_label", "").orEmpty()
            val url = prefs.getString("rpc_button${n}_url", "").orEmpty()
            if (label.isNotBlank() && url.startsWith("https://")) buttons.put(org.json.JSONObject().put("label", label).put("url", url))
        }
        if (buttons.length() > 0) activity.put("buttons", buttons)
        val payload = org.json.JSONObject.quote(activity.toString())
        val script = """
            (function() {
              try {
                var raw = $payload;
                var activity = JSON.parse(raw);
                var dispatcher = null;
                var source = '';

                // Vencord exposes its Webpack helpers on the Vencord global.
                var vc = window.Vencord;
                var webpack = vc && vc.Webpack;
                var common = webpack && webpack.Common;
                var candidates = [
                  common && common.FluxDispatcher,
                  vc && vc.Webpack && vc.Webpack.Common && vc.Webpack.Common.FluxDispatcher
                ];
                for (var i = 0; i < candidates.length; i++) {
                  var d = candidates[i];
                  if (d && typeof d.dispatch === 'function') {
                    dispatcher = d;
                    source = 'Vencord Webpack.Common';
                    break;
                  }
                }

                // Some Vencord builds expose findByProps rather than Common.
                if (!dispatcher && webpack && typeof webpack.findByProps === 'function') {
                  try {
                    var found = webpack.findByProps('dispatch', 'subscribe');
                    if (found && typeof found.dispatch === 'function') {
                      dispatcher = found;
                      source = 'Vencord Webpack.findByProps';
                    }
                  } catch (_) {}
                }

                // Last resort: inspect the actual Discord webpack chunk runtime.
                if (!dispatcher) {
                  var names = Object.keys(window).filter(function(k) {
                    return /^webpackChunk.*discord/i.test(k);
                  });
                  for (var n = 0; n < names.length && !dispatcher; n++) {
                    var chunks = window[names[n]];
                    if (!chunks || typeof chunks.push !== 'function') continue;
                    var req;
                    try {
                      chunks.push([[('vcm_rpc_' + Date.now())], {}, function(r) { req = r; }]);
                    } catch (_) { continue; }
                    if (!req || !req.c) continue;
                    Object.keys(req.c).some(function(id) {
                      var ex = req.c[id] && req.c[id].exports;
                      var list = ex ? [ex, ex.default, ex.FluxDispatcher] : [];
                      return list.some(function(v) {
                        if (v && typeof v.dispatch === 'function' && typeof v.subscribe === 'function') {
                          dispatcher = v;
                          source = names[n] + ' module cache';
                          return true;
                        }
                        if (v && v.FluxDispatcher && typeof v.FluxDispatcher.dispatch === 'function') {
                          dispatcher = v.FluxDispatcher;
                          source = names[n] + ' module cache';
                          return true;
                        }
                        return false;
                      });
                    });
                  }
                }

                if (!dispatcher) {
                  var hasVencord = !!window.Vencord;
                  var chunkNames = Object.keys(window).filter(function(k) { return /^webpackChunk/i.test(k); });
                  return 'Dispatcher not found. Vencord=' + hasVencord + '; webpack chunks=' + (chunkNames.join(',') || 'none') + '. Reload Discord and try again.';
                }

                // Match Siguma's LOCAL_ACTIVITY_UPDATE payload shape.
                dispatcher.dispatch({
                  type: 'LOCAL_ACTIVITY_UPDATE',
                  activity: activity,
                  pid: 1608,
                  socketId: 'RPC@VencordMobile'
                });
                return 'Local activity event dispatched via ' + source + '. Profile publication is not confirmed in this WebView client.';
              } catch (e) {
                return 'Rich Presence error: ' + String(e && e.stack || e);
              }
            })();
        """.trimIndent()
        web.evaluateJavascript(script) { result ->
            val message = try { org.json.JSONTokener(result).nextValue() as? String ?: "Unknown result" } catch (_: Exception) { "Unknown result" }
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
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

    private fun showFeatureLibrary() {
        val groups = linkedMapOf(
            "Messages & readability" to listOf(
                "compact" to "Compact message spacing",
                "cards" to "Message cards",
                "bigtext" to "Larger message text",
                "noavatars" to "Hide message avatars",
                "hide_embeds" to "Hide link embeds",
                "hide_attachments" to "Hide image and video attachments",
                "hide_blocked" to "Hide blocked-message placeholders",
                "hide_reactions" to "Hide reaction rows"
            ),
            "Navigation & layout" to listOf(
                "compact_channels" to "Compact channel list",
                "hide_members" to "Hide member list",
                "hide_activity" to "Hide activity panels",
                "hide_server_boost" to "Hide server boost promotions",
                "hide_discover" to "Hide discovery promotions",
                "hide_shop" to "Hide shop entry points"
            ),
            "Distraction controls" to listOf(
                "upsells" to "Hide Nitro promotions",
                "hide_gift" to "Hide gift buttons",
                "hide_gif" to "Hide GIF buttons",
                "hide_stickers" to "Hide sticker buttons",
                "hide_typing" to "Hide typing indicator",
                "reduce_motion" to "Reduce animations",
                "hide_profile_banners" to "Hide profile banners"
            )
        )
        val all = groups.values.flatten()
        val labels = all.map { it.second }.toTypedArray()
        val checked = BooleanArray(all.size) { i -> prefs.getBoolean("feature_" + all[i].first, false) }
        AlertDialog.Builder(this)
            .setTitle("Feature library")
            .setMessage("Choose the local interface features to enable. These are visual controls only and do not change server data or grant Discord account perks.")
            .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                checked[which] = isChecked
                prefs.edit().putBoolean("feature_" + all[which].first, isChecked).apply()
            }
            .setPositiveButton("Apply") { _, _ ->
                val editor = prefs.edit()
                all.forEachIndexed { i, pair -> editor.putBoolean("feature_" + pair.first, checked[i]) }
                editor.apply()
                applyCustomizations()
                Toast.makeText(this, "Feature library settings applied", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Enable recommended") { _, _ ->
                val recommended = setOf("compact", "cards", "hide_server_boost", "hide_discover", "hide_shop", "hide_gift", "hide_gif", "hide_stickers", "reduce_motion")
                val editor = prefs.edit()
                all.forEach { editor.putBoolean("feature_" + it.first, it.first in recommended) }
                editor.apply()
                applyCustomizations()
                Toast.makeText(this, "Recommended features enabled", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showSnippetVault() {
        fun load(): org.json.JSONArray = try {
            org.json.JSONArray(prefs.getString("snippet_vault", "[]") ?: "[]")
        } catch (_: Exception) { org.json.JSONArray() }

        fun refresh() {
            val data = load()
            val labels = Array(data.length()) { i ->
                val item = data.optJSONObject(i)
                (item?.optString("title") ?: "Untitled") + "  •  " + (item?.optString("body") ?: "").take(45)
            }
            AlertDialog.Builder(this).setTitle("Snippet vault")
                .setItems(labels) { _, index ->
                    val item = data.optJSONObject(index) ?: return@setItems
                    val body = item.optString("body")
                    AlertDialog.Builder(this).setTitle(item.optString("title"))
                        .setMessage(body)
                        .setPositiveButton("Copy") { _, _ ->
                            val cm = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            cm.setPrimaryClip(android.content.ClipData.newPlainText(item.optString("title"), body))
                            Toast.makeText(this, "Snippet copied", Toast.LENGTH_SHORT).show()
                        }
                        .setNeutralButton("Delete") { _, _ ->
                            val updated = org.json.JSONArray()
                            for (i in 0 until data.length()) if (i != index) updated.put(data.get(i))
                            prefs.edit().putString("snippet_vault", updated.toString()).apply()
                            Toast.makeText(this, "Snippet deleted", Toast.LENGTH_SHORT).show()
                            refresh()
                        }.setNegativeButton("Close", null).show()
                }
                .setPositiveButton("Add snippet") { _, _ ->
                    val title = EditText(this).apply { hint = "Snippet name" }
                    val body = EditText(this).apply { hint = "Reusable message"; minLines = 4; gravity = Gravity.TOP; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE }
                    val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(8), dp(18), 0); addView(title); addView(body) }
                    AlertDialog.Builder(this).setTitle("New snippet").setView(box)
                        .setPositiveButton("Save") { _, _ ->
                            val name = title.text.toString().trim()
                            val text = body.text.toString()
                            if (name.isNotBlank() && text.isNotBlank()) {
                                val updated = load().put(org.json.JSONObject().put("title", name).put("body", text))
                                prefs.edit().putString("snippet_vault", updated.toString()).apply()
                                Toast.makeText(this, "Snippet saved", Toast.LENGTH_SHORT).show()
                            } else Toast.makeText(this, "Enter a name and message", Toast.LENGTH_SHORT).show()
                        }.setNegativeButton("Cancel", null).show()
                }
                .setNegativeButton("Close", null).show()
        }
        refresh()
    }

    private fun showMessageStudio() {
        val input = EditText(this).apply {
            hint = "Write or paste text here"
            minLines = 5
            gravity = Gravity.TOP
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setText(prefs.getString("message_studio_draft", ""))
        }
        val actions = arrayOf("Bold", "Italic", "Underline", "Strikethrough", "Spoiler", "Inline code", "Code block", "Quote", "Bullet list", "Numbered list", "Heading")
        fun wrap(before: String, after: String = before) {
            val text = input.text.toString()
            val a = input.selectionStart.coerceAtLeast(0).coerceAtMost(text.length)
            val b = input.selectionEnd.coerceAtLeast(a).coerceAtMost(text.length)
            val selected = text.substring(a, b)
            val result = text.substring(0, a) + before + selected + after + text.substring(b)
            input.setText(result)
            input.setSelection((a + before.length + selected.length + after.length).coerceAtMost(result.length))
        }
        val buttons = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val rows = actions.toList().chunked(3)
        rows.forEach { rowItems ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            rowItems.forEach { label ->
                val button = Button(this@MainActivity).apply {
                    text = label
                    textSize = 10f
                    setOnClickListener {
                        when(label) {
                            "Bold" -> wrap("**")
                            "Italic" -> wrap("*")
                            "Underline" -> wrap("__")
                            "Strikethrough" -> wrap("~~")
                            "Spoiler" -> wrap("||")
                            "Inline code" -> wrap("`")
                            "Code block" -> wrap("```\n", "\n```")
                            "Quote" -> wrap("> ", "")
                            "Bullet list" -> wrap("- ", "")
                            "Numbered list" -> wrap("1. ", "")
                            "Heading" -> wrap("# ", "")
                        }
                    }
                }
                row.addView(button, LinearLayout.LayoutParams(0, dp(44), 1f))
            }
            buttons.addView(row)
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), 0)
            addView(buttons)
            addView(input, LinearLayout.LayoutParams(-1, dp(180)))
        }
        AlertDialog.Builder(this).setTitle("Message studio")
            .setMessage("Format your draft, then copy it into Discord. Nothing is sent automatically.")
            .setView(root)
            .setPositiveButton("Copy") { _, _ ->
                val value = input.text.toString()
                val cm = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                cm.setPrimaryClip(android.content.ClipData.newPlainText("Discord message", value))
                prefs.edit().putString("message_studio_draft", value).apply()
                Toast.makeText(this, "Formatted message copied", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Save draft") { _, _ -> prefs.edit().putString("message_studio_draft", input.text.toString()).apply(); Toast.makeText(this, "Draft saved locally", Toast.LENGTH_SHORT).show() }
            .setNegativeButton("Close", null).show()
    }

    private fun showDeveloperToolbox() {
        val tools = arrayOf("Format / validate JSON", "URL encode text", "URL decode text", "Base64 encode", "Base64 decode", "Inspect Discord Snowflake ID", "Count characters and words")
        AlertDialog.Builder(this).setTitle("Developer toolbox").setItems(tools) { _, index ->
            val input = EditText(this).apply {
                hint = when(index) {
                    0 -> "Paste JSON"
                    5 -> "Paste a Discord ID"
                    else -> "Enter text"
                }
                minLines = if(index == 0) 5 else 2
                gravity = Gravity.TOP
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            }
            AlertDialog.Builder(this).setTitle(tools[index]).setView(input)
                .setPositiveButton("Run") { _, _ ->
                    val source = input.text.toString()
                    val result = try {
                        when(index) {
                            0 -> org.json.JSONTokener(source).nextValue().let { value ->
                                when(value) {
                                    is org.json.JSONObject -> value.toString(2)
                                    is org.json.JSONArray -> value.toString(2)
                                    else -> value.toString()
                                }
                            }
                            1 -> java.net.URLEncoder.encode(source, "UTF-8")
                            2 -> java.net.URLDecoder.decode(source, "UTF-8")
                            3 -> android.util.Base64.encodeToString(source.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
                            4 -> String(android.util.Base64.decode(source, android.util.Base64.DEFAULT), Charsets.UTF_8)
                            5 -> {
                                val id = source.trim().toLong()
                                val millis = (id shr 22) + 1420070400000L
                                "Created: " + java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(java.util.Date(millis)) + "\nTimestamp: " + millis
                            }
                            else -> "Characters: " + source.length + "\nWords: " + source.trim().split(Regex("\\s+")).count { it.isNotEmpty() } + "\nLines: " + if(source.isEmpty()) 0 else source.count { it == '\n' } + 1
                        }
                    } catch(e: Exception) { "Error: " + (e.message ?: "Invalid input") }
                    val output = EditText(this).apply { setText(result); minLines = 4; gravity = Gravity.TOP; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE }
                    AlertDialog.Builder(this).setTitle("Result").setView(output)
                        .setPositiveButton("Copy") { _, _ ->
                            val cm = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            cm.setPrimaryClip(android.content.ClipData.newPlainText("Tool result", output.text.toString()))
                            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
                        }.setNegativeButton("Close", null).show()
                }.setNegativeButton("Cancel", null).show()
        }.show()
    }

    private fun toggleReaderMode() {
        val enabled = !prefs.getBoolean("reader_mode", false)
        prefs.edit().putBoolean("reader_mode", enabled).apply()
        web.evaluateJavascript("""
            (function(on) {
              var id='vcm-reader-style';
              var old=document.getElementById(id);
              if(old) old.remove();
              if(on) {
                var style=document.createElement('style');
                style.id=id;
                style.textContent='html,body{background:#f5f3ed!important;color:#202124!important}body{font-family:Georgia,serif!important;font-size:18px!important;line-height:1.75!important} [class*=sidebar],[class*=membersWrap],[class*=guilds],[class*=channelTabs],[class*=toolbar],[class*=nowPlayingColumn]{display:none!important} [class*=chatContent]{max-width:900px!important;margin:auto!important;background:#f5f3ed!important;color:#202124!important} [class*=messageContent]{color:#202124!important;font-size:18px!important;line-height:1.75!important} img,video{max-width:100%!important;height:auto!important}';
                document.head.appendChild(style);
              }
            })(${enabled});
        """.trimIndent(), null)
        Toast.makeText(this, if(enabled) "Reader mode enabled" else "Reader mode disabled", Toast.LENGTH_SHORT).show()
    }

    private fun showPageZoom() {
        val current = web.settings.textZoom.coerceIn(75, 175)
        val seek = SeekBar(this).apply { max = 100; progress = current - 75 }
        val label = TextView(this).apply { text = "Text size: " + current + "%"; setTextColor(Color.WHITE); gravity = Gravity.CENTER; setPadding(0, dp(10), 0, dp(6)) }
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress + 75
                label.text = "Text size: " + value + "%"
                web.settings.textZoom = value
            }
            override fun onStartTrackingTouch(bar: SeekBar?) {}
            override fun onStopTrackingTouch(bar: SeekBar?) {}
        })
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), 0); addView(label); addView(seek) }
        AlertDialog.Builder(this).setTitle("Page zoom").setView(box)
            .setPositiveButton("Save") { _, _ -> prefs.edit().putInt("page_text_zoom", seek.progress + 75).apply() }
            .setNeutralButton("Reset") { _, _ -> web.settings.textZoom = 100; prefs.edit().putInt("page_text_zoom", 100).apply() }
            .setNegativeButton("Close", null).show()
    }

    private fun showQuickNavigation() {
        val destinations = arrayOf(
            "Direct Messages" to "https://discord.com/channels/@me",
            "Friends" to "https://discord.com/channels/@me",
            "Nitro" to "https://discord.com/store",
            "Shop" to "https://discord.com/shop",
            "Discover servers" to "https://discord.com/guild-discovery",
            "User settings" to "https://discord.com/settings",
            "Connections" to "https://discord.com/settings/connections",
            "Authorized apps" to "https://discord.com/settings/authorized-apps",
            "Accessibility" to "https://discord.com/settings/accessibility",
            "Keybinds" to "https://discord.com/settings/keybinds"
        )
        AlertDialog.Builder(this).setTitle("Discord quick navigation")
            .setItems(destinations.map { it.first }.toTypedArray()) { _, index ->
                web.loadUrl(destinations[index].second)
            }.setNegativeButton("Close", null).show()
    }

    private fun showBookmarks() {
        fun readBookmarks(): org.json.JSONArray = try {
            org.json.JSONArray(prefs.getString("bookmarks", "[]") ?: "[]")
        } catch (_: Exception) { org.json.JSONArray() }

        fun display(filter: String = "") {
            val all = readBookmarks()
            val matches = (0 until all.length()).mapNotNull { i ->
                val item = all.optJSONObject(i) ?: return@mapNotNull null
                Triple(i, item.optString("title", "Untitled"), item.optString("url"))
            }.filter { it.second.contains(filter, true) || it.third.contains(filter, true) }
            val labels = matches.map { it.second + "\n" + it.third }.toTypedArray()
            AlertDialog.Builder(this).setTitle("Bookmarks (" + matches.size + ")")
                .setItems(labels) { _, index ->
                    val selected = matches[index]
                    AlertDialog.Builder(this).setTitle(selected.second)
                        .setMessage(selected.third)
                        .setPositiveButton("Open") { _, _ -> web.loadUrl(selected.third) }
                        .setNeutralButton("Delete") { _, _ ->
                            val updated = org.json.JSONArray()
                            for (i in 0 until all.length()) if (i != selected.first) updated.put(all.get(i))
                            prefs.edit().putString("bookmarks", updated.toString()).apply()
                            Toast.makeText(this, "Bookmark deleted", Toast.LENGTH_SHORT).show()
                        }.setNegativeButton("Close", null).show()
                }
                .setPositiveButton("Add current page") { _, _ ->
                    val url = web.url ?: return@setPositiveButton
                    if (!url.startsWith("https://")) {
                        Toast.makeText(this, "Only HTTPS pages can be bookmarked", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    val title = EditText(this).apply { setText(web.title ?: url); hint = "Bookmark title" }
                    AlertDialog.Builder(this).setTitle("Save bookmark").setView(title)
                        .setPositiveButton("Save") { _, _ ->
                            val updated = readBookmarks()
                            updated.put(org.json.JSONObject().put("title", title.text.toString().ifBlank { url }).put("url", url))
                            prefs.edit().putString("bookmarks", updated.toString()).apply()
                            Toast.makeText(this, "Bookmark saved", Toast.LENGTH_SHORT).show()
                        }.setNegativeButton("Cancel", null).show()
                }
                .setNeutralButton("Search") { _, _ ->
                    val search = EditText(this).apply { hint = "Search titles and URLs" }
                    AlertDialog.Builder(this).setTitle("Search bookmarks").setView(search)
                        .setPositiveButton("Find") { _, _ -> display(search.text.toString()) }
                        .setNegativeButton("Cancel", null).show()
                }.setNegativeButton("Close", null).show()
        }
        display()
    }

    private fun showChatTools() {
        val options = arrayOf("Quick reaction palette", "Emoji library", "Translate selected text", "Compact message spacing", "Hide Nitro promotional panels", "Format a message", "Create Discord timestamp", "Markdown reference", "Copy selected text")
        AlertDialog.Builder(this).setTitle("Chat tools").setItems(options) { _, index ->
            when(index) {
                0 -> quickReactionPalette()
                1 -> showEmojiLibrary()
                2 -> translateSelection()
                3 -> { val enabled = !prefs.getBoolean("compact", false); prefs.edit().putBoolean("compact", enabled).apply(); applyCustomizations(); Toast.makeText(this, if(enabled) "Compact spacing on" else "Compact spacing off", Toast.LENGTH_SHORT).show() }
                4 -> { val enabled = !prefs.getBoolean("upsells", false); prefs.edit().putBoolean("upsells", enabled).apply(); applyCustomizations(); Toast.makeText(this, if(enabled) "Promotional panels hidden where supported" else "Promotional panels shown", Toast.LENGTH_SHORT).show() }
                5 -> formatDiscordMessage()
                6 -> createDiscordTimestamp()
                7 -> showMarkdownReference()
                8 -> copySelection()
            }
        }.show()
    }

    private fun showEmojiLibrary() {
        val groups = linkedMapOf(
            "Faces" to "😀 😃 😄 😁 😆 😅 😂 🙂 🙃 🫠 😉 😊 😇 🥰 😍 🤩 😘 😗 ☺️ 😚 😙 🥲",
            "Reactions" to "👍 👎 👏 🙌 🫶 🤝 🙏 💪 🫡 👀 💀 🤯 🥳 😭 😂 🤣 😮 😱 🫣 🥹",
            "Hearts" to "❤️ 🧡 💛 💚 💙 💜 🖤 🤍 🤎 💔 ❤️‍🔥 💕 💞 💓 💗 💖 💘 💝",
            "Hands" to "👋 🤚 🖐️ ✋ 🖖 👌 🤌 🤏 ✌️ 🤞 🫰 🤟 🤘 🤙 👈 👉 👆 👇 ☝️",
            "Animals" to "🐶 🐱 🐭 🐹 🐰 🦊 🐻 🐼 🐨 🐯 🦁 🐮 🐸 🐵 🐧 🐦 🦄 🐝 🦋 🐢",
            "Food" to "🍕 🍔 🍟 🌭 🍿 🧂 🥓 🥚 🍳 🧇 🥞 🧀 🍗 🍖 🌮 🌯 🍜 🍣 🍩 🍪",
            "Objects" to "💎 💰 🎁 🎈 🎉 🎊 🏆 🥇 ⚽ 🎮 🎧 💻 📱 🔥 ⭐ 🌟 ✨ 💡 🚀",
            "Symbols" to "✅ ❌ ⚠️ ❗ ❓ 💯 🔞 ♻️ ➡️ ⬅️ ⬆️ ⬇️ 🔥 💢 ♾️ ™️ ©️ ®️"
        )
        AlertDialog.Builder(this).setTitle("Emoji library")
            .setItems(groups.keys.toTypedArray()) { _, groupIndex ->
                val group = groups.entries.elementAt(groupIndex)
                val emojis = group.value.split(" ")
                AlertDialog.Builder(this).setTitle(group.key + " • tap to copy")
                    .setItems(emojis.toTypedArray()) { _, emojiIndex ->
                        val emoji = emojis[emojiIndex]
                        val cm = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("Emoji", emoji))
                        Toast.makeText(this, emoji + " copied", Toast.LENGTH_SHORT).show()
                    }.setNegativeButton("Back") { _, _ -> showEmojiLibrary() }.show()
            }.setNegativeButton("Close", null).show()
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
            // Executable plugin source is intentionally excluded from portable backups.
            if (key == "custom_plugins") continue
            val type = when(value) { is Boolean -> "boolean"; is Int -> "int"; is Long -> "long"; is Float -> "float"; else -> "string" }
            values.put(key, org.json.JSONObject().put("type",type).put("value",value.toString()))
        }
        snapshot.put("format","nexora-settings").put("version",2).put("preferences",values)
        val editor = EditText(this).apply { setText(snapshot.toString(2)); minLines=8; gravity=Gravity.TOP; inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE }
        AlertDialog.Builder(this).setTitle("Backup & restore")
            .setMessage("Copy this JSON to back up your settings, or paste an older NEXORA backup and choose Restore. Plugin code is excluded for safety.")
            .setView(editor)
            .setPositiveButton("Copy backup") { _, _ ->
                val cm=getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                cm.setPrimaryClip(android.content.ClipData.newPlainText("NEXORA backup",editor.text.toString()))
                Toast.makeText(this,"Backup copied",Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Restore") { _, _ ->
                try {
                    val backupText = editor.text.toString()
                    require(backupText.length <= 1000000) { "Backup exceeds the 1 MB restore limit" }
                    val root=org.json.JSONObject(backupText)
                    require(root.optString("format") in setOf("nexora-settings", "vencord-mobile-settings")) { "This is not a supported NEXORA backup" }
                    require(root.optInt("version", 1) in 1..2) { "Unsupported backup version" }
                    val values=root.getJSONObject("preferences")
                    require(values.length() <= 500) { "Backup contains too many settings" }
                    val out=prefs.edit()
                    var skipped = 0
                    val keys=values.keys()
                    while(keys.hasNext()) {
                        val key=keys.next()
                        // Executable plugins are never imported. Ignore unknown preference names
                        // so crafted backups cannot create unexpected SharedPreferences types.
                        if (key == "custom_plugins") continue
                        val item=values.optJSONObject(key)
                        if (item == null) { skipped++; continue }
                        val value=item.optString("value", "")
                        val declared=item.optString("type", "")
                        val parsed = BackupPreferenceValidator.parse(key, declared, value)
                        when (parsed) {
                            is Boolean -> out.putBoolean(key, parsed)
                            is Int -> out.putInt(key, parsed)
                            is String -> out.putString(key, parsed)
                            else -> { skipped++; continue }
                        }
                    }
                    out.apply()
                    applyCustomizations()
                    Toast.makeText(this, if (skipped == 0) "Settings restored" else "Settings restored; skipped $skipped invalid entries", Toast.LENGTH_LONG).show()
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
                if (code.isBlank() || code.toByteArray(Charsets.UTF_8).size > 500000) {
                    android.util.Log.w("NEXORA", "Skipped invalid or oversized plugin: " + p.optString("name", "Unnamed"))
                    continue
                }
                // onPageFinished may fire more than once for a single document. Track each
                // plugin in that document so its listeners and observers are not duplicated.
                val identity = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(
                        org.json.JSONObject()
                            .put("source", p.optString("source", ""))
                            .put("name", p.optString("name", ""))
                            .put("code", code)
                            .toString()
                            .toByteArray(Charsets.UTF_8)
                    )
                    .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
                val key = org.json.JSONObject.quote(identity)
                web.evaluateJavascript(
                    "(function(){var r=window.__NEXORA_PLUGIN_RUNS__||(window.__NEXORA_PLUGIN_RUNS__={});" +
                        "var k=" + key + ";if(r[k])return;r[k]=true;" +
                        "try{(function(){" + code + "}).call(window)}catch(e){delete r[k];" +
                        "console.error('NEXORA plugin error ('+k+')',e)}})();",
                    null
                )
            }
        }
    }

    private fun applyCustomizations() {
        val theme = prefs.getInt("theme", 0).coerceIn(0, 10)
        val palettes = arrayOf(
            arrayOf("#313338","#2b2d31","#1e1f22","#5865f2","#f2f3f5","#b5bac1"),
            arrayOf("#000000","#080808","#101010","#8b5cf6","#f5f5f5","#a1a1aa"),
            arrayOf("#171321","#211a30","#100d18","#a78bfa","#f5efff","#c4b5fd"),
            arrayOf("#10202d","#162a3a","#0b1722","#38bdf8","#e5f4ff","#a8c9df"),
            arrayOf("#12231b","#1a3025","#0b1710","#34d399","#e8fff0","#a9d7b8"),
            arrayOf("#2a1b14","#38251b","#1d120d","#fb923c","#fff2e8","#e7b99b"),
            arrayOf("#ffffff","#f2f3f5","#e3e5e8","#5865f2","#232428","#5c5e66"),
            arrayOf("#20242b","#292e38","#171a20","#9ca3af","#f3f4f6","#aab2c0"),
            arrayOf("#271b24","#35232f","#1b1119","#f472b6","#fff1f7","#d6a8c0"),
            arrayOf("#2b1719","#3a2023","#1c1012","#fb7185","#fff1f2","#e6a8ad"),
            arrayOf("#142720","#1b382d","#0c1913","#6ee7b7","#eafff4","#a6d8c0")
        )
        val p = palettes[theme].clone()
        prefs.getString("custom_accent", "")?.takeIf { Regex("^#[0-9a-fA-F]{6}$").matches(it) }?.let { p[3] = it }
        val rules = StringBuilder()
        rules.append(":root{color-scheme:" + (if(theme == 6) "light" else "dark") + ";}:root,body,#app-mount,.theme-dark,.theme-light{--background-primary:"+p[0]+"!important;--background-secondary:"+p[1]+"!important;--background-secondary-alt:"+p[1]+"!important;--background-tertiary:"+p[2]+"!important;--background-floating:"+p[1]+"!important;--channeltextarea-background:"+p[2]+"!important;--modal-background:"+p[1]+"!important;--header-primary:"+p[4]+"!important;--text-normal:"+p[4]+"!important;--header-secondary:"+p[5]+"!important;--text-muted:"+p[5]+"!important;--interactive-normal:"+p[5]+"!important;--brand-experiment:"+p[3]+"!important;--brand-experiment-560:"+p[3]+"!important;--button-secondary-background:"+p[2]+"!important;--scrollbar-thin-thumb:"+p[5]+"!important;}")
        rules.append("html,body,#app-mount{background:"+p[0]+"!important;color:"+p[4]+"!important;}")
        val fontFamilies = arrayOf("system-ui, sans-serif", "Arial, sans-serif", "Georgia, serif", "'Trebuchet MS', sans-serif", "Verdana, sans-serif", "monospace")
        val font = fontFamilies[prefs.getInt("font_choice", 0).coerceIn(0, fontFamilies.lastIndex)]
        rules.append("body,#app-mount{font-family:"+font+"!important;}")
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
        if(prefs.getBoolean("feature_compact",false)) rules.append("[class*=messageListItem]{padding-top:2px!important;padding-bottom:2px!important}[class*=messageContent]{line-height:1.25!important}")
        if(prefs.getBoolean("feature_cards",false)) rules.append("[class*=messageListItem]{border-radius:10px!important;margin:3px!important;background:rgba(255,255,255,.035)!important}")
        if(prefs.getBoolean("feature_bigtext",false)) rules.append("[class*=messageContent]{font-size:16px!important;line-height:1.45!important}")
        if(prefs.getBoolean("feature_noavatars",false)) rules.append("[class*=messageListItem] [class*=avatar]{display:none!important}")
        if(prefs.getBoolean("feature_hide_embeds",false)) rules.append("[class*=embedWrapper],[class*=embedFull]{display:none!important}")
        if(prefs.getBoolean("feature_hide_attachments",false)) rules.append("[class*=attachmentContentContainer],[class*=imageContent],[class*=videoContainer]{display:none!important}")
        if(prefs.getBoolean("feature_compact_channels",false)) rules.append("[class*=sidebar] [class*=containerDefault]{padding-top:0!important;padding-bottom:0!important}[class*=sidebar] [class*=name]{font-size:12px!important}")
        if(prefs.getBoolean("feature_hide_members",false)) rules.append("[class*=membersWrap]{display:none!important}")
        if(prefs.getBoolean("feature_hide_activity",false)) rules.append("[class*=activityPanel],[class*=nowPlayingColumn]{display:none!important}")
        if(prefs.getBoolean("feature_upsells",false)) rules.append("[class*=premiumPromo],[class*=nitroTopDivider],[class*=upsell],[class*=premiumUpsell]{display:none!important}")
        if(prefs.getBoolean("feature_reduce_motion",false)) rules.append("*,*::before,*::after{animation-duration:.001ms!important;animation-iteration-count:1!important;transition-duration:.001ms!important;scroll-behavior:auto!important}")
        if(prefs.getBoolean("feature_hide_blocked",false)) rules.append("[class*=blockedMessage],[class*=blockedSystemMessage]{display:none!important}")
        if(prefs.getBoolean("feature_hide_reactions",false)) rules.append("[class*=reactions]{display:none!important}")
        if(prefs.getBoolean("feature_hide_server_boost",false)) rules.append("[class*=guildBoost],[class*=boostedGuildTier]{display:none!important}")
        if(prefs.getBoolean("feature_hide_discover",false)) rules.append("[class*=discoveryUpsell],[class*=discoverUpsell]{display:none!important}")
        if(prefs.getBoolean("feature_hide_shop",false)) rules.append("[class*=shopButton],[aria-label*=Shop]{display:none!important}")
        if(prefs.getBoolean("feature_hide_gift",false)) rules.append("button[aria-label*=gift i],[class*=giftButton]{display:none!important}")
        if(prefs.getBoolean("feature_hide_gif",false)) rules.append("button[aria-label*=GIF i],[class*=gifButton]{display:none!important}")
        if(prefs.getBoolean("feature_hide_stickers",false)) rules.append("button[aria-label*=sticker i],[class*=stickerButton]{display:none!important}")
        if(prefs.getBoolean("feature_hide_typing",false)) rules.append("[class*=typing],[class*=typingIndicator]{display:none!important}")
        if(prefs.getBoolean("feature_hide_profile_banners",false)) rules.append("[class*=profileBanner]{display:none!important}")
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
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == fileChooserRequestCode) {
            val result = if (resultCode == RESULT_OK && data != null) {
                try {
                    WebChromeClient.FileChooserParams.parseResult(resultCode, data)
                } catch (_: Exception) {
                    data.clipData?.let { clip ->
                        Array(clip.itemCount) { index -> clip.getItemAt(index).uri }
                    } ?: data.data?.let { arrayOf(it) }
                }
            } else null
            pendingFileChooser?.onReceiveValue(result)
            pendingFileChooser = null
        }
    }

    override fun onPause() {
        super.onPause()
        if (::web.isInitialized) web.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (::web.isInitialized) web.onResume()
    }

    override fun onDestroy() {
        pendingFileChooser?.onReceiveValue(null)
        pendingFileChooser = null
        if (::web.isInitialized) {
            web.stopLoading()
            web.destroy()
        }
        super.onDestroy()
    }

    @Deprecated("Deprecated in Android")
    override fun onBackPressed() {
        // WebView often loses focus after tapping Discord controls, so focus is not a
        // reliable test for whether browser history can handle Back.
        if (::web.isInitialized && web.canGoBack()) web.goBack() else super.onBackPressed()
    }
}
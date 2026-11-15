package com.vencordmobile

import android.app.Activity
import android.app.AlertDialog
import android.text.InputType
import android.view.Gravity
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class CustomPlugins(private val activity: Activity, private val onChanged: (JSONArray) -> Unit) {
    private val prefs = activity.getSharedPreferences("vcm_prefs", Activity.MODE_PRIVATE)
    private fun load(): JSONArray = try { JSONArray(prefs.getString("custom_plugins", "[]")) } catch (_: Exception) { JSONArray() }
    private fun save(a: JSONArray) {
        val problem = PluginStoragePolicy.validate(a)
        if (problem != null) {
            Toast.makeText(activity, problem, Toast.LENGTH_LONG).show()
            return
        }
        prefs.edit().putString("custom_plugins", a.toString()).apply()
        onChanged(a)
    }
    private fun postToUi(action: () -> Unit) {
        if (activity.isFinishing || activity.isDestroyed) return
        activity.runOnUiThread {
            if (!activity.isFinishing && !activity.isDestroyed) action()
        }
    }
    fun open() {
        val a = load()
        val names = Array(a.length() + 3) { i ->
            when (i) {
                0 -> "Export plugin backup (${a.length()})"
                1 -> "Import plugin backup"
                2 -> "＋ Add plugin manually"
                else -> {
                    val p=a.optJSONObject(i - 3)
                    (if(p?.optBoolean("enabled",false)==true) "✓ " else "○ ")+(p?.optString("name","Plugin") ?: "Plugin")
                }
            }
        }
        AlertDialog.Builder(activity).setTitle("Plugins • ${a.length()} installed")
            .setItems(names) { _, i -> when(i) {
                0 -> exportBackup(a)
                1 -> importBackup(a)
                2 -> edit(a, -1)
                else -> manage(a, i - 3)
            } }
            .setPositiveButton("Install from URL") { _, _ -> installUrl(a) }
            .setNeutralButton("Disable all") { _, _ ->
                for (index in 0 until a.length()) a.optJSONObject(index)?.put("enabled", false)
                save(a)
                Toast.makeText(activity, "All plugins disabled", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Security info") { _, _ ->
                AlertDialog.Builder(activity).setTitle("Plugin security")
                    .setMessage("Plugins execute JavaScript inside your logged-in Discord page and can access account session/data. Only install sources you trust. Direct JS/GitHub .js files work; desktop Vencord/Revenge packages do not run natively.")
                    .setPositiveButton("I understand", null).show()
            }.show()
    }
    private fun exportBackup(a: JSONArray) {
        val json=JSONObject().put("format","NEXORA_PLUGIN_BACKUP_V1").put("plugins",a).toString(2)
        val cm=activity.getSystemService(Activity.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        cm.setPrimaryClip(android.content.ClipData.newPlainText("NEXORA plugin backup",json))
        Toast.makeText(activity,"Backup copied to clipboard (${a.length()} plugins)",Toast.LENGTH_LONG).show()
    }
    private fun importBackup(a: JSONArray) {
        val input=EditText(activity).apply { hint="Paste NEXORA plugin backup JSON"; minLines=8; gravity=Gravity.TOP; inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS }
        AlertDialog.Builder(activity).setTitle("Import plugin backup").setMessage("Paste exported JSON. Existing plugins remain; matching source/name entries are skipped.")
            .setView(input).setPositiveButton("Validate & import") { _, _ ->
                try {
                    val root=JSONObject(input.text.toString())
                    if(root.optString("format")!="NEXORA_PLUGIN_BACKUP_V1") throw Exception("Unknown backup format")
                    val incoming=root.optJSONArray("plugins") ?: throw Exception("Missing plugins array")
                    val merged=JSONArray()
                    for(i in 0 until a.length()) merged.put(a.get(i))
                    var added=0
                    for(i in 0 until incoming.length()) {
                        val p=incoming.optJSONObject(i) ?: continue
                        val duplicate=(0 until merged.length()).any { j ->
                            val q=merged.optJSONObject(j)
                            q?.optString("source","")==p.optString("source","") && q?.optString("name","")==p.optString("name","")
                        }
                        if(!duplicate){merged.put(JSONObject(p.toString()));added++}
                    }
                    val problem=PluginStoragePolicy.validate(merged)
                    if(problem!=null) throw Exception(problem)
                    AlertDialog.Builder(activity).setTitle("Import ${added} plugins?").setMessage("Validated ${incoming.length()} entries. ${added} new plugins will be added. Imported JavaScript can access your Discord page/session.")
                        .setPositiveButton("Import") { _, _ -> save(merged);Toast.makeText(activity,"Imported ${added} plugins",Toast.LENGTH_LONG).show() }
                        .setNegativeButton("Cancel",null).show()
                } catch(e:Exception) {
                    AlertDialog.Builder(activity).setTitle("Import failed").setMessage(e.message ?: "Invalid JSON backup").setPositiveButton("OK",null).show()
                }
            }.setNegativeButton("Cancel",null).show()
    }
    private fun installUrl(a: JSONArray) {
        val urlBox=EditText(activity).apply { hint="GitHub .js URL or raw.githubusercontent.com URL"; inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI }
        AlertDialog.Builder(activity).setTitle("Install plugin from URL").setMessage("Paste a direct JavaScript file link (GitHub file links ending in .js are supported).")
            .setView(urlBox).setPositiveButton("Download") { _, _ ->
                val entered=urlBox.text.toString().trim()
                if(entered.isBlank()){ Toast.makeText(activity,"Paste a URL",Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                Toast.makeText(activity,"Downloading plugin…",Toast.LENGTH_SHORT).show()
                thread {
                    try {
                        val raw=PluginSourceUrl.toRawUrl(entered)
                        val downloaded=SecurePluginFetcher.fetch(raw,500000,"NEXORA-PluginLoader")
                        val code=downloaded.text
                        if(code.isBlank()) throw Exception("The plugin file is empty")
                        val name=raw.substringAfterLast('/').substringBefore('?').removeSuffix(".js").ifBlank{"Imported plugin"}
                        postToUi {
                            val existing=(0 until a.length()).any { index ->
                                a.optJSONObject(index)?.optString("source","") == entered
                            }
                            if(existing) {
                                Toast.makeText(activity,"That plugin URL is already installed",Toast.LENGTH_LONG).show()
                            } else {
                                AlertDialog.Builder(activity).setTitle("Review plugin: " + name)
                                    .setMessage("Final source: " + downloaded.finalUrl + "\nSize: " + code.toByteArray(Charsets.UTF_8).size + " bytes\n\nThis JavaScript will execute inside your signed-in Discord page and may access account data. Install only if you trust this source.")
                                    .setPositiveButton("Install") { _, _ ->
                                        a.put(JSONObject().put("name",name).put("source",entered).put("code",code).put("enabled",true))
                                        save(a)
                                        Toast.makeText(activity,"Installed: $name. Discord is reloading to apply it.",Toast.LENGTH_LONG).show()
                                    }
                                    .setNegativeButton("Cancel",null).show()
                            }
                        }
                    } catch(e:Exception) {
                        postToUi { AlertDialog.Builder(activity).setTitle("Plugin download failed").setMessage(e.message ?: "Could not download this URL. Use a direct public .js file link.").setPositiveButton("OK",null).show() }
                    }
                }
            }.setNegativeButton("Cancel",null).show()
    }

    private fun manage(a: JSONArray, index: Int) {
        val plugin = a.optJSONObject(index) ?: return
        val enabled = plugin.optBoolean("enabled", false)
        val source=plugin.optString("source","")
        val actions=mutableListOf(if(enabled) "Disable plugin" else "Enable plugin","Edit plugin code","Reload Discord and run enabled plugins","Delete plugin")
        if(source.startsWith("https://")) actions.add("Update from source URL")
        AlertDialog.Builder(activity).setTitle(plugin.optString("name","Plugin"))
            .setItems(actions.toTypedArray()) { _, action ->
                when(action) {
                    0 -> { plugin.put("enabled",!enabled);save(a);Toast.makeText(activity,if(!enabled)"Enabled. Reloading Discord…" else "Disabled. Reloading Discord…",Toast.LENGTH_SHORT).show() }
                    1 -> edit(a,index)
                    2 -> { onChanged(a);Toast.makeText(activity,"Discord is reloading to run enabled plugins",Toast.LENGTH_SHORT).show() }
                    3 -> AlertDialog.Builder(activity).setMessage("Delete "+plugin.optString("name","this plugin")+"?").setPositiveButton("Delete") { _, _ -> a.remove(index);save(a) }.setNegativeButton("Cancel",null).show()
                    4 -> updateFromSource(plugin,a,index)
                }
            }.show()
    }
    private fun updateFromSource(plugin:JSONObject,all:JSONArray,index:Int) {
        val source=plugin.optString("source","")
        Toast.makeText(activity,"Checking plugin source…",Toast.LENGTH_SHORT).show()
        thread {
            try {
                val result=SecurePluginFetcher.fetch(PluginSourceUrl.toRawUrl(source),500000,"NEXORA-PluginUpdater")
                require(result.text.isNotBlank()){"Downloaded source is empty"}
                postToUi {
                    AlertDialog.Builder(activity).setTitle("Review update: "+plugin.optString("name","Plugin")).setMessage("Final URL: "+result.finalUrl+"\nNew size: "+result.text.toByteArray(Charsets.UTF_8).size+" bytes\n\nReplace installed code with this source?").setPositiveButton("Update") { _, _ ->
                        plugin.put("code",result.text).put("enabled",true)
                        all.put(index,plugin)
                        save(all)
                        Toast.makeText(activity,"Updated plugin. Reloading…",Toast.LENGTH_SHORT).show()
                    }.setNegativeButton("Cancel",null).show()
                }
            } catch(e:Exception) { postToUi { AlertDialog.Builder(activity).setTitle("Update failed").setMessage(e.message ?: "Could not fetch update").setPositiveButton("OK",null).show() } }
        }
    }
    private fun edit(a: JSONArray, index: Int) {
        val old = if(index>=0) a.optJSONObject(index) ?: JSONObject() else JSONObject()
        val name=EditText(activity).apply { hint="Plugin name"; setText(old.optString("name","")) }
        val code=EditText(activity).apply { hint="JavaScript code"; minLines=10; gravity=Gravity.TOP; inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS; setText(old.optString("code","")) }
        val box=LinearLayout(activity).apply { orientation=LinearLayout.VERTICAL; setPadding(24,8,24,0); addView(name); addView(code, LinearLayout.LayoutParams(-1, activity.resources.displayMetrics.heightPixels/3)) }
        val builder=AlertDialog.Builder(activity).setTitle(if(index<0) "New plugin" else "Edit plugin").setView(box)
            .setPositiveButton("Save") { _,_ ->
                val pluginName = name.text.toString().trim().ifBlank { "My plugin" }.take(80)
                val pluginCode = code.text.toString()
                val codeSize = pluginCode.toByteArray(Charsets.UTF_8).size
                if (pluginCode.isBlank()) {
                    Toast.makeText(activity, "Plugin code cannot be empty", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                if (codeSize > 500000) {
                    Toast.makeText(activity, "Plugin code exceeds the 500 KB limit", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                val p=JSONObject().put("name",pluginName).put("code",pluginCode).put("enabled",true)
                old.optString("source","").takeIf { it.isNotBlank() }?.let { p.put("source",it) }
                if(index<0) a.put(p) else a.put(index,p)
                save(a)
                Toast.makeText(activity,"Saved. Discord is reloading to apply changes.",Toast.LENGTH_SHORT).show()
            }.setNegativeButton("Cancel",null)
        if(index>=0) builder.setNeutralButton("Delete") { _,_ -> a.remove(index); save(a) }
        builder.show()
    }
}

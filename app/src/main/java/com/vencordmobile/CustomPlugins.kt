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
        val names = Array(a.length() + 1) { i ->
            if (i == 0) "＋ Add plugin manually" else {
                val p=a.optJSONObject(i - 1)
                (if(p?.optBoolean("enabled",false)==true) "✓ " else "○ ")+(p?.optString("name","Plugin") ?: "Plugin")
            }
        }
        AlertDialog.Builder(activity).setTitle("Plugins")
            .setItems(names) { _, i -> if (i == 0) edit(a, -1) else manage(a, i - 1) }
            .setPositiveButton("Install from URL") { _, _ -> installUrl(a) }
            .setNeutralButton("Disable all") { _, _ ->
                for (index in 0 until a.length()) a.optJSONObject(index)?.put("enabled", false)
                save(a)
                Toast.makeText(activity, "All plugins disabled", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Security info") { _, _ ->
                AlertDialog.Builder(activity).setTitle("Plugin security")
                    .setMessage("Plugins execute JavaScript inside your logged-in Discord page and can access your account session/data. Only install sources you trust. This loader accepts direct JavaScript files or GitHub links to a .js file, not native Vencord/Revenge plugin packages.")
                    .setPositiveButton("I understand", null).show()
            }.show()
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
        val actions = arrayOf(if(enabled) "Disable plugin" else "Enable plugin", "Edit plugin code", "Reload Discord and run enabled plugins", "Delete plugin")
        AlertDialog.Builder(activity).setTitle(plugin.optString("name", "Plugin"))
            .setItems(actions) { _, action ->
                when(action) {
                    0 -> { plugin.put("enabled", !enabled); save(a); Toast.makeText(activity, if(!enabled) "Enabled. Reloading Discord…" else "Disabled. Reloading Discord…", Toast.LENGTH_SHORT).show() }
                    1 -> edit(a, index)
                    2 -> { onChanged(a); Toast.makeText(activity,"Discord is reloading to run enabled plugins",Toast.LENGTH_SHORT).show() }
                    3 -> AlertDialog.Builder(activity).setMessage("Delete ${plugin.optString("name", "this plugin")}? ").setPositiveButton("Delete") { _, _ -> a.remove(index); save(a) }.setNegativeButton("Cancel",null).show()
                }
            }.show()
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

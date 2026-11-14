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
    private fun save(a: JSONArray) { prefs.edit().putString("custom_plugins", a.toString()).apply(); onChanged(a) }
    fun open() {
        val a = load()
        val names = Array(a.length()) { i -> val p=a.optJSONObject(i); (if(p?.optBoolean("enabled",false)==true) "✓ " else "○ ")+(p?.optString("name","Plugin") ?: "Plugin") }
        AlertDialog.Builder(activity).setTitle("Plugins")
            .setItems(names) { _, i -> manage(a, i) }
            .setPositiveButton("Install from URL") { _, _ -> installUrl(a) }
            .setNeutralButton("Add manually") { _, _ -> edit(a, -1) }
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
                        val raw=toRawUrl(entered)
                        val conn=(URL(raw).openConnection() as HttpURLConnection).apply { connectTimeout=15000; readTimeout=20000; setRequestProperty("User-Agent","VencordMobile-PluginLoader") }
                        val status=conn.responseCode
                        if(status !in 200..299) throw Exception("HTTP $status")
                        val code=conn.inputStream.bufferedReader().use { it.readText() }
                        conn.disconnect()
                        if(code.isBlank() || code.length>500000) throw Exception("Empty file or larger than 500 KB")
                        val name=raw.substringAfterLast('/').substringBefore('?').removeSuffix(".js").ifBlank{"Imported plugin"}
                        activity.runOnUiThread {
                            a.put(JSONObject().put("name",name).put("source",entered).put("code",code).put("enabled",true))
                            save(a)
                            Toast.makeText(activity,"Installed: $name. Reload Discord to run it.",Toast.LENGTH_LONG).show()
                        }
                    } catch(e:Exception) {
                        activity.runOnUiThread { AlertDialog.Builder(activity).setTitle("Plugin download failed").setMessage(e.message ?: "Could not download this URL. Use a direct public .js file link.").setPositiveButton("OK",null).show() }
                    }
                }
            }.setNegativeButton("Cancel",null).show()
    }
    private fun toRawUrl(input:String):String {
        require(input.startsWith("https://")) { "Only HTTPS URLs are allowed" }
        if(input.contains("github.com/") && input.contains("/blob/")) {
            val parts=input.removePrefix("https://github.com/").split("/")
            require(parts.size>=5) { "Use a GitHub link to a specific .js file" }
            return "https://raw.githubusercontent.com/${parts[0]}/${parts[1]}/${parts[3]}/"+parts.drop(4).joinToString("/")
        }
        require(input.contains("raw.githubusercontent.com/") || input.endsWith(".js")) { "Use a direct .js URL or GitHub file link" }
        return input
    }
    private fun manage(a: JSONArray, index: Int) {
        val plugin = a.optJSONObject(index) ?: return
        val enabled = plugin.optBoolean("enabled", false)
        val actions = arrayOf(if(enabled) "Disable plugin" else "Enable plugin", "Edit plugin code", "Run enabled plugins now", "Delete plugin")
        AlertDialog.Builder(activity).setTitle(plugin.optString("name", "Plugin"))
            .setItems(actions) { _, action ->
                when(action) {
                    0 -> { plugin.put("enabled", !enabled); save(a); Toast.makeText(activity, if(!enabled) "Plugin enabled" else "Plugin disabled", Toast.LENGTH_SHORT).show() }
                    1 -> edit(a, index)
                    2 -> { onChanged(a); Toast.makeText(activity,"Enabled plugins reloaded",Toast.LENGTH_SHORT).show() }
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
                val p=JSONObject().put("name",name.text.toString().ifBlank{"My plugin"}).put("code",code.text.toString()).put("enabled",true)
                if(index<0) a.put(p) else a.put(index,p); save(a)
                Toast.makeText(activity,"Saved. Reload Discord to run it.",Toast.LENGTH_SHORT).show()
            }.setNegativeButton("Cancel",null)
        if(index>=0) builder.setNeutralButton("Delete") { _,_ -> a.remove(index); save(a) }
        builder.show()
    }
}

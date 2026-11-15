package com.vencordmobile

import android.app.Activity
import android.app.AlertDialog
import android.text.InputType
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class PluginManager(private val activity: Activity, private val openFeature: (Int) -> Unit) {
    private val prefs = activity.getSharedPreferences("vcm_prefs", Activity.MODE_PRIVATE)
    private val features = arrayOf("Install / manage JavaScript plugins", "Themes & appearance", "Custom CSS editor", "Chat & message tools", "Cosmetic lab", "Rich Presence preview")
    fun open() {
        AlertDialog.Builder(activity).setTitle("NEXORA • Extensions")
            .setMessage("Manage local add-ons and interface tools. Downloaded plugins execute inside your signed-in Discord page and may access page data. Only install code you trust. Desktop Vencord, Revenge and Aliucord packages are not directly compatible.")
            .setItems(features) { _, index -> openFeature(index) }
            .setNeutralButton("Plugin repositories") { _, _ -> repositories() }
            .setNegativeButton("Close", null).show()
    }
    private fun repositories() {
        val saved = try { JSONArray(prefs.getString("plugin_repos", "[]")) } catch (_: Exception) { JSONArray() }
        val input = EditText(activity).apply { hint = "https://raw.githubusercontent.com/user/repo/main/plugins.json"; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI }
        AlertDialog.Builder(activity).setTitle("Plugin marketplace")
            .setMessage("Add a public JSON manifest. Expected format: {\"plugins\":[{\"name\":\"Example\",\"description\":\"What it does\",\"url\":\"https://.../plugin.js\"}]}\nOnly install plugins you trust. They run inside your logged-in Discord page.")
            .setView(input)
            .setPositiveButton("Fetch repository") { _, _ ->
                val url = input.text.toString().trim()
                if (!url.startsWith("https://")) { Toast.makeText(activity,"HTTPS repository URL required",Toast.LENGTH_SHORT).show() }
                else {
                    if (!saved.toString().contains(url)) saved.put(url)
                    prefs.edit().putString("plugin_repos",saved.toString()).apply()
                    fetchRepository(url)
                }
            }
            .setNeutralButton("Saved repositories") { _, _ ->
                val names = Array(saved.length()) { i -> saved.optString(i) }
                if (names.isEmpty()) Toast.makeText(activity,"No saved repositories",Toast.LENGTH_SHORT).show()
                else AlertDialog.Builder(activity).setTitle("Saved repositories").setItems(names) { _, i -> fetchRepository(names[i]) }.setNegativeButton("Close",null).show()
            }
            .setNegativeButton("Close", null).show()
    }

    private fun fetchRepository(manifestUrl: String) {
        Toast.makeText(activity,"Fetching plugin list…",Toast.LENGTH_SHORT).show()
        thread {
            try {
                val body = SecurePluginFetcher.fetch(manifestUrl,1000000,"NEXORA-PluginManager").text
                val entries = JSONObject(body).optJSONArray("plugins") ?: throw Exception("Manifest needs a plugins array")
                activity.runOnUiThread {
                    if (entries.length() == 0) { Toast.makeText(activity,"No plugins in this repository",Toast.LENGTH_SHORT).show(); return@runOnUiThread }
                    val names = Array(entries.length()) { i -> val p=entries.optJSONObject(i) ?: JSONObject(); p.optString("name","Unnamed plugin") + "  •  " + p.optString("description", "No description") }
                    AlertDialog.Builder(activity).setTitle("Repository plugins").setItems(names) { _, index -> installRepositoryPlugin(entries.optJSONObject(index) ?: JSONObject()) }.setNegativeButton("Close",null).show()
                }
            } catch (e: Exception) { activity.runOnUiThread { AlertDialog.Builder(activity).setTitle("Repository error").setMessage(e.message ?: "Could not read repository").setPositiveButton("OK",null).show() } }
        }
    }

    private fun installRepositoryPlugin(plugin: JSONObject) {
        val name = plugin.optString("name", "Imported plugin")
        val url = plugin.optString("url", "")
        if (!url.startsWith("https://") || !url.endsWith(".js")) { Toast.makeText(activity,"Plugin URL must be an HTTPS .js file",Toast.LENGTH_LONG).show(); return }
        AlertDialog.Builder(activity).setTitle("Install $name?").setMessage("${plugin.optString("description", "No description")}\n\nThis code will execute in your signed-in Discord page and may access account data. Install only if you trust its author.").setPositiveButton("Install") { _, _ ->
            thread {
                try {
                    val downloaded = SecurePluginFetcher.fetch(url,500000,"NEXORA-PluginManager")
                    val code = downloaded.text
                    require(code.isNotBlank()) { "Plugin file is empty" }
                    val finalUrl = downloaded.finalUrl
                    activity.runOnUiThread {
                        val all = try { JSONArray(prefs.getString("custom_plugins", "[]")) } catch (_: Exception) { JSONArray() }
                        val duplicate = (0 until all.length()).any { index ->
                            all.optJSONObject(index)?.optString("source","") == url
                        }
                        if(duplicate) {
                            Toast.makeText(activity,"That plugin URL is already installed",Toast.LENGTH_LONG).show()
                        } else {
                            AlertDialog.Builder(activity).setTitle("Review plugin: " + name)
                                .setMessage("Final source: " + finalUrl + "\nSize: " + code.toByteArray(Charsets.UTF_8).size + " bytes\n\nThis JavaScript runs inside your signed-in Discord page and may access account data. Install only if you trust the author.")
                                .setPositiveButton("Install") { _, _ ->
                                    all.put(JSONObject().put("name",name).put("source",url).put("code",code).put("enabled",true))
                                    prefs.edit().putString("custom_plugins",all.toString()).apply()
                                    Toast.makeText(activity,"Installed $name. Reload Discord to run it.",Toast.LENGTH_LONG).show()
                                }
                                .setNegativeButton("Cancel",null).show()
                        }
                    }
                } catch (e: Exception) { activity.runOnUiThread { Toast.makeText(activity,"Install failed: ${e.message}",Toast.LENGTH_LONG).show() } }
            }
        }.setNegativeButton("Cancel",null).show()
    }
}

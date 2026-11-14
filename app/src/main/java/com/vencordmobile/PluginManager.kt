package com.vencordmobile

import android.app.Activity
import android.app.AlertDialog
import android.text.InputType
import android.widget.*
import org.json.JSONArray

class PluginManager(private val activity: Activity, private val openFeature: (Int) -> Unit) {
    private val prefs = activity.getSharedPreferences("vcm_prefs", Activity.MODE_PRIVATE)
    private val features = arrayOf("Rich Presence editor (local preview)", "Themes & appearance", "Custom CSS editor", "Chat & message tools", "Privacy controls")
    fun open() {
        AlertDialog.Builder(activity).setTitle("Vencord Mobile • Features")
            .setItems(features) { _, index -> openFeature(index) }
            .setNeutralButton("Plugin repositories") { _, _ -> repositories() }
            .setNegativeButton("Compatibility") { _, _ ->
                AlertDialog.Builder(activity).setTitle("Plugin compatibility")
                    .setMessage("Vencord, Revenge and Aliucord have different plugin APIs and runtimes. This app cannot directly run their plugins. This menu opens Vencord Mobile's own features; repository URLs are saved for reference only and are not downloaded or executed. Rich Presence is a local preview/configuration, not a Discord status.")
                    .setPositiveButton("OK", null).show()
            }.show()
    }
    private fun repositories() {
        val arr = try { JSONArray(prefs.getString("plugin_repos", "[]")) } catch (_: Exception) { JSONArray() }
        val input = EditText(activity).apply {
            hint = "https://example.com/plugins.json"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        }
        AlertDialog.Builder(activity).setTitle("Plugin repositories")
            .setMessage("Saved URLs (not fetched or executed):\n\n$arr")
            .setView(input)
            .setPositiveButton("Add URL") { _, _ ->
                val url = input.text.toString().trim()
                if (url.startsWith("https://") && !arr.toString().contains(url)) {
                    arr.put(url); prefs.edit().putString("plugin_repos", arr.toString()).apply()
                    Toast.makeText(activity, "Repository saved", Toast.LENGTH_SHORT).show()
                } else Toast.makeText(activity, "Enter a new HTTPS URL", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Clear all") { _, _ -> prefs.edit().putString("plugin_repos", "[]").apply() }
            .setNegativeButton("Close", null).show()
    }
}

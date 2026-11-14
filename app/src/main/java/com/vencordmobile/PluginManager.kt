package com.vencordmobile

import android.app.Activity
import android.app.AlertDialog
import android.text.InputType
import android.view.Gravity
import android.widget.*
import org.json.JSONArray

class PluginManager(private val activity: Activity) {
    private val prefs = activity.getSharedPreferences("vcm_prefs", Activity.MODE_PRIVATE)
    private val builtins = listOf("Custom RPC (configuration)", "Theme manager", "Custom CSS", "Compact chat", "Message tools", "Profile cosmetics preview", "Privacy controls")
    private val keys = listOf("rpc", "themes", "cssplugin", "compact", "messages", "profilepreview", "privacy")
    fun open() {
        val labels = builtins.mapIndexed { i, s -> (if (prefs.getBoolean("builtin_"+keys[i], i < 3)) "✓ " else "○ ") + s }.toTypedArray()
        AlertDialog.Builder(activity).setTitle("Built-in plugins")
            .setMultiChoiceItems(labels, keys.mapIndexed { i,k -> prefs.getBoolean("builtin_"+k, i<3) }.toBooleanArray()) { _, i, checked ->
                prefs.edit().putBoolean("builtin_"+keys[i],checked).apply()
            }.setPositiveButton("Save",null).setNeutralButton("Repositories") { _,_ -> repositories() }
            .setNegativeButton("Compatibility info") { _,_ ->
                AlertDialog.Builder(activity).setTitle("Plugin compatibility")
                    .setMessage("Vencord (Web/desktop), Revenge (Android) and Aliucord (Android) use different plugin APIs. This app currently offers native built-in settings and does not execute third-party plugins from those ecosystems. Compatibility adapters require a compatible Discord client runtime, API mapping and security review.")
                    .setPositiveButton("OK",null).show()
            }.show()
    }
    private fun repositories() {
        val existing = prefs.getString("plugin_repos","[]") ?: "[]"
        val arr = try { JSONArray(existing) } catch (_:Exception) { JSONArray() }
        val input = EditText(activity).apply { hint="https://example.com/plugins.json"; inputType=InputType.TYPE_TEXT_VARIATION_URI }
        val list = arr.toString()
        AlertDialog.Builder(activity).setTitle("Plugin repositories")
            .setMessage("Saved repository URLs (not fetched/executed in this build):\n$list")
            .setView(input).setPositiveButton("Add URL") { _,_ ->
                val u=input.text.toString().trim()
                if (u.startsWith("https://")) { arr.put(u); prefs.edit().putString("plugin_repos",arr.toString()).apply(); Toast.makeText(activity,"Repository URL saved",Toast.LENGTH_SHORT).show() }
                else Toast.makeText(activity,"Use an HTTPS URL",Toast.LENGTH_SHORT).show()
            }.setNeutralButton("Clear") { _,_ -> prefs.edit().putString("plugin_repos","[]").apply() }
            .setNegativeButton("Close",null).show()
    }
}
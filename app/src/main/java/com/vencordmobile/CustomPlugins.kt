package com.vencordmobile

import android.app.Activity
import android.app.AlertDialog
import android.text.InputType
import android.view.Gravity
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject

class CustomPlugins(private val activity: Activity, private val onChanged: (JSONArray) -> Unit) {
    private val prefs = activity.getSharedPreferences("vcm_prefs", Activity.MODE_PRIVATE)
    private fun load(): JSONArray = try { JSONArray(prefs.getString("custom_plugins", "[]")) } catch (_: Exception) { JSONArray() }
    private fun save(a: JSONArray) { prefs.edit().putString("custom_plugins", a.toString()).apply(); onChanged(a) }
    fun open() {
        val a = load()
        val names = Array(a.length()) { i -> val p=a.optJSONObject(i); (if(p?.optBoolean("enabled",false)==true) "✓ " else "○ ")+(p?.optString("name","Plugin") ?: "Plugin") }
        AlertDialog.Builder(activity).setTitle("JavaScript plugins • experimental")
            .setItems(names) { _, i -> edit(a, i) }
            .setPositiveButton("Add plugin") { _, _ -> edit(a, -1) }
            .setNeutralButton("Enable / disable") { _, _ -> toggle(a) }
            .setNegativeButton("Security info") { _, _ ->
                AlertDialog.Builder(activity).setTitle("Plugin security")
                    .setMessage("Plugins run JavaScript inside your logged-in Discord WebView and can access page data and act as you. Only install code you trust. These are Vencord Mobile scripts, not automatically compatible Vencord/Revenge/Aliucord plugins.")
                    .setPositiveButton("I understand", null).show()
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
    private fun toggle(a:JSONArray) {
        if(a.length()==0){ Toast.makeText(activity,"Add a plugin first",Toast.LENGTH_SHORT).show(); return }
        val labels=Array(a.length()){i->a.optJSONObject(i)?.optString("name","Plugin") ?: "Plugin"}
        val checked=BooleanArray(a.length()){i->a.optJSONObject(i)?.optBoolean("enabled",false)==true}
        AlertDialog.Builder(activity).setTitle("Enabled plugins").setMultiChoiceItems(labels,checked){_,i,on->a.optJSONObject(i)?.put("enabled",on)}
            .setPositiveButton("Save"){_,_->save(a)}.show()
    }
}

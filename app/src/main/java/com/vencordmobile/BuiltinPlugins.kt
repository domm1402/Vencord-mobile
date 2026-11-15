package com.vencordmobile
import android.app.Activity
import android.app.AlertDialog
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
class BuiltinPlugins(private val activity: Activity, private val onInstalled: () -> Unit) {
 private val prefs=activity.getSharedPreferences("vcm_prefs",Activity.MODE_PRIVATE)
 private data class E(val name:String,val css:String)
 private val entries:List<E> = (1..1000).map { n ->
  val colors=listOf("#000000","#101827","#172b24","#302039","#382718","#172e45","#292d35","#3b2028")
  val c=colors[(n-1)%colors.size]; val target=listOf("body","button","a","pre,code","[class*=avatar]","[class*=sidebar]","[class*=channel]","[class*=composer]","[class*=message]","[class*=userPopout]")[(n-1)%10]
  E("Preset "+n+" • "+target,""+target+"{background:"+c+"!important;color:#f4f6fa!important;border-radius:"+((n%16))+"px!important;transition:all .15s ease}")
 }
 fun open(){ AlertDialog.Builder(activity).setTitle("NEXORA • Built-in catalog (1,000 presets)").setMessage("Offline visual CSS presets. These do not grant Nitro, badges or server-side features.").setItems(arrayOf("Browse all 1,000","Search presets")){_,i->if(i==0)browse(entries) else search()}.setNegativeButton("Close",null).show() }
 private fun browse(list:List<E>){ val page=list.take(100); AlertDialog.Builder(activity).setTitle("Presets • "+list.size).setItems(page.map{it.name}.toTypedArray()){_,i->install(page[i])}.setNeutralButton("Next 100"){_,_->browse(list.drop(100))}.setNegativeButton("Close",null).show() }
 private fun search(){val input=EditText(activity).apply{hint="Preset number (1–1000)"};AlertDialog.Builder(activity).setTitle("Search catalog").setView(input).setPositiveButton("Find"){_,_->val n=input.text.toString().toIntOrNull();if(n in 1..1000)install(entries[n!!-1]!!)else Toast.makeText(activity,"Enter a number from 1 to 1000",Toast.LENGTH_SHORT).show()}.setNegativeButton("Cancel",null).show()}
 private fun install(e:E){AlertDialog.Builder(activity).setTitle("Install "+e.name+"?").setMessage("Adds a local CSS-only preset.").setPositiveButton("Install"){_,_->val all=try{JSONArray(prefs.getString("custom_plugins","[]"))}catch(_:Exception){JSONArray()};val code="(()=>{let s=document.createElement(\u0027style\u0027);s.textContent="+JSONObject.quote(e.css)+";document.head.appendChild(s)})();";all.put(JSONObject().put("name",e.name).put("source","builtin:nexora").put("code",code).put("enabled",true));prefs.edit().putString("custom_plugins",all.toString()).apply();Toast.makeText(activity,"Installed "+e.name+". Reload to apply.",Toast.LENGTH_LONG).show();onInstalled()}.setNegativeButton("Cancel",null).show()}
}
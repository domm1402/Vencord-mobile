package com.vencordmobile
import android.app.Activity
import android.app.AlertDialog
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
class BuiltinPlugins(private val activity:Activity,private val onInstalled:()->Unit){
 private val prefs=activity.getSharedPreferences("vcm_prefs",Activity.MODE_PRIVATE)
 private data class P(val name:String,val cat:String,val desc:String,val code:String)
 private fun css(s:String)="(()=>{let e=document.createElement('style');e.dataset.nexora='1';e.textContent="+JSONObject.quote(s)+";document.head.append(e)})()"
 private val list=listOf(
 P("Readable message width","Appearance","Comfortable line length",css("[class*=messageContent]{max-width:min(78ch,100%)!important}")),
 P("Compact messages","Appearance","Reduce message spacing",css("[class*=messageListItem]{padding:2px 0!important}")),
 P("Large chat font","Accessibility","Increase message size",css("[class*=messageContent]{font-size:16px!important;line-height:1.55!important}")),
 P("Focus outlines","Accessibility","Keyboard focus visibility",css(":focus-visible{outline:2px solid #5b9cff!important}")),
 P("Disable animations","Accessibility","Remove transitions",css("*,*::before,*::after{animation:none!important;transition:none!important}")),
 P("Underline links","Appearance","Improve link visibility",css("a{text-decoration:underline!important}")),
 P("Square avatars","Appearance","Square avatar corners",css("img[class*=avatar],[class*=avatar] img{border-radius:5px!important}")),
 P("Hide reactions","Messages","Hide reaction rows",css("[class*=reactions]{display:none!important}")),
 P("Hide typing","Interface","Hide typing indicator",css("[class*=typing]{display:none!important}")),
 P("Hide Nitro promos","Interface","Hide promotion panels",css("[class*=premiumTab],[class*=upsell]{display:none!important}")),
 P("Hide member list","Interface","Collapse members",css("[class*=membersWrap]{display:none!important}[class*=chatContent]{flex:1!important}")),
 P("Hide channel sidebar","Interface","Hide channel list",css("[class*=sidebar]{display:none!important}")),
 P("Hide embeds","Messages","Hide link embeds",css("[class*=embed]{display:none!important}")),
 P("Hide inline media","Messages","Hide chat images/video",css("[class*=messageContent] img,[class*=messageContent] video{display:none!important}")),
 P("Highlight mentions","Messages","Emphasize mentions",css("[class*=mention]{background:rgba(88,101,242,.25)!important;border-radius:4px}")),
 P("Code borders","Developer","Outline code blocks",css("pre{border:1px solid var(--border-subtle)!important;padding:10px!important;border-radius:8px}")),
 P("Monospace code","Developer","Readable code font",css("pre,code{font-family:ui-monospace,Consolas,monospace!important}")),
 P("Wide chat","Appearance","Expand message width",css("[class*=messageContent]{max-width:100%!important}")),
 P("Hide stickers","Messages","Hide stickers",css("[class*=sticker]{display:none!important}")),
 P("Violet accent","Themes","Local violet accent","document.documentElement.style.setProperty('--brand-experiment','#8b5cf6')"),
 P("Teal accent","Themes","Local teal accent","document.documentElement.style.setProperty('--brand-experiment','#14b8a6')"),
 P("Pink accent","Themes","Local pink accent","document.documentElement.style.setProperty('--brand-experiment','#ec4899')"),
 P("Orange accent","Themes","Local orange accent","document.documentElement.style.setProperty('--brand-experiment','#f97316')"),
 P("Timestamp hover","Messages","Show full timestamp on hover","document.querySelectorAll('time').forEach(e=>e.title=new Date(e.dateTime||Date.now()).toLocaleString())"),
 P("Copy message","Messages","Add copy buttons","document.querySelectorAll('[class*=messageContent]').forEach(e=>{if(e.dataset.nxc)return;e.dataset.nxc=1;let b=document.createElement('button');b.textContent='Copy';b.onclick=()=>navigator.clipboard.writeText(e.innerText);e.append(b)})"),
 P("Jump latest","Navigation","Scroll to bottom","document.querySelector('[class*=messagesWrapper]')?.scrollTo({top:999999,behavior:'smooth'})"),
 P("Jump top","Navigation","Scroll to top","document.querySelector('[class*=messagesWrapper]')?.scrollTo({top:0,behavior:'smooth'})"),
 P("Mask usernames","Privacy","Replace visible names locally","document.querySelectorAll('[class*=username]').forEach(e=>{if(!e.dataset.nxm){e.dataset.nxm=1;e.textContent='User'}})"),
 P("Blur spoilers","Privacy","Blur spoiler images; click to reveal","document.querySelectorAll('[class*=spoiler] img').forEach(e=>{e.style.filter='blur(12px)';e.onclick=()=>e.style.filter='none'})"),
 P("Hide profile effects","Profiles","Hide profile effect visuals",css("[class*=profileEffects]{display:none!important}")),
 P("Hide soundboard","Interface","Hide soundboard",css("[class*=soundboard]{display:none!important}")),
 P("Smooth scroll","Accessibility","Smooth scrolling",css("*{scroll-behavior:smooth!important}")),
 P("Highlight unread","Messages","Emphasize unread divider",css("[class*=unread]{border-top:2px solid #ed4245!important;color:#ed4245!important}")),
 P("Compact toolbar","Appearance","Reduce toolbar spacing",css("[class*=toolbar] button{padding:4px!important;margin:0 2px!important}")),
 P("Hide boost prompts","Interface","Hide boost prompts",css("[class*=boostPrompt],[class*=guildBoost]{display:none!important}")),
 P("Hide game invites","Interface","Hide activity cards",css("[class*=activityInvite],[class*=gameInvite]{display:none!important}")),
 P("High contrast text","Accessibility","Boost text contrast",css("[class*=messageContent]{text-shadow:0 0 .01px currentColor}")),
 P("Select message text","Messages","Enable text selection",css("[class*=messageContent]{user-select:text!important}")),
 P("Hide call controls","Interface","Hide call controls",css("[class*=callContainer]{display:none!important}")),
 P("Hide online dots","Profiles","Dim presence dots",css("[class*=status]{opacity:.25!important}"))
 )
 fun open(){AlertDialog.Builder(activity).setTitle("NEXORA functional plugin library ("+list.size+")").setItems(arrayOf("Browse categories","Search","Installed plugins","Safety info")){_,i->when(i){0->cats();1->search();2->installed();else->info()}}.setNegativeButton("Close",null).show()}
 private fun data()=try{JSONArray(prefs.getString("custom_plugins","[]"))}catch(_:Exception){JSONArray()}
 private fun cats(){val c=list.map{it.cat}.distinct().sorted();AlertDialog.Builder(activity).setTitle("Categories").setItems(c.toTypedArray()){_,i->browse(list.filter{it.cat==c[i]})}.setNegativeButton("Back",null).show()}
 private fun search(){val e=EditText(activity).apply{hint="Search plugins"};AlertDialog.Builder(activity).setTitle("Search").setView(e).setPositiveButton("Find"){_,_->val q=e.text.toString().lowercase();browse(list.filter{it.name.lowercase().contains(q)||it.desc.lowercase().contains(q)||it.cat.lowercase().contains(q)})}.setNegativeButton("Cancel",null).show()}
 private fun browse(a:List<P>){if(a.isEmpty()){Toast.makeText(activity,"No results",0).show();return};AlertDialog.Builder(activity).setTitle("Select plugin ("+a.size+")").setItems(a.map{it.name+" • "+it.desc}.toTypedArray()){_,i->install(a[i])}.setNegativeButton("Back",null).show()}
 private fun install(p:P){AlertDialog.Builder(activity).setTitle(p.name).setMessage(p.cat+"\n"+p.desc+"\n\nRuns locally in Discord WebView.").setPositiveButton("Install"){_,_->val a=data();a.put(JSONObject().put("name","NEXORA • "+p.name).put("source","builtin:nexora:"+p.name).put("code",p.code).put("enabled",true));val err=PluginStoragePolicy.validate(a);if(err!=null){Toast.makeText(activity,err,1).show();return@setPositiveButton};prefs.edit().putString("custom_plugins",a.toString()).apply();Toast.makeText(activity,"Installed "+p.name+"; reload…",1).show();onInstalled()}.setNegativeButton("Cancel",null).show()}
 private fun installed(){val a=data();val ix=(0 until a.length()).filter{a.optJSONObject(it)?.optString("source","")?.startsWith("builtin:nexora:")==true};if(ix.isEmpty()){Toast.makeText(activity,"No built-ins installed",0).show();return};AlertDialog.Builder(activity).setTitle("Installed").setItems(ix.map{a.optJSONObject(it)?.optString("name")?:"Plugin"}.toTypedArray()){_,i->val n=ix[i];val p=a.optJSONObject(n)!!;AlertDialog.Builder(activity).setTitle(p.optString("name")).setItems(arrayOf(if(p.optBoolean("enabled",true))"Disable" else "Enable","Delete")){_,j->if(j==0)p.put("enabled",!p.optBoolean("enabled",true))else a.remove(n);prefs.edit().putString("custom_plugins",a.toString()).apply();onInstalled();installed()}.setNegativeButton("Cancel",null).show()}.setNegativeButton("Back",null).show()}
 private fun info(){AlertDialog.Builder(activity).setTitle("Plugin safety").setMessage("These are actual executable WebView add-ons and can access page/session data. Effects are local only; they do not grant Nitro, badges or permissions. Disable/delete in Installed plugins.").setPositiveButton("OK",null).show()}
}
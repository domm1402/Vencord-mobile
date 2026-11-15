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
 ,
 P("Hide message toolbar","Interface","Hide hover action bar",css("[class*=buttonContainer]{display:none!important}")),
 P("Hide reply previews","Messages","Hide quoted replies",css("[class*=repliedMessage]{display:none!important}")),
 P("Hide thread indicators","Interface","Hide thread badges",css("[class*=threadIcon],[class*=threadMessage]{display:none!important}")),
 P("Hide channel descriptions","Interface","Hide channel topic panel",css("[class*=channelTopic]{display:none!important}")),
 P("Hide pinned banner","Interface","Hide pinned message banner",css("[class*=pinnedMessageBar]{display:none!important}")),
 P("Hide search suggestions","Interface","Hide search suggestion panel",css("[class*=searchSuggestion]{display:none!important}")),
 P("Bigger buttons","Accessibility","Increase button hit targets",css("button{min-height:30px!important}")),
 P("Roomier line spacing","Accessibility","Increase message line height",css("[class*=messageContent]{line-height:1.7!important}")),
 P("Reduced motion","Accessibility","Respect reduced-motion preference",css("@media(prefers-reduced-motion:reduce){*{animation:none!important;transition:none!important;scroll-behavior:auto!important}}")),
 P("Dim inactive channels","Appearance","Dim channel entries",css("[class*=channel]:not([class*=selected]){opacity:.82}")),
 P("Emphasize selected channel","Appearance","Highlight active channel",css("[class*=selected][class*=channel]{background:rgba(88,101,242,.22)!important}")),
 P("Hide server icons","Appearance","Hide server rail icons",css("[class*=guilds] img{opacity:.25!important}")),
 P("Round message corners","Appearance","Round message blocks",css("[class*=messageListItem]{border-radius:7px!important}")),
 P("Remove message shadows","Appearance","Remove drop shadows",css("*{box-shadow:none!important}")),
 P("Darken code blocks","Developer","Dark code block background",css("pre{background:#101116!important}")),
 P("Wrap long code","Developer","Wrap long code lines",css("pre{white-space:pre-wrap!important;overflow-wrap:anywhere!important}")),
 P("Larger code text","Developer","Increase code font size",css("pre,code{font-size:14px!important}")),
 P("Highlight links on hover","Appearance","Underline hovered links",css("a:hover{text-decoration:underline!important;filter:brightness(1.2)}")),
 P("Hide bot tags","Messages","Hide BOT labels",css("[class*=botTag]{display:none!important}")),
 P("Hide usernames","Messages","Hide author labels",css("[class*=username]{display:none!important}")),
 P("Hide avatars in chat","Appearance","Hide message avatars",css("[class*=avatar]{display:none!important}")),
 P("Full-width embeds","Messages","Expand embeds to message width",css("[class*=embed]{max-width:100%!important}")),
 P("Limit embed height","Messages","Clamp tall embeds",css("[class*=embed]{max-height:420px!important;overflow:auto!important}")),
 P("Hide attachment previews","Messages","Hide file preview cards",css("[class*=attachment]{display:none!important}")),
 P("Hide invite cards","Messages","Hide invite embeds",css("[class*=inviteEmbed]{display:none!important}")),
 P("Hide poll cards","Messages","Hide poll blocks",css("[class*=poll]{display:none!important}")),
 P("Hide emoji reactions count","Messages","Hide reaction counts",css("[class*=reactionCount]{display:none!important}")),
 P("Hide member avatars","Interface","Hide avatars in member list",css("[class*=membersWrap] img{display:none!important}")),
 P("Hide DM activity","Interface","Hide DM activity banners",css("[class*=activityPanel]{display:none!important}")),
 P("Disable text shadows","Appearance","Remove text shadow effects",css("*{text-shadow:none!important}")),
 P("Custom accent: cyan","Themes","Apply cyan accent locally","document.documentElement.style.setProperty('--brand-experiment','#06b6d4')"),
 P("Custom accent: red","Themes","Apply red accent locally","document.documentElement.style.setProperty('--brand-experiment','#ef4444')"),
 P("Custom accent: yellow","Themes","Apply yellow accent locally","document.documentElement.style.setProperty('--brand-experiment','#eab308')"),
 P("Custom accent: indigo","Themes","Apply indigo accent locally","document.documentElement.style.setProperty('--brand-experiment','#6366f1')"),
 P("Custom accent: lime","Themes","Apply lime accent locally","document.documentElement.style.setProperty('--brand-experiment','#84cc16')"),
 P("Custom accent: slate","Themes","Apply slate accent locally","document.documentElement.style.setProperty('--brand-experiment','#64748b')"),
 P("Hide status text","Profiles","Hide custom status text",css("[class*=customStatus]{display:none!important}")),
 P("Hide profile banners","Profiles","Hide profile banner images",css("[class*=profileBanner]{display:none!important}")),
 P("Hide profile bio","Profiles","Hide profile about section",css("[class*=userProfile] [class*=aboutMe]{display:none!important}")),
 P("Hide profile badges","Profiles","Hide profile badge row locally",css("[class*=profileBadges]{display:none!important}")),
 P("Hide member role icons","Profiles","Hide role icons",css("[class*=roleIcon]{display:none!important}")),
 P("Hide channel unread pills","Interface","Hide unread counters",css("[class*=unreadBadge]{display:none!important}")),
 P("Hide channel mute icons","Interface","Hide muted channel icons",css("[class*=muted]{opacity:.5!important}")),
 P("Hide separators","Appearance","Remove horizontal separators",css("hr,[class*=separator]{opacity:.25!important}")),
 P("Reduce sidebar width","Appearance","Narrow channel sidebar",css("[class*=sidebar]{width:220px!important;min-width:220px!important}")),
 P("Increase sidebar width","Appearance","Widen channel sidebar",css("[class*=sidebar]{width:300px!important;min-width:300px!important}")),
 P("Pin composer bottom","Interface","Keep composer visible",css("[class*=chatContent]{display:flex!important;flex-direction:column!important}[class*=channelTextArea]{position:sticky!important;bottom:0!important}"))
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
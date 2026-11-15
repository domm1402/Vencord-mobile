// NEXORA Community plugin: Larger emoji
// Category: Appearance
// Increase standalone emoji size in chat.
// CSS-only local tweak. No network calls, account data access, or Discord API actions.
(()=>{if(document.getElementById("nxp-bigger-emoji"))return;const s=document.createElement("style");s.id="nxp-bigger-emoji";s.textContent="[class*=emoji]{min-width:1.4em;min-height:1.4em}";document.head.appendChild(s)})();

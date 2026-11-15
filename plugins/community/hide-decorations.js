// NEXORA Community plugin: Hide profile decorations
// Category: Minimal
// Hide avatar decorations locally.
// CSS-only local tweak. No network calls, account data access, or Discord API actions.
(()=>{if(document.getElementById("nxp-hide-decorations"))return;const s=document.createElement("style");s.id="nxp-hide-decorations";s.textContent="[class*=avatarDecoration]{display:none!important}";document.head.appendChild(s)})();

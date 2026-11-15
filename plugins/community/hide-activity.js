// NEXORA Community plugin: Hide activity cards
// Category: Focus
// Hide activity and game status panels in member lists.
// CSS-only local tweak. No network calls, account data access, or Discord API actions.
(()=>{if(document.getElementById("nxp-hide-activity"))return;const s=document.createElement("style");s.id="nxp-hide-activity";s.textContent="[class*=activity] [class*=details]{display:none!important}";document.head.appendChild(s)})();

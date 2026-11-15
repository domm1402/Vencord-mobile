// NEXORA Community plugin: Hide typing indicators
// Category: Focus
// Hide typing indicators to reduce visual noise.
// CSS-only local tweak. No network calls, account data access, or Discord API actions.
(()=>{if(document.getElementById("nxp-hide-typing"))return;const s=document.createElement("style");s.id="nxp-hide-typing";s.textContent="[class*=typing]{display:none!important}";document.head.appendChild(s)})();

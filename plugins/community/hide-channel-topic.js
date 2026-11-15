// NEXORA Community plugin: Hide channel topic
// Category: Minimal
// Hide channel topic banners.
// CSS-only local tweak. No network calls, account data access, or Discord API actions.
(()=>{if(document.getElementById("nxp-hide-channel-topic"))return;const s=document.createElement("style");s.id="nxp-hide-channel-topic";s.textContent="[class*=topic]{display:none!important}";document.head.appendChild(s)})();

// NEXORA Community plugin: Hide status dots
// Category: Minimal
// Remove online status dots from avatars.
// CSS-only local tweak. No network calls, account data access, or Discord API actions.
(()=>{if(document.getElementById("nxp-hide-status-dots"))return;const s=document.createElement("style");s.id="nxp-hide-status-dots";s.textContent="[class*=status]{display:none!important}";document.head.appendChild(s)})();

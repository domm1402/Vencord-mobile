// NEXORA Community plugin: Square corners
// Category: Appearance
// Reduce rounded corners across common cards and buttons.
// CSS-only local tweak. No network calls, account data access, or Discord API actions.
(()=>{if(document.getElementById("nxp-square-corners"))return;const s=document.createElement("style");s.id="nxp-square-corners";s.textContent="[class*=card],[class*=container],[class*=button]{border-radius:6px!important}";document.head.appendChild(s)})();

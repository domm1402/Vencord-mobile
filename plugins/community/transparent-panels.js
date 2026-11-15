// NEXORA Community plugin: Transparent panels
// Category: Appearance
// Reduce solid panel backgrounds for a lighter interface.
// CSS-only local tweak. No network calls, account data access, or Discord API actions.
(()=>{if(document.getElementById("nxp-transparent-panels"))return;const s=document.createElement("style");s.id="nxp-transparent-panels";s.textContent="[class*=sidebar],[class*=members]{background:transparent!important}";document.head.appendChild(s)})();

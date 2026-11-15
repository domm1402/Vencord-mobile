// NEXORA Community plugin: Visible scrollbars
// Category: Accessibility
// Make scrollbars easier to spot.
// CSS-only local tweak. No network calls, account data access, or Discord API actions.
(()=>{if(document.getElementById("nxp-scrollbar-visible"))return;const s=document.createElement("style");s.id="nxp-scrollbar-visible";s.textContent="*{scrollbar-width:thin!important}::-webkit-scrollbar{width:10px!important;height:10px!important}";document.head.appendChild(s)})();

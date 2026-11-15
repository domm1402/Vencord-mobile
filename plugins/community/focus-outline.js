// NEXORA Community plugin: Keyboard focus outlines
// Category: Accessibility
// Make keyboard focus easier to see.
// CSS-only local tweak. No network calls, account data access, or Discord API actions.
(()=>{if(document.getElementById("nxp-focus-outline"))return;const s=document.createElement("style");s.id="nxp-focus-outline";s.textContent=":focus-visible{outline:2px solid #5865f2!important;outline-offset:2px!important}";document.head.appendChild(s)})();

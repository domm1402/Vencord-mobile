// NEXORA Community plugin: Sticky message composer
// Category: Layout
// Keep the message composer visible while scrolling.
// CSS-only local tweak. No network calls, account data access, or Discord API actions.
(()=>{if(document.getElementById("nxp-sticky-composer"))return;const s=document.createElement("style");s.id="nxp-sticky-composer";s.textContent="[class*=channelTextArea]{position:sticky!important;bottom:0!important;z-index:5}";document.head.appendChild(s)})();

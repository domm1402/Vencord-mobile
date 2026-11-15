// NEXORA Community plugin: High contrast
// Category: Accessibility
// Boost contrast for text and interactive controls.
// CSS-only local tweak. No network calls, account data access, or Discord API actions.
(()=>{if(document.getElementById("nxp-high-contrast"))return;const s=document.createElement("style");s.id="nxp-high-contrast";s.textContent="body{filter:contrast(1.12)}";document.head.appendChild(s)})();

// NEXORA Community plugin: Message separators
// Category: Layout
// Add subtle separators between messages.
// CSS-only local tweak. No network calls, account data access, or Discord API actions.
(()=>{if(document.getElementById("nxp-message-borders"))return;const s=document.createElement("style");s.id="nxp-message-borders";s.textContent="[class*=messageListItem]{border-bottom:1px solid color-mix(in srgb,currentColor 8%,transparent)}";document.head.appendChild(s)})();

// NEXORA Community plugin: Hide message avatars
// Category: Layout
// Hide avatars to create a compact text-first chat.
// CSS-only local tweak. No network calls, account data access, or Discord API actions.
(()=>{if(document.getElementById("nxp-hide-avatar"))return;const s=document.createElement("style");s.id="nxp-hide-avatar";s.textContent="[class*=messageListItem] [class*=avatar]{visibility:hidden!important}";document.head.appendChild(s)})();

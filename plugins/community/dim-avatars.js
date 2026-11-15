// NEXORA Community plugin: Dim avatars
// Category: Focus
// Tone down avatar prominence.
// CSS-only local tweak. No network calls, account data access, or Discord API actions.
(()=>{if(document.getElementById("nxp-dim-avatars"))return;const s=document.createElement("style");s.id="nxp-dim-avatars";s.textContent="[class*=avatar]{opacity:.72} [class*=avatar]:hover{opacity:1}";document.head.appendChild(s)})();

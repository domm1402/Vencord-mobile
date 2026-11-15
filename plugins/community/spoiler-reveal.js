// NEXORA Community plugin: Reveal spoilers
// Category: Convenience
// Show spoiler contents inline (use with care in public channels).
// CSS-only local tweak. No network calls, account data access, or Discord API actions.
(()=>{if(document.getElementById("nxp-spoiler-reveal"))return;const s=document.createElement("style");s.id="nxp-spoiler-reveal";s.textContent="[class*=spoiler] [class*=hidden]{filter:none!important;opacity:1!important} [class*=spoiler]{background:transparent!important}";document.head.appendChild(s)})();

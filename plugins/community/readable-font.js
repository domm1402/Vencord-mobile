// NEXORA Community plugin: Readable typography
// Category: Accessibility
// Use a clean system font and improve line spacing.
// CSS-only local tweak. No network calls, account data access, or Discord API actions.
(()=>{if(document.getElementById("nxp-readable-font"))return;const s=document.createElement("style");s.id="nxp-readable-font";s.textContent="body{font-family:system-ui,-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif!important} [class*=messageContent]{line-height:1.55!important}";document.head.appendChild(s)})();

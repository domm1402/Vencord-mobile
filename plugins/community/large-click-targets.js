// NEXORA Community plugin: Larger click targets
// Category: Accessibility
// Increase hit areas for common buttons.
// CSS-only local tweak. No network calls, account data access, or Discord API actions.
(()=>{if(document.getElementById("nxp-large-click-targets"))return;const s=document.createElement("style");s.id="nxp-large-click-targets";s.textContent="button,[role=button]{min-height:32px}";document.head.appendChild(s)})();

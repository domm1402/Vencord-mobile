// NEXORA Community plugin: Wrap code blocks
// Category: Readability
// Prevent long code lines from overflowing horizontally.
// CSS-only local tweak. No network calls, account data access, or Discord API actions.
(()=>{if(document.getElementById("nxp-code-wrap"))return;const s=document.createElement("style");s.id="nxp-code-wrap";s.textContent="pre,code{white-space:pre-wrap!important;overflow-wrap:anywhere!important}";document.head.appendChild(s)})();

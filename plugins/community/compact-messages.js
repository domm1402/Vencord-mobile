// NEXORA Community plugin: Compact messages
// Category: Layout
// Reduce vertical gaps between messages.
// CSS-only local tweak. No network calls, account data access, or Discord API actions.
(()=>{if(document.getElementById("nxp-compact-messages"))return;const s=document.createElement("style");s.id="nxp-compact-messages";s.textContent="[class*=messageListItem]{padding-top:2px!important;padding-bottom:2px!important}";document.head.appendChild(s)})();

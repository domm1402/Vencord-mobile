// NEXORA Community plugin: Wide chat
// Category: Layout
// Use more horizontal space for messages.
// CSS-only local tweak. No network calls, account data access, or Discord API actions.
(()=>{if(document.getElementById("nxp-wide-chat"))return;const s=document.createElement("style");s.id="nxp-wide-chat";s.textContent="[class*=chatContent] [class*=messagesWrapper]{max-width:none!important} [class*=messageContent]{max-width:none!important}";document.head.appendChild(s)})();

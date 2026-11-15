// NEXORA Community plugin: Underline links
// Category: Readability
// Always underline links in chat for clarity.
// CSS-only local tweak. No network calls, account data access, or Discord API actions.
(()=>{if(document.getElementById("nxp-underline-links"))return;const s=document.createElement("style");s.id="nxp-underline-links";s.textContent="[class*=messageContent] a{text-decoration:underline!important;text-underline-offset:2px}";document.head.appendChild(s)})();

// NEXORA Community plugin: Hide promotional upsells
// Category: Focus
// Hide common Nitro upsell banners locally.
// CSS-only local tweak. No network calls, account data access, or Discord API actions.
(()=>{if(document.getElementById("nxp-hide-nitro-upsells"))return;const s=document.createElement("style");s.id="nxp-hide-nitro-upsells";s.textContent="[class*=upsell],[class*=premiumBanner]{display:none!important}";document.head.appendChild(s)})();

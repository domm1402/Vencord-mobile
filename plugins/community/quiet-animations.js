// NEXORA Community plugin: Reduce motion
// Category: Accessibility
// Disable nonessential Discord animations and transitions.
// CSS-only local tweak. No network calls, account data access, or Discord API actions.
(()=>{if(document.getElementById("nxp-quiet-animations"))return;const s=document.createElement("style");s.id="nxp-quiet-animations";s.textContent="*,*::before,*::after{animation-duration:.001ms!important;animation-iteration-count:1!important;transition-duration:.001ms!important;scroll-behavior:auto!important}";document.head.appendChild(s)})();

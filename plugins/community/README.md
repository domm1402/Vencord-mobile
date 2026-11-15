# NEXORA Community Starter Pack

A starter collection of lightweight, local UI plugins for NEXORA/Vencord Mobile's HTTPS plugin repository importer. These are CSS-only tweaks; selectors can change when Discord updates its interface.

## Install

In NEXORA: Extensions → Plugin repositories → Fetch repository, paste:

`https://raw.githubusercontent.com/tiktoknotificationtroll-wq/Vencord-mobile/main/plugins/community/plugins.json`

Choose a plugin, inspect its source URL/code and permissions warning, then install. Reload Discord after installation. Uninstall/disable any plugin that causes layout issues.

## Safety

All entries here only inject CSS into the Discord page. They do not call Discord APIs, send messages, read tokens, or make network requests. CSS selectors are best-effort and may stop matching after Discord UI updates. Never install arbitrary third-party JavaScript unless you trust its author.

## Included

- **Reduce motion** (Accessibility): Disable nonessential Discord animations and transitions. [source](./quiet-animations.js)
- **High contrast** (Accessibility): Boost contrast for text and interactive controls. [source](./high-contrast.js)
- **Readable typography** (Accessibility): Use a clean system font and improve line spacing. [source](./readable-font.js)
- **Wide chat** (Layout): Use more horizontal space for messages. [source](./wide-chat.js)
- **Compact messages** (Layout): Reduce vertical gaps between messages. [source](./compact-messages.js)
- **Hide message avatars** (Layout): Hide avatars to create a compact text-first chat. [source](./hide-avatar.js)
- **Square corners** (Appearance): Reduce rounded corners across common cards and buttons. [source](./square-corners.js)
- **Underline links** (Readability): Always underline links in chat for clarity. [source](./underline-links.js)
- **Wrap code blocks** (Readability): Prevent long code lines from overflowing horizontally. [source](./code-wrap.js)
- **Reveal spoilers** (Convenience): Show spoiler contents inline (use with care in public channels). [source](./spoiler-reveal.js)
- **Dim avatars** (Focus): Tone down avatar prominence. [source](./dim-avatars.js)
- **Hide typing indicators** (Focus): Hide typing indicators to reduce visual noise. [source](./hide-typing.js)
- **Hide activity cards** (Focus): Hide activity and game status panels in member lists. [source](./hide-activity.js)
- **Message separators** (Layout): Add subtle separators between messages. [source](./message-borders.js)
- **Larger emoji** (Appearance): Increase standalone emoji size in chat. [source](./bigger-emoji.js)
- **Keyboard focus outlines** (Accessibility): Make keyboard focus easier to see. [source](./focus-outline.js)
- **Hide profile decorations** (Minimal): Hide avatar decorations locally. [source](./hide-decorations.js)
- **Hide status dots** (Minimal): Remove online status dots from avatars. [source](./hide-status-dots.js)
- **Transparent panels** (Appearance): Reduce solid panel backgrounds for a lighter interface. [source](./transparent-panels.js)
- **Sticky message composer** (Layout): Keep the message composer visible while scrolling. [source](./sticky-composer.js)
- **Hide channel topic** (Minimal): Hide channel topic banners. [source](./hide-channel-topic.js)
- **Hide promotional upsells** (Focus): Hide common Nitro upsell banners locally. [source](./hide-nitro-upsells.js)
- **Larger click targets** (Accessibility): Increase hit areas for common buttons. [source](./large-click-targets.js)
- **Visible scrollbars** (Accessibility): Make scrollbars easier to spot. [source](./scrollbar-visible.js)

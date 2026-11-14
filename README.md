# Vencord Mobile

An independent Android Discord WebView shell with local interface customisation. It is not affiliated with Discord or the Vencord project.

## Features

- Discord web app in an Android WebView, with swipe refresh and Android file downloads.
- Modern dark Control Centre, mobile/desktop page mode, navigation and find-on-page.
- Seven built-in themes, custom CSS, compact messages, avatar controls, rounded message cards, and a feature library with 22 local chat/layout toggles.
- Feature library controls for embeds, attachments, reactions, blocked-message placeholders, typing indicators, GIF/sticker/gift buttons, shop/discovery/boost promotions, profile banners, motion reduction and more.
- JavaScript plugin installer for direct HTTPS `.js` links and GitHub file links.
- JSON plugin repository browser.
- Quick reaction emoji copy palette and selected-text translation.
- Rich Presence activity builder, local preview and JSON export.
- Local profile badge/Nitro-style cosmetic preview.

## Plugin repository format

Host a public HTTPS JSON file with this structure:

```json
{
  "plugins": [
    {
      "name": "Example plugin",
      "description": "Short description",
      "url": "https://example.com/example.js"
    }
  ]
}
```

Plugin JavaScript executes in the signed-in Discord page and can access page data. Only install code you trust. This loader is for this app's own JavaScript format, not Vencord desktop, Revenge or Aliucord packages.

## Important limitations

- Local cosmetic previews do not grant genuine Discord badges, Nitro or account entitlements.
- Rich Presence publishing is experimental and unverified. The editor can save and preview activity data, but the internal Discord dispatcher approach is not a supported public API and may not work across Discord builds. Do not assume an activity has published unless it appears in Discord itself.
- Discord's interface changes frequently; custom CSS selectors and plugins may need updates.

## Feature scope

This project prioritises working, testable features over artificial APK size or empty feature counts. A larger APK is not inherently better; bundled assets, libraries and offline resources should only be added when they provide real functionality. The feature library currently provides 22 selectable local interface controls. Desktop Vencord plugins that depend on native desktop APIs are not automatically compatible with Android WebView.

## Build

GitHub Actions builds the debug APK. Open the Actions tab, select a successful Android build, then download its APK artifact.
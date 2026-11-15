# NEXORA

**Current version: 2.4**

A redesigned Android Discord client shell with a full-screen NEXORA control deck with local interface customisation. It is not affiliated with Discord or the Vencord project.

## NEXORA 2.4

NEXORA 2.2 added duplicate-safe plugin execution per page, bounded manual plugin editing, main-page network error feedback and WebView lifecycle handling. NEXORA 2.3 hardened backup restore and download filenames. NEXORA 2.4 bounds and validates marketplace manifests, limits saved repositories, and preserves the page-zoom preference in safe backups. The app also includes native Android file selection for Discord uploads and attachment flows, secure extension fetching and WebView hardening. NEXORA 2.0 introduced a full-screen, four-section control deck with Home, Appearance, Tools and Extensions. It has its own name, launcher icon, colour identity and redesigned navigation. It remains an independent Discord WebView client shell, not an official Discord client.

## Features

- Discord web app in an Android WebView, with swipe refresh, Android file downloads, a native download history and Android file-picker support for uploads/attachments. Third-party links open outside the signed-in Discord WebView.
- Modern dark Control Centre, mobile/desktop page mode, navigation and find-on-page.
- Eleven built-in themes, custom CSS, compact messages, avatar controls, rounded message cards, and a feature library with 21 local chat/layout toggles.
- Feature library controls for embeds, attachments, reactions, blocked-message placeholders, typing indicators, GIF/sticker/gift buttons, shop/discovery/boost promotions, profile banners, motion reduction and more.
- JavaScript plugin installer for direct HTTPS `.js` links and GitHub file links, with bounded downloads, strict HTTPS URL validation, redirect protection and duplicate-install checks.
- JSON plugin repository browser with the same secure download handling.
- Quick reaction emoji copy palette, selected-text translation, and a Discord Message Studio with formatting shortcuts.
- Snippet Vault for reusable replies and templates, saved locally on-device.
- Searchable bookmarks, Discord quick navigation, reader mode and adjustable text zoom.
- Eight built-in CSS style presets for rounded, compact, bubble, minimal, high-contrast, developer and glass-like layouts.
- Categorised emoji library covering faces, reactions, hearts, hands, animals, food, objects and symbols.
- Custom accent colours and six interface font choices.
- Developer Toolbox for JSON formatting, URL encoding/decoding, Base64 conversion, Discord Snowflake timestamps, and text statistics.
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

Plugin JavaScript executes in the signed-in Discord page and can access page data. The app limits download sizes, blocks insecure redirect downgrades, shows the final source and requires a final install confirmation. Only install code you trust. This loader is for this app's own JavaScript format, not Vencord desktop, Revenge or Aliucord packages.

## Important limitations

- Settings backup/restore excludes executable plugin code so importing a settings file cannot silently install scripts. Existing installed plugins are left untouched during restore.
- Local cosmetic previews do not grant genuine Discord badges, Nitro or account entitlements.
- Rich Presence publishing is experimental and unverified. The editor can save and preview activity data, but the internal Discord dispatcher approach is not a supported public API and may not work across Discord builds. Do not assume an activity has published unless it appears in Discord itself.
- Discord's interface changes frequently; custom CSS selectors and plugins may need updates.

## Feature scope

This project prioritises working, testable features over artificial APK size or empty feature counts. A larger APK is not inherently better; bundled assets, libraries and offline resources should only be added when they provide real functionality. The feature library currently provides 21 selectable local interface controls. Desktop Vencord plugins that depend on native desktop APIs are not automatically compatible with Android WebView.

## Build

GitHub Actions builds the debug APK. Open the Actions tab, select a successful Android build, then download its APK artifact.
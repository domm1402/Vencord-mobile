# Vencord Mobile

An independent Android Discord WebView shell with local interface customisation. It is not affiliated with Discord or the Vencord project.

## Features

- Discord web app in an Android WebView, with swipe refresh and Android file downloads.
- Modern dark Control Centre, mobile/desktop page mode, navigation and find-on-page.
- Themes, custom CSS, compact messages, avatar hiding, rounded message cards and local promotional-panel hiding.
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
- The Rich Presence builder saves and previews configuration. Android Discord does not expose the desktop RPC IPC interface, so this app cannot publish a live Discord Rich Presence status on its own.
- Discord's interface changes frequently; custom CSS selectors and plugins may need updates.

## Build

GitHub Actions builds the debug APK. Open the Actions tab, select a successful Android build, then download its APK artifact.
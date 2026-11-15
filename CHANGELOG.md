# Changelog

## 2.18

- Wire 12 invite, event, call, popout, tooltip and link-button filters into the CSS engine.
- Bump Android version code to 22 and version name to 2.18.


## 2.17

- Normalize all remaining palette array separators after CI caught escaped newline literals.
- Bump Android version code to 21 and version name to 2.17.


## 2.16

- Fix a malformed theme palette separator caught by Kotlin CI.
- Restore and verify all accessibility CSS controls, and add 10 channel/embed customisations.
- Bump Android version code to 20 and version name to 2.16.


## 2.15

- Restore and wire 15 accessibility toggles plus add 10 embed/channel controls: line-height, keyboard focus, larger touch targets, reaction filtering, role chips, embed metadata, attachment names and channel labels.
- Bump Android version code to 19 and version name to 2.15.


## 2.14

- Add five one-tap feature profiles for recommended cleanup, focus, accessibility, minimal UI and developer workspace.
- Bump Android version code to 18 and version name to 2.14.

## 2.13

- Add 15 accessibility and visual tuning switches including readable line spacing, larger targets, visible keyboard focus, dim secondary content, reaction filtering, and bot badge suppression.
- Bump Android version code to 17 and version name to 2.13.

## 2.12

- Add 12 focus and layout controls for banners, channel topics, unread badges, online member rows, blur, transparency, focus mode, high contrast, sticky composer and thread controls.
- Bump Android version code to 16 and version name to 2.12.


## 2.11

- Add 12 more focus/layout controls: promotional banners, channel topics, unread pills, online member rows, server labels, blur, transparency, focus mode, contrast, sticky composer, thread actions and animated backgrounds.
- Bump Android version code to 15 and version name to 2.11.


## 2.10

- Add 12 advanced feature-library switches with CSS implementations for stage/event panels, status dots, activity cards, message dividers, inline code, spoilers, stickers, mentions, links, corners and hover effects.
- Bump Android version code to 14 and version name to 2.10.


## 2.9

- Expand emoji library from 8 to 17 categories, adding Discord palette favourites, weather, space, plants, sports, travel, music, celebration and flags.
- Bump Android version code to 13 and version name to 2.9.


## 2.8

- Expand the theme gallery from 11 to 20 colour palettes, including Nord, Dracula, Catppuccin, Cyberpunk, Solarized, and Matrix-inspired palettes.
- Expand typography choices from 6 to 12 CSS font stacks.
- Bump Android version code to 12 and version name to 2.8.


## 2.7

- Reject malformed UTF-8 plugin downloads rather than silently replacing invalid bytes.
- Canonicalize GitHub blob links using exact host matching, complete path checks, standard HTTPS ports, and bounded URL input.
- Apply the secure plugin-fetch URL policy before showing the install review dialog.
- Validate plugin metadata types and bounds before saving: names, source URLs, enabled flags, individual code size, collection count, and aggregate code size.
- Expand automated coverage for redirect destinations, local/IP plugin URLs, malformed GitHub paths, UTF-8 decoding, metadata validation, and storage/URL size boundaries.
- Bump Android version code to 11 and version name to 2.7.

## 2.6

- Add dashboard screen-reader labels and selected-tab state.
- Add JVM JSON test support and stricter download filename cleanup.
- Keep settings restore from silently installing executable plugin code.

# Changelog

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

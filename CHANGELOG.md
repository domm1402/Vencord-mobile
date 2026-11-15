# Changelog

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

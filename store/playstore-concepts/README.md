# Rafeeq Play Store concept review

Three first-slide concepts at 1080 × 1920 are in `previews/a.png`, `b.png`,
`c.png`. `previews/comparison.png` presents them together. Choose a direction
before producing the eight final slides. The theme slide will use dark mode;
the remainder will use light mode.

- A: Ivory Architecture — warm ivory, sage arch, angled metal phone.
- B: Everyday Teal — bold typography, teal field, opposite phone angle.
- C: Sand Editorial — warm stone, feature annotations, offset phone.

All concepts use `captures/home-light.png`, freshly captured from the Samsung
S21 after installing the icon fix on 24 November 2026. No earlier listing
screenshots or precomposited phone mockups are used. UI and device status bar
remain from the actual capture. Phone framing, perspective, typography and
backgrounds are reproducible HTML/CSS, rendered with Playwright.

Run `node store/playstore-concepts/render.mjs` from the repository root.
The renderer currently uses the existing Playwright installation in the sibling
Budget-AI project and installed Chrome.

## Native icon fix

Before the fix, the installed 2.0.0 (54) APK matched the latest local release
byte for byte. The shared text-based icon renderer clipped glyph outlines.
`ui/Icons.kt` now measures glyph ink bounds, centres it, and fits oversized ink
within the requested dp square. The release was built and installed in place;
app data was preserved. All four tab states and dark mode were inspected on
the Samsung. The app was returned to light mode. Verification crops are in
`native/tool/verification/nav-icons/`.

Fresh post-fix captures: home-light, home-dark, quran-list-light,
qibla-light, reader-light, prayer-light. `home-current.png` is a pre-fix
reference only and must not be used in deliverables.

Pending: selected direction, remaining feature captures, eight final exports.

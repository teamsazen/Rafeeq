# Five-phone feature graphic

User-requested replacement for the text-led feature graphic: exactly five
realistic phones in a layered stack, emerald campaign background, and no
external headline, captions, badges or branding. Each screen carries a different
real app feature. Text naturally present inside the app remains unchanged.

The studio plate is generated with built-in imagegen; exact prompt is in
`imagegen-prompt.txt`. Screenshot composition preserves the fresh Samsung UI,
including the requested matching padding above the status bar.

Left to right: Quran reader, prayer times, home, 16-line Mushaf, Qibla.

Final file: `../out/feature-graphic-five-phones-1024x500.png` (RGB 1024 × 500).
Review: `../previews/feature-five-phones-review.png`, full, 512px and 320px sizes.
Updated bundle: `../rafeeq-playstore-complete-v2.zip` (8 screenshots + this banner).
The previous text-led banner is preserved but excluded from the updated bundle.

`quads.json` records the five full display quadrilaterals in original plate
coordinates, including portions naturally hidden behind the foreground phones.
The compositing mask replaces only visible green display pixels, preserving
phone overlap and metal edges. `composite_five.py` orders screens left-to-right
and includes shaded chroma regions to avoid green seams. `export.py` crops the
composition minimally to the exact 1024 × 500 ratio and exports RGB.

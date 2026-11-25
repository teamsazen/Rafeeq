# Rafeeq — In Your Hands / Emerald — final eight slides

## Deliverables

- `out/01-home.png` — app introduction
- `out/02-quran.png` — Quran reading and recitation
- `out/03-prayer.png` — prayer times and reminders
- `out/04-mushaf.png` — 16-line IndoPak Mushaf
- `out/05-qibla.png` — Qibla compass
- `out/06-ayah.png` — Ayah Picture
- `out/07-namaz.png` — Namaz and wudu guide
- `out/08-theme.png` — dark theme
- `previews/full-set.png` — contact sheet
- `rafeeq-playstore-8-screenshots.zip` — eight upload images only

All final slides are RGB PNG, 1080 × 1920. Seven screens use light mode;
only the theme screen uses dark mode. Nothing has been published to Play Console.

## Approved art direction

The user selected the realistic hand-held concept with an emerald background,
clearer Plus Jakarta Sans ExtraBold headings, white/ivory type, no camera hole
or notch, and extra space above the status bar. Matching emerald studio-phone
compositions provide variation on the prayer, Qibla and Namaz slides. The theme
slide uses a darker hand-held plate.

All app UI was captured afresh on the connected Samsung S21 on 25 November 2026.
The app was restored to light mode after the dark capture. Raw captures are
retained in `captures/`; screenshot pixels, Arabic and translations were not
redrawn by imagegen.

`compose.py` adds 40 pixels of matching app background above each capture
(approximately 13 dp at the device capture density), then perspective-composites
it into the photographic display. Source capture files are unchanged. The
compositor uses only a 0.3% overscan so the added breathing room is retained.

Photo plates were created/edited with the built-in imagegen tool. The selected
background edit prompt is `imagegen-prompt.txt`; the additional studio/dark
plate prompts are in `additional-imagegen-prompts.txt`. No API-key CLI generation
was used. The plates are approximately 941 × 1672 pixels, with real app UI
composited at 2× before final 1080 × 1920 export.

## Editable source and reproduction

- `slides.json`: order, copy, capture source, photographic plate and font size
- `approved-preview.html`: approved typography/style baseline
- `design.html`: generated eight-slide layout
- `compose.py`, `composite_screen.py`: inset and perspective composition
- `export.mjs`: final slide export and contact sheet

From repository root:

```
/tmp/rafeeq-composite-env/bin/python store/playstore-selected-green/compose.py
node store/playstore-selected-green/export.mjs
```

Python requires Pillow, NumPy and OpenCV. The renderer currently uses the
existing Playwright installation in the sibling Budget-AI project and Chrome.
Earlier two-slide previews in `previews/home.png`, `reader.png` and `pair.png`
are historical approvals; `out/` and `previews/full-set.png` are the final assets.

## Feature graphic

`out/feature-graphic-1024x500.png` is the matching RGB PNG feature graphic.
`rafeeq-playstore-complete.zip` includes all eight screenshots plus the banner.
The original eight-screenshot ZIP is unchanged.

The headline starts 176 px from the left edge; its text container occupies
x=176–596, y=108–358. All text stays inside our conservative central review
rectangle (x=154–870, y=75–425). These are design margins, not a promise of
identical cropping on every Google Play placement. Tested at 512px and 320px
wide in `previews/feature-review.png`; review guides are not in the final PNG.
The graphic reuses the campaign's original imagegen photo plus real device UI;
no new generative edit was needed. Source: `feature-graphic.html`.
Re-render with `node store/playstore-selected-green/feature-export.mjs`.

Google Play specification/reference checked 25 November 2026:
https://support.google.com/googleplay/android-developer/answer/9866151

Suggested alt text: Rafeeq: Faith, in your hands. Prayer times, Quran and Qibla, shown on an emerald green background.

### Latest banner revision — five phones, no external text

The user replaced the text-led banner with five stacked phones, each showing a
different feature. The latest banner is
`out/feature-graphic-five-phones-1024x500.png`; latest complete pack is
`rafeeq-playstore-complete-v2.zip`. Earlier banners and ZIPs are preserved as
history. The new ZIP contains only the eight screenshots and the new banner.
Source, built-in imagegen prompt and compositing details: `feature-stack/`.

Alt text: Five stacked phones show Rafeeq's Quran reader, prayer times, home screen, 16-line Quran and Qibla compass on emerald green.

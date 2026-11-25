# Rafeeq — photographic Play Store directions, revision 2

Review `previews/comparison.png`, then the full two-slide previews:
`a-pair.png` (Emerald & Gold), `b-pair.png` (Saffron Studio), and
`c-pair.png` (In Your Hands). Six individual 1080 × 1920 PNGs are included.
These are direction previews; the eight-slide final set awaits style selection.

## Source and fidelity

Three photographic background/phone plates were created with the built-in
imagegen tool. Exact prompts are in `prompts.json`; original plates are retained
in `assets/*-plate.png`. No CLI image generation or API key was used.

Screens are real Samsung S21 captures from the corrected native release:
`../playstore-concepts/captures/home-light.png` and `reader-light.png`.
They are perspective-composited into the chroma-key displays without generative
redrawing of app UI, Arabic, or translations. No punch-hole camera or notch is
present. Only outside-screen scenery and phone hardware are generated.
Typography and branding are rendered separately in HTML/CSS.

## Rebuild

Compositing requires Python with NumPy, OpenCV and Pillow (temporary environment
used: `/tmp/rafeeq-composite-env`). For example:

```
NO_HOLE=1 /tmp/rafeeq-composite-env/bin/python composite_screen.py assets/a-composed.png assets/a-plate.png ../playstore-concepts/captures/home-light.png
```

Run `node store/playstore-concepts-v2/render.mjs` from the repository root.
The renderer uses the existing Playwright installation in the sibling Budget-AI
project and system Chrome. Each photo plate is 941 × 1672; UI compositing is
performed at 2× and layouts exported at 1080 × 1920. Final campaign production
can revisit plate resolution after the direction is chosen.

The earlier pale/CSS-phone concepts remain in `../playstore-concepts/` as
superseded work. No files were published or uploaded to Play Console.

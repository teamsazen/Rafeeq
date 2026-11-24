# Play listing for Rafeeq 2.0

Eight 1080x1920 screenshots and the 1024x500 feature graphic, made the way
Exit Rush's listing-v2 was (ArrowMax/store/playstore-v2).

1. `src/raw/`: screenshots from a Samsung S21 running the release build.
2. `tools/clean_status.py raw.png clean.png src/fonts/PlusJakartaSans-SemiBold.ttf`
   draws a clean 9:41 status bar into `src/clean/`.
3. `tools/composite_screen.py src/comp/NAME.png src/phones/MOCKUP.png clean.png [clean2.png]`
   maps them onto the green screens of the phone mockups (OpenCV, NumPy).
4. `node render.mjs` renders `src/rafeeq.html` to `out/` (Playwright with Chrome).

Play wants 24-bit PNGs with no alpha; `out/` is RGB.

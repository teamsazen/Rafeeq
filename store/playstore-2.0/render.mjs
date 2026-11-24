// Renders src/rafeeq.html's slides and feature graphic to out/ (run from this folder).
// Playwright comes from the Budget-AI store-screenshots project, as in ArrowMax.
import { chromium } from '/Users/waqas/Muzamil/Projects/Budget-AI/store-screenshots/node_modules/playwright/index.mjs';
import { mkdirSync } from 'fs';
mkdirSync('out', { recursive: true });
const browser = await chromium.launch({ channel: 'chrome' });
const page = await browser.newPage({ viewport: { width: 1200, height: 2000 } });
await page.goto('file://' + process.cwd() + '/src/rafeeq.html');
await page.evaluate(() => document.fonts.ready);
await page.waitForTimeout(600);
for (let i = 1; i <= 8; i++) await page.locator('#s' + i).screenshot({ path: `out/0${i}.png` });
await page.locator('#fg').screenshot({ path: 'out/feature-graphic-1024x500.png' });
await browser.close();

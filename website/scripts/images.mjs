// Makes the page's images in public/img/ from the source files.
// Run: npm run images
//
// Screens: screens/android/<name>.png, 1080×2400 captures from an Android phone
// in light mode (theme-dark.png is the one dark capture, for the theme). Each is
// drawn into a plain Android frame with no notch or camera hole.
// Store: screens/store/*.png, the Play Store screenshots in listing order, shown
// side by side as one banner.
// Qibla video: from LINKEDIN_DIR (default ~/Desktop/Rafeeq-LinkedIn).
import sharp from 'sharp';
import { existsSync, mkdirSync, readdirSync, rmSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import { homedir } from 'node:os';
import { join } from 'node:path';

const OUT = 'public/img';
const LINKEDIN_DIR = process.env.LINKEDIN_DIR ?? join(homedir(), 'Desktop/Rafeeq-LinkedIn');

// The phone: a 1080×2400 screen inside a thin, evenly rounded bezel, with the
// volume and power keys on the right. Keep PHONE's size in sync with Shot.astro.
const SCREEN = { width: 1080, height: 2400, radius: 92 };
const BEZEL = 34;
const KEY = 7; // how far the side keys stand out
const PHONE = { width: SCREEN.width + 2 * BEZEL + 2 * KEY, height: SCREEN.height + 2 * BEZEL };
const body = { x: KEY, y: 0, w: SCREEN.width + 2 * BEZEL, h: PHONE.height, r: SCREEN.radius + BEZEL };

const frameSvg = `<svg xmlns="http://www.w3.org/2000/svg" width="${PHONE.width}" height="${PHONE.height}">
  <defs>
    <linearGradient id="edge" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="#3a3d40"/><stop offset=".5" stop-color="#1c1e20"/><stop offset="1" stop-color="#2e3134"/>
    </linearGradient>
    <mask id="hole">
      <rect width="100%" height="100%" fill="#fff"/>
      <rect x="${KEY + BEZEL}" y="${BEZEL}" width="${SCREEN.width}" height="${SCREEN.height}" rx="${SCREEN.radius}" fill="#000"/>
    </mask>
  </defs>
  <rect x="${body.x + body.w - 2}" y="560" width="${KEY + 2}" height="300" rx="${KEY / 2}" fill="#2a2c2f"/>
  <rect x="${body.x + body.w - 2}" y="960" width="${KEY + 2}" height="170" rx="${KEY / 2}" fill="#2a2c2f"/>
  <g mask="url(#hole)">
    <rect x="${body.x}" y="${body.y}" width="${body.w}" height="${body.h}" rx="${body.r}" fill="url(#edge)"/>
    <rect x="${body.x + 5}" y="${body.y + 5}" width="${body.w - 10}" height="${body.h - 10}" rx="${body.r - 5}" fill="#0b0c0d"/>
  </g>
</svg>`;
const screenMask = Buffer.from(`<svg xmlns="http://www.w3.org/2000/svg" width="${SCREEN.width}" height="${SCREEN.height}"><rect width="100%" height="100%" rx="${SCREEN.radius}" fill="#fff"/></svg>`);

const webp = (input, width, file) => sharp(input).resize(width).webp({ quality: 82, effort: 6 }).toFile(file);

for (const dir of ['screens', 'phones', 'posts', 'store', 'qibla']) rmSync(join(OUT, dir), { recursive: true, force: true });

const shots = readdirSync('screens/android').filter((f) => f.endsWith('.png')).sort();
mkdirSync(join(OUT, 'phones'), { recursive: true });
for (const f of shots) {
  const name = f.replace('.png', '');
  const shot = await sharp(join('screens/android', f))
    .resize(SCREEN.width, SCREEN.height, { fit: 'cover', position: 'top' })
    .composite([{ input: screenMask, blend: 'dest-in' }])
    .png().toBuffer();
  const framed = await sharp({ create: { width: PHONE.width, height: PHONE.height, channels: 4, background: { r: 0, g: 0, b: 0, alpha: 0 } } })
    .composite([{ input: shot, left: KEY + BEZEL, top: BEZEL }, { input: Buffer.from(frameSvg) }]).png().toBuffer();
  await webp(framed, 720, join(OUT, 'phones', `${name}.webp`));
  await webp(framed, 360, join(OUT, 'phones', `${name}-sm.webp`));
}
console.log(`${shots.length} phones, ${PHONE.width}×${PHONE.height} before scaling`);

const store = existsSync('screens/store') ? readdirSync('screens/store').filter((f) => f.endsWith('.png')).sort() : [];
if (store.length) mkdirSync(join(OUT, 'store'), { recursive: true });
for (const [i, f] of store.entries()) {
  const n = String(i + 1).padStart(2, '0');
  await sharp(`screens/store/${f}`).resize({ height: 1000 }).webp({ quality: 84, effort: 6 }).toFile(join(OUT, 'store', `${n}.webp`));
  await sharp(`screens/store/${f}`).resize({ height: 500 }).webp({ quality: 84, effort: 6 }).toFile(join(OUT, 'store', `${n}-sm.webp`));
}
if (!store.length) console.warn('! no Play Store screenshots in screens/store/, the banner is left out');

// The Qibla video plays in its own section, at 720×900 with no sound.
const QIBLA = join(LINKEDIN_DIR, 'qibla-video/Rafeeq-Qibla.mp4');
mkdirSync(join(OUT, 'qibla'), { recursive: true });
execFileSync('ffmpeg', ['-y', '-loglevel', 'error', '-i', QIBLA, '-vf', 'scale=720:900', '-an', '-c:v', 'libx264', '-crf', '24', '-preset', 'slow', '-pix_fmt', 'yuv420p', '-movflags', '+faststart', join(OUT, 'qibla/qibla-720.mp4')]);
await webp(join(LINKEDIN_DIR, 'qibla-video/cover-frame.png'), 720, join(OUT, 'qibla/poster.webp'));

await sharp('../assets/logo.png').resize(128).png().toFile(join(OUT, 'logo.png'));
console.log('images written to', OUT);

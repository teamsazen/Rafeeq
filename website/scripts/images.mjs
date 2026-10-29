// Makes the page's images in public/img/ from the source files.
// Run: npm run images
//
// Screens: screens/light/<file>.png and screens/dark/<file>.png, the app's
// 1206×2622 screenshots with the same file names as ../screenshots/. The page
// shows the light set in a light theme and the dark set in a dark one; a
// screenshot missing from one set falls back to the other, with a warning.
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
const THEMES = ['light', 'dark'];

// Page name → screenshot file name. Each is drawn in Apple's iPhone bezel.
const SCREENS = {
  home: '01-home',
  prayer: '03-home-share',
  surahs: '04-quran-surahs',
  reader: '07-surah-al-baqara',
  qibla: '09-qibla',
  language: '11-settings-language',
  theme: '13-settings-theme',
  namaz: '16-namaz-guide',
  hijri: '18-hijri-calendar',
  names: '19-asma-ul-husna',
  ayah: '20-ayah-picture',
  recite: '21-surah-recitation',
};
// The bezel's transparent screen cutout (1470×3000 frame), measured from the PNG.
const BEZEL = { file: 'screens/iphone-bezel.png', width: 1470, height: 3000, screen: { left: 75, top: 67, width: 1320, height: 2866 } };

const webp = (input, width, file) => sharp(input).resize(width).webp({ quality: 82, effort: 6 }).toFile(file);
const source = (theme, name) => {
  const own = `screens/${theme}/${SCREENS[name]}.png`;
  if (existsSync(own)) return own;
  const other = `screens/${THEMES.find((t) => t !== theme)}/${SCREENS[name]}.png`;
  if (!existsSync(other)) throw new Error(`no screenshot for ${name} (${SCREENS[name]}.png)`);
  console.warn(`! ${theme}: ${SCREENS[name]}.png missing, using the other theme's`);
  return other;
};

for (const dir of ['screens', 'phones', 'posts', 'store', 'qibla']) rmSync(join(OUT, dir), { recursive: true, force: true });

for (const theme of THEMES) {
  mkdirSync(join(OUT, 'phones', theme), { recursive: true });
  for (const name of Object.keys(SCREENS)) {
    const src = source(theme, name);
    const { left, top, width, height } = BEZEL.screen;
    const shot = await sharp(src).resize(width, height).toBuffer();
    const framed = await sharp({ create: { width: BEZEL.width, height: BEZEL.height, channels: 4, background: { r: 0, g: 0, b: 0, alpha: 0 } } })
      .composite([{ input: shot, left, top }, { input: BEZEL.file }]).png().toBuffer();
    await webp(framed, 720, join(OUT, 'phones', theme, `${name}.webp`));
    await webp(framed, 360, join(OUT, 'phones', theme, `${name}-sm.webp`));
  }
}

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

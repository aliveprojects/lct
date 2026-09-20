// Генерирует иконки Android (адаптивные + обычные), заставку и иконку 512×512 для RuStore из assets/icon-foreground.svg.
// Запуск: node scripts/gen-icons.mjs
import fs from 'node:fs';
import path from 'node:path';
import sharp from 'sharp';

const ROOT = path.resolve(import.meta.dirname, '..');
const RES = path.join(ROOT, 'android/app/src/main/res');
const BG = '#1B1F6E';
const SPLASH_BG = '#0A0D35';
const fg = fs.readFileSync(path.join(ROOT, 'assets/icon-foreground.svg'));

const render = (size) => sharp(fg, { density: 384 }).resize(size, size).png().toBuffer();
const solid = (size, color) => sharp({ create: { width: size, height: size, channels: 4, background: color } }).png().toBuffer();
async function flat(size) {
  const base = await solid(size, BG);
  return sharp(base).composite([{ input: await render(size) }]).png().toBuffer();
}
async function round(size) {
  const img = await flat(size);
  const mask = Buffer.from(`<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}"><circle cx="${size / 2}" cy="${size / 2}" r="${size / 2}"/></svg>`);
  return sharp(img).composite([{ input: mask, blend: 'dest-in' }]).png().toBuffer();
}

const write = (file, buf) => {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, buf);
};

const DENS = { mdpi: 1, hdpi: 1.5, xhdpi: 2, xxhdpi: 3, xxxhdpi: 4 };
for (const [d, k] of Object.entries(DENS)) {
  const dir = path.join(RES, `mipmap-${d}`);
  write(path.join(dir, 'ic_launcher.png'), await flat(Math.round(48 * k)));
  write(path.join(dir, 'ic_launcher_round.png'), await round(Math.round(48 * k)));
  write(path.join(dir, 'ic_launcher_foreground.png'), await render(Math.round(108 * k)));
}
fs.writeFileSync(path.join(RES, 'values/ic_launcher_background.xml'), `<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <color name="ic_launcher_background">${BG}</color>\n</resources>\n`);
fs.rmSync(path.join(RES, 'drawable-v24/ic_launcher_foreground.xml'), { force: true });

// Заставка: кремовый фон и питомец по центру (все размеры, которые создал Capacitor)
for (const dir of fs.readdirSync(RES).filter((n) => n.startsWith('drawable') && fs.existsSync(path.join(RES, n, 'splash.png')))) {
  const file = path.join(RES, dir, 'splash.png');
  const { width, height } = await sharp(file).metadata();
  const s = Math.round(Math.min(width, height) * 0.42);
  const bg = await sharp({ create: { width, height, channels: 4, background: SPLASH_BG } }).png().toBuffer();
  write(file, await sharp(bg).composite([{ input: await render(s), gravity: 'center' }]).png().toBuffer());
}

// Иконка для карточки RuStore (512×512): композиция крупнее, чем в адаптивной иконке (там нужна safe-зона)
async function store(size) {
  const zoom = 1.3;
  const inner = Math.round(size * zoom);
  const big = await sharp(fg, { density: 384 }).resize(inner, inner).png().toBuffer();
  const cropped = await sharp(big).extract({ left: Math.round((inner - size) / 2), top: Math.round((inner - size) / 2), width: size, height: size }).png().toBuffer();
  return sharp(await solid(size, BG)).composite([{ input: cropped }]).png().toBuffer();
}
write(path.join(ROOT, 'store/icon-512.png'), await store(512));
write(path.join(ROOT, 'store/icon-1024.png'), await store(1024));
console.log('Иконки и заставка обновлены.');

#!/usr/bin/env node
// Проверка нативного приложения на эмуляторе или устройстве: устанавливает подписанный APK и проходит игровой цикл.
// Интерфейс Compose виден для uiautomator (как для программ чтения с экрана), поэтому нажатия делаются по тексту.
// Использование: node scripts/device-smoke.mjs [путь к APK]   (нужен запущенный эмулятор или устройство и adb)

import { execFileSync } from 'node:child_process';
import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..');
const APK = process.argv[2] ?? join(ROOT, 'app/build/outputs/apk/release/app-release.apk');
const SHOTS = join(ROOT, 'store/screenshots');
const ANDROID_HOME = process.env.ANDROID_HOME ?? `${process.env.HOME}/Library/Android/sdk`;
const ADB = `${ANDROID_HOME}/platform-tools/adb`;
const PKG = 'app.finni.kids';
mkdirSync(SHOTS, { recursive: true });

const adb = (...a) => execFileSync(ADB, a, { encoding: 'utf8', maxBuffer: 1 << 26 });
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const results = [];
const check = (name, ok, extra = '') => {
  results.push({ name, ok });
  console.log(`${ok ? '✔' : '✘'} ${name}${extra ? ` — ${extra}` : ''}`);
};

/** Все элементы на экране с текстом или описанием. */
function screen() {
  adb('shell', 'uiautomator', 'dump', '/sdcard/u.xml');
  const xml = adb('exec-out', 'cat', '/sdcard/u.xml');
  const dec = (s) => s.replace(/&quot;/g, '"').replace(/&amp;/g, '&').replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&#10;/g, '\n');
  return [...xml.matchAll(/<node [^>]*?text="([^"]*)"[^>]*?content-desc="([^"]*)"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"/g)]
    .map((m) => ({ text: dec(m[1]), desc: dec(m[2]), x: (+m[3] + +m[5]) / 2 | 0, y: (+m[4] + +m[6]) / 2 | 0 }))
    .filter((n) => n.text || n.desc);
}
const label = (n) => `${n.text} ${n.desc}`;
const has = (nodes, t) => nodes.some((n) => label(n).includes(t));
const find = (nodes, t, exact = false, last = false) => { const m = nodes.filter((n) => (exact ? n.text === t || n.desc === t : label(n).includes(t))); return last ? m[m.length - 1] : m[0]; };

async function see(t, tries = 6) {
  for (let i = 0; i < tries; i++) {
    if (has(screen(), t)) return true;
    await sleep(500);
  }
  return false;
}
async function tap(t, { exact = true, scroll = false, last = false } = {}) {
  for (let i = 0; i < (scroll ? 12 : 3); i++) {
    const n = find(screen(), t, exact, last);
    if (n) { adb('shell', 'input', 'tap', String(n.x), String(n.y)); await sleep(900); return true; }
    if (scroll) adb('shell', 'input', 'swipe', '540', '1500', '540', '600', '300');
    await sleep(500);
  }
  throw new Error(`не нашёл на экране: «${t}»`);
}
const shot = (name) => writeFileSync(join(SHOTS, `${name}.png`), execFileSync(ADB, ['exec-out', 'screencap', '-p'], { maxBuffer: 1 << 26 }));
const key = (k) => adb('shell', 'input', 'keyevent', String(k));
const text = (s) => adb('shell', 'input', 'text', s);
const start = async () => { adb('shell', 'am', 'start', '-W', '-n', `${PKG}/.MainActivity`); await sleep(2000); };
const coins = () => { const n = screen().find((x) => x.desc.startsWith('Баланс:')); return n ? Number(/Баланс: (\d+)/.exec(n.desc)?.[1]) : NaN; };
/** Возвращается на главный экран кнопкой «назад» (на самом главном она закрыла бы приложение, поэтому проверяем заранее). */
const goHome = async () => { for (let i = 0; i < 6; i++) { if (screen().some((n) => n.desc.startsWith("Баланс:"))) return; key(4); await sleep(700); } };
const scrollDown = () => { adb('shell', 'input', 'swipe', '540', '1500', '540', '500', '300'); return sleep(700); };
const crashes = () => adb('logcat', '-d', '-s', 'AndroidRuntime:E').split('\n').filter((l) => l.includes(PKG)).length;

// Отключаем анимации системы, чтобы экраны появлялись сразу (значения вернём в конце).
const scales = ['window_animation_scale', 'transition_animation_scale', 'animator_duration_scale'];
scales.forEach((s) => adb('shell', 'settings', 'put', 'global', s, '0'));

try {
  // ---- Установка ----
  adb('uninstall', PKG).toString();
  adb('logcat', '-c');
  adb('install', APK);
  check('APK устанавливается', true);
  const aapt = execFileSync('bash', ['-c', `ls -d ${ANDROID_HOME}/build-tools/*/aapt2 | sort -V | tail -1`], { encoding: 'utf8' }).trim();
  const perms = execFileSync(aapt, ['dump', 'permissions', APK], { encoding: 'utf8' });
  check('в манифесте нет ни одного разрешения', !/uses-permission/.test(perms), perms.trim().split('\n').slice(1).join('; '));

  await start();
  check('запускается без падений', crashes() === 0);
  check('приветствие: есть имя Финни и кнопка «Начать»', (await see('Начать')) && has(screen(), 'Финни'));
  shot('01-welcome');

  // ---- Знакомство и создание питомца ----
  await tap('Начать');
  shot('02-intro');
  await tap('Дальше'); shot('03-three-choices');
  await tap('Дальше');
  await tap('Создать питомца');
  await tap('Придумать за меня');
  await tap('Дальше');
  await tap('Зайка'); await tap('Сирень');
  shot('04-create-pet');
  await tap('Дальше');
  check('питомец по умолчанию называется Финни', has(screen(), 'Финни'));
  await tap('Готово!');
  check('стартовый подарок объяснён', await see('Стартовый подарок!'));
  shot('05-start-gift');
  await tap('Понятно');

  // ---- Главный экран и план ----
  check('на главном экране 100 монет', coins() === 100, `баланс ${coins()}`);
  shot('06-home');
  await tap('К плану');
  await scrollDown();
  await tap('Подсказка', { exact: true });
  await scrollDown();
  shot('07-plan');
  await tap('Подтвердить план');
  check('окно подтверждения плана', await see('Подтвердить план?'));
  await tap('Подтвердить');
  check('план подтверждён, есть отчёт «что изменилось»', await see('План на неделю готов!'));
  await tap('В магазин');

  // ---- Магазин ----
  shot('08-shop');
  await tap('Обед');
  shot('09-item');
  await tap('Купить за 15');
  check('покупка обеда объяснена', await see('Обед: куплено!'));
  shot('10-buy-report');
  await tap('Понятно');
  await goHome();

  // ---- Копилка ----
  await tap('Копилка', { exact: true });
  await tap('Отложить ', { exact: false, scroll: true });
  check('взнос в копилку объяснён', await see('Отложено!'));
  shot('12-savings-report');
  await tap('Понятно');

  // ---- Задание ----
  await goHome();
  await tap('Задания', { exact: true });
  await tap('Неделя Миши', { exact: false, scroll: true });
  check('игровое задание открывается', await see('Всего монет'));
  shot('14-task');
  await goHome();

  // ---- Итоги недели ----
  await tap('План', { exact: true });
  await tap('Завершить неделю');
  await tap('Завершить неделю', { exact: true });
  check('итоги недели показаны', await see('Неделя 1 завершена'));
  await tap('Смотреть итоги');
  check('экран итогов: очки роста объяснены', await see('Почему столько очков', 8) || (await scrollDown(), has(screen(), 'очков')));
  shot('15-review');

  // ---- Перезапуск ----
  const before = (key(3), await sleep(500), adb('shell', 'am', 'force-stop', PKG), await start(), coins());
  check('прогресс сохраняется после перезапуска', before > 0, `монет ${before}`);
  shot('16-after-restart');

  // ---- Раздел для взрослых ----
  await tap('Раздел для взрослого');
  const q = screen().find((n) => /\d+ × \d+/.test(n.text));
  const [, a, b] = /(\d+) × (\d+)/.exec(q?.text ?? '0 × 0');
  await tap('Ответ на пример', { exact: true });
  text(String(a * b)); key(66); await sleep(1200);
  check('раздел для взрослых открывается после примера', await see('Для чего это приложение'));
  shot('18-adult');

  // ---- Удаление профиля ----
  await tap('Удалить профиль', { exact: true, scroll: true });
  await tap('Удалить профиль', { exact: true, last: true });
  check('профиль удаляется, возвращается приветствие', await see('Начать'));

  check('за весь прогон не было падений приложения', crashes() === 0);
} catch (e) {
  check(`сценарий дошёл до конца (${e.message})`, false);
  try { shot('ошибка'); } catch { /* ignore */ }
} finally {
  scales.forEach((s) => adb('shell', 'settings', 'put', 'global', s, '1'));
}

const failed = results.filter((r) => !r.ok);
console.log(`\nПроверок: ${results.length}, успешно: ${results.length - failed.length}`);
process.exit(failed.length ? 1 : 0);

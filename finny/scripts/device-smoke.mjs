// Дымовой тест на подключённом устройстве или эмуляторе Android (через adb).
//
//   node scripts/device-smoke.mjs [--serial <id>] [--release <apk>] [--flow <apk>]
//
// Часть А — на релизном APK: установка, отсутствие разрешений, время холодного запуска, отсутствие аварий.
// Часть Б — на тестовой сборке с включённой отладкой WebView (`FINNI_WEBVIEW_DEBUG=1 npx cap sync android`,
//           затем `gradlew assembleDebug`): проходит обязательный сценарий настоящими касаниями (`adb input tap`),
//           проверяет результат по содержимому экрана (Chrome DevTools Protocol), закрывает и открывает приложение.
//
// Нужны: adb (platform-tools), Node 22+. Скриншоты — в store/screenshots/. Текст вводится латиницей (adb не вводит кириллицу).
import { execFileSync } from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';

const ROOT = path.resolve(import.meta.dirname, '..');
const argv = process.argv.slice(2);
const opt = (name, fallback) => (argv.includes(name) ? argv[argv.indexOf(name) + 1] : fallback);
const serial = opt('--serial', null);
const RELEASE_APK = opt('--release', path.join(ROOT, 'release/finni-1.0.0-release.apk'));
const FLOW_APK = opt('--flow', path.join(ROOT, 'release/finni-1.0.0-debug-webview.apk'));
const PKG = 'app.finni.kids';
const ACT = `${PKG}/.MainActivity`;
const SHOTS = path.join(ROOT, 'store/screenshots');
const PORT = 9222;
const ADB = process.env.ADB ?? path.join(process.env.ANDROID_HOME ?? path.join(os.homedir(), 'Library/Android/sdk'), 'platform-tools/adb');

const adb = (...a) => execFileSync(ADB, [...(serial ? ['-s', serial] : []), ...a], { encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 });
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const results = [];
const check = (name, ok, extra = '') => {
  results.push({ name, ok, extra });
  console.log(`${ok ? '  ✓' : '  ✗'} ${name}${extra ? ` — ${extra}` : ''}`);
};

fs.mkdirSync(SHOTS, { recursive: true });
const shot = (name) => fs.writeFileSync(path.join(SHOTS, `${name}.png`), execFileSync(ADB, [...(serial ? ['-s', serial] : []), 'exec-out', 'screencap', '-p'], { maxBuffer: 64 * 1024 * 1024 }));

// ---------- Chrome DevTools Protocol ----------
let ws = null;
let msgId = 0;
const pending = new Map();

async function connect(timeoutMs = 20000) {
  const end = Date.now() + timeoutMs;
  while (Date.now() < end) {
    try {
      const pid = adb('shell', 'pidof', PKG).trim().split(/\s+/)[0];
      if (pid) {
        adb('forward', `tcp:${PORT}`, `localabstract:webview_devtools_remote_${pid}`);
        const targets = await (await fetch(`http://127.0.0.1:${PORT}/json`)).json();
        const page = targets.find((t) => t.type === 'page');
        if (page) {
          ws = new WebSocket(page.webSocketDebuggerUrl);
          await new Promise((res, rej) => {
            ws.onopen = res;
            ws.onerror = rej;
          });
          ws.onmessage = (ev) => {
            const m = JSON.parse(ev.data);
            if (m.id && pending.has(m.id)) {
              pending.get(m.id)(m);
              pending.delete(m.id);
            }
          };
          return true;
        }
      }
    } catch {
      /* пробуем ещё */
    }
    await sleep(400);
  }
  return false;
}
function disconnect() {
  try {
    ws?.close();
  } catch {
    /* ignore */
  }
  ws = null;
}
async function js(expression) {
  const id = ++msgId;
  const reply = new Promise((res) => pending.set(id, res));
  ws.send(JSON.stringify({ id, method: 'Runtime.evaluate', params: { expression, returnByValue: true, awaitPromise: true } }));
  const m = await Promise.race([reply, sleep(8000).then(() => ({ error: { message: 'timeout' } }))]);
  if (m.error || m.result?.exceptionDetails) throw new Error(`js: ${m.error?.message ?? m.result.exceptionDetails.text}`);
  return m.result.result.value;
}
const text = () => js('document.body.innerText');
async function waitText(needle, ms = 10000) {
  const t0 = Date.now();
  while (Date.now() - t0 < ms) {
    try {
      if ((await text()).includes(needle)) return Date.now() - t0;
    } catch {
      /* страница ещё грузится */
    }
    await sleep(250);
  }
  return -1;
}

// Смещение WebView на экране (панель состояния) берём из иерархии окна; запасное значение — 72 px.
let webviewTop = 72;
function detectWebviewTop() {
  try {
    adb('shell', 'uiautomator', 'dump', '/sdcard/ui.xml');
    const xml = adb('exec-out', 'cat', '/sdcard/ui.xml');
    const nodes = [...xml.matchAll(/class="([^"]+)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"/g)];
    const web = nodes.find((n) => /WebView/.test(n[1])) ?? nodes.filter((n) => n[1] === 'android.view.View').pop();
    if (web) webviewTop = Number(web[3]);
  } catch {
    /* оставляем запасное */
  }
}

/** Находит элемент по тексту и возвращает координаты его центра. */
async function locate(needle, { last = false, selector = 'button, [role=button], label, input, summary, a' } = {}) {
  return js(`(() => {
    const els = [...document.querySelectorAll(${JSON.stringify(selector)})].filter(e => !e.disabled && e.getClientRects().length && (e.textContent.includes(${JSON.stringify(needle)}) || (e.getAttribute('aria-label')||'').includes(${JSON.stringify(needle)})));
    const el = ${last ? 'els[els.length-1]' : 'els[0]'};
    if (!el) return null;
    el.scrollIntoView({block: 'center'});
    const b = el.getBoundingClientRect();
    return { x: b.left + b.width/2, y: b.top + b.height/2, dpr: window.devicePixelRatio };
  })()`);
}
async function tap(needle, opts = {}) {
  const t0 = Date.now();
  let r = null;
  while (Date.now() - t0 < 8000 && !r) {
    r = await locate(needle, opts);
    if (!r) await sleep(300);
  }
  if (!r) throw new Error(`Не нашёл на экране: «${needle}»`);
  await sleep(150); // после прокрутки
  const p = (await locate(needle, opts)) ?? r;
  adb('shell', 'input', 'tap', String(Math.round(p.x * p.dpr)), String(Math.round(p.y * p.dpr + webviewTop)));
  await sleep(650);
}
async function hideKeyboard() {
  if (/mInputShown=true/.test(adb('shell', 'dumpsys', 'input_method'))) {
    adb('shell', 'input', 'keyevent', 'KEYCODE_BACK');
    await sleep(600);
  }
}
const launch = () => adb('shell', 'am', 'start', '-n', ACT);
const stop = () => adb('shell', 'am', 'force-stop', PKG);

// ---------- Часть А: релизный APK ----------
async function partA() {
  console.log('\n[А] Релизная сборка');
  console.log(`APK: ${path.relative(ROOT, RELEASE_APK)} (${(fs.statSync(RELEASE_APK).size / 1048576).toFixed(1)} МБ)`);
  try { adb('uninstall', PKG); } catch { /* не был установлен */ }
  check('APK устанавливается без среды разработки', /Success/.test(adb('install', '-r', RELEASE_APK)));
  const dump = adb('shell', 'dumpsys', 'package', PKG);
  const requested = (dump.match(/requested permissions:([\s\S]*?)(install permissions|User \d)/) ?? [])[1] ?? '';
  const declared = requested.split('\n').map((s) => s.trim()).filter((s) => /^android\.permission\./.test(s));
  check('Приложение не запрашивает разрешений Android', declared.length === 0, declared.join(', '));

  const times = [];
  for (let i = 0; i < 5; i++) {
    stop();
    await sleep(900);
    const out = adb('shell', 'am', 'start', '-W', '-n', ACT);
    times.push(Number((out.match(/TotalTime:\s*(\d+)/) ?? [])[1]));
    await sleep(1800);
  }
  check('Запуск активности (am start -W) ≤ 5 с', Math.max(...times) <= 5000, `${times.join(', ')} мс`);
  await sleep(5000);
  shot('release-launch');
  const crash = adb('logcat', '-d', '-b', 'crash', '-v', 'brief');
  check('Нет аварийных завершений (logcat -b crash)', !crash.includes(PKG));
}

// ---------- Часть Б: обязательный сценарий ----------
async function partB() {
  console.log('\n[Б] Обязательный сценарий (тестовая сборка с отладкой WebView)');
  if (!fs.existsSync(FLOW_APK)) return console.log('  пропущено: нет', path.relative(ROOT, FLOW_APK));
  try { adb('uninstall', PKG); } catch { /* ok */ }
  check('Тестовая сборка установлена', /Success/.test(adb('install', '-r', FLOW_APK)));

  // Холодный запуск: (1) активность показана — am start -W; (2) первое содержимое страницы — First Contentful Paint от начала загрузки.
  // Их сумма — верхняя оценка времени до стартового экрана (без накладных расходов тестового обвеса).
  const launches = [];
  const paints = [];
  for (let i = 0; i < 4; i++) {
    stop();
    await sleep(1000);
    const out = adb('shell', 'am', 'start', '-W', '-n', ACT);
    launches.push(Number((out.match(/TotalTime:\s*(\d+)/) ?? [])[1]));
    if (await connect(20000)) {
      await waitText('Начать', 15000);
      const fcp = await js("Math.round((performance.getEntriesByName('first-contentful-paint')[0] || {startTime: -1}).startTime)");
      paints.push(fcp);
    }
    disconnect();
  }
  const sums = launches.map((l, i) => l + (paints[i] ?? 99999));
  check('Холодный запуск до стартового экрана ≤ 5 с', Math.max(...sums) <= 5000, `запуск ${launches.join('/')} мс + отрисовка ${paints.join('/')} мс = ${sums.join('/')} мс`);

  stop();
  await sleep(700);
  launch();
  check('Отладочное подключение к WebView', await connect());
  check('Стартовый экран показан', (await waitText('Начать')) >= 0);
  detectWebviewTop();
  shot('01-welcome');

  console.log('  Знакомство и создание профиля');
  await tap('Начать');
  check('Знакомство: цель игры', (await waitText('Привет, я Финни')) >= 0);
  shot('02-intro');
  await tap('Дальше');
  await waitText('Три решения');
  shot('03-three-choices');
  await tap('Пропустить');
  check('Шаг «игровое имя» без реального имени и телефона', (await waitText('Настоящее имя писать не нужно')) >= 0);
  await tap('Игровое имя', { selector: 'label' });
  await sleep(500);
  adb('shell', 'input', 'text', 'Rocket');
  await sleep(500);
  check('Текст вводится с экранной клавиатуры', (await js('document.querySelector("input.input").value')) === 'Rocket');
  await hideKeyboard();
  await tap('Дальше');
  check('Шаг выбора питомца', (await waitText('Выбери друга')) >= 0);
  await tap('Зайка');
  await tap('Сирень');
  await tap('Бантик');
  shot('04-create-pet');
  await tap('Дальше');
  check('Имя питомца по умолчанию — Финни', (await js('document.querySelector("input.input").value')) === 'Финни');
  await tap('Готово!');
  check('Стартовый подарок с объяснением', (await waitText('Это подарок для начала игры')) >= 0);
  shot('05-start-gift');
  await tap('Понятно');
  check('Главный экран: баланс, цель, показатели, следующий шаг', (await waitText('Мечта')) >= 0 && (await text()).includes('Сытость'));
  shot('06-home');

  console.log('  Фоновая музыка');
  check('Музыка запускается после первого касания', (await js('document.documentElement.dataset.music')) === 'playing');
  await tap('Настройки', { selector: '.iconbtn' }).catch(() => undefined);
  if ((await text()).includes('Фоновая музыка')) {
    await tap('Фоновая музыка', { selector: 'button' });
    await sleep(1200);
    check('Переключатель выключает музыку', (await js('document.documentElement.dataset.music')) === 'off');
    await tap('Фоновая музыка', { selector: 'button' });
    await sleep(1200);
    check('И включает её снова', (await js('document.documentElement.dataset.music')) === 'playing');
    shot('06b-settings');
    await tap('Готово');
  } else check('Настройки открываются с главного экрана', false);


  console.log('  План, покупки, нехватка средств');
  await tap('К плану');
  await tap('Подсказка', { last: true });
  shot('07-plan');
  await tap('Подтвердить план');
  await tap('Подтвердить', { last: true });
  check('План подтверждён', (await waitText('План на неделю готов')) >= 0);
  await tap('В магазин');
  await waitText('Обед');
  shot('08-shop');
  await tap('Обед', { selector: '.item' });
  shot('09-item');
  await tap('Купить за 15');
  check('Покупка: «что изменилось и почему»', (await waitText('куплено')) >= 0);
  shot('10-buy-report');
  await tap('Понятно');
  await tap('Хочется', { last: true });
  await tap('Игровой городок', { selector: '.item' });
  await tap('Купить за 120');
  check('Нехватка средств: объяснение и варианты', (await waitText('Не хватает')) >= 0 && (await text()).includes('Заработать монеты'));
  shot('11-shortage');
  await tap('Понятно');

  console.log('  Кнопка «назад»');
  adb('shell', 'input', 'keyevent', 'KEYCODE_BACK');
  await sleep(900);
  adb('shell', 'input', 'keyevent', 'KEYCODE_BACK');
  await sleep(900);
  check('Аппаратная «назад» ведёт по экранам до главного', (await waitText('Мечта', 3000)) >= 0);

  console.log('  Копилка и задание');
  await tap('Копилка', { selector: '.tab' });
  await waitText('Копилка');
  await tap('Отложить', { selector: '.btn--good' });
  check('Взнос в копилку: объяснение', (await waitText('Отложено')) >= 0);
  shot('12-savings-report');
  await tap('Понятно');
  shot('13-goal');
  adb('shell', 'input', 'keyevent', 'KEYCODE_BACK');
  await sleep(800);
  await tap('Задания', { selector: '.tab' });
  await tap('Корзина для питомца', { selector: '.taskrow' });
  await tap('Корм', { selector: '.item' });
  await tap('Мыло', { selector: '.item' });
  await tap('Оплатить');
  check('Задание: разбор результата и награда', (await waitText('Запомни')) >= 0);
  shot('14-task-result');
  await tap('К заданиям');
  adb('shell', 'input', 'keyevent', 'KEYCODE_BACK');
  await sleep(800);

  console.log('  Итоги недели');
  await tap('План', { selector: '.tab' });
  await tap('Завершить неделю');
  await tap('Завершить неделю', { last: true });
  check('Итоги недели с объяснением роста', (await waitText('Смотреть итоги')) >= 0);
  await tap('Смотреть итоги');
  await waitText('Почему столько очков');
  shot('15-review');
  await tap('На главный');
  await waitText('Мечта');
  const before = await js('document.querySelector(".chip--coin").getAttribute("aria-label")');
  shot('16-home-week2');

  console.log('  Сохранение после закрытия');
  await sleep(700);
  adb('shell', 'input', 'keyevent', 'KEYCODE_HOME');
  await sleep(900);
  disconnect();
  stop();
  await sleep(900);
  launch();
  check('Отладочное подключение после перезапуска', await connect());
  check('После перезапуска открыт главный экран, а не приветствие', (await waitText('Мечта', 12000)) >= 0);
  const after = await js('document.querySelector(".chip--coin")?.getAttribute("aria-label")');
  check('Баланс сохранён', after === before && !!after, `${before} → ${after}`);
  check('Неделя сохранена (2-я неделя)', (await text()).includes('План недели 2'));
  shot('17-after-restart');

  console.log('  Раздел для взрослого и удаление данных');
  await tap('Раздел для взрослого', { selector: '.iconbtn' });
  const q = await js('document.querySelector(".gate__q").textContent');
  const m = q.match(/(\d+)\s*×\s*(\d+)/);
  await tap('Ответ на пример', { selector: 'input' });
  adb('shell', 'input', 'text', String(Number(m[1]) * Number(m[2])));
  await hideKeyboard();
  await tap('Войти');
  check('Раздел для взрослого открывается после примера', (await waitText('Для чего это приложение')) >= 0);
  shot('18-adult');
  await tap('Удалить профиль', { last: true });
  await tap('Удалить профиль', { last: true });
  check('Удаление профиля возвращает на первый экран', (await waitText('Начать', 6000)) >= 0);
  disconnect();

  const crash = adb('logcat', '-d', '-b', 'crash', '-v', 'brief');
  check('Нет аварийных завершений (logcat -b crash)', !crash.includes(PKG));
}

async function main() {
  console.log(`Устройство: ${adb('shell', 'getprop', 'ro.product.model').trim()}, Android ${adb('shell', 'getprop', 'ro.build.version.release').trim()} (API ${adb('shell', 'getprop', 'ro.build.version.sdk').trim()}), ${adb('shell', 'wm', 'size').trim().replace('Physical size: ', '')}`);
  const anim = ['animator_duration_scale', 'transition_animation_scale', 'window_animation_scale'];
  const saved = anim.map((k) => adb('shell', 'settings', 'get', 'global', k).trim());
  try {
    await partA();
    await partB();
  } finally {
    anim.forEach((k, i) => adb('shell', 'settings', 'put', 'global', k, saved[i] === 'null' ? '1' : saved[i]));
  }
  const failed = results.filter((r) => !r.ok);
  console.log(`\nИтого: ${results.length - failed.length} из ${results.length} проверок пройдено. Скриншоты: ${path.relative(ROOT, SHOTS)}/`);
  process.exit(failed.length ? 1 : 0);
}

main().catch((e) => {
  console.error('\nТест прерван:', e.message);
  try {
    shot('zz-failure');
  } catch {
    /* ignore */
  }
  process.exit(2);
});

// Фоновая музыка: два трека по кругу, тихо, с плавным включением. Работает офлайн (файлы лежат в приложении).
// Правила: не играет, пока ребёнок не коснулся экрана (так требует WebView), останавливается при сворачивании
// приложения и выключается переключателем в настройках. Важная информация никогда не передаётся только музыкой.

export interface Track {
  id: string;
  title: string;
  artist: string;
  file: string;
}

/** Треки и их авторы (показываются в разделе для взрослых). Чтобы заменить музыку, поменяйте файлы в public/music/ и этот список. */
export const TRACKS: Track[] = [
  { id: 'sky', title: 'Sensai Masopi Sky', artist: 'DEX 1200', file: './music/sky.m4a' },
  { id: 'metamorphosis', title: 'Metamorphosis', artist: 'Laura Platt', file: './music/metamorphosis.m4a' },
];

const VOLUME = 0.22;
const FADE_MS = 700;

let el: HTMLAudioElement | null = null;
let index = 0;
let enabled = false;
let touched = false; // был ли жест пользователя
let background = false; // приложение свёрнуто
let fade: ReturnType<typeof setInterval> | undefined;

export const nextIndex = (i: number, n = TRACKS.length): number => (i + 1) % n;

/** Состояние для тестов и отладки: <html data-music="playing|paused|off">. */
function publish(): void {
  if (typeof document === 'undefined') return;
  document.documentElement.dataset.music = !enabled ? 'off' : el && !el.paused ? 'playing' : 'paused';
}

function audio(): HTMLAudioElement {
  if (el) return el;
  el = new Audio();
  el.preload = 'auto';
  el.volume = 0;
  el.src = TRACKS[index].file;
  el.addEventListener('ended', () => {
    index = nextIndex(index);
    if (el) el.src = TRACKS[index].file;
    play();
  });
  return el;
}

function ramp(to: number, then?: () => void): void {
  clearInterval(fade);
  const a = el;
  if (!a) return;
  const from = a.volume;
  const steps = Math.max(1, Math.round(FADE_MS / 50));
  let n = 0;
  fade = setInterval(() => {
    n += 1;
    a.volume = Math.min(1, Math.max(0, from + ((to - from) * n) / steps));
    if (n >= steps) {
      clearInterval(fade);
      then?.();
    }
  }, 50);
}

function play(): void {
  if (!enabled || !touched || background) return publish();
  const a = audio();
  a.play().then(
    () => {
      ramp(VOLUME);
      publish();
    },
    () => publish(), // WebView мог отказать: попробуем при следующем касании
  );
}

function stop(): void {
  if (!el) return publish();
  const a = el;
  ramp(0, () => {
    a.pause();
    publish();
  });
}

/** Включает или выключает музыку (переключатель в настройках). */
export function setMusicEnabled(on: boolean): void {
  enabled = on;
  if (on) play();
  else stop();
  publish();
}

/** Приложение свёрнуто / вернулось: музыка не должна играть в фоне. */
export function setMusicBackgrounded(isBackground: boolean): void {
  background = isBackground;
  if (isBackground) {
    clearInterval(fade);
    el?.pause();
    publish();
  } else play();
}

/** Запускается один раз при старте: ждёт первого касания и начинает играть. */
export function initMusic(on: boolean): void {
  enabled = on;
  index = Math.floor(Math.random() * TRACKS.length);
  const unlock = () => {
    touched = true;
    play();
  };
  window.addEventListener('pointerdown', unlock, { once: true, capture: true });
  window.addEventListener('keydown', unlock, { once: true, capture: true });
  publish();
}

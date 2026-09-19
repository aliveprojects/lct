// Короткие звуки-сигналы синтезируются на лету (WebAudio), без аудиофайлов. Фоновая музыка — отдельно, см. music.ts.
// Звук можно отключить в настройках; важная информация никогда не передаётся только звуком.

type Kind = 'tap' | 'coin' | 'good' | 'oops' | 'level';

let audio: AudioContext | null = null;
let enabled = true;

export const setSoundEnabled = (on: boolean): void => {
  enabled = on;
};

function ctx(): AudioContext | null {
  if (audio) return audio;
  const Ctor = (window as unknown as { AudioContext?: typeof AudioContext; webkitAudioContext?: typeof AudioContext }).AudioContext ??
    (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
  if (!Ctor) return null;
  try {
    audio = new Ctor();
  } catch {
    audio = null;
  }
  return audio;
}

const SEQ: Record<Kind, { f: number; t: number; d: number; type?: OscillatorType }[]> = {
  tap: [{ f: 520, t: 0, d: 0.05, type: 'triangle' }],
  coin: [
    { f: 880, t: 0, d: 0.07, type: 'square' },
    { f: 1320, t: 0.07, d: 0.16, type: 'square' },
  ],
  good: [
    { f: 523, t: 0, d: 0.1, type: 'triangle' },
    { f: 659, t: 0.1, d: 0.1, type: 'triangle' },
    { f: 784, t: 0.2, d: 0.18, type: 'triangle' },
  ],
  oops: [
    { f: 330, t: 0, d: 0.12, type: 'sine' },
    { f: 262, t: 0.12, d: 0.18, type: 'sine' },
  ],
  level: [
    { f: 523, t: 0, d: 0.1, type: 'triangle' },
    { f: 659, t: 0.1, d: 0.1, type: 'triangle' },
    { f: 784, t: 0.2, d: 0.1, type: 'triangle' },
    { f: 1047, t: 0.3, d: 0.3, type: 'triangle' },
  ],
};

export function playSound(kind: Kind): void {
  if (!enabled) return;
  const ac = ctx();
  if (!ac) return;
  try {
    if (ac.state === 'suspended') void ac.resume();
    const now = ac.currentTime;
    for (const n of SEQ[kind]) {
      const osc = ac.createOscillator();
      const gain = ac.createGain();
      osc.type = n.type ?? 'sine';
      osc.frequency.value = n.f;
      gain.gain.setValueAtTime(0.0001, now + n.t);
      gain.gain.exponentialRampToValueAtTime(0.07, now + n.t + 0.01);
      gain.gain.exponentialRampToValueAtTime(0.0001, now + n.t + n.d);
      osc.connect(gain).connect(ac.destination);
      osc.start(now + n.t);
      osc.stop(now + n.t + n.d + 0.02);
    }
  } catch {
    /* звук не критичен */
  }
}

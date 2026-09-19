import { describe, expect, it } from 'vitest';
import { TRACKS, nextIndex } from '../src/platform/music';

describe('Фоновая музыка', () => {
  it('плейлист идёт по кругу', () => {
    expect(TRACKS.length).toBeGreaterThanOrEqual(2);
    let i = 0;
    const seen: number[] = [];
    for (let k = 0; k < TRACKS.length * 2; k++) {
      seen.push(i);
      i = nextIndex(i);
    }
    expect(seen).toEqual([...Array(TRACKS.length).keys(), ...Array(TRACKS.length).keys()]);
  });

  it('у каждого трека есть название, автор и файл внутри приложения', () => {
    for (const t of TRACKS) {
      expect(t.title.length).toBeGreaterThan(0);
      expect(t.artist.length).toBeGreaterThan(0);
      expect(t.file.startsWith('./music/')).toBe(true); // никаких внешних адресов: приложение офлайн
    }
  });
});

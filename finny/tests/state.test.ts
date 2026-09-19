import { describe, expect, it } from 'vitest';
import { content } from '../src/content';
import { playPeriodAuto } from '../src/domain/demo';
import { initialApp, parseState, resetDemoProfile, resetProgress, serializeState, todayKey, withProfile, activeProfile, advanceDemoDay, setMode, makeCtx } from '../src/domain/state';
import { ctx, makeProfile } from './helpers';

describe('Сохранение состояния', () => {
  it('профиль, баланс, покупки, цель и прогресс переживают перезапуск', () => {
    let s = initialApp();
    s = { ...s, introSeen: true };
    let p = makeProfile();
    p = playPeriodAuto(p, ctx());
    p = playPeriodAuto(p, ctx());
    s = withProfile(s, p);
    const { state, recovered } = parseState(serializeState(s));
    expect(recovered).toBe(false);
    expect(state).toEqual(s);
    const q = activeProfile(state)!;
    expect(q.wallet).toBe(p.wallet);
    expect(q.ledger).toEqual(p.ledger);
    expect(q.goals).toEqual(p.goals);
    expect(q.tasks).toEqual(p.tasks);
    expect(q.pet.growth).toBe(p.pet.growth);
  });

  it('фоновая музыка включена по умолчанию и выключается настройкой; в старых данных без неё — значение по умолчанию', () => {
    expect(initialApp().settings.music).toBe(true);
    const off = withProfile({ ...initialApp(), settings: { ...initialApp().settings, music: false } }, makeProfile());
    expect(parseState(serializeState(off)).state.settings.music).toBe(false);
    const raw = JSON.parse(serializeState(off));
    delete raw.settings.music;
    expect(parseState(JSON.stringify(raw)).state.settings.music).toBe(true);
  });

  it('пустое хранилище — чистое начало', () => {
    expect(parseState(null)).toEqual({ state: initialApp(), recovered: false });
    expect(parseState('')).toEqual({ state: initialApp(), recovered: false });
  });

  it('повреждённые данные не роняют приложение', () => {
    for (const raw of ['{oops', '"строка"', '[]', '{"schema":1,"main":{"wallet":"много"}}', '{"schema":99}', 'null']) {
      const r = parseState(raw);
      expect(r.recovered).toBe(true);
      expect(r.state).toEqual(initialApp());
    }
  });

  it('данные без новых полей достраиваются значениями по умолчанию', () => {
    const s = withProfile(initialApp(), makeProfile());
    const raw = JSON.parse(serializeState(s));
    delete raw.main.wishlist;
    delete raw.main.trophies;
    delete raw.settings.highContrast;
    const { state, recovered } = parseState(JSON.stringify(raw));
    expect(recovered).toBe(false);
    expect(state.main!.wishlist).toEqual([]);
    expect(state.main!.trophies).toEqual([]);
    expect(state.settings.highContrast).toBe(false);
  });

  it('игровой и тестовый профили хранятся раздельно', () => {
    let s = withProfile(initialApp(), makeProfile(false));
    s = setMode(s, 'demo');
    expect(activeProfile(s)).toBeNull();
    s = withProfile(s, makeProfile(true));
    expect(s.main!.isDemo).toBe(false);
    expect(s.demo!.isDemo).toBe(true);
    s = resetDemoProfile(advanceDemoDay(s));
    expect(s.demo).toBeNull();
    expect(s.demoDayOffset).toBe(0);
    expect(s.main).not.toBeNull(); // настоящий профиль не тронут
  });

  it('демо-календарь можно «прокрутить» без ожидания', () => {
    let s = setMode(initialApp(), 'demo');
    const today = makeCtx(s, content, new Date(2026, 8, 19)).today;
    expect(today).toBe('2026-09-19');
    s = advanceDemoDay(s);
    expect(makeCtx(s, content, new Date(2026, 8, 19)).today).toBe('2026-09-20');
    expect(makeCtx({ ...s, mode: 'normal' }, content, new Date(2026, 8, 19)).today).toBe('2026-09-19');
    expect(todayKey(1, new Date(2026, 11, 31))).toBe('2027-01-01');
  });

  it('сброс прогресса оставляет питомца и имя, но начинает игру заново', () => {
    const played = playPeriodAuto(playPeriodAuto(makeProfile(), ctx()), ctx());
    expect(played.history.length).toBe(2);
    const fresh = resetProgress(played, ctx());
    expect(fresh.playerName).toBe(played.playerName);
    expect(fresh.pet.name).toBe(played.pet.name);
    expect(fresh.pet.appearance.species).toBe(played.pet.appearance.species);
    expect(fresh).toMatchObject({ wallet: 100, history: [], tasks: {}, trophies: [] });
    expect(fresh.pet.growth).toBe(0);
  });
});

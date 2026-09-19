import type { Content } from '../content/types';
import { newProfile } from './profile';
import type { AppState, Ctx, Mode, Profile, Settings } from './types';
import { clone } from './util';

export const SCHEMA = 1;

export const DEFAULT_SETTINGS: Settings = {
  music: true,
  sound: true,
  animations: true,
  textScale: 'normal',
  highContrast: false,
};

export function initialApp(): AppState {
  return { schema: SCHEMA, settings: { ...DEFAULT_SETTINGS }, introSeen: false, mode: 'normal', main: null, demo: null, demoDayOffset: 0 };
}

export const activeProfile = (s: AppState): Profile | null => (s.mode === 'demo' ? s.demo : s.main);

export function withProfile(s: AppState, p: Profile | null): AppState {
  return s.mode === 'demo' ? { ...s, demo: p } : { ...s, main: p };
}

export function dateKey(d: Date): string {
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${m}-${day}`;
}

/** Сегодняшняя дата. В демо-режиме календарь можно «прокрутить» вперёд без ожидания. */
export function todayKey(offsetDays = 0, now: Date = new Date()): string {
  const d = new Date(now.getFullYear(), now.getMonth(), now.getDate() + offsetDays);
  return dateKey(d);
}

export function makeCtx(s: AppState, content: Content, now: Date = new Date()): Ctx {
  return { content, today: todayKey(s.mode === 'demo' ? s.demoDayOffset : 0, now) };
}

// ---------- Режимы и сброс ----------

export const setMode = (s: AppState, mode: Mode): AppState => ({ ...s, mode });

export const resetDemoProfile = (s: AppState): AppState => ({ ...s, demo: null, demoDayOffset: 0 });

export const advanceDemoDay = (s: AppState): AppState => ({ ...s, demoDayOffset: s.demoDayOffset + 1 });

/** «Сбросить прогресс»: игра начинается заново, но имя и облик питомца остаются. */
export function resetProgress(p: Profile, ctx: Ctx): Profile {
  const keepAccessory = ctx.content.pet.accessories.find((a) => a.id === p.pet.appearance.accessory && a.atStart) ? p.pet.appearance.accessory : 'none';
  return newProfile(
    { playerName: p.playerName, petName: p.pet.name, appearance: { ...p.pet.appearance, accessory: keepAccessory }, isDemo: p.isDemo },
    ctx,
  );
}

// ---------- Сохранение и разбор ----------

const isObj = (x: unknown): x is Record<string, unknown> => typeof x === 'object' && x !== null && !Array.isArray(x);

function looksLikeProfile(x: unknown): x is Profile {
  return (
    isObj(x) &&
    typeof x.wallet === 'number' &&
    isObj(x.pet) &&
    isObj((x.pet as Record<string, unknown>).stats) &&
    isObj(x.period) &&
    Array.isArray(x.goals) &&
    Array.isArray(x.ledger) &&
    isObj(x.tasks)
  );
}

/** Добавляет поля, которых нет в данных старой версии, чтобы новая версия не падала. */
function normalizeProfile(p: Profile): Profile {
  const q = clone(p);
  q.history ??= [];
  q.wishlist ??= [];
  q.trophies ??= [];
  q.resolvedEvents ??= [];
  q.pendingEventId ??= null;
  q.dailyGiftDate ??= null;
  q.activeGoalId ??= null;
  q.nextTxId ??= q.ledger.length + 1;
  q.pet.ownedAccessories ??= [];
  q.pet.reason ??= null;
  return q;
}

export function serializeState(s: AppState): string {
  return JSON.stringify(s);
}

export interface ParseResult {
  state: AppState;
  /** true — данные были повреждены, и мы начали заново (старый текст стоит сохранить в резерв). */
  recovered: boolean;
}

/** Безопасный разбор: повреждённые данные не должны ронять приложение. */
export function parseState(raw: string | null): ParseResult {
  if (raw === null || raw === '') return { state: initialApp(), recovered: false };
  try {
    const data: unknown = JSON.parse(raw);
    if (!isObj(data) || typeof data.schema !== 'number' || data.schema > SCHEMA) throw new Error('unknown schema');
    const main = data.main === null || data.main === undefined ? null : looksLikeProfile(data.main) ? normalizeProfile(data.main) : undefined;
    const demo = data.demo === null || data.demo === undefined ? null : looksLikeProfile(data.demo) ? normalizeProfile(data.demo) : undefined;
    if (main === undefined || demo === undefined) throw new Error('bad profile');
    const settings: Settings = { ...DEFAULT_SETTINGS, ...(isObj(data.settings) ? (data.settings as Partial<Settings>) : {}) };
    return {
      recovered: false,
      state: {
        schema: SCHEMA,
        settings,
        introSeen: data.introSeen === true,
        mode: data.mode === 'demo' ? 'demo' : 'normal',
        main,
        demo,
        demoDayOffset: typeof data.demoDayOffset === 'number' ? data.demoDayOffset : 0,
      },
    };
  } catch {
    return { state: initialApp(), recovered: true };
  }
}

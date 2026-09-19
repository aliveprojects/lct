import type { Change, ErrorCode, GameError, Outcome, Profile, Report } from './types';

/** Профиль — простые JSON-данные, поэтому копируем через JSON (работает в любой WebView). */
export const clone = <T>(x: T): T => JSON.parse(JSON.stringify(x)) as T;

export function fail(code: ErrorCode, message: string, extra: Partial<GameError> = {}): { ok: false; error: GameError } {
  return { ok: false, error: { code, message, ...extra } };
}

export function done<T extends object = object>(profile: Profile, report: Report, extra?: T): Outcome<T> {
  return { ok: true, profile, report, ...(extra ?? ({} as T)) };
}

export const walletChange = (label: string, before: number, after: number, why: string): Change => ({
  kind: 'wallet',
  label,
  before,
  after,
  delta: after - before,
  why,
});

export const isInt = (n: unknown): n is number => typeof n === 'number' && Number.isInteger(n);

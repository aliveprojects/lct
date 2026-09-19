import { useEffect, useState } from 'preact/hooks';
import { content } from '../content';
import type { AppState, Ctx, Outcome, Profile } from '../domain/types';
import { activeProfile, initialApp, makeCtx, withProfile } from '../domain/state';

type Listener = () => void;

let state: AppState = initialApp();
let onChange: ((s: AppState) => void) | null = null;
const listeners = new Set<Listener>();

export const getState = (): AppState => state;

export function initStore(initial: AppState, persist: (s: AppState) => void): void {
  state = initial;
  onChange = persist;
  listeners.forEach((l) => l());
}

export function setState(next: AppState): void {
  state = next;
  listeners.forEach((l) => l());
  onChange?.(state);
}

export const mutate = (fn: (s: AppState) => AppState): void => setState(fn(state));

export function subscribe(l: Listener): () => void {
  listeners.add(l);
  return () => listeners.delete(l);
}

/** Подписка компонента на состояние. */
export function useAppState(): AppState {
  const [, force] = useState(0);
  useEffect(() => subscribe(() => force((n) => n + 1)), []);
  return state;
}

export const getCtx = (): Ctx => makeCtx(state, content);

export const getProfile = (): Profile | null => activeProfile(state);

/**
 * Единственный путь изменения игрового профиля: движок возвращает новый профиль (или ошибку),
 * стор его сохраняет. Экран потом показывает пользователю объяснение из outcome.
 */
export function act<T extends object>(fn: (p: Profile, ctx: Ctx) => Outcome<T>): Outcome<T> {
  const p = activeProfile(state);
  if (!p) return { ok: false, error: { code: 'NO_PROFILE', message: 'Профиль ещё не создан.' } };
  const res = fn(p, getCtx());
  if (res.ok) setState(withProfile(state, res.profile));
  return res;
}
